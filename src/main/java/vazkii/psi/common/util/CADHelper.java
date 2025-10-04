package vazkii.psi.common.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import vazkii.psi.api.PsiAPI;
import vazkii.psi.api.cad.EnumCADComponent;
import vazkii.psi.api.cad.ICAD;
import vazkii.psi.api.spell.SpellContext;
import vazkii.psi.common.core.handler.PlayerDataHandler;

/**
 * Utility class for CAD-related operations to eliminate duplicate code patterns
 */
public final class CADHelper {

	private CADHelper() {
		// Utility class
	}

	/**
	 * Gets the effective CAD stack for a spell context.
	 * Returns context.tool if not empty, otherwise returns the player's CAD.
	 */
	public static ItemStack getEffectiveCAD(SpellContext context) {
		return context.tool.isEmpty() ? PsiAPI.getPlayerCAD(context.caster) : context.tool;
	}

	/**
	 * Gets the colorizer component from a CAD stack
	 */
	public static ItemStack getCADColorizer(ItemStack cadStack) {
		if(cadStack.isEmpty() || !(cadStack.getItem() instanceof ICAD cad)) {
			return ItemStack.EMPTY;
		}
		return cad.getComponentInSlot(cadStack, EnumCADComponent.DYE);
	}

	/**
	 * Checks if loopcast should be updated for the given context and CAD stack
	 */
	public static boolean shouldUpdateLoopcast(SpellContext context, ItemStack cadStack) {
		return (cadStack.getItem() instanceof ICAD) &&
				(context.castFrom == PlayerDataHandler.get(context.caster).loopcastHand);
	}

	/**
	 * Updates loopcast stack if conditions are met
	 */
	public static void updateLoopcastIfNeeded(SpellContext context, ItemStack cadStack) {
		if(shouldUpdateLoopcast(context, cadStack)) {
			PlayerDataHandler.get(context.caster).lastTickLoopcastStack = cadStack.copy();
		}
	}

	/**
	 * Gets the player's CAD or returns empty stack if not available
	 */
	public static ItemStack getPlayerCADSafe(Player player) {
		ItemStack cad = PsiAPI.getPlayerCAD(player);
		return cad != null ? cad : ItemStack.EMPTY;
	}
}
