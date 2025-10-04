/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.common.item.tool;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.items.ComponentItemHandler;

import vazkii.psi.api.PsiAPI;
import vazkii.psi.api.cad.IPsiBarDisplay;
import vazkii.psi.api.cad.ISocketable;
import vazkii.psi.api.internal.IPlayerData;
import vazkii.psi.api.spell.ISpellAcceptor;
import vazkii.psi.api.spell.Spell;
import vazkii.psi.common.Psi;
import vazkii.psi.common.util.DataComponentHelper;

import java.util.ArrayList;
import java.util.List;

public class ToolSocketable implements ICapabilityProvider<ItemCapability<?, Void>, Void, ToolSocketable>, ISocketable, IPsiBarDisplay, ISpellAcceptor {
	protected final ItemStack tool;
	private final ComponentItemHandler toolHandler;
	protected final int slots;

	public ToolSocketable(ItemStack tool, int slots) {
		this.tool = tool;
		this.slots = Mth.clamp(slots, 1, MAX_ASSEMBLER_SLOTS - 1);
		this.toolHandler = (ComponentItemHandler) tool.getCapability(Capabilities.ItemHandler.ITEM);
	}

	@Override
	public ToolSocketable getCapability(ItemCapability<?, Void> capability, Void facing) {
		if(capability == PsiAPI.SOCKETABLE_CAPABILITY
				|| capability == PsiAPI.PSI_BAR_DISPLAY_CAPABILITY
				|| capability == PsiAPI.SPELL_ACCEPTOR_CAPABILITY) {
			return this;
		}
		return null;
	}

	@Override
	public boolean isSocketSlotAvailable(int slot) {
		return slot < slots && slot >= 0;
	}

	@Override
	public List<Integer> getRadialMenuSlots() {
		List<Integer> list = new ArrayList<>();
		for(int i = 0; i < slots; i++) {
			list.add(i);
		}
		return list;
	}

	@Override
	public ItemStack getBulletInSocket(int slot) {
		return toolHandler.getStackInSlot(slot);
	}

	@Override
	public void setBulletInSocket(int slot, ItemStack bullet) {
		if(slot >= 0 && slot < slots) {
			toolHandler.setStackInSlot(slot, bullet);
		} else {
			Psi.logger.warn("Attempted to access invalid slot {} in ToolSocketable with {} slots", slot, slots);
		}
	}

	@Override
	public int getSelectedSlot() {
		return DataComponentHelper.getSelectedSlot(tool);
	}

	@Override
	public void setSelectedSlot(int slot) {
		DataComponentHelper.setSelectedSlot(tool, slot);
	}

	@Override
	public int getLastSlot() {
		return slots - 1;
	}

	@Override
	public boolean shouldShow(IPlayerData data) {
		return false;
	}

	@Override
	public void setSpell(Player player, Spell spell) {
		int slot = getSelectedSlot();
		ItemStack bullet = getBulletInSocket(slot);
		if(!bullet.isEmpty() && ISpellAcceptor.isAcceptor(bullet)) {
			ISpellAcceptor.acceptor(bullet).setSpell(player, spell);
			setBulletInSocket(slot, bullet);
		}
	}

}
