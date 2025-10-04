/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.api.cad;

import com.mojang.serialization.Codec;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.Locale;

/**
 * An Enum defining all types of CAD components.
 */
public enum EnumCADComponent {

	/**
	 * If you define an item using this component, it must implement ICADAssembly
	 */
	ASSEMBLY,
	CORE,
	SOCKET,
	BATTERY,
	/**
	 * If you define an item using this component, it must implement ICADColorizer
	 */
	DYE;

	public String getName() {
		return "psi.component." + name().toLowerCase(Locale.ROOT);
	}

	// 添加编解码器支持
	public static final Codec<EnumCADComponent> CODEC = Codec.stringResolver(
			component -> component.name().toLowerCase(Locale.ROOT),
			name -> {
				try {
					return EnumCADComponent.valueOf(name.toUpperCase(Locale.ROOT));
				} catch (IllegalArgumentException e) {
					return ASSEMBLY; // 默认值
				}
			}
	);

	public static final StreamCodec<FriendlyByteBuf, EnumCADComponent> STREAM_CODEC =
			StreamCodec.of(
					(buf, component) -> buf.writeEnum(component),
					buf -> buf.readEnum(EnumCADComponent.class)
			);

}
