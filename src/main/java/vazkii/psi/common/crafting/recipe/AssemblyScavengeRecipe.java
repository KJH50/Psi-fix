/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.common.crafting.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

import vazkii.psi.api.cad.EnumCADComponent;
import vazkii.psi.api.cad.ICAD;
import vazkii.psi.common.crafting.ModCraftingRecipes;

public class AssemblyScavengeRecipe extends CustomRecipe {

	public AssemblyScavengeRecipe(CraftingBookCategory category) {
		super(category);
	}

	/* ---------- 核心逻辑 ---------- */

	@Override
	public boolean matches(@NotNull CraftingInput inv, @NotNull Level level) {
		ItemStack cad = ItemStack.EMPTY;
		int cadCount = 0;

		/* 只能有 1 个 CAD，其余槽必须空 */
		for(int i = 0; i < inv.size(); i++) {
			ItemStack s = inv.getItem(i);
			if(s.isEmpty())
				continue;
			if(!(s.getItem() instanceof ICAD))
				return false; // 混入其他物品
			cad = s;
			cadCount++;
		}
		if(cadCount != 1)
			return false;

		/* 一行 Stream：非 ASSEMBLY 组件必须全空 */
		final ICAD icad = (ICAD) cad.getItem();
		final ItemStack finalCad = cad;
		return EnumCADComponent.values().length == 1 +
				java.util.Arrays.stream(EnumCADComponent.values())
						.filter(c -> c != EnumCADComponent.ASSEMBLY)
						.map(c -> icad.getComponentInSlot(finalCad, c))
						.filter(s -> !s.isEmpty())
						.count(); // 0 表示全空
	}

	@Override
	public @NotNull ItemStack assemble(@NotNull CraftingInput inv, HolderLookup.Provider reg) {
		for(int i = 0; i < inv.size(); i++) {
			ItemStack s = inv.getItem(i);
			if(s.getItem() instanceof ICAD icad)
				return icad.getComponentInSlot(s, EnumCADComponent.ASSEMBLY).copy();
		}
		return ItemStack.EMPTY;
	}

	@Override
	public @NotNull NonNullList<ItemStack> getRemainingItems(@NotNull CraftingInput inv) {
		return NonNullList.withSize(inv.size(), ItemStack.EMPTY);
	}

	/* ---------- 注册 ---------- */

	@Override
	public @NotNull RecipeType<?> getType() {
		return ModCraftingRecipes.SCAVENGE_TYPE.get();
	}

	@Override
	public @NotNull RecipeSerializer<?> getSerializer() {
		return ModCraftingRecipes.SCAVENGE_SERIALIZER.get();
	}

	@Override
	public boolean canCraftInDimensions(int w, int h) {
		return w * h >= 1;
	}
}
