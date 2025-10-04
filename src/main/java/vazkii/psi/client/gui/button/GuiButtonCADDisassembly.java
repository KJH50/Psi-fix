/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.client.gui.button;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.NotNull;

import vazkii.psi.common.Psi;
import vazkii.psi.common.block.tile.CADDisassemblyHandler;

/**
 * CAD拆解按钮GUI组件
 */
public class GuiButtonCADDisassembly extends Button {

	private static final ResourceLocation BUTTON_TEXTURE =
			Psi.location("textures/gui/cad_disassembly_button.png");

	private CADDisassemblyHandler.DisassemblyState currentState = CADDisassemblyHandler.DisassemblyState.IDLE;
	private boolean hasValidCAD = false;

	public GuiButtonCADDisassembly(int x, int y, OnPress onPress) {
		super(x, y, 20, 20, Component.literal("拆解"), onPress, DEFAULT_NARRATION);
	}

	/**
	 * 更新按钮状态
	 */
	public void updateState(CADDisassemblyHandler.DisassemblyState state, boolean hasCAD) {
		this.currentState = state;
		this.hasValidCAD = hasCAD;
		this.active = hasCAD && (state == CADDisassemblyHandler.DisassemblyState.IDLE ||
				state == CADDisassemblyHandler.DisassemblyState.COMPLETED ||
				state == CADDisassemblyHandler.DisassemblyState.ERROR);
		updateMessage();
	}

	/**
	 * 更新按钮显示文本
	 */
	private void updateMessage() {
		switch(currentState) {
		case IDLE:
			setMessage(Component.literal("拆解"));
			break;
		case IN_PROGRESS:
			setMessage(Component.literal("拆解中..."));
			break;
		case COMPLETED:
			setMessage(Component.literal("拆解完成"));
			break;
		case ERROR:
			setMessage(Component.literal("拆解失败"));
			break;
		}
	}

	@Override
	public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
		if(!visible) {
			return;
		}

		// 设置按钮颜色
		int color = getButtonColor();

		// 绘制按钮背景
		guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, color);

		// 绘制边框
		int borderColor = isHovered ? 0xFFFFFFFF : 0xFF888888;
		guiGraphics.fill(getX(), getY(), getX() + width, getY() + 1, borderColor);
		guiGraphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor);
		guiGraphics.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);
		guiGraphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor);

		// 绘制文本
		int textColor = active ? 0xFFFFFFFF : 0xFF666666;
		guiGraphics.drawCenteredString(
				net.minecraft.client.Minecraft.getInstance().font,
				getMessage(),
				getX() + width / 2,
				getY() + (height - 8) / 2,
				textColor
		);
	}

	/**
	 * 获取按钮颜色
	 */
	private int getButtonColor() {
		if(!active) {
			return 0xFF666666;
		}

		switch(currentState) {
		case IDLE:
			return isHovered ? 0xFF5555FF : 0xFF3333DD;
		case IN_PROGRESS:
			return 0xFFFFAA00;
		case COMPLETED:
			return 0xFF55FF55;
		case ERROR:
			return 0xFFFF5555;
		default:
			return 0xFF888888;
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if(!active || !visible) {
			return false;
		}

		if(isValidClickButton(button) && clicked(mouseX, mouseY)) {
			playDownSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());
			onClick(mouseX, mouseY);
			return true;
		}

		return false;
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		// 只有在空闲、完成或错误状态下才能点击
		if(currentState == CADDisassemblyHandler.DisassemblyState.IDLE ||
				currentState == CADDisassemblyHandler.DisassemblyState.COMPLETED ||
				currentState == CADDisassemblyHandler.DisassemblyState.ERROR) {
			onPress.onPress(this);
		}
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		// 失去焦点时清除悬停状态
		if(!focused) {
			this.isHovered = false;
		}
	}
}
