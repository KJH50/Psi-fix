/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.common.item.armor;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.jetbrains.annotations.NotNull;

import vazkii.psi.api.exosuit.IExosuitSensor;
import vazkii.psi.api.exosuit.ISensorHoldable;
import vazkii.psi.common.util.DataComponentHelper;

public class ItemPsimetalExosuitHelmet extends ItemPsimetalArmor implements ISensorHoldable {

	public ItemPsimetalExosuitHelmet(ArmorItem.Type type, Item.Properties properties) {
		super(type, properties);
	}

	@Override
	public String getEvent(ItemStack stack) {
		ItemStack sensor = getAttachedSensor(stack);
		if(!sensor.isEmpty() && sensor.getItem() instanceof IExosuitSensor) {
			return ((IExosuitSensor) sensor.getItem()).getEventType(sensor);
		}

		return super.getEvent(stack);
	}

	@Override
	public int getCastCooldown(ItemStack stack) {
		return 40;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public int getColor(@NotNull ItemStack stack) {
		ItemStack sensor = getAttachedSensor(stack);
		if(!sensor.isEmpty() && sensor.getItem() instanceof IExosuitSensor) {
			return ((IExosuitSensor) sensor.getItem()).getColor(sensor);
		}

		return super.getColor(stack);
	}

	@Override
	public ItemStack getAttachedSensor(ItemStack stack) {
		return DataComponentHelper.getSensor(stack);
	}

	@Override
	public void attachSensor(ItemStack stack, ItemStack sensor) {
		DataComponentHelper.setSensor(stack, sensor.getItem());
	}
}
