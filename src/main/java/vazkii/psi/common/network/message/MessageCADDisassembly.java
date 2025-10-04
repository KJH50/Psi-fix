/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.common.network.message;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import org.jetbrains.annotations.NotNull;

import vazkii.psi.common.Psi;
import vazkii.psi.common.block.tile.CADDisassemblyHandler;
import vazkii.psi.common.block.tile.TileCADAssembler;

/**
 * CAD拆解操作网络消息
 */
public record MessageCADDisassembly(BlockPos pos, DisassemblyAction action) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<MessageCADDisassembly> TYPE = new CustomPacketPayload.Type<>(Psi.location("cad_disassembly"));

	public static final StreamCodec<FriendlyByteBuf, MessageCADDisassembly> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, MessageCADDisassembly::pos,
			DisassemblyAction.CODEC, MessageCADDisassembly::action,
			MessageCADDisassembly::new
	);

	@Override
	public @NotNull Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(MessageCADDisassembly message, IPayloadContext context) {
		context.enqueueWork(() -> {
			if(context.player() instanceof ServerPlayer player) {
				Level level = player.level();
				BlockEntity blockEntity = level.getBlockEntity(message.pos());

				if(blockEntity instanceof TileCADAssembler assembler) {
					CADDisassemblyHandler handler = new CADDisassemblyHandler(assembler);
					CADDisassemblyHandler.DisassemblyResult result;

					switch(message.action()) {
					case START:
						result = handler.performDirectDisassembly(assembler.getSocketableStack(), player);
						break;
					case NEXT_STEP:
						result = handler.performDirectDisassembly(assembler.getSocketableStack(), player);
						break;
					case RESET:
						handler.reset();
						result = new CADDisassemblyHandler.DisassemblyResult(true, "拆解状态已重置", null, null, CADDisassemblyHandler.DisassemblyState.IDLE);
						break;
					default:
						result = CADDisassemblyHandler.DisassemblyResult.failure("未知操作", CADDisassemblyHandler.DisassemblyState.ERROR);
					}

					if(result.success) {
						assembler.setChanged();
					} else {
						player.sendSystemMessage(net.minecraft.network.chat.Component.literal(result.message));
					}
				}
			}
		});
	}

	public enum DisassemblyAction {
		START("开始拆解"),
		NEXT_STEP("下一步"),
		RESET("重置");

		private final String description;

		DisassemblyAction(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}

		public static final StreamCodec<FriendlyByteBuf, DisassemblyAction> CODEC = StreamCodec.of(
				(buf, action) -> buf.writeEnum(action),
				buf -> buf.readEnum(DisassemblyAction.class)
		);
	}
}
