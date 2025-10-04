package vazkii.psi.common.util;

import net.minecraft.world.entity.Entity;

import vazkii.psi.api.spell.SpellContext;

import java.util.ArrayList;

/**
 * Utility class for spell execution patterns to eliminate duplicate code
 */
public final class SpellExecutionHelper {

	private SpellExecutionHelper() {
		// Utility class
	}

	/**
	 * Safely executes a spell and returns an empty entity list.
	 * This is a common pattern used in many spell bullet implementations.
	 */
	public static ArrayList<Entity> executeSpellAndReturnEmpty(SpellContext context) {
		context.cspell.safeExecute(context);
		return new ArrayList<>();
	}

	/**
	 * Safely executes a spell and returns a list containing the given entity.
	 * This is a common pattern used in projectile spell bullets.
	 */
	public static ArrayList<Entity> executeSpellAndReturnEntity(SpellContext context, Entity entity) {
		context.cspell.safeExecute(context);
		ArrayList<Entity> spellEntities = new ArrayList<>();
		spellEntities.add(entity);
		return spellEntities;
	}

	/**
	 * Just safely executes a spell without returning anything.
	 * This is used in contexts where no return value is needed.
	 */
	public static void executeSpell(SpellContext context) {
		context.cspell.safeExecute(context);
	}
}
