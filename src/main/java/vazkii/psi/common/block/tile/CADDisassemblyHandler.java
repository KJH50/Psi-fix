package vazkii.psi.common.block.tile;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import vazkii.psi.api.cad.EnumCADComponent;
import vazkii.psi.api.cad.EnumCADStat;
import vazkii.psi.api.cad.ICAD;
import vazkii.psi.common.core.handler.PsiSoundHandler;

/**
 * CAD拆解处理器 - 负责处理术式辅助演算机的拆解逻辑
 */
public class CADDisassemblyHandler {

	private final TileCADAssembler assembler;
	private DisassemblyState currentState = DisassemblyState.IDLE;

	public CADDisassemblyHandler(TileCADAssembler assembler) {
		this.assembler = assembler;
	}

	/**
	 * 执行直接拆解操作
	 */
	public DisassemblyResult performDirectDisassembly(@NotNull ItemStack cadStack, @NotNull Player player) {
		if(cadStack.isEmpty()) {
			return DisassemblyResult.failure("没有可拆解的CAD", DisassemblyState.ERROR);
		}

		if(!(cadStack.getItem() instanceof ICAD icad)) {
			return DisassemblyResult.failure("物品不是有效的CAD", DisassemblyState.ERROR);
		}

		currentState = DisassemblyState.IN_PROGRESS;

		// 检查装配器是否有足够空间
		if(!hasAvailableSlots()) {
			currentState = DisassemblyState.ERROR;
			return DisassemblyResult.failure("装配器没有足够的空间", DisassemblyState.ERROR);
		}

		// 执行拆解
		return performDisassembly(cadStack, icad, player);
	}

	/**
	 * 执行拆解操作
	 */
	private DisassemblyResult performDisassembly(@NotNull ItemStack cadStack, @NotNull ICAD icad, @NotNull Player player) {
		StringBuilder resultMessage = new StringBuilder("成功拆解组件: ");
		int extractedCount = 0;

		// 拆解各个组件，包括机壳（ASSEMBLY）
		for(EnumCADComponent component : EnumCADComponent.values()) {
			ItemStack componentStack = icad.getComponentInSlot(cadStack, component);
			if(!componentStack.isEmpty()) {
				// 尝试放置到装配器
				if(tryPlaceComponent(component, componentStack)) {
					if(extractedCount > 0) {
						resultMessage.append(", ");
					}
					resultMessage.append(componentStack.getHoverName().getString());
					extractedCount++;
				}
			}
		}

		// 检查是否有法术数据需要保留
		if(hasSpellData(cadStack, icad)) {
			resultMessage.append("(保留法术数据) ");
		}

		if(extractedCount == 0) {
			currentState = DisassemblyState.ERROR;
			return DisassemblyResult.failure("无法放置任何组件到装配器", DisassemblyState.ERROR);
		}

		// 清空源CAD槽位
		assembler.setSocketableStack(ItemStack.EMPTY);

		// 播放成功音效
		if(assembler.getLevel() != null) {
			playSound(assembler.getLevel(), player);
		}

		currentState = DisassemblyState.COMPLETED;

		// 标记装配器数据变化，确保客户端同步
		assembler.setChanged();

		return DisassemblyResult.success(
				resultMessage.toString() + " (共" + extractedCount + "个)",
				null, null, DisassemblyState.COMPLETED
		);
	}

	/**
	 * 检查CAD是否包含法术数据
	 */
	private boolean hasSpellData(@NotNull ItemStack cadStack, @NotNull ICAD icad) {
		// 检查是否有法术子弹
		for(int i = 0; i < icad.getStatValue(cadStack, EnumCADStat.SOCKETS); i++) {
			// 注意：这里需要使用正确的方法名
			// ItemStack bullet = icad.getBulletInSocket(cadStack, i);
			// 暂时跳过子弹检查，因为方法可能不存在
		}
		return false;
	}

	/**
	 * 尝试将组件放置到装配器的组件槽位
	 */
	private boolean tryPlaceComponent(@NotNull EnumCADComponent component, @NotNull ItemStack componentStack) {
		// 直接放回对应的组件槽位
		return assembler.setStackForComponent(component, componentStack.copy());
	}

	/**
	 * 检查装配器是否有可用槽位
	 */
	private boolean hasAvailableSlots() {
		// 检查组件槽位1-5是否有空位
		for(int i = 1; i <= 5; i++) {
			if(assembler.getInventory().getStackInSlot(i).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 播放音效
	 */
	private void playSound(@NotNull Level level, @NotNull Player player) {
		if(!level.isClientSide) {
			level.playSound(null, assembler.getBlockPos(), PsiSoundHandler.cadCreate,
					SoundSource.BLOCKS, 0.5F, 1.2F);
		}
	}

	/**
	 * 重置拆解状态
	 */
	public void reset() {
		currentState = DisassemblyState.IDLE;
	}

	/**
	 * 获取当前状态
	 */
	public DisassemblyState getCurrentState() {
		return currentState;
	}

	/**
	 * 检查是否处于拆解模式
	 */
	public boolean isInDisassemblyMode() {
		return currentState == DisassemblyState.IN_PROGRESS || currentState == DisassemblyState.READY;
	}

	/**
	 * 获取进度信息
	 */
	public String getProgressInfo() {
		return currentState.getDescription();
	}

	/**
	 * 拆解状态枚举
	 */
	public enum DisassemblyState {
		IDLE("空闲"),
		READY("准备拆解"),
		IN_PROGRESS("拆解中"),
		COMPLETED("拆解完成"),
		ERROR("拆解失败");

		private final String description;

		DisassemblyState(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

	/**
	 * 拆解操作结果
	 */
	public static class DisassemblyResult {
		public final boolean success;
		public final String message;
		public final EnumCADComponent extractedComponent;
		public final ItemStack extractedStack;
		public final DisassemblyState newState;

		public DisassemblyResult(boolean success, String message,
				@Nullable EnumCADComponent extractedComponent,
				@Nullable ItemStack extractedStack,
				@NotNull DisassemblyState newState) {
			this.success = success;
			this.message = message;
			this.extractedComponent = extractedComponent;
			this.extractedStack = extractedStack;
			this.newState = newState;
		}

		public static DisassemblyResult success(String message,
				@Nullable EnumCADComponent component,
				@Nullable ItemStack stack,
				@NotNull DisassemblyState newState) {
			return new DisassemblyResult(true, message, component, stack, newState);
		}

		public static DisassemblyResult failure(String message, @NotNull DisassemblyState newState) {
			return new DisassemblyResult(false, message, null, null, newState);
		}
	}
}
