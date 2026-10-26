package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client clicked attack while blocking with a shield (vanilla ignores that click). The server checks the perk. */
public record ShieldBashPayload() implements CustomPacketPayload {
	public static final Type<ShieldBashPayload> TYPE = new Type<>(ProgressiveAdventure.id("shield_bash"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ShieldBashPayload> CODEC = StreamCodec.unit(new ShieldBashPayload());

	@Override
	public Type<ShieldBashPayload> type() {
		return TYPE;
	}
}
