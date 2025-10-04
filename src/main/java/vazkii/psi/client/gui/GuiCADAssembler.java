/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import vazkii.psi.api.cad.EnumCADStat;
import vazkii.psi.api.cad.ICAD;
import vazkii.psi.api.cad.ICADComponent;
import vazkii.psi.api.cad.ISocketable;
import vazkii.psi.common.Psi;
import vazkii.psi.common.block.base.ModBlocks;
import vazkii.psi.common.block.tile.TileCADAssembler;
import vazkii.psi.common.block.tile.container.ContainerCADAssembler;
import vazkii.psi.common.lib.LibResources;
import vazkii.psi.common.network.MessageRegister;
import vazkii.psi.common.network.message.MessageCADDisassembly;
import vazkii.psi.common.network.message.MessageCADDisassembly.DisassemblyAction;

public class GuiCADAssembler extends AbstractContainerScreen<ContainerCADAssembler> {

	private static final ResourceLocation texture = ResourceLocation.parse(LibResources.GUI_CAD_ASSEMBLER);
	private final Player player;
	private final TileCADAssembler assembler;

	// 拆解功能相关字段
	private Button disassemblyButton;

	// 错误提示系统字段
	private String errorMessage = "";
	private long errorDisplayStartTime = 0;
	private static final long ERROR_DISPLAY_DURATION = 3000; // 3秒显示时长
	private boolean showingError = false;

	public GuiCADAssembler(ContainerCADAssembler containerCADAssembler, Inventory inventory, Component component) {
		super(containerCADAssembler, inventory, component);
		this.player = inventory.player;
		this.assembler = containerCADAssembler.assembler;
		imageWidth = 256;
		imageHeight = 225;
	}

	@Override
	protected void init() {
		super.init();

		// 添加拆解按钮
		disassemblyButton = Button.builder(Component.literal("拆"), button -> {
			ItemStack socketableStack = assembler.getSocketableStack();
			boolean hasItemsInAssemblySlots = hasItemsInAssemblySlots();
			boolean hasBulletsInSlots = hasBulletsInSlots();

			// 检查拆解条件并给出相应提示
			if(socketableStack.isEmpty()) {
				showErrorMessage("无可拆解物品");
				return;
			}

			if(hasItemsInAssemblySlots) {
				showErrorMessage("合成槽位有物品，无法拆解");
				return;
			}

			if(hasBulletsInSlots) {
				showErrorMessage("子弹槽位有子弹，无法拆解");
				return;
			}

			// 满足所有条件时执行拆解
			MessageRegister.sendToServer(new MessageCADDisassembly(assembler.getBlockPos(), DisassemblyAction.START));
		})
				.bounds(leftPos + 68, topPos + 22, 18, 18)
				.build();

		addRenderableWidget(disassemblyButton);
	}

	@Override
	public void render(GuiGraphics graphics, int x, int y, float pTicks) {
		this.renderBackground(graphics, x, y, pTicks);
		super.render(graphics, x, y, pTicks);
		this.renderTooltip(graphics, x, y);

		// 更新拆解按钮状态
		updateDisassemblyButton();
	}

	/**
	 * 更新拆解按钮状态
	 */
	private void updateDisassemblyButton() {
		ItemStack socketableStack = assembler.getSocketableStack();
		boolean hasItemsInAssemblySlots = hasItemsInAssemblySlots();
		boolean hasBulletsInSlots = hasBulletsInSlots();

		// 拆解条件：必须为空栏位且子弹栏无子弹才能进行拆解
		boolean canDisassemble = !socketableStack.isEmpty() &&
				(socketableStack.getItem() instanceof ICADComponent || ISocketable.isSocketable(socketableStack)) &&
				!hasItemsInAssemblySlots && !hasBulletsInSlots;

		// 按钮始终保持激活状态，但会根据条件给出不同提示
		disassemblyButton.active = true;

		// 按钮文本
		if(assembler.isInDisassemblyMode()) {
			disassemblyButton.setMessage(Component.literal("拆解中"));
		} else {
			disassemblyButton.setMessage(Component.literal("拆"));
		}
	}

	/**
	 * 检查合成栏位是否有物品（拆解条件检查）
	 */
	private boolean hasItemsInAssemblySlots() {
		// 检查CAD组件槽位（槽位1-5：ASSEMBLY, CORE, SOCKET, BATTERY, DYE）
		for(int i = 1; i <= 5; i++) {
			ItemStack stack = menu.getSlot(i).getItem();
			if(!stack.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 检查子弹槽位是否有子弹（拆解条件检查）
	 */
	private boolean hasBulletsInSlots() {
		// 检查子弹槽位（槽位7-18：12个子弹槽位）
		for(int i = 7; i <= 18; i++) {
			ItemStack stack = menu.getSlot(i).getItem();
			if(!stack.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 显示错误提示信息
	 */
	private void showErrorMessage(String message) {
		this.errorMessage = message;
		this.errorDisplayStartTime = System.currentTimeMillis();
		this.showingError = true;
	}

	/**
	 * 检查错误提示是否应该继续显示
	 */
	private boolean shouldShowError() {
		if(!showingError) {
			return false;
		}

		long currentTime = System.currentTimeMillis();
		if(currentTime - errorDisplayStartTime > ERROR_DISPLAY_DURATION) {
			showingError = false;
			errorMessage = "";
			return false;
		}

		return true;
	}

	/**
	 * 获取拆解无法执行的原因提示
	 */
	private String getDisassemblyBlockReason() {
		ItemStack socketableStack = assembler.getSocketableStack();

		if(socketableStack.isEmpty()) {
			return "无可拆解物品";
		}

		if(hasItemsInAssemblySlots()) {
			return "合成槽位有物品，无法拆解";
		}

		if(hasBulletsInSlots()) {
			return "子弹槽位有子弹，无法拆解";
		}

		return null; // 可以拆解
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		int color = 4210752;

		String name = new ItemStack(ModBlocks.cadAssembler).getHoverName().getString();
		graphics.drawString(this.font, name, imageWidth / 2 - font.width(name) / 2, 10, color, false);

		ItemStack cad = assembler.getCachedCAD(player);
		if(!cad.isEmpty()) {
			color = 0xFFFFFF;

			int i = 0;
			ICAD cadItem = (ICAD) cad.getItem();
			String stats = I18n.get("psimisc.stats");
			String s = ChatFormatting.BOLD + stats;
			graphics.drawString(this.font, s, 213 - font.width(s) / 2f, 32, color, true);

			for(EnumCADStat stat : EnumCADStat.class.getEnumConstants()) {
				s = (Psi.magical ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA) + I18n.get(stat.getName()) + ChatFormatting.RESET + ": " + cadItem.getStatValue(cad, stat);
				graphics.drawString(this.font, s, 179, 45 + i * 10, color, true);
				i++;
			}
		}

		// 显示拆解进度信息
		if(assembler.isInDisassemblyMode()) {
			String progress = assembler.getDisassemblyProgress();
			graphics.drawString(font, progress, 10, 200, 0x404040, false);
		}

		// 显示错误提示信息（位于界面材质上方，确保完全可见）
		if(shouldShowError()) {
			// 计算提示信息的显示位置：界面材质上方中央位置
			int messageWidth = font.width(errorMessage);
			int messageX = (imageWidth - messageWidth) / 2; // 水平居中
			int messageY = -18; // 位于界面材质上方，确保不与界面元素重叠

			// 绘制半透明背景框，提升可读性和视觉层次
			graphics.fill(messageX - 6, messageY - 3, messageX + messageWidth + 6, messageY + font.lineHeight + 3, 0x90000000);

			// 绘制错误提示文字（红色，醒目且清晰可见）
			graphics.drawString(font, errorMessage, messageX, messageY, 0xFFFF4444, false);
		}
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		graphics.setColor(1F, 1F, 1F, 1F);
		int x = (width - imageWidth) / 2;
		int y = (height - imageHeight) / 2;
		graphics.blit(texture, x, y, 0, 0, imageWidth, imageHeight);

		for(int i = 0; i < 12; i++) {
			if(!assembler.isBulletSlotEnabled(i)) {
				graphics.blit(texture, x + 17 + i % 3 * 18, y + 57 + i / 3 * 18, 16, imageHeight, 16, 16);
			}
		}
	}

	@Override
	protected void containerTick() {
		super.containerTick();

		// 定期更新按钮状态
		if(disassemblyButton != null) {
			updateDisassemblyButton();
		}
	}
}
