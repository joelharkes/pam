package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client opened a mob's bestiary page and asks for what only the server knows (drops, unlock rank, elite traits). */
public record BestiaryInfoRequestPayload(String entityType) implements CustomPacketPayload {
	public static final Type<BestiaryInfoRequestPayload> TYPE = new Type<>(ProgressiveAdventure.id("bestiary_info_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, BestiaryInfoRequestPayload> CODEC =
			ByteBufCodecs.STRING_UTF8.map(BestiaryInfoRequestPayload::new, BestiaryInfoRequestPayload::entityType).cast();

	@Override
	public Type<BestiaryInfoRequestPayload> type() {
		return TYPE;
	}
}
