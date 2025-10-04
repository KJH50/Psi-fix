package vazkii.psi.common.util;

import net.minecraft.world.item.ItemStack;

import vazkii.psi.api.PsiAPI;
import vazkii.psi.api.cad.ISocketable;

import java.util.Objects;

/**
 * Utility class for capability-related operations to eliminate duplicate code patterns
 */
public final class CapabilityHelper {

	private CapabilityHelper() {
		// Utility class
	}

	/**
	 * Gets the socketable capability from an ItemStack, throwing an exception if not present.
	 * This is a common pattern used throughout the codebase.
	 */
	public static ISocketable getSocketableCapability(ItemStack stack) {
		return Objects.requireNonNull(stack.getCapability(PsiAPI.SOCKETABLE_CAPABILITY));
	}

	/**
	 * Safely gets the socketable capability from an ItemStack, returning null if not present.
	 */
	public static ISocketable getSocketableCapabilitySafe(ItemStack stack) {
		return stack.getCapability(PsiAPI.SOCKETABLE_CAPABILITY);
	}

	/**
	 * Checks if an ItemStack has a socketable capability
	 */
	public static boolean hasSocketableCapability(ItemStack stack) {
		return !stack.isEmpty() && stack.getCapability(PsiAPI.SOCKETABLE_CAPABILITY) != null;
	}
}
