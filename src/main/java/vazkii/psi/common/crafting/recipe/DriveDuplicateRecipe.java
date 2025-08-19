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
import net.neoforged.neoforge.data.loading.DatagenModLoader;

import org.jetbrains.annotations.NotNull;

import vazkii.psi.api.spell.Spell;
import vazkii.psi.common.crafting.ModCraftingRecipes;
import vazkii.psi.common.item.ItemSpellDrive;
import vazkii.psi.common.item.base.ModDataComponents;

public class DriveDuplicateRecipe extends CustomRecipe {
	public DriveDuplicateRecipe(CraftingBookCategory category) {
		super(category);
	}

	@Override
	public boolean matches(@NotNull CraftingInput inv, @NotNull Level world) {
		boolean foundSource = false;
		boolean foundTarget = false;

		for(int i = 0; i < inv.size(); i++) {
			ItemStack stack = inv.getItem(i);
			if(!stack.isEmpty()) {
				if(stack.getItem() instanceof ItemSpellDrive) {
					if(!stack.has(ModDataComponents.SPELL)) {
						if(foundTarget) {
							return false;
						}
						foundTarget = true;
					} else {
						if(foundSource) {
							return false;
						}
						foundSource = true;
					}
				} else {
					return false;
				}
			}
		}

		return foundSource && foundTarget;
	}

	@NotNull
	@Override
	public ItemStack assemble(@NotNull CraftingInput inv, HolderLookup.Provider pRegistries) {
		Spell source = null;
		ItemStack target = ItemStack.EMPTY;

		for(int i = 0; i < inv.size(); i++) {
			ItemStack stack = inv.getItem(i);
			if(!stack.isEmpty()) {
				if(stack.has(ModDataComponents.SPELL)) {
					source = ItemSpellDrive.getSpell(stack);
				} else {
					target = stack;
				}
			}
		}

		ItemStack copy = target.copy();
		ItemSpellDrive.setSpell(copy, source);
		return copy;
	}

	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
		NonNullList<ItemStack> list = NonNullList.withSize(inv.size(), ItemStack.EMPTY);

		for(int i = 0; i < list.size(); ++i) {
			ItemStack item = inv.getItem(i);
			if(!item.isEmpty() && item.has(ModDataComponents.SPELL)) {
				list.set(i, item.copy());
				break;
			}
		}

		return list;
	}

	@Override
	public @NotNull RecipeType<?> getType() {
		return !DatagenModLoader.isRunningDataGen() ? RecipeType.CRAFTING : ModCraftingRecipes.DRIVE_DUPLICATE_TYPE.get();
	}

	@NotNull
	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModCraftingRecipes.DRIVE_DUPLICATE_SERIALIZER.get();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

}
