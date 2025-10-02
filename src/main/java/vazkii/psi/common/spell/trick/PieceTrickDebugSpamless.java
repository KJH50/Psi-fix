/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.common.spell.trick;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import vazkii.psi.api.spell.*;
import vazkii.psi.api.spell.param.ParamAny;
import vazkii.psi.api.spell.param.ParamNumber;
import vazkii.psi.api.spell.piece.PieceTrick;
import vazkii.psi.common.network.MessageRegister;
import vazkii.psi.common.network.message.MessageSpamlessChat;

public class PieceTrickDebugSpamless extends PieceTrick {

	SpellParam<SpellParam.Any> target;
	SpellParam<Number> number;

	public PieceTrickDebugSpamless(Spell spell) {
		super(spell);
	}

	@Override
	public void initParams() {
		addParam(target = new ParamAny(SpellParam.GENERIC_NAME_TARGET, SpellParam.BLUE, false));
		addParam(number = new ParamNumber(SpellParam.GENERIC_NAME_NUMBER, SpellParam.RED, true, false));
	}

	@Override
	public void addToMetadata(SpellMetadata meta) {
		// NO-OP
	}

	@Override
	public Object execute(SpellContext context) throws SpellRuntimeException {
		Number numberVal = this.getParamValue(context, number);
		Object targetVal = getParamValue(context, target);

		Component component = createBaseComponent(targetVal);
		if(numberVal != null) {
			component = createNumberedComponent(numberVal, component);
		}

		sendToPlayer(context, component, numberVal);
		return null;
	}

	private Component createBaseComponent(Object targetVal) {
		return Component.literal(String.valueOf(targetVal));
	}

	private Component createNumberedComponent(Number numberVal, Component baseComponent) {
		String numStr = formatNumber(numberVal);

		return Component.literal("[" + numStr + "]")
				.setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA))
				.append(Component.literal(" ")
						.setStyle(Style.EMPTY.withColor(ChatFormatting.RESET)))
				.append(baseComponent.plainCopy().setStyle(Style.EMPTY.withColor(ChatFormatting.RESET)));
	}

	private String formatNumber(Number numberVal) {
		if(numberVal.doubleValue() - numberVal.intValue() == 0) {
			return String.valueOf(numberVal.intValue());
		}
		return String.valueOf(numberVal);
	}

	private void sendToPlayer(SpellContext context, Component component, Number numberVal) {
		if(context.caster instanceof ServerPlayer) {
			int messageId = numberVal == null ? -1 : numberVal.intValue();
			MessageSpamlessChat chatMessage = new MessageSpamlessChat(component, messageId);
			MessageRegister.sendToPlayer((ServerPlayer) context.caster, chatMessage);
		}
	}
}
