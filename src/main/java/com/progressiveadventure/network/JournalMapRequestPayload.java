package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client looks at this area of its journal (in chunks, inclusive) and asks for the drawn chunks there that changed. */
public record JournalMapRequestPayload(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) implements CustomPacketPayload {
	public static final Type<JournalMapRequestPayload> TYPE = new Type<>(ProgressiveAdventure.id("journal_map_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, JournalMapRequestPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, JournalMapRequestPayload::minChunkX,
			ByteBufCodecs.VAR_INT, JournalMapRequestPayload::minChunkZ,
			ByteBufCodecs.VAR_INT, JournalMapRequestPayload::maxChunkX,
			ByteBufCodecs.VAR_INT, JournalMapRequestPayload::maxChunkZ,
			JournalMapRequestPayload::new);

	@Override
	public Type<JournalMapRequestPayload> type() {
		return TYPE;
	}
}
