package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client asks to refund all perks. */
public record ResetPerksPayload() implements CustomPacketPayload {
	public static final Type<ResetPerksPayload> TYPE = new Type<>(ProgressiveAdventure.id("reset_perks"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ResetPerksPayload> CODEC = StreamCodec.unit(new ResetPerksPayload());

	@Override
	public Type<ResetPerksPayload> type() {
		return TYPE;
	}
}
