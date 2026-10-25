package com.progressiveadventure.skill;

import com.progressiveadventure.data.PamAttachments;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Remembers player-placed blocks per chunk so breaking them gives no skill xp or perk loot (stops place/break farming).
 * Only blocks where that matters are remembered: logs, ores and farm blocks that give xp only when grown. The set is
 * changed in place (it isn't synced, so it doesn't need a new value) and forgets the oldest block when full.
 */
public final class PlacedBlocks {
	private static final int MAX_PER_CHUNK = 10_000;

	private PlacedBlocks() {
	}

	private static boolean isTracked(BlockState state) {
		return state.is(BlockTags.LOGS) || state.is(ConventionalBlockTags.ORES) || state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN)
				|| state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS) || state.is(Blocks.BAMBOO);
	}

	public static void mark(Level level, BlockPos pos) {
		if (!isTracked(level.getBlockState(pos))) {
			return;
		}
		LevelChunk chunk = level.getChunkAt(pos);
		LongLinkedOpenHashSet placed = chunk.getAttachedOrCreate(PamAttachments.PLACED_BLOCKS);
		if (placed.add(pos.asLong())) {
			if (placed.size() > MAX_PER_CHUNK) {
				placed.removeFirstLong();
			}
			chunk.markUnsaved();
		}
	}

	public static boolean isPlaced(Level level, BlockPos pos) {
		LongLinkedOpenHashSet placed = level.getChunkAt(pos).getAttached(PamAttachments.PLACED_BLOCKS);
		return placed != null && placed.contains(pos.asLong());
	}

	/** Forgets the block and returns whether it was player-placed. */
	public static boolean remove(Level level, BlockPos pos) {
		LevelChunk chunk = level.getChunkAt(pos);
		LongLinkedOpenHashSet placed = chunk.getAttached(PamAttachments.PLACED_BLOCKS);
		if (placed == null || !placed.remove(pos.asLong())) {
			return false;
		}
		chunk.markUnsaved();
		return true;
	}
}
