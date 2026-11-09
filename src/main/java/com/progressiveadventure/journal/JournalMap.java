package com.progressiveadventure.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The one shared Overworld map every Explorer's Journal draws into: per chunk, 16×16 vanilla packed map colours
 * (index z * 16 + x, 0 = not drawn). Mutable and not synced through the attachment (clients ask for the part they
 * look at), so the immutable-value rule doesn't apply. Versions only exist in memory: they tell which chunks changed
 * since they were last sent to a player.
 */
public final class JournalMap {
	public static final int CHUNK_PIXELS = 16 * 16;

	private record StoredChunk(long pos, ByteBuffer colors) {
		static final Codec<StoredChunk> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.LONG.fieldOf("pos").forGetter(StoredChunk::pos),
				Codec.BYTE_BUFFER.fieldOf("colors").forGetter(StoredChunk::colors)
		).apply(instance, StoredChunk::new));
	}

	public static final Codec<JournalMap> CODEC = StoredChunk.CODEC.listOf().xmap(JournalMap::fromStored, JournalMap::toStored);

	private final Long2ObjectMap<byte[]> chunks = new Long2ObjectOpenHashMap<>();
	private final Long2IntOpenHashMap versions = new Long2IntOpenHashMap();
	private int nextVersion = 1;

	private static JournalMap fromStored(List<StoredChunk> stored) {
		JournalMap map = new JournalMap();
		for (StoredChunk chunk : stored) {
			byte[] colors = new byte[CHUNK_PIXELS];
			ByteBuffer buffer = chunk.colors().duplicate();
			buffer.get(colors, 0, Math.min(CHUNK_PIXELS, buffer.remaining()));
			map.put(chunk.pos(), colors);
		}
		return map;
	}

	private List<StoredChunk> toStored() {
		List<StoredChunk> stored = new ArrayList<>(chunks.size());
		chunks.long2ObjectEntrySet().forEach(entry -> stored.add(new StoredChunk(entry.getLongKey(), ByteBuffer.wrap(entry.getValue()))));
		return stored;
	}

	public boolean isDrawn(long chunkPos) {
		return chunks.containsKey(chunkPos);
	}

	public byte[] get(long chunkPos) {
		return chunks.get(chunkPos);
	}

	/** 0 when the chunk isn't drawn. */
	public int version(long chunkPos) {
		return versions.get(chunkPos);
	}

	/** Stores the chunk's colours; only a real change gets a new version (so it's sent again). */
	public void put(long chunkPos, byte[] colors) {
		byte[] old = chunks.get(chunkPos);
		if (old != null && Arrays.equals(old, colors)) {
			return;
		}
		chunks.put(chunkPos, colors);
		versions.put(chunkPos, nextVersion++);
	}
}
