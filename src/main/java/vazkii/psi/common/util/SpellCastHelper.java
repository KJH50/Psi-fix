/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.common.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import vazkii.psi.api.spell.SpellContext;
import vazkii.psi.common.core.handler.PlayerDataHandler;
import vazkii.psi.common.core.handler.PlayerDataHandler.PlayerData;
import vazkii.psi.common.item.ItemCAD;

import java.util.function.Consumer;

/**
 * 法术施放的工具类，用于统一处理常见的法术施放模式
 */
public final class SpellCastHelper {

	private SpellCastHelper() {
		// 工具类，禁止实例化
	}

	/**
	 * 为工具类物品施放法术的通用方法
	 */
	public static void castToolSpell(Player player, ItemStack tool, ItemStack bullet, ItemStack playerCad,
			int cooldown, int cost, float volume, Consumer<SpellContext> contextSetup) {
		PlayerData data = PlayerDataHandler.get(player);
		Level world = player.getCommandSenderWorld();

		ItemCAD.cast(world, player, data, bullet, playerCad, cooldown, cost, volume, (SpellContext context) -> {
			context.tool = tool;
			if(contextSetup != null) {
				contextSetup.accept(context);
			}
		});
	}

	/**
	 * 为护甲类物品施放法术的通用方法
	 */
	public static void castArmorSpell(LivingEntity entity, ItemStack armor, ItemStack bullet, ItemStack playerCad,
			int cooldown, int cost, float volume, Entity attacker, float damage) {
		if(!(entity instanceof Player player))
			return;

		PlayerData data = PlayerDataHandler.get(player);
		Level world = player.getCommandSenderWorld();
		int timesCast = DataComponentHelper.getTimesCast(armor);

		ItemCAD.cast(world, player, data, bullet, playerCad, cooldown, cost, volume, (SpellContext context) -> {
			context.tool = armor;
			context.attackingEntity = attacker instanceof LivingEntity ? (LivingEntity) attacker : null;
			context.damageTaken = damage;
			context.loopcastIndex = timesCast;
		}, cost);
	}

	/**
	 * 为剑类物品施放法术的通用方法
	 */
	public static void castSwordSpell(Player player, ItemStack sword, ItemStack bullet, ItemStack playerCad) {
		PlayerData data = PlayerDataHandler.get(player);
		Level world = player.getCommandSenderWorld();

		ItemCAD.cast(world, player, data, bullet, playerCad, 5, 10, 0.05F, (SpellContext context) -> {
			context.tool = sword;
		});
	}

	/**
	 * 为工具类物品施放法术，包含位置信息
	 */
	public static void castToolSpellWithPosition(Player player, ItemStack tool, ItemStack bullet, ItemStack playerCad,
			BlockHitResult hitResult, Entity target) {
		PlayerData data = PlayerDataHandler.get(player);
		Level world = player.getCommandSenderWorld();

		ItemCAD.cast(world, player, data, bullet, playerCad, 5, 10, 0.05F, (SpellContext context) -> {
			context.tool = tool;
			if(target != null && target instanceof LivingEntity) {
				context.attackedEntity = (LivingEntity) target;
			}
			context.positionBroken = hitResult;
		});
	}
}
