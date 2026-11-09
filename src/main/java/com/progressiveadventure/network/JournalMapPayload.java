package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;
import com.progressiveadventure.journal.JournalMap;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Server sends drawn journal chunks (packed chunk position + 16×16 map colours) the client doesn't have yet. */
public record JournalMapPayload(List<Chunk> chunks) implements CustomPacketPayload {
	public static final Type<JournalMapPayload> TYPE = new Type<>(ProgressiveAdventure.id("journal_map"));

	public record Chunk(long pos, byte[] colors) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Chunk> CODEC = StreamCodec.composite(
				ByteBufCodecs.LONG, Chunk::pos,
				ByteBufCodecs.byteArray(JournalMap.CHUNK_PIXELS), Chunk::colors,
				Chunk::new);
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, JournalMapPayload> CODEC =
			Chunk.CODEC.apply(ByteBufCodecs.list()).map(JournalMapPayload::new, JournalMapPayload::chunks);

	@Override
	public Type<JournalMapPayload> type() {
		return TYPE;
	}
}
