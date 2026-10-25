package com.progressiveadventure.world;

import com.progressiveadventure.ProgressiveAdventure;
import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.config.PamConfig.MobUnlock;
import com.progressiveadventure.data.PamAttachments;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Overworld hostile mobs unlock one group at a time as Adventure Rank goes up. Near several players, the lowest
 * rank counts. Only natural spawns from the biome list are affected: structures, spawners and other mods' mobs
 * (unless listed in the config) are left alone.
 */
public final class MobUnlocks {
	/**
	 * Players within this distance of a spawn count. Fixed, not in the config: it matches vanilla's despawn distance
	 * (mobs only spawn this close to a player), so a smaller value would let far spawns skip the lock (nobody "near")
	 * and a bigger one would only let a far-away player keep the area easy.
	 */
	private static final double PLAYER_RADIUS = 128;

	/** Rank needed per locked mob type, built from the config once mobs are registered (asked on every spawn attempt). */
	private static Map<EntityType<?>, Integer> requiredRanks;

	private MobUnlocks() {
	}

	/** At server start: builds the rank table and logs config entries that don't name a known mob (typos would otherwise silently do nothing). */
	public static void loadConfig() {
		Map<EntityType<?>, Integer> ranks = new IdentityHashMap<>();
		for (MobUnlock unlock : PamConfig.get().mobUnlocks) {
			for (String mob : unlock.mobs()) {
				Identifier id = Identifier.tryParse(mob);
				Optional<EntityType<?>> type = id == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(id);
				if (type.isEmpty()) {
					ProgressiveAdventure.LOGGER.warn("Unknown mob '{}' in mob unlock '{}'", mob, unlock.name());
				} else {
					// First group that lists a mob wins, like before
					ranks.putIfAbsent(type.get(), unlock.rank());
				}
			}
		}
		requiredRanks = ranks;
	}

	/** Rank needed before this mob spawns naturally, or 0 if it's never locked. */
	public static int requiredRank(EntityType<?> type) {
		if (requiredRanks == null) {
			loadConfig();
		}
		return requiredRanks.getOrDefault(type, 0);
	}

	/** Lowest Adventure Rank among non-spectator players near the position, or MAX_VALUE if nobody is near. */
	public static int lowestRankNear(ServerLevel level, BlockPos pos) {
		// Measured like vanilla's spawn check, which needs a player this close, so every natural spawn has one
		Vec3 spawnPoint = Vec3.atBottomCenterOf(pos);
		int lowest = Integer.MAX_VALUE;
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && player.distanceToSqr(spawnPoint) <= PLAYER_RADIUS * PLAYER_RADIUS) {
				lowest = Math.min(lowest, PamAttachments.snapshot(player).rank());
			}
		}
		return lowest;
	}

	public static boolean isLockedAt(ServerLevel level, EntityType<?> type, BlockPos pos) {
		if (!PamConfig.get().mobUnlocksEnabled || level.dimension() != Level.OVERWORLD) {
			return false;
		}
		int required = requiredRank(type);
		return required > 0 && lowestRankNear(level, pos) < required;
	}

	/**
	 * Filters the mob vanilla picked for a natural spawn. A locked mob is dropped (fewer mobs), or with the
	 * configured chance replaced by an unlocked mob from the same biome.
	 */
	public static Optional<SpawnerData> filterNaturalSpawn(ServerLevel level, StructureManager structureManager, ChunkGenerator generator,
			MobCategory category, RandomSource random, BlockPos pos, SpawnerData picked) {
		if (category != MobCategory.MONSTER || !isLockedAt(level, picked.type(), pos)) {
			return Optional.of(picked);
		}
		Holder<Biome> biome = level.getBiome(pos);
		WeightedList<SpawnerData> biomeMobs = biome.value().getMobSettings().getMobs(category);
		// Inside a structure with its own spawn list (witch hut, outpost, monument...): always vanilla
		if (generator.getMobsAt(biome, structureManager, category, pos) != biomeMobs) {
			return Optional.of(picked);
		}
		if (random.nextDouble() >= PamConfig.get().lockedMobReplacementChance) {
			return Optional.empty();
		}
		List<Weighted<SpawnerData>> unlocked = biomeMobs.unwrap().stream()
				.filter(entry -> !isLockedAt(level, entry.value().type(), pos))
				.toList();
		return WeightedList.of(unlocked).getRandom(random);
	}

	/** The next group to unlock after this rank, if any. */
	public static Optional<MobUnlock> next(int rank) {
		if (!PamConfig.get().mobUnlocksEnabled) {
			return Optional.empty();
		}
		return PamConfig.get().mobUnlocks.stream()
				.filter(unlock -> unlock.rank() > rank)
				.min(Comparator.comparingInt(MobUnlock::rank));
	}

	/** Tells the player about every group unlocked by going from oldRank to newRank. */
	public static void announce(ServerPlayer player, int oldRank, int newRank) {
		if (!PamConfig.get().mobUnlocksEnabled) {
			return;
		}
		PamConfig.get().mobUnlocks.stream()
				.filter(unlock -> unlock.rank() > oldRank && unlock.rank() <= newRank)
				.sorted(Comparator.comparingInt(MobUnlock::rank))
				.forEach(unlock -> player.sendSystemMessage(Component.translatable("message.progressive_adventure.mob_unlocked",
						groupName(unlock.name())).withStyle(ChatFormatting.DARK_RED)));
	}

	public static Component groupName(String name) {
		return Component.translatable("mob_group.progressive_adventure." + name);
	}
}
