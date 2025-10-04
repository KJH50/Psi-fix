/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.common.crafting.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

import vazkii.psi.api.cad.EnumCADComponent;
import vazkii.psi.api.cad.ICAD;

/**
 * CAD组件提取配方 - 用于在装配器中逐步提取CAD组件
 */
public class CADComponentExtractionRecipe implements Recipe<CraftingInput> {

	private final EnumCADComponent targetComponent;

	public CADComponentExtractionRecipe(EnumCADComponent targetComponent) {
		this.targetComponent = targetComponent;
	}

	@Override
	public boolean matches(@NotNull CraftingInput input, @NotNull Level level) {
		// 这个配方不用于普通工作台，仅用于CAD装配器内部逻辑
		return false;
	}

	@Override
	public @NotNull ItemStack assemble(@NotNull CraftingInput input, HolderLookup.@NotNull Provider registryAccess) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return false;
	}

	@Override
	public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registryAccess) {
		return ItemStack.EMPTY;
	}

	@Override
	public @NotNull RecipeSerializer<?> getSerializer() {
		return Serializer.INSTANCE;
	}

	@Override
	public @NotNull RecipeType<?> getType() {
		return Type.INSTANCE;
	}

	/**
	 * 从CAD中提取指定组件
	 */
	public ItemStack extractComponent(ItemStack cadStack) {
		if(!(cadStack.getItem() instanceof ICAD icad)) {
			return ItemStack.EMPTY;
		}

		return icad.getComponentInSlot(cadStack, targetComponent);
	}

	/**
	 * 检查CAD是否包含指定组件
	 */
	public boolean hasComponent(ItemStack cadStack) {
		if(!(cadStack.getItem() instanceof ICAD icad)) {
			return false;
		}

		ItemStack component = icad.getComponentInSlot(cadStack, targetComponent);
		return !component.isEmpty();
	}

	/**
	 * 从CAD中移除指定组件
	 */
	public ItemStack removeComponent(ItemStack cadStack) {
		if(!(cadStack.getItem() instanceof ICAD icad)) {
			return cadStack;
		}

		ItemStack newCAD = cadStack.copy();
		// 使用ICAD.setComponent方法来移除组件
		ICAD.setComponent(newCAD, ItemStack.EMPTY);
		return newCAD;
	}

	public EnumCADComponent getTargetComponent() {
		return targetComponent;
	}

	/**
	 * 配方类型
	 */
	public static class Type implements RecipeType<CADComponentExtractionRecipe> {
		public static final Type INSTANCE = new Type();
		public static final String ID = "cad_component_extraction";

		private Type() {}

		@Override
		public String toString() {
			return ID;
		}
	}

	/**
	 * 配方序列化器
	 */
	public static class Serializer implements RecipeSerializer<CADComponentExtractionRecipe> {
		public static final Serializer INSTANCE = new Serializer();

		private static final MapCodec<CADComponentExtractionRecipe> CODEC =
				RecordCodecBuilder.mapCodec(instance -> instance.group(
						EnumCADComponent.CODEC.fieldOf("target_component").forGetter(r -> r.targetComponent)
				).apply(instance, CADComponentExtractionRecipe::new));

		private static final StreamCodec<RegistryFriendlyByteBuf, CADComponentExtractionRecipe> STREAM_CODEC =
				StreamCodec.composite(
						EnumCADComponent.STREAM_CODEC, r -> r.targetComponent,
						CADComponentExtractionRecipe::new
				);

		@Override
		public @NotNull MapCodec<CADComponentExtractionRecipe> codec() {
			return CODEC;
		}

		@Override
		public @NotNull StreamCodec<RegistryFriendlyByteBuf, CADComponentExtractionRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
