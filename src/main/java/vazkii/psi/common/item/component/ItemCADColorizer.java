/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.common.item.component;

import net.minecraft.util.FastColor;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import vazkii.psi.api.cad.ICADColorizer;
import vazkii.psi.common.util.DataComponentHelper;

public class ItemCADColorizer extends ItemCADComponent implements ICADColorizer {

	private final DyeColor color;

	public ItemCADColorizer(Properties properties, DyeColor color) {
		super(properties);
		this.color = color;
	}

	public ItemCADColorizer(Properties properties) {
		super(properties);
		color = DyeColor.BLACK;
	}

	private static String getProperDyeName(DyeColor color) {
		return color.getSerializedName();
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public int getColor(ItemStack stack) {
		// 在1.21.1中，getTextColor()返回正确的ARGB颜色值
		// FastColor.ARGB32.opaque确保alpha通道为不透明
		return FastColor.ARGB32.opaque(color.getTextColor());
	}

	@Override
	public String getContributorName(ItemStack stack) {
		return DataComponentHelper.getContributor(stack);
	}

	@Override
	public void setContributorName(ItemStack stack, String name) {
		DataComponentHelper.setContributor(stack, name);
	}
}
