package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client pulled itself up a ledge (it already moved itself). The server checks the perk and resets fall distance. */
public record MantlePayload() implements CustomPacketPayload {
	public static final Type<MantlePayload> TYPE = new Type<>(ProgressiveAdventure.id("mantle"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MantlePayload> CODEC = StreamCodec.unit(new MantlePayload());

	@Override
	public Type<MantlePayload> type() {
		return TYPE;
	}
}
