package com.progressiveadventure.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Derived progression values, recomputed on the server and synced to the owning player for the UI.
 *
 * @param rank              Adventure Rank: perk points earned from all completed advancements
 * @param maxHalfHearts     current max health in half hearts, including bonus heart perks
 * @param heartCap          max hearts reachable from milestones
 * @param stomach           stomach level
 * @param hungerDrain       hunger drain multiplier for the stomach level
 */
public record ProgressionSnapshot(int rank, int maxHalfHearts, int heartCap, int stomach, float hungerDrain) {
	public static final ProgressionSnapshot EMPTY = new ProgressionSnapshot(0, 0, 0, 0, 1f);

	public static final Codec<ProgressionSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("rank").forGetter(ProgressionSnapshot::rank),
			Codec.INT.fieldOf("max_half_hearts").forGetter(ProgressionSnapshot::maxHalfHearts),
			Codec.INT.fieldOf("heart_cap").forGetter(ProgressionSnapshot::heartCap),
			Codec.INT.fieldOf("stomach").forGetter(ProgressionSnapshot::stomach),
			Codec.FLOAT.fieldOf("hunger_drain").forGetter(ProgressionSnapshot::hungerDrain)
	).apply(instance, ProgressionSnapshot::new));
}
