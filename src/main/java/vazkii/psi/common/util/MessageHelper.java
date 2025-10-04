/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.common.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;

/**
 * 消息处理的工具类，用于统一格式化和发送消息
 */
public final class MessageHelper {

	private MessageHelper() {
		// 工具类，禁止实例化
	}

	/**
	 * 发送红色错误消息给玩家
	 */
	public static void sendErrorMessage(Player player, String translationKey, Object... args) {
		Component message = Component.translatable(translationKey, args)
				.setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
		player.sendSystemMessage(message);
	}

	/**
	 * 发送绿色成功消息给玩家
	 */
	public static void sendSuccessMessage(Player player, String translationKey, Object... args) {
		Component message = Component.translatable(translationKey, args)
				.setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
		player.sendSystemMessage(message);
	}

	/**
	 * 发送黄色警告消息给玩家
	 */
	public static void sendWarningMessage(Player player, String translationKey, Object... args) {
		Component message = Component.translatable(translationKey, args)
				.setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
		player.sendSystemMessage(message);
	}

	/**
	 * 发送普通消息给玩家
	 */
	public static void sendMessage(Player player, String translationKey, Object... args) {
		Component message = Component.translatable(translationKey, args);
		player.sendSystemMessage(message);
	}

	/**
	 * 创建红色错误消息组件
	 */
	public static Component createErrorMessage(String translationKey, Object... args) {
		return Component.translatable(translationKey, args)
				.setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
	}

	/**
	 * 创建绿色成功消息组件
	 */
	public static Component createSuccessMessage(String translationKey, Object... args) {
		return Component.translatable(translationKey, args)
				.setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN));
	}

	/**
	 * 创建黄色警告消息组件
	 */
	public static Component createWarningMessage(String translationKey, Object... args) {
		return Component.translatable(translationKey, args)
				.setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW));
	}

	/**
	 * 检查并发送取消消息（如果消息不为空）
	 */
	public static void sendCancelMessageIfPresent(Player player, String cancelMessage) {
		if(cancelMessage != null && !cancelMessage.isEmpty()) {
			sendErrorMessage(player, cancelMessage);
		}
	}
}
