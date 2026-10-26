package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client started a Ground Pound dive (it already moves itself down). The server checks the perk and handles the landing. */
public record GroundPoundPayload() implements CustomPacketPayload {
	public static final Type<GroundPoundPayload> TYPE = new Type<>(ProgressiveAdventure.id("ground_pound"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GroundPoundPayload> CODEC = StreamCodec.unit(new GroundPoundPayload());

	@Override
	public Type<GroundPoundPayload> type() {
		return TYPE;
	}
}
