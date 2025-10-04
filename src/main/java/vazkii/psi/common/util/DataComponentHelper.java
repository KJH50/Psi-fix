/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

import vazkii.psi.api.cad.EnumCADComponent;
import vazkii.psi.api.spell.Spell;
import vazkii.psi.common.item.base.ModDataComponents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 数据组件访问的工具类，用于统一处理ModDataComponents的常见操作
 */
public final class DataComponentHelper {

	private DataComponentHelper() {
		// 工具类，禁止实例化
	}

	/**
	 * 获取CAD组件列表，如果不存在则返回默认的空组件列表
	 */
	public static List<Item> getCADComponents(ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.COMPONENTS.get(),
				new ArrayList<>(Collections.nCopies(EnumCADComponent.values().length, Items.AIR)));
	}

	/**
	 * 设置CAD组件列表
	 */
	public static void setCADComponents(ItemStack stack, List<Item> components) {
		stack.set(ModDataComponents.COMPONENTS.get(), components);
	}

	/**
	 * 创建默认的CAD组件列表
	 */
	public static List<Item> createDefaultCADComponents() {
		return new ArrayList<>(Collections.nCopies(EnumCADComponent.values().length, Items.AIR));
	}

	/**
	 * 安全地设置法术到物品堆栈
	 */
	public static void setSpell(ItemStack stack, Spell spell) {
		if(spell != null) {
			stack.set(ModDataComponents.SPELL, spell);
			stack.set(DataComponents.RARITY, Rarity.RARE);
		} else {
			stack.remove(ModDataComponents.SPELL);
			stack.set(DataComponents.RARITY, Rarity.COMMON);
		}
	}

	/**
	 * 安全地获取法术从物品堆栈
	 */
	public static Spell getSpell(ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.SPELL, new Spell());
	}

	/**
	 * 设置位置数据组件
	 */
	public static void setPosition(ItemStack stack, String componentKey, BlockPos pos) {
		if("SRC_POS".equals(componentKey)) {
			stack.set(ModDataComponents.SRC_POS, pos);
		} else if("DST_POS".equals(componentKey)) {
			stack.set(ModDataComponents.DST_POS, pos);
		}
	}

	/**
	 * 获取位置数据组件
	 */
	public static BlockPos getPosition(ItemStack stack, String componentKey) {
		if("SRC_POS".equals(componentKey)) {
			return stack.getOrDefault(ModDataComponents.SRC_POS, BlockPos.ZERO);
		} else if("DST_POS".equals(componentKey)) {
			return stack.getOrDefault(ModDataComponents.DST_POS, BlockPos.ZERO);
		}
		return BlockPos.ZERO;
	}

	/**
	 * 增加施法次数计数器
	 */
	public static void incrementTimesCast(ItemStack stack) {
		int timesCast = stack.getOrDefault(ModDataComponents.TIMES_CAST, 0);
		stack.set(ModDataComponents.TIMES_CAST, timesCast + 1);
	}

	public static void resetTimesCast(ItemStack stack) {
		stack.set(ModDataComponents.TIMES_CAST, 0);
	}

	/**
	 * 增加重生时间计数器
	 */
	public static void incrementRegenTime(ItemStack stack) {
		int regenTime = stack.getOrDefault(ModDataComponents.REGEN_TIME, 0);
		stack.set(ModDataComponents.REGEN_TIME, regenTime + 1);
	}

	/**
	 * 设置贡献者名称
	 */
	public static void setContributor(ItemStack stack, String name) {
		stack.set(ModDataComponents.CONTRIBUTOR, name);
	}

	/**
	 * 获取贡献者名称
	 */
	public static String getContributor(ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.CONTRIBUTOR, "");
	}

	/**
	 * 设置传感器物品
	 */
	public static void setSensor(ItemStack stack, Item sensor) {
		stack.set(ModDataComponents.SENSOR, sensor);
	}

	/**
	 * 获取传感器物品堆栈
	 */
	public static ItemStack getSensor(ItemStack stack) {
		return new ItemStack(stack.getOrDefault(ModDataComponents.SENSOR, Items.AIR));
	}

	/**
	 * 获取选定的控制槽位
	 */
	public static int getSelectedControlSlot(ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.SELECTED_CONTROL_SLOT, 0);
	}

	/**
	 * 设置选定的控制槽位
	 */
	public static void setSelectedControlSlot(ItemStack stack, int slot) {
		stack.set(ModDataComponents.SELECTED_CONTROL_SLOT, slot);
	}

	/**
	 * 获取选定的槽位
	 */
	public static int getSelectedSlot(ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.SELECTED_SLOT, 0);
	}

	/**
	 * 设置选定的槽位
	 */
	public static void setSelectedSlot(ItemStack stack, int slot) {
		stack.set(ModDataComponents.SELECTED_SLOT, slot);
	}

	/**
	 * 获取施法次数
	 */
	public static int getTimesCast(ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.TIMES_CAST, 0);
	}

	/**
	 * 获取重生时间
	 */
	public static int getRegenTime(ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.REGEN_TIME, 0);
	}
}
