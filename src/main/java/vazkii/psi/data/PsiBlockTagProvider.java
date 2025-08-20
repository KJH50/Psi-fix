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
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import vazkii.psi.common.block.base.ModBlocks;
import vazkii.psi.common.lib.LibMisc;
import vazkii.psi.common.lib.ModTags;

import java.util.concurrent.CompletableFuture;

public class PsiBlockTagProvider extends BlockTagsProvider {

	public PsiBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
		super(output, lookupProvider, LibMisc.MOD_ID, existingFileHelper);
	}

	@Override
	public String getName() {
		return "Psi block tags";
	}

	@Override
	protected void addTags(HolderLookup.Provider pProvider) {
		tag(ModTags.Blocks.BLOCK_PSIMETAL).add(ModBlocks.psimetalBlock);
		tag(ModTags.Blocks.BLOCK_PSIGEM).add(ModBlocks.psigemBlock);
		tag(ModTags.Blocks.BLOCK_EBONY_PSIMETAL).add(ModBlocks.psimetalEbony);
		tag(ModTags.Blocks.BLOCK_IVORY_PSIMETAL).add(ModBlocks.psimetalIvory);

		tag(Tags.Blocks.STORAGE_BLOCKS).add(ModBlocks.psimetalBlock);
		tag(Tags.Blocks.STORAGE_BLOCKS).add(ModBlocks.psigemBlock);
		tag(Tags.Blocks.STORAGE_BLOCKS).add(ModBlocks.psimetalEbony);
		tag(Tags.Blocks.STORAGE_BLOCKS).add(ModBlocks.psimetalIvory);

		// 添加挖掘标签，使方块可以被镐子挖掘
		tag(BlockTags.MINEABLE_WITH_PICKAXE)
				.add(ModBlocks.cadAssembler)
				.add(ModBlocks.programmer)
				.add(ModBlocks.psimetalBlock)
				.add(ModBlocks.psigemBlock)
				.add(ModBlocks.psimetalPlateBlack)
				.add(ModBlocks.psimetalPlateBlackLight)
				.add(ModBlocks.psimetalPlateWhite)
				.add(ModBlocks.psimetalPlateWhiteLight)
				.add(ModBlocks.psimetalEbony)
				.add(ModBlocks.psimetalIvory);

		// 添加挖掘等级标签，使这些方块需要铁镐或更高级别工具才能挖掘掉落
		tag(BlockTags.NEEDS_IRON_TOOL)
				.add(ModBlocks.psigemBlock)
				.add(ModBlocks.psimetalBlock)
				.add(ModBlocks.psimetalEbony)
				.add(ModBlocks.psimetalIvory);

		// 添加挖掘标签，使psidust_block可以被铲子挖掘
		tag(BlockTags.MINEABLE_WITH_SHOVEL)
				.add(ModBlocks.psidustBlock);
	}
}
