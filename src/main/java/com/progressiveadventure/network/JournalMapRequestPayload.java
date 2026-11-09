package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client looks at its journal centred on this chunk and asks for the drawn chunks around it that changed. */
public record JournalMapRequestPayload(int chunkX, int chunkZ) implements CustomPacketPayload {
	public static final Type<JournalMapRequestPayload> TYPE = new Type<>(ProgressiveAdventure.id("journal_map_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, JournalMapRequestPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, JournalMapRequestPayload::chunkX,
			ByteBufCodecs.VAR_INT, JournalMapRequestPayload::chunkZ,
			JournalMapRequestPayload::new);

	@Override
	public Type<JournalMapRequestPayload> type() {
		return TYPE;
	}
}
