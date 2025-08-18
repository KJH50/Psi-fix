/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.api.inventory;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import vazkii.psi.api.cad.ISocketable;
import vazkii.psi.common.block.tile.TileCADAssembler;

import java.util.Collections;
import java.util.Iterator;

public class InventorySocketable implements Container, Nameable, ContainerData {

	@Nullable
	private ISocketable socketable;
	private TileCADAssembler assembler;

	// 添加一个字段来缓存插槽数量
	private int cachedSize = -1;

	public InventorySocketable(TileCADAssembler assembler, ItemStack stack) {
		this.assembler = assembler;
		if(stack.isEmpty()) {
			socketable = null;
		} else {
			socketable = ISocketable.socketable(stack);
		}
	}

	// 当 socketable 更新时，重置缓存
	public void setStack(ItemStack stack) {
		cachedSize = -1;
		if(stack.isEmpty()) {
			socketable = null;
		} else {
			socketable = ISocketable.socketable(stack);
		}
	}

	private Iterator<ItemStack> getSockerator() {
		if(socketable == null) {
			return Collections.emptyIterator();
		}
		return new IteratorSocketable(socketable);
	}

	@Override
	public int getContainerSize() {
		if(socketable == null) {
			return 0;
		}

		// 使用缓存来避免重复计算插槽数量
		if(cachedSize != -1) {
			return cachedSize;
		}

		int size = 0;
		while(socketable.isSocketSlotAvailable(size)) {
			size++;
		}
		cachedSize = size;
		return size;
	}

	@Override
	public boolean isEmpty() {
		Iterator<ItemStack> sockerator = getSockerator();
		while(sockerator.hasNext()) {
			if(!sockerator.next().isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@NotNull
	@Override
	public ItemStack getItem(int index) {
		if(socketable == null || index < 0 || !socketable.isSocketSlotAvailable(index)) {
			return ItemStack.EMPTY;
		}
		return socketable.getBulletInSocket(index);
	}

	@NotNull
	@Override
	public ItemStack removeItem(int index, int count) {
		if(socketable == null) {
			return ItemStack.EMPTY;
		}

		ItemStack bullet = socketable.getBulletInSocket(index);
		if(!bullet.isEmpty()) {
			socketable.setBulletInSocket(index, ItemStack.EMPTY);
			this.assembler.setChanged();
		}
		return bullet;
	}

	@NotNull
	@Override
	public ItemStack removeItemNoUpdate(int index) {
		return removeItem(index, 1);
	}

	@Override
	public void setItem(int index, @NotNull ItemStack bullet) {
		if(socketable == null) {
			return;
		}

		socketable.setBulletInSocket(index, bullet);
		this.assembler.setChanged();
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}

	@Override
	public void setChanged() {
		// NO-OP
	}

	@Override
	public boolean stillValid(@NotNull Player player) {
		return true;
	}

	@Override
	public void startOpen(@NotNull Player player) {
		// NO-OP
	}

	@Override
	public void stopOpen(@NotNull Player player) {
		// NO-OP
	}

	@Override
	public boolean canPlaceItem(int index, @NotNull ItemStack stack) {
		return socketable != null && socketable.isItemValid(index, stack);
	}

	@Override
	public int get(int id) {
		return 0;
	}

	@Override
	public void set(int id, int value) {
		// NO-OP
	}

	@Override
	public int getCount() {
		return 0;
	}

	@Override
	public void clearContent() {
		Iterator<ItemStack> sockerator = getSockerator();
		while(sockerator.hasNext()) {
			sockerator.next();
			sockerator.remove();
		}
	}

	@NotNull
	@Override
	public Component getName() {
		return Component.translatable("psi.container.socketable");
	}

}
