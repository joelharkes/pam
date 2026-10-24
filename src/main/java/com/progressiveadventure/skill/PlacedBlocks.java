package com.progressiveadventure.skill;

import com.progressiveadventure.data.PamAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashSet;
import java.util.Set;

/** Remembers player-placed blocks per chunk so breaking them gives no skill xp (stops place/break farming). */
public final class PlacedBlocks {
	private PlacedBlocks() {
	}

	public static void mark(Level level, BlockPos pos) {
		LevelChunk chunk = level.getChunkAt(pos);
		Set<Long> placed = new HashSet<>(chunk.getAttachedOrCreate(PamAttachments.PLACED_BLOCKS));
		if (placed.add(pos.asLong())) {
			chunk.setAttached(PamAttachments.PLACED_BLOCKS, Set.copyOf(placed));
		}
	}

	/** Forgets the block and returns whether it was player-placed. */
	public static boolean remove(Level level, BlockPos pos) {
		LevelChunk chunk = level.getChunkAt(pos);
		Set<Long> placed = chunk.getAttached(PamAttachments.PLACED_BLOCKS);
		if (placed == null || !placed.contains(pos.asLong())) {
			return false;
		}
		Set<Long> copy = new HashSet<>(placed);
		copy.remove(pos.asLong());
		chunk.setAttached(PamAttachments.PLACED_BLOCKS, Set.copyOf(copy));
		return true;
	}
}
