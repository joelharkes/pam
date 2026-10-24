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
 * @param perks      ids of owned perks
 * @param crops      block ids of distinct crops harvested
 * @param bred       entity type ids of distinct animals bred
 */
public record PlayerProgress(Map<String, Integer> kills, Map<String, Integer> skillXp, Set<String> foodsEaten, Set<String> biomes,
		Set<String> perks, Set<String> crops, Set<String> bred) {
	public static final PlayerProgress EMPTY = new PlayerProgress(Map.of(), Map.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of());

	private static final Codec<Set<String>> STRING_SET = Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf);

	public static final Codec<PlayerProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("kills", Map.of()).forGetter(PlayerProgress::kills),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("skill_xp", Map.of()).forGetter(PlayerProgress::skillXp),
			STRING_SET.optionalFieldOf("foods_eaten", Set.of()).forGetter(PlayerProgress::foodsEaten),
			STRING_SET.optionalFieldOf("biomes", Set.of()).forGetter(PlayerProgress::biomes),
			STRING_SET.optionalFieldOf("perks", Set.of()).forGetter(PlayerProgress::perks),
			STRING_SET.optionalFieldOf("crops", Set.of()).forGetter(PlayerProgress::crops),
			STRING_SET.optionalFieldOf("bred", Set.of()).forGetter(PlayerProgress::bred)
	).apply(instance, PlayerProgress::new));

	public int killsOf(String entityType) {
		return kills.getOrDefault(entityType, 0);
	}

	public int xpOf(String skill) {
		return skillXp.getOrDefault(skill, 0);
	}

	public PlayerProgress withKills(String entityType, int amount) {
		Map<String, Integer> copy = new HashMap<>(kills);
		copy.merge(entityType, amount, Integer::sum);
		return new PlayerProgress(Map.copyOf(copy), skillXp, foodsEaten, biomes, perks, crops, bred);
	}

	public PlayerProgress withSkillXp(String skill, int amount) {
		Map<String, Integer> copy = new HashMap<>(skillXp);
		copy.merge(skill, amount, Integer::sum);
		return new PlayerProgress(kills, Map.copyOf(copy), foodsEaten, biomes, perks, crops, bred);
	}

	public PlayerProgress withFood(String item) {
		return new PlayerProgress(kills, skillXp, add(foodsEaten, item), biomes, perks, crops, bred);
	}

	public PlayerProgress withBiome(String biome) {
		return new PlayerProgress(kills, skillXp, foodsEaten, add(biomes, biome), perks, crops, bred);
	}

	public PlayerProgress withPerk(String perk) {
		return new PlayerProgress(kills, skillXp, foodsEaten, biomes, add(perks, perk), crops, bred);
	}

	public PlayerProgress withoutPerks() {
		return new PlayerProgress(kills, skillXp, foodsEaten, biomes, Set.of(), crops, bred);
	}

	public PlayerProgress withCrop(String crop) {
		return new PlayerProgress(kills, skillXp, foodsEaten, biomes, perks, add(crops, crop), bred);
	}

	public PlayerProgress withBred(String entityType) {
		return new PlayerProgress(kills, skillXp, foodsEaten, biomes, perks, crops, add(bred, entityType));
	}

	private static Set<String> add(Set<String> set, String value) {
		Set<String> copy = new HashSet<>(set);
		copy.add(value);
		return Set.copyOf(copy);
	}
}
