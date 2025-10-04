/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.common.block.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import vazkii.psi.api.PsiAPI;
import vazkii.psi.api.cad.*;
import vazkii.psi.common.block.base.ModBlocks;
import vazkii.psi.common.block.tile.container.ContainerCADAssembler;
import vazkii.psi.common.core.handler.PsiSoundHandler;
import vazkii.psi.common.item.ItemCAD;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TileCADAssembler extends BlockEntity implements ITileCADAssembler, MenuProvider {
	private ItemStack cachedCAD = null;
	private final CADStackHandler inventory = new CADStackHandler(12);
	private final CADDisassemblyHandler disassemblyHandler = new CADDisassemblyHandler(this);

	public TileCADAssembler(BlockPos pos, BlockState state) {
		super(ModBlocks.cadAssemblerType, pos, state);
	}

	public IItemHandlerModifiable getInventory() {
		return inventory;
	}

	@Override
	public void clearCachedCAD() {
		cachedCAD = null;
	}

	@Override
	public ItemStack getCachedCAD(Player player) {
		ItemStack cad = cachedCAD;
		if(cad == null) {
			ItemStack assembly = getStackForComponent(EnumCADComponent.ASSEMBLY);
			if(!assembly.isEmpty()) {
				List<ItemStack> components = IntStream.range(1, 6).mapToObj(inventory::getStackInSlot).collect(Collectors.toList());
				cad = ItemCAD.makeCADWithAssembly(assembly, components);
			} else {
				cad = ItemStack.EMPTY;
			}

			AssembleCADEvent assembling = new AssembleCADEvent(cad, this, player);
			NeoForge.EVENT_BUS.post(assembling);

			if(assembling.isCanceled()) {
				cad = ItemStack.EMPTY;
			} else {
				cad = assembling.getCad();
			}

			cachedCAD = cad;
		}

		return cad;
	}

	@Override
	public ItemStack getStackForComponent(EnumCADComponent componentType) {
		return inventory.getStackInSlot(componentType.ordinal() + 1);
	}

	@Override
	public boolean setStackForComponent(EnumCADComponent componentType, ItemStack component) {
		int slot = componentType.ordinal() + 1;
		if(component.isEmpty()) {
			inventory.setStackInSlot(slot, component);
			return true;
		} else if(component.getItem() instanceof ICADComponent componentItem) {
			if(componentItem.getComponentType(component) == componentType) {
				inventory.setStackInSlot(slot, component);
				return true;
			}
		}

		return false;
	}

	@Override
	public ItemStack getSocketableStack() {
		return inventory.getStackInSlot(0);
	}

	@Override
	public ISocketable getSocketable() {
		return ISocketable.socketable(getSocketableStack());
	}

	@Override
	public boolean setSocketableStack(ItemStack stack) {
		if(stack.isEmpty() || ISocketable.isSocketable(stack)) {
			inventory.setStackInSlot(0, stack);
			return true;
		}

		return false;
	}

	@Override
	public void onCraftCAD(ItemStack cad) {
		NeoForge.EVENT_BUS.post(new PostCADCraftEvent(cad, this));
		for(int i = 1; i < 6; i++) {
			inventory.setStackInSlot(i, ItemStack.EMPTY);
		}
		if(!level.isClientSide) {
			level.playSound(null, getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5, PsiSoundHandler.cadCreate, SoundSource.BLOCKS, 0.5F, 1F);
		}
	}

	@Override
	public boolean isBulletSlotEnabled(int slot) {
		if(getSocketableStack().isEmpty()) {
			return false;
		}
		ISocketable socketable = getSocketable();
		return socketable != null && socketable.isSocketSlotAvailable(slot);
	}

	/**
	 * 获取拆解处理器
	 */
	public CADDisassemblyHandler getDisassemblyHandler() {
		return disassemblyHandler;
	}

	/**
	 * 开始CAD拆解过程
	 */
	public CADDisassemblyHandler.DisassemblyResult startCADDisassembly(ItemStack cadStack, Player player) {
		return disassemblyHandler.performDirectDisassembly(cadStack, player);
	}

	/**
	 * 执行拆解下一步
	 */
	public CADDisassemblyHandler.DisassemblyResult performDisassemblyStep(Player player) {
		ItemStack socketableStack = getSocketableStack();
		if(!socketableStack.isEmpty()) {
			return disassemblyHandler.performDirectDisassembly(socketableStack, player);
		}
		return CADDisassemblyHandler.DisassemblyResult.failure("没有可拆解的CAD", CADDisassemblyHandler.DisassemblyState.ERROR);
	}

	/**
	 * 检查是否处于拆解模式
	 */
	public boolean isInDisassemblyMode() {
		return disassemblyHandler.isInDisassemblyMode();
	}

	/**
	 * 重置拆解状态
	 */
	public void resetDisassemblyState() {
		disassemblyHandler.reset();
	}

	/**
	 * 获取拆解进度信息
	 */
	public String getDisassemblyProgress() {
		return disassemblyHandler.getProgressInfo();
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
		super.saveAdditional(tag, provider);
		ContainerHelper.saveAllItems(tag, inventory.getItems(), provider);
	}

	@Override
	public void loadAdditional(CompoundTag cmp, HolderLookup.Provider provider) {
		super.loadAdditional(cmp, provider);
		readPacketNBT(cmp, provider);
	}

	public void readPacketNBT(@NotNull CompoundTag tag, HolderLookup.Provider provider) {
		// Migrate old CAD assemblers to the new format
		ListTag items = tag.getList("Items", 10);
		if(items.size() == 19) {
			for(int i = 0; i < inventory.getSlots(); i++) {
				inventory.setStackInSlot(i, ItemStack.EMPTY);
			}

			ISocketable socketable = null;

			for(int i = 0; i < items.size(); ++i) {
				if(i == 0) // Skip the fake CAD slot
				{
					continue;
				}

				ItemStack stack = ItemStack.parseOptional(provider, items.getCompound(i));

				if(i == 6) { // Socketable item
					setSocketableStack(stack);

					if(!stack.isEmpty()) {
						socketable = stack.getCapability(PsiAPI.SOCKETABLE_CAPABILITY);
					}
				} else if(i == 1) // CORE
				{
					setStackForComponent(EnumCADComponent.CORE, stack);
				} else if(i == 2) // ASSEMBLY
				{
					setStackForComponent(EnumCADComponent.ASSEMBLY, stack);
				} else if(i == 3) // SOCKET
				{
					setStackForComponent(EnumCADComponent.SOCKET, stack);
				} else if(i == 4) // BATTERY
				{
					setStackForComponent(EnumCADComponent.BATTERY, stack);
				} else if(i == 5) // DYE
				{
					setStackForComponent(EnumCADComponent.DYE, stack);
				} else { // If we've gotten here, the item is a bullet.
					int idx = i - 7;
					if(socketable != null) {
						socketable.setBulletInSocket(idx, stack);
					}
				}
			}
		} else {
			for(int i = 0; i < items.size(); i++) {
				CompoundTag compoundtag = items.getCompound(i);
				int j = compoundtag.getByte("Slot") & 255;
				if(j >= 0 && j < inventory.getItems().size()) {
					inventory.getItems().set(j, ItemStack.parse(provider, compoundtag).orElse(ItemStack.EMPTY));
				}
			}
		}
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		// 在1.21.1中使用新的create方法是正确的，已修复网络同步问题
		return ClientboundBlockEntityDataPacket.create(this, (BlockEntity e, RegistryAccess provider) -> getUpdateTag(provider));
	}

	@NotNull
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
		CompoundTag cmp = new CompoundTag();
		saveAdditional(cmp, provider);
		// 添加拆解状态信息
		cmp.putString("DisassemblyState", disassemblyHandler.getCurrentState().name());
		return cmp;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
		super.handleUpdateTag(tag, provider);
		// 处理拆解状态同步
		if(tag.contains("DisassemblyState")) {
			try {
				CADDisassemblyHandler.DisassemblyState state =
						CADDisassemblyHandler.DisassemblyState.valueOf(tag.getString("DisassemblyState"));
				// 注意：这里不能直接设置状态，因为客户端的disassemblyHandler是只读的
				// 状态会通过inventory同步自动更新
			} catch (IllegalArgumentException e) {
				// 忽略无效的状态值
			}
		}
	}

	@NotNull
	@Override
	public Component getDisplayName() {
		return Component.translatable(ModBlocks.cadAssembler.getDescriptionId());
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
		return new ContainerCADAssembler(i, playerInventory, this);
	}

	private class CADStackHandler extends ItemStackHandler {

		private CADStackHandler(int size) {
			super(size);
		}

		private NonNullList<ItemStack> getItems() {
			return this.stacks;
		}

		private void setItems(NonNullList<ItemStack> pItems) {
			this.stacks = pItems;
		}

		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			if(0 < slot && slot < 6) {
				clearCachedCAD();
			}
			setChanged();

			// 强制同步到客户端
			if(level != null && !level.isClientSide) {
				level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
			}
		}

		@Override
		public boolean isItemValid(int slot, @NotNull ItemStack stack) {
			if(stack.isEmpty()) {
				return true;
			}

			if(slot == 0) {
				return ISocketable.isSocketable(stack);
			} else if(slot < 6) {
				return stack.getItem() instanceof ICADComponent &&
						((ICADComponent) stack.getItem()).getComponentType(stack).ordinal() == slot - 1;
			} else if(slot < 12) {
				return true;
			}

			return false;
		}
	}

}
