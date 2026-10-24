package com.progressiveadventure.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Per-player progression. Immutable: attachments only sync to the client when re-set, so every change
 * returns a new instance that gets stored with {@code setAttached}.
 *
 * @param kills      kills per entity type id (bestiary)
 * @param skillXp    xp per skill id
 * @param foodsEaten item ids of distinct foods eaten
 * @param biomes     ids of distinct biomes visited
 */
public record PlayerProgress(Map<String, Integer> kills, Map<String, Integer> skillXp, Set<String> foodsEaten, Set<String> biomes) {
	public static final PlayerProgress EMPTY = new PlayerProgress(Map.of(), Map.of(), Set.of(), Set.of());

	public static final Codec<PlayerProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("kills", Map.of()).forGetter(PlayerProgress::kills),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("skill_xp", Map.of()).forGetter(PlayerProgress::skillXp),
			Codec.STRING.listOf().<Set<String>>xmap(Set::copyOf, List::copyOf).optionalFieldOf("foods_eaten", Set.of()).forGetter(PlayerProgress::foodsEaten),
			Codec.STRING.listOf().<Set<String>>xmap(Set::copyOf, List::copyOf).optionalFieldOf("biomes", Set.of()).forGetter(PlayerProgress::biomes)
	).apply(instance, PlayerProgress::new));

	public int killsOf(String entityType) {
		return kills.getOrDefault(entityType, 0);
	}

	public int xpOf(String skill) {
		return skillXp.getOrDefault(skill, 0);
	}

	public PlayerProgress withKill(String entityType) {
		Map<String, Integer> copy = new HashMap<>(kills);
		copy.merge(entityType, 1, Integer::sum);
		return new PlayerProgress(Map.copyOf(copy), skillXp, foodsEaten, biomes);
	}

	public PlayerProgress withSkillXp(String skill, int amount) {
		Map<String, Integer> copy = new HashMap<>(skillXp);
		copy.merge(skill, amount, Integer::sum);
		return new PlayerProgress(kills, Map.copyOf(copy), foodsEaten, biomes);
	}

	public PlayerProgress withFood(String item) {
		Set<String> copy = new HashSet<>(foodsEaten);
		copy.add(item);
		return new PlayerProgress(kills, skillXp, Set.copyOf(copy), biomes);
	}

	public PlayerProgress withBiome(String biome) {
		Set<String> copy = new HashSet<>(biomes);
		copy.add(biome);
		return new PlayerProgress(kills, skillXp, foodsEaten, Set.copyOf(copy));
	}
}
