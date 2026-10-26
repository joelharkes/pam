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
 * @param eliteKills elite kills per entity type id (bestiary elite records)
 * @param eliteTraits traits of defeated elites, as "entity type id/trait id"
 */
public record PlayerProgress(Map<String, Integer> kills, Map<String, Integer> skillXp, Set<String> foodsEaten, Set<String> biomes,
		Set<String> perks, Set<String> crops, Set<String> bred, Map<String, Integer> eliteKills, Set<String> eliteTraits) {
	public static final PlayerProgress EMPTY = new PlayerProgress(Map.of(), Map.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
			Map.of(), Set.of());

	private static final Codec<Set<String>> STRING_SET = Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf);

	public static final Codec<PlayerProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("kills", Map.of()).forGetter(PlayerProgress::kills),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("skill_xp", Map.of()).forGetter(PlayerProgress::skillXp),
			STRING_SET.optionalFieldOf("foods_eaten", Set.of()).forGetter(PlayerProgress::foodsEaten),
			STRING_SET.optionalFieldOf("biomes", Set.of()).forGetter(PlayerProgress::biomes),
			STRING_SET.optionalFieldOf("perks", Set.of()).forGetter(PlayerProgress::perks),
			STRING_SET.optionalFieldOf("crops", Set.of()).forGetter(PlayerProgress::crops),
			STRING_SET.optionalFieldOf("bred", Set.of()).forGetter(PlayerProgress::bred),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("elite_kills", Map.of()).forGetter(PlayerProgress::eliteKills),
			STRING_SET.optionalFieldOf("elite_traits", Set.of()).forGetter(PlayerProgress::eliteTraits)
	).apply(instance, PlayerProgress::new));

	public int killsOf(String entityType) {
		return kills.getOrDefault(entityType, 0);
	}

	public int eliteKillsOf(String entityType) {
		return eliteKills.getOrDefault(entityType, 0);
	}

	public int xpOf(String skill) {
		return skillXp.getOrDefault(skill, 0);
	}

	public PlayerProgress withKills(String entityType, int amount) {
		Map<String, Integer> copy = new HashMap<>(kills);
		copy.merge(entityType, amount, Integer::sum);
		// A count of 0 (only from /pam kills) means not discovered, so it mustn't count as a defeated type
		copy.remove(entityType, 0);
		return new PlayerProgress(Map.copyOf(copy), skillXp, foodsEaten, biomes, perks, crops, bred, eliteKills, eliteTraits);
	}

	public PlayerProgress withSkillXp(String skill, int amount) {
		Map<String, Integer> copy = new HashMap<>(skillXp);
		copy.merge(skill, amount, Integer::sum);
		return new PlayerProgress(kills, Map.copyOf(copy), foodsEaten, biomes, perks, crops, bred, eliteKills, eliteTraits);
	}

	public PlayerProgress withFood(String item) {
		return new PlayerProgress(kills, skillXp, add(foodsEaten, item), biomes, perks, crops, bred, eliteKills, eliteTraits);
	}

	public PlayerProgress withBiome(String biome) {
		return new PlayerProgress(kills, skillXp, foodsEaten, add(biomes, biome), perks, crops, bred, eliteKills, eliteTraits);
	}

	public PlayerProgress withPerk(String perk) {
		return new PlayerProgress(kills, skillXp, foodsEaten, biomes, add(perks, perk), crops, bred, eliteKills, eliteTraits);
	}

	public PlayerProgress withoutPerks() {
		return new PlayerProgress(kills, skillXp, foodsEaten, biomes, Set.of(), crops, bred, eliteKills, eliteTraits);
	}

	public PlayerProgress withCrop(String crop) {
		return new PlayerProgress(kills, skillXp, foodsEaten, biomes, perks, add(crops, crop), bred, eliteKills, eliteTraits);
	}

	public PlayerProgress withBred(String entityType) {
		return new PlayerProgress(kills, skillXp, foodsEaten, biomes, perks, crops, add(bred, entityType), eliteKills, eliteTraits);
	}

	/** @param trait trait id, or null for an elite without one */
	public PlayerProgress withEliteKill(String entityType, String trait) {
		Map<String, Integer> copy = new HashMap<>(eliteKills);
		copy.merge(entityType, 1, Integer::sum);
		Set<String> traits = trait == null ? eliteTraits : add(eliteTraits, entityType + "/" + trait);
		return new PlayerProgress(kills, skillXp, foodsEaten, biomes, perks, crops, bred, Map.copyOf(copy), traits);
	}

	private static Set<String> add(Set<String> set, String value) {
		Set<String> copy = new HashSet<>(set);
		copy.add(value);
		return Set.copyOf(copy);
	}
}
