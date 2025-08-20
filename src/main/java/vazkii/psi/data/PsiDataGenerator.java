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
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import vazkii.psi.common.lib.LibMisc;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = LibMisc.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class PsiDataGenerator {

	@SubscribeEvent
	public static void gatherData(GatherDataEvent event) {
		DataGenerator gen = event.getGenerator();
		PackOutput output = gen.getPackOutput();
		CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
		ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

		if(event.includeServer()) {
			gen.addProvider(event.includeServer(), new PsiRecipeGenerator(output, lookupProvider));
			gen.addProvider(event.includeServer(), new PsiLootTableProvider(output, lookupProvider));
			PsiBlockTagProvider blockTagProvider = new PsiBlockTagProvider(output, lookupProvider, existingFileHelper);
			gen.addProvider(true, blockTagProvider);
			gen.addProvider(true, new PsiDamageTypeTagsProvider(gen.getPackOutput(), lookupProvider, existingFileHelper));
			gen.addProvider(true, new PsiItemTagProvider(gen.getPackOutput(), lookupProvider, blockTagProvider.contentsGetter(), existingFileHelper));
		}

		if(event.includeClient()) {
			gen.addProvider(true, new PsiBlockModelGenerator(gen.getPackOutput(), existingFileHelper));
			gen.addProvider(true, new PsiItemModelGenerator(gen.getPackOutput(), existingFileHelper));
		}
	}
}
