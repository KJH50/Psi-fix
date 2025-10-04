/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.common.spell;

import com.mojang.datafixers.util.Either;

import vazkii.psi.api.spell.*;
import vazkii.psi.api.spell.CompiledSpell.Action;
import vazkii.psi.api.spell.CompiledSpell.CatchHandler;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/* Probably not thread-safe. */
public final class SpellCompiler implements ISpellCompiler {

	// 优化: 使用更高效的集合实现和预分配容量
	private final Set<SpellPiece> redirectionPieces = new HashSet<>(64);
	private final Map<SpellPiece, Set<SpellPiece>> visitedCache = new HashMap<>(32);
	/**
	 * The current spell being compiled.
	 */
	private CompiledSpell compiled;

	@Override
	public Either<CompiledSpell, SpellCompilationException> compile(Spell in) {
		try {
			return Either.left(doCompile(in));
		} catch (SpellCompilationException e) {
			return Either.right(e);
		}
	}

	public CompiledSpell doCompile(Spell spell) throws SpellCompilationException {
		if(spell == null) {
			throw new SpellCompilationException(SpellCompilationException.NO_SPELL);
		}

		redirectionPieces.clear();
		compiled = new CompiledSpell(spell);

		for(SpellPiece piece : findPieces(EnumPieceType.ERROR_HANDLER::equals)) {
			buildHandler(piece);
		}

		List<SpellPiece> tricks = findPieces(EnumPieceType::isTrick);
		if(tricks.isEmpty()) {
			throw new SpellCompilationException(SpellCompilationException.NO_TRICKS);
		}
		for(SpellPiece trick : tricks) {
			buildPiece(trick);
		}

		if(compiled.metadata.getStat(EnumSpellStat.COST) < 0 || compiled.metadata.getStat(EnumSpellStat.POTENCY) < 0) {
			throw new SpellCompilationException(SpellCompilationException.STAT_OVERFLOW);
		}

		if(spell.name == null || spell.name.isEmpty()) {
			throw new SpellCompilationException(SpellCompilationException.NO_NAME);
		}
		return compiled;
	}

	public void buildPiece(SpellPiece piece) throws SpellCompilationException {
		// 优化: 重用缓存的访问集合，减少对象分配
		Set<SpellPiece> visited = visitedCache.computeIfAbsent(piece, k -> new HashSet<>(16));
		visited.clear();
		buildPiece(piece, visited);
	}

	public void buildPiece(SpellPiece piece, Set<SpellPiece> visited) throws SpellCompilationException {
		if(!visited.add(piece)) {
			throw new SpellCompilationException(SpellCompilationException.INFINITE_LOOP, piece.x, piece.y);
		}

		if(compiled.actionMap.containsKey(piece)) { // move to top
			Action a = compiled.actionMap.get(piece);
			compiled.actions.remove(a);
			compiled.actions.add(a);
		} else {
			Action a = compiled.new Action(piece);
			compiled.actions.add(a);
			compiled.actionMap.put(piece, a);
			piece.addToMetadata(compiled.metadata);
		}

		// error handler params must be evaluated before the handled piece
		CatchHandler catchHandler = compiled.errorHandlers.get(piece);
		if(catchHandler != null) {
			buildPiece(catchHandler.handlerPiece, new HashSet<>(visited));
		}

		EnumSet<SpellParam.Side> usedSides = EnumSet.noneOf(SpellParam.Side.class);

		// 优化: 使用ArrayList替代HashSet，提升小集合性能
		List<SpellPiece> params = new ArrayList<>(8);
		List<SpellPiece> handledErrors = new ArrayList<>(4);
		for(SpellParam<?> param : piece.paramSides.keySet()) {
			if(checkSideDisabled(param, piece, usedSides)) {
				continue;
			}

			SpellParam.Side side = piece.paramSides.get(param);

			SpellPiece pieceAt = compiled.sourceSpell.grid.getPieceAtSideWithRedirections(piece.x, piece.y, side, this::buildRedirect);

			if(pieceAt == null) {
				throw new SpellCompilationException(SpellCompilationException.NULL_PARAM, piece.x, piece.y);
			}
			if(!param.canAccept(pieceAt)) {
				throw new SpellCompilationException(SpellCompilationException.INVALID_PARAM, piece.x, piece.y);
			}

			if(piece instanceof IErrorCatcher && ((IErrorCatcher) piece).catchParam(param)) {
				handledErrors.add(pieceAt);
			} else {
				params.add(pieceAt);
			}
		}
		// 优化: 批量处理参数，减少重复的集合操作
		if(!params.isEmpty()) {
			Set<SpellPiece> baseVisited = new HashSet<>(visited.size() + handledErrors.size());
			baseVisited.addAll(visited);
			baseVisited.addAll(handledErrors);

			for(SpellPiece pieceAt : params) {
				Set<SpellPiece> visitedCopy = new HashSet<>(baseVisited);
				buildPiece(pieceAt, visitedCopy);
			}
		}
	}

	public void buildHandler(SpellPiece piece) throws SpellCompilationException {
		if(!(piece instanceof IErrorCatcher errorCatcher)) {
			return;
		}
		CompiledSpell.CatchHandler errorHandler = compiled.new CatchHandler(piece);

		EnumSet<SpellParam.Side> usedSides = EnumSet.noneOf(SpellParam.Side.class);

		for(SpellParam<?> param : piece.paramSides.keySet()) {
			if(!errorCatcher.catchParam(param) || checkSideDisabled(param, piece, usedSides)) {
				continue;
			}

			SpellParam.Side side = piece.paramSides.get(param);

			SpellPiece pieceAt = compiled.sourceSpell.grid.getPieceAtSideWithRedirections(piece.x, piece.y, side, this::buildRedirect);

			if(pieceAt == null) {
				throw new SpellCompilationException(SpellCompilationException.NULL_PARAM, piece.x, piece.y);
			}
			if(!param.canAccept(pieceAt)) {
				throw new SpellCompilationException(SpellCompilationException.INVALID_PARAM, piece.x, piece.y);
			}

			compiled.errorHandlers.put(pieceAt, errorHandler);
		}
	}

	public void buildRedirect(SpellPiece piece) throws SpellCompilationException {
		if(redirectionPieces.add(piece)) {
			piece.addToMetadata(compiled.metadata);

			EnumSet<SpellParam.Side> usedSides = EnumSet.noneOf(SpellParam.Side.class);

			for(SpellParam<?> param : piece.paramSides.keySet()) {
				checkSideDisabled(param, piece, usedSides);
			}
		}
	}

	/**
	 * @return whether this piece should get skipped over
	 */
	private boolean checkSideDisabled(SpellParam<?> param, SpellPiece parent, EnumSet<SpellParam.Side> seen) throws SpellCompilationException {
		SpellParam.Side side = parent.paramSides.get(param);
		if(side.isEnabled()) {
			if(!seen.add(side)) {
				throw new SpellCompilationException(SpellCompilationException.SAME_SIDE_PARAMS, parent.x, parent.y);
			}
			return false;
		} else {
			if(!param.canDisable) {
				throw new SpellCompilationException(SpellCompilationException.UNSET_PARAM, parent.x, parent.y);
			}
			return true;
		}
	}

	public List<SpellPiece> findPieces(Predicate<EnumPieceType> match) throws SpellCompilationException {
		// 优化: 预分配容量，避免频繁扩容
		List<SpellPiece> results = new ArrayList<>(SpellGrid.GRID_SIZE);

		// 优化: 使用增强for循环，提升可读性和性能
		for(SpellPiece[] row : compiled.sourceSpell.grid.gridData) {
			for(SpellPiece piece : row) {
				if(piece != null && match.test(piece.getPieceType())) {
					results.add(0, piece); // 保持原有的addFirst行为
				}
			}
		}

		return results;
	}

}
