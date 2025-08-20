/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableProvider.SubProviderEntry;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import vazkii.psi.common.block.base.ModBlocks;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class PsiLootTableProvider extends LootTableProvider {
	public PsiLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
		super(
				output,
				Set.of(), // 空 Set<ResourceKey<LootTable>>
				List.of(new SubProviderEntry(provider -> new BlockLoot(provider), LootContextParamSets.BLOCK)),
				lookupProvider
		);
	}

	private static class BlockLoot extends BlockLootSubProvider {
		protected BlockLoot(HolderLookup.Provider provider) {
			super(Set.of(), FeatureFlags.VANILLA_SET, provider);
		}

		@Override
		protected void generate() {
			dropSelf(ModBlocks.cadAssembler);
			dropSelf(ModBlocks.programmer);
			dropSelf(ModBlocks.psidustBlock);
			dropSelf(ModBlocks.psimetalBlock);
			dropSelf(ModBlocks.psigemBlock);
			dropSelf(ModBlocks.psimetalPlateBlack);
			dropSelf(ModBlocks.psimetalPlateBlackLight);
			dropSelf(ModBlocks.psimetalPlateWhite);
			dropSelf(ModBlocks.psimetalPlateWhiteLight);
			dropSelf(ModBlocks.psimetalEbony);
			dropSelf(ModBlocks.psimetalIvory);
		}

		@Override
		protected Iterable<Block> getKnownBlocks() {
			return ModBlocks.getAllBlocks();
		}
	}
}
