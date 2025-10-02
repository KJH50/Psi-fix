/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.common.item;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

import vazkii.psi.api.PsiAPI;
import vazkii.psi.api.cad.ISocketable;
import vazkii.psi.api.cad.ISocketableController;
import vazkii.psi.common.core.handler.PsiSoundHandler;
import vazkii.psi.common.item.base.ModDataComponents;

import java.util.ArrayList;
import java.util.List;

/**
 * ItemExosuitController - 外骨骼控制器物品
 * 用于控制玩家穿戴的Psi金属外骨骼装备的插槽选择
 */
public class ItemExosuitController extends Item implements ISocketableController {

	private static final int MAX_ARMOR_SLOTS = 4;
	private static final int DEFAULT_SLOT_INDEX = 3;
	private static final float SOUND_VOLUME = 0.25F;
	private static final float SOUND_PITCH = 1.0F;

	public ItemExosuitController(Item.Properties properties) {
		super(properties.stacksTo(1));
	}

	/**
	 * 处理玩家使用控制器物品的交互
	 * 当玩家按下Shift键时，重置所有受控装备的插槽选择
	 */
	@NotNull
	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, @NotNull InteractionHand hand) {
		ItemStack controllerStack = player.getItemInHand(hand);

		if(!player.isShiftKeyDown()) {
			return new InteractionResultHolder<>(InteractionResult.PASS, controllerStack);
		}

		handleShiftClickInteraction(world, player, hand, controllerStack);
		return new InteractionResultHolder<>(InteractionResult.SUCCESS, controllerStack);
	}

	/**
	 * 处理Shift键点击交互逻辑
	 */
	private void handleShiftClickInteraction(Level world, Player player, InteractionHand hand, ItemStack controllerStack) {
		// 服务器端播放声音，客户端播放动画
		if(!world.isClientSide) {
			playResetSound(world, player);
		} else {
			player.swing(hand);
		}

		resetAllControlledSlots(player, controllerStack);
	}

	/**
	 * 播放重置插槽的音效
	 */
	private void playResetSound(Level world, Player player) {
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				PsiSoundHandler.compileError, SoundSource.PLAYERS, SOUND_VOLUME, SOUND_PITCH);
	}

	/**
	 * 重置所有受控装备的插槽选择
	 */
	private void resetAllControlledSlots(Player player, ItemStack controllerStack) {
		ItemStack[] controlledStacks = getControlledStacks(player, controllerStack);

		for(ItemStack armorStack : controlledStacks) {
			ISocketable socketable = armorStack.getCapability(PsiAPI.SOCKETABLE_CAPABILITY);
			if(socketable != null) {
				socketable.setSelectedSlot(DEFAULT_SLOT_INDEX);
			}
		}
	}

	/**
	 * 获取玩家当前穿戴的可控Psi金属外骨骼装备列表
	 * 按头盔、胸甲、护腿、靴子的顺序返回
	 */
	@Override
	public ItemStack[] getControlledStacks(Player player, ItemStack controllerStack) {
		List<ItemStack> controlledStacks = new ArrayList<>(MAX_ARMOR_SLOTS);

		for(int armorSlotIndex = 0; armorSlotIndex < MAX_ARMOR_SLOTS; armorSlotIndex++) {
			ItemStack armorStack = getArmorStackInSlot(player, armorSlotIndex);
			if(isValidSocketableArmor(armorStack)) {
				controlledStacks.add(armorStack);
			}
		}

		return controlledStacks.toArray(new ItemStack[0]);
	}

	/**
	 * 获取指定槽位的护甲物品
	 */
	private ItemStack getArmorStackInSlot(Player player, int slotIndex) {
		// 护甲槽位顺序：3=靴子, 2=护腿, 1=胸甲, 0=头盔
		return player.getInventory().armor.get(MAX_ARMOR_SLOTS - 1 - slotIndex);
	}

	/**
	 * 检查护甲物品是否为有效的可插槽装备
	 */
	private boolean isValidSocketableArmor(ItemStack armorStack) {
		return !armorStack.isEmpty() && ISocketable.isSocketable(armorStack);
	}

	/**
	 * 获取默认的控制槽位索引
	 */
	@Override
	public int getDefaultControlSlot(ItemStack controllerStack) {
		return controllerStack.getOrDefault(ModDataComponents.SELECTED_CONTROL_SLOT, 0);
	}

	/**
	 * 设置选定的控制槽位和插槽
	 * 更新控制器状态并同步到对应的护甲装备
	 */
	@Override
	public void setSelectedSlot(Player player, ItemStack controllerStack, int controlSlot, int targetSlot) {
		// 更新控制器的选定槽位
		controllerStack.set(ModDataComponents.SELECTED_CONTROL_SLOT, controlSlot);

		// 同步到对应的护甲装备
		syncSlotSelectionToArmor(player, controllerStack, controlSlot, targetSlot);
	}

	/**
	 * 将插槽选择同步到指定的护甲装备
	 */
	private void syncSlotSelectionToArmor(Player player, ItemStack controllerStack, int controlSlot, int targetSlot) {
		ItemStack[] controlledStacks = getControlledStacks(player, controllerStack);

		if(isValidControlSlot(controlSlot, controlledStacks)) {
			ItemStack targetArmor = controlledStacks[controlSlot];
			ISocketable socketable = targetArmor.getCapability(PsiAPI.SOCKETABLE_CAPABILITY);

			if(socketable != null) {
				socketable.setSelectedSlot(targetSlot);
			}
		}
	}

	/**
	 * 检查控制槽位是否有效
	 */
	private boolean isValidControlSlot(int controlSlot, ItemStack[] controlledStacks) {
		return controlSlot >= 0 && controlSlot < controlledStacks.length &&
				!controlledStacks[controlSlot].isEmpty();
	}
}
