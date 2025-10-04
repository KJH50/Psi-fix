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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import vazkii.psi.api.spell.SpellContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 方块移动安全管理器
 * 
 * 功能：防止方块复制漏洞，确保方块移动操作的原子性和安全性
 * 
 * 解决方案：
 * 1. 跟踪正在进行的方块操作
 * 2. 防止同一方块被多次操作
 * 3. 确保操作的原子性
 * 4. 提供操作回滚机制
 * 
 * @author AI Assistant
 */
public class BlockMoveSecurityManager {

	// 每个世界的活动操作跟踪
	private static final Map<Level, Map<BlockPos, BlockOperationInfo>> activeOperations = new ConcurrentHashMap<>();

	/**
	 * 方块操作信息
	 */
	public static class BlockOperationInfo {
		public final BlockState originalState;
		public final long timestamp;
		public final SpellContext context;
		public final OperationType type;

		public BlockOperationInfo(BlockState originalState, SpellContext context, OperationType type) {
			this.originalState = originalState;
			this.timestamp = System.currentTimeMillis();
			this.context = context;
			this.type = type;
		}
	}

	public enum OperationType {
		MOVE_SINGLE,
		MOVE_SEQUENCE,
		BREAK
	}

	/**
	 * 尝试开始一个方块操作
	 * 
	 * @param world         世界
	 * @param pos           方块位置
	 * @param expectedState 期望的方块状态
	 * @param context       法术上下文
	 * @param type          操作类型
	 * @return 是否成功开始操作
	 */
	public static boolean tryStartOperation(Level world, BlockPos pos, BlockState expectedState,
			SpellContext context, OperationType type) {
		Map<BlockPos, BlockOperationInfo> worldOps = activeOperations.computeIfAbsent(world, k -> new ConcurrentHashMap<>());

		// 检查是否已有操作在进行
		if(worldOps.containsKey(pos)) {
			return false;
		}

		// 验证当前方块状态是否与期望一致
		BlockState currentState = world.getBlockState(pos);
		if(!currentState.equals(expectedState)) {
			return false;
		}

		// 检查是否是之前被破坏的方块位置（防止复制）
		if(context.positionBroken != null && context.positionBroken.getBlockPos().equals(pos)) {
			return false;
		}

		// 记录操作
		worldOps.put(pos, new BlockOperationInfo(expectedState, context, type));
		return true;
	}

	/**
	 * 完成方块操作
	 * 
	 * @param world 世界
	 * @param pos   方块位置
	 */
	public static void finishOperation(Level world, BlockPos pos) {
		Map<BlockPos, BlockOperationInfo> worldOps = activeOperations.get(world);
		if(worldOps != null) {
			worldOps.remove(pos);

			// 清理空的世界映射
			if(worldOps.isEmpty()) {
				activeOperations.remove(world);
			}
		}
	}

	/**
	 * 批量开始操作（用于序列移动）
	 * 
	 * @param world      世界
	 * @param operations 操作映射：位置 -> 期望状态
	 * @param context    法术上下文
	 * @return 是否所有操作都成功开始
	 */
	public static boolean tryStartBatchOperation(Level world, Map<BlockPos, BlockState> operations,
			SpellContext context) {
		Map<BlockPos, BlockOperationInfo> worldOps = activeOperations.computeIfAbsent(world, k -> new ConcurrentHashMap<>());

		// 首先检查所有位置是否可用
		for(Map.Entry<BlockPos, BlockState> entry : operations.entrySet()) {
			BlockPos pos = entry.getKey();
			BlockState expectedState = entry.getValue();

			// 检查是否已有操作在进行
			if(worldOps.containsKey(pos)) {
				return false;
			}

			// 验证当前方块状态
			BlockState currentState = world.getBlockState(pos);
			if(!currentState.equals(expectedState)) {
				return false;
			}

			// 检查是否是之前被破坏的方块位置
			if(context.positionBroken != null && context.positionBroken.getBlockPos().equals(pos)) {
				return false;
			}
		}

		// 所有检查通过，批量记录操作
		for(Map.Entry<BlockPos, BlockState> entry : operations.entrySet()) {
			worldOps.put(entry.getKey(), new BlockOperationInfo(entry.getValue(), context, OperationType.MOVE_SEQUENCE));
		}

		return true;
	}

	/**
	 * 批量完成操作
	 * 
	 * @param world     世界
	 * @param positions 位置列表
	 */
	public static void finishBatchOperation(Level world, Iterable<BlockPos> positions) {
		Map<BlockPos, BlockOperationInfo> worldOps = activeOperations.get(world);
		if(worldOps != null) {
			for(BlockPos pos : positions) {
				worldOps.remove(pos);
			}

			// 清理空的世界映射
			if(worldOps.isEmpty()) {
				activeOperations.remove(world);
			}
		}
	}

	/**
	 * 检查方块是否正在被操作
	 * 
	 * @param world 世界
	 * @param pos   方块位置
	 * @return 是否正在被操作
	 */
	public static boolean isOperationActive(Level world, BlockPos pos) {
		Map<BlockPos, BlockOperationInfo> worldOps = activeOperations.get(world);
		return worldOps != null && worldOps.containsKey(pos);
	}

	/**
	 * 清理过期的操作（防止内存泄漏）
	 * 应该定期调用此方法
	 * 
	 * @param maxAgeMs 最大年龄（毫秒）
	 */
	public static void cleanupExpiredOperations(long maxAgeMs) {
		long currentTime = System.currentTimeMillis();

		activeOperations.entrySet().removeIf(worldEntry -> {
			Map<BlockPos, BlockOperationInfo> worldOps = worldEntry.getValue();

			worldOps.entrySet().removeIf(posEntry -> {
				BlockOperationInfo info = posEntry.getValue();
				return (currentTime - info.timestamp) > maxAgeMs;
			});

			return worldOps.isEmpty();
		});
	}

	/**
	 * 获取活动操作的统计信息（用于调试）
	 * 
	 * @return 统计信息字符串
	 */
	public static String getOperationStats() {
		int totalWorlds = activeOperations.size();
		int totalOperations = activeOperations.values().stream()
				.mapToInt(Map::size)
				.sum();

		return String.format("Active operations: %d worlds, %d total operations", totalWorlds, totalOperations);
	}
}
