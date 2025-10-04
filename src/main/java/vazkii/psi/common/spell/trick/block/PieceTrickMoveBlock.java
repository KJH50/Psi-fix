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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import vazkii.psi.api.internal.Vector3;
import vazkii.psi.api.spell.*;
import vazkii.psi.api.spell.param.ParamVector;
import vazkii.psi.api.spell.piece.PieceTrick;

public class PieceTrickMoveBlock extends PieceTrick {

	SpellParam<Vector3> position;
	SpellParam<Vector3> target;

	public PieceTrickMoveBlock(Spell spell) {
		super(spell);
		setStatLabel(EnumSpellStat.POTENCY, new StatLabel(10));
		setStatLabel(EnumSpellStat.COST, new StatLabel(15));
	}

	@Override
	public void initParams() {
		addParam(position = new ParamVector(SpellParam.GENERIC_NAME_POSITION, SpellParam.BLUE, false, false));
		addParam(target = new ParamVector(SpellParam.GENERIC_NAME_TARGET, SpellParam.GREEN, false, false));
	}

	@Override
	public void addToMetadata(SpellMetadata meta) throws SpellCompilationException {
		super.addToMetadata(meta);

		meta.addStat(EnumSpellStat.POTENCY, 10);
		meta.addStat(EnumSpellStat.COST, 15);
	}

	@Override
	public Object execute(SpellContext context) throws SpellRuntimeException {
		ItemStack tool = context.getHarvestTool();
		Vector3 positionVal = this.getParamValue(context, position);
		Vector3 targetVal = this.getParamValue(context, target);

		if(positionVal == null) {
			throw new SpellRuntimeException(SpellRuntimeException.NULL_VECTOR);
		}
		if(!context.isInRadius(positionVal)) {
			throw new SpellRuntimeException(SpellRuntimeException.OUTSIDE_RADIUS);
		}

		Level world = context.focalPoint.getCommandSenderWorld();
		BlockPos pos = positionVal.toBlockPos();

		BlockState state = world.getBlockState(pos);

		// 使用新的安全管理器防止方块复制
		if(!BlockMoveSecurityManager.tryStartOperation(world, pos, state, context, BlockMoveSecurityManager.OperationType.MOVE_SINGLE)) {
			return null;
		}

		try {
			// 检查方块是否可移动
			if(world.getBlockEntity(pos) != null || state.getPistonPushReaction() != PushReaction.NORMAL ||
					state.getDestroySpeed(world, pos) == -1 ||
					!PieceTrickBreakBlock.canHarvestBlock(state, context.caster, world, pos, tool)) {
				return null;
			}

			BlockEvent.BreakEvent event = PieceTrickBreakBlock.createBreakEvent(state, context.caster, world, pos, tool);
			NeoForge.EVENT_BUS.post(event);
			if(event.isCanceled()) {
				return null;
			}

			if(!targetVal.isAxial() || targetVal.isZero()) {
				return null;
			}

			Vector3 axis = targetVal.normalize();
			int x = pos.getX() + (int) axis.x;
			int y = pos.getY() + (int) axis.y;
			int z = pos.getZ() + (int) axis.z;
			BlockPos pos1 = new BlockPos(x, y, z);
			BlockState state1 = world.getBlockState(pos1);

			if(!world.mayInteract(context.caster, pos) || !world.mayInteract(context.caster, pos1)) {
				return null;
			}

			if(state1.isAir() || state1.canBeReplaced()) {
				// 安全的原子性方块移动操作
				// 再次验证源方块状态（双重检查）
				BlockState currentState = world.getBlockState(pos);
				if(!currentState.equals(state)) {
					return null;
				}

				// 原子性操作：先设置目标位置，再移除源位置
				world.setBlock(pos1, state, 3); // 使用标志3：UPDATE_ALL | SEND_TO_CLIENTS
				world.removeBlock(pos, false);
				world.levelEvent(2001, pos, Block.getId(state));
			}
		} finally {
			// 确保操作完成后清理安全管理器状态
			BlockMoveSecurityManager.finishOperation(world, pos);
		}

		return null;
	}

}
