package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** First kill of a mob type: the client shows a "new bestiary entry" toast. */
public record BestiaryDiscoveredPayload(String entityType) implements CustomPacketPayload {
	public static final Type<BestiaryDiscoveredPayload> TYPE = new Type<>(ProgressiveAdventure.id("bestiary_discovered"));

	public static final StreamCodec<RegistryFriendlyByteBuf, BestiaryDiscoveredPayload> CODEC =
			ByteBufCodecs.STRING_UTF8.map(BestiaryDiscoveredPayload::new, BestiaryDiscoveredPayload::entityType).cast();

	@Override
	public Type<BestiaryDiscoveredPayload> type() {
		return TYPE;
	}
}
