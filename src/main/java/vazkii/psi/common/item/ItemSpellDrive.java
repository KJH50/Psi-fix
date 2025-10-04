/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.NotNull;

import vazkii.psi.api.internal.VanillaPacketDispatcher;
import vazkii.psi.api.spell.Spell;
import vazkii.psi.common.block.tile.TileProgrammer;
import vazkii.psi.common.util.DataComponentHelper;
import vazkii.psi.common.util.MessageHelper;
import vazkii.psi.common.util.SoundHelper;

public class ItemSpellDrive extends Item {

	public ItemSpellDrive(Item.Properties properties) {
		super(properties.stacksTo(1));
	}

	public static void setSpell(ItemStack stack, Spell spell) {
		DataComponentHelper.setSpell(stack, spell);
	}

	public static Spell getSpell(ItemStack stack) {
		return DataComponentHelper.getSpell(stack);
	}

	@NotNull
	@Override
	public Component getName(ItemStack stack) {
		String name = super.getName(stack).getString();
		Spell cmp = DataComponentHelper.getSpell(stack);
		String spellName = cmp.name;
		if(spellName.isEmpty()) {
			return Component.literal(name);
		}

		return Component.literal(name + " (" + ChatFormatting.GREEN + spellName + ChatFormatting.RESET + ")");
	}

	@NotNull
	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Player playerIn = ctx.getPlayer();
		InteractionHand hand = ctx.getHand();
		Level worldIn = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		ItemStack stack = playerIn.getItemInHand(hand);
		BlockEntity tile = worldIn.getBlockEntity(pos);
		if(tile instanceof TileProgrammer programmer) {
			Spell spell = getSpell(stack);
			if(spell == null && programmer.canCompile()) {
				setSpell(stack, programmer.spell);
				if(!worldIn.isClientSide) {
					SoundHelper.playBulletCreateSound(worldIn, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundSource.PLAYERS);
				}
				return InteractionResult.SUCCESS;
			} else if(spell != null) {
				boolean enabled = programmer.isEnabled();
				if(enabled && !programmer.playerLock.isEmpty()) {
					if(!programmer.playerLock.equals(playerIn.getName().getString())) {
						if(!worldIn.isClientSide) {
							MessageHelper.sendErrorMessage(playerIn, "psimisc.not_your_programmer");
						}
						return InteractionResult.SUCCESS;
					}
				} else {
					programmer.playerLock = playerIn.getName().getString();
				}

				programmer.spell = spell;
				programmer.onSpellChanged();
				if(!worldIn.isClientSide) {
					SoundHelper.playBulletCreateSound(worldIn, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundSource.PLAYERS);
					VanillaPacketDispatcher.dispatchTEToNearbyPlayers(programmer);
				}
				return InteractionResult.SUCCESS;
			}
		}

		return InteractionResult.PASS;
	}

	@NotNull
	@Override
	public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, @NotNull InteractionHand hand) {
		ItemStack itemStackIn = playerIn.getItemInHand(hand);
		if(getSpell(itemStackIn) != null && playerIn.isShiftKeyDown()) {
			if(!worldIn.isClientSide) {
				SoundHelper.playCompileErrorSound(worldIn, playerIn);
			} else {
				playerIn.swing(hand);
			}
			setSpell(itemStackIn, null);

			return new InteractionResultHolder<>(InteractionResult.SUCCESS, itemStackIn);
		}

		return new InteractionResultHolder<>(InteractionResult.PASS, itemStackIn);
	}

}
