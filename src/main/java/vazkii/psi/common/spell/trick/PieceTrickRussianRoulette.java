/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.common.spell.trick;

import net.minecraft.world.item.ItemStack;

import vazkii.psi.api.cad.ISocketable;
import vazkii.psi.api.spell.*;
import vazkii.psi.api.spell.piece.PieceTrick;
import vazkii.psi.common.util.CADHelper;
import vazkii.psi.common.util.CapabilityHelper;

import java.util.Random;

public class PieceTrickRussianRoulette extends PieceTrick {

	private static final Random RANDOM = new Random();

	public PieceTrickRussianRoulette(Spell spell) {
		super(spell);
	}

	public static int getRandomSocketableSlot(ISocketable socketable) {
		return RANDOM.nextInt(socketable.getLastSlot() + 1);
	}

	@Override
	public void addToMetadata(SpellMetadata meta) throws SpellCompilationException, ArithmeticException {
		super.addToMetadata(meta);
	}

	@Override
	public Object execute(SpellContext context) throws SpellRuntimeException {
		ItemStack stack = CADHelper.getEffectiveCAD(context);
		boolean updateLoopcast = CADHelper.shouldUpdateLoopcast(context, stack);
		ISocketable capability = CapabilityHelper.getSocketableCapability(stack);
		int targetSlot = getRandomSocketableSlot(capability);

		capability.setSelectedSlot(targetSlot);
		CADHelper.updateLoopcastIfNeeded(context, stack);
		return null;
	}

}
