/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.common.spell.trick.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import vazkii.psi.api.internal.MathHelper;
import vazkii.psi.api.internal.Vector3;
import vazkii.psi.api.spell.*;
import vazkii.psi.api.spell.param.ParamNumber;
import vazkii.psi.api.spell.param.ParamVector;
import vazkii.psi.api.spell.piece.PieceTrick;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * 序列方块移动法术组件
 * 
 * 功能：将一系列方块按照指定方向移动
 * 特点：支持批量移动、防复制机制、边界安全检查
 * 
 * @author Vazkii (原始作者)
 * @author AI Assistant (代码优化)
 */
public class PieceTrickMoveBlockSequence extends PieceTrick {

	SpellParam<Vector3> position;
	SpellParam<Vector3> target;
	SpellParam<Vector3> direction;
	SpellParam<Number> maxBlocks;

	public PieceTrickMoveBlockSequence(Spell spell) {
		super(spell);
		setStatLabel(EnumSpellStat.POTENCY, new StatLabel(SpellParam.GENERIC_NAME_MAX, true).mul(10));
		setStatLabel(EnumSpellStat.COST, new StatLabel(SpellParam.GENERIC_NAME_MAX, true).sub(1).parenthesize().mul(10.5).add(18).floor());
	}

	@Override
	public void initParams() {
		addParam(position = new ParamVector(SpellParam.GENERIC_NAME_POSITION, SpellParam.BLUE, false, false));
		addParam(target = new ParamVector(SpellParam.GENERIC_NAME_TARGET, SpellParam.YELLOW, false, false));
		addParam(maxBlocks = new ParamNumber(SpellParam.GENERIC_NAME_MAX, SpellParam.RED, false, true));
		addParam(direction = new ParamVector(SpellParam.GENERIC_NAME_DIRECTION, SpellParam.GREEN, false, false));
	}

	@Override
	public void addToMetadata(SpellMetadata meta) throws SpellCompilationException, ArithmeticException {
		super.addToMetadata(meta);
		double maxBlocksVal = SpellHelpers.ensurePositiveAndNonzero(this, maxBlocks);

		meta.addStat(EnumSpellStat.POTENCY, (int) (maxBlocksVal * 10));
		meta.addStat(EnumSpellStat.COST, (int) ((18 + (maxBlocksVal - 1) * 10.5)));
	}

	@Override
	public Object execute(SpellContext context) throws SpellRuntimeException {
		// 参数验证和边界检查
		Vector3 directionVal = SpellHelpers.getVector3(this, context, direction, false, true);
		Vector3 positionVal = SpellHelpers.getVector3(this, context, position, true, false);
		Vector3 targetVal = SpellHelpers.getVector3(this, context, target, false, false);
		int maxBlocksVal = this.getParamValue(context, maxBlocks).intValue();

		// 边界条件检查
		if(maxBlocksVal <= 0) {
			throw new SpellRuntimeException(SpellRuntimeException.NON_POSITIVE_VALUE);
		}
		if(maxBlocksVal > 64) {
			throw new SpellRuntimeException("psi.spellerror.too_many_blocks");
		}

		Level world = context.focalPoint.level();
		Player player = context.caster;
		ItemStack tool = context.getHarvestTool();

		Map<BlockPos, BlockState> toSet = new HashMap<>(maxBlocksVal);
		Map<BlockPos, BlockState> toRemove = new HashMap<>(maxBlocksVal);

		Vector3 directNorm = directionVal.copy().normalize();
		Vector3 targetNorm = targetVal.copy().normalize();

		LinkedHashSet<BlockPos> positions = MathHelper.getBlocksAlongRay(positionVal.toVec3D(), positionVal.copy().add(targetNorm.copy().multiply(maxBlocksVal)).toVec3D(), maxBlocksVal);
		LinkedHashSet<BlockPos> moveableBlocks = new LinkedHashSet<>();
		LinkedHashSet<BlockPos> immovableBlocks = new LinkedHashSet<>();

		// 使用新的安全管理器防止方块复制
		// 预先标记已破坏的位置为不可移动
		if(context.positionBroken != null) {
			immovableBlocks.add(context.positionBroken.getBlockPos());
		}

		for(BlockPos blockPos : positions) {
			BlockState state = world.getBlockState(blockPos);

			// 跳过空气方块
			if(world.isEmptyBlock(blockPos)) {
				continue;
			}

			// 检查方块是否可移动
			boolean isMovable = checkBlockMovability(state, world, blockPos, player, tool, context);
			if(!isMovable) {
				immovableBlocks.add(blockPos);
				continue;
			}

			BlockPos pushToPos = blockPos.offset((int) directNorm.x, (int) directNorm.y, (int) directNorm.z);
			boolean isOffWorld = pushToPos.getY() < 0 || pushToPos.getY() > 256;
			if(isOffWorld) {
				immovableBlocks.add(blockPos);
				continue;
			}

			BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world, blockPos, state, context.caster);

			if(NeoForge.EVENT_BUS.post(event).isCanceled()) {
				immovableBlocks.add(blockPos);
				continue;
			}
			moveableBlocks.add(blockPos);
		}

		outer: for(BlockPos blockPos : moveableBlocks) {
			BlockState state = world.getBlockState(blockPos);
			BlockPos pushToPos = blockPos.offset((int) directNorm.x, (int) directNorm.y, (int) directNorm.z);
			BlockState pushToState = world.getBlockState(pushToPos);
			if(immovableBlocks.contains(pushToPos) || immovableBlocks.contains(blockPos)) {
				continue;
			}
			if(moveableBlocks.contains(pushToPos)) {
				BlockPos nextPos = pushToPos;
				while(moveableBlocks.contains(nextPos)) {
					BlockPos nextPosPushPos = nextPos.offset((int) directNorm.x, (int) directNorm.y, (int) directNorm.z);
					BlockState nextPosPushPosState = world.getBlockState(nextPosPushPos);

					if(moveableBlocks.contains(nextPosPushPos)) {
						nextPos = nextPosPushPos;
						continue;
					}

					if(immovableBlocks.contains(nextPosPushPos) || !(world.isEmptyBlock(nextPosPushPos) || nextPosPushPosState.canBeReplaced())) {
						continue outer;
					}
					break;
				}
			} else if(!(world.isEmptyBlock(pushToPos) || pushToState.canBeReplaced())) {
				continue;
			}
			toRemove.put(blockPos, state);
			toSet.put(pushToPos, state);
		}

		// 使用安全管理器进行批量操作防护
		if(!BlockMoveSecurityManager.tryStartBatchOperation(world, toRemove, context)) {
			return null;
		}

		try {
			// 原子性批量方块移动操作
			// 再次验证所有源方块状态（双重检查）
			for(Map.Entry<BlockPos, BlockState> entry : toRemove.entrySet()) {
				BlockPos pos = entry.getKey();
				BlockState expectedState = entry.getValue();
				BlockState currentState = world.getBlockState(pos);

				if(!currentState.equals(expectedState)) {
					// 状态不一致，拒绝整个序列操作
					return null;
				}
			}

			// 先设置所有目标位置
			for(Map.Entry<BlockPos, BlockState> pairToSet : toSet.entrySet()) {
				world.setBlockAndUpdate(pairToSet.getKey(), pairToSet.getValue());
			}

			// 再移除所有源位置
			for(Map.Entry<BlockPos, BlockState> pairtoRemove : toRemove.entrySet()) {
				world.removeBlock(pairtoRemove.getKey(), false);
				world.levelEvent(2001, pairtoRemove.getKey(), Block.getId(pairtoRemove.getValue()));
			}
		} finally {
			// 确保批量操作完成后清理安全管理器状态
			BlockMoveSecurityManager.finishBatchOperation(world, toRemove.keySet());
		}

		return null;
	}

	/**
	 * 检查方块是否可以被移动
	 * 基于PieceTrickMoveBlock.java中的逻辑
	 */
	private boolean checkBlockMovability(BlockState state, Level world, BlockPos pos, Player player, ItemStack tool, SpellContext context) {
		// 检查方块实体（有方块实体的方块通常不可移动）
		if(world.getBlockEntity(pos) != null) {
			return false;
		}

		// 检查活塞推动反应
		if(state.getPistonPushReaction() != net.minecraft.world.level.material.PushReaction.NORMAL) {
			return false;
		}

		// 检查方块硬度（不可破坏的方块）
		if(state.getDestroySpeed(world, pos) == -1) {
			return false;
		}

		// 检查是否可以收获（使用工具检查）
		if(!PieceTrickBreakBlock.canHarvestBlock(state, player, world, pos, tool)) {
			return false;
		}

		// 检查玩家是否可以与方块交互
		if(!world.mayInteract(player, pos)) {
			return false;
		}

		// 检查方块事件是否被取消
		net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event = PieceTrickBreakBlock.createBreakEvent(state, player, world, pos, tool);
		if(net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled()) {
			return false;
		}

		return true;
	}
}
