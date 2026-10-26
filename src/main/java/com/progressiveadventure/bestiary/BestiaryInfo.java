package com.progressiveadventure.bestiary;

import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.network.BestiaryInfoPayload;
import com.progressiveadventure.network.BestiaryInfoPayload.Drop;
import com.progressiveadventure.world.EliteTrait;
import com.progressiveadventure.world.MobUnlocks;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Answers the bestiary page's request: drop chances (sampled from the loot table), unlock rank and elite traits. */
public final class BestiaryInfo {
	/** Loot table rolls per mob type; enough to see a 1% drop about 10 times. */
	private static final int SAMPLES = 1000;

	/** Sampled drops per mob type; loot tables only change on /reload, which clears this. */
	private static final Map<EntityType<?>, List<Drop>> DROPS = new IdentityHashMap<>();

	private BestiaryInfo() {
	}

	public static void clearCache() {
		DROPS.clear();
	}

	public static void onRequest(ServerPlayer player, String entityType) {
		Identifier id = Identifier.tryParse(entityType);
		Optional<EntityType<?>> found = id == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(id);
		if (found.isEmpty()) {
			return;
		}
		EntityType<?> type = found.get();
		ServerLevel level = player.level();
		PamConfig config = PamConfig.get();
		// A throwaway mob (never added to the world) for its loot table and which traits it can have
		Entity sample = type.create(level, EntitySpawnReason.LOAD);

		List<Drop> drops = DROPS.get(type);
		if (drops == null) {
			drops = sampleDrops(level, player, sample);
			DROPS.put(type, drops);
		}
		boolean canBeElite = config.elitesEnabled && config.eliteMobs.contains(entityType) && sample instanceof Mob;
		List<String> traits = canBeElite
				? Arrays.stream(EliteTrait.values()).filter(trait -> trait.canHave((Mob) sample)).map(trait -> trait.id).toList()
				: List.of();
		int unlockRank = config.mobUnlocksEnabled ? MobUnlocks.requiredRank(type) : 0;
		ServerPlayNetworking.send(player, new BestiaryInfoPayload(entityType, unlockRank, drops, traits, canBeElite,
				(float) config.masterDoubleLootChance));
	}

	/**
	 * Rolls the loot table as a player kill without Looting (no attacker weapon) and counts how often each item
	 * shows up. Sampling handles every condition the same way the game does, also for other mods' mobs.
	 */
	private static List<Drop> sampleDrops(ServerLevel level, ServerPlayer player, Entity sample) {
		if (sample == null) {
			return List.of();
		}
		Optional<ResourceKey<LootTable>> key = sample.getLootTable();
		if (key.isEmpty()) {
			return List.of();
		}
		LootTable table = level.getServer().reloadableRegistries().getLootTable(key.get());
		LootParams params = new LootParams.Builder(level)
				.withParameter(LootContextParams.THIS_ENTITY, sample)
				.withParameter(LootContextParams.ORIGIN, player.position())
				.withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
				.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
				.create(LootContextParamSets.ENTITY);

		Map<Item, ItemStack> icons = new LinkedHashMap<>();
		Map<Item, Integer> hits = new IdentityHashMap<>();
		for (int i = 0; i < SAMPLES; i++) {
			Set<Item> seen = new HashSet<>();
			for (ItemStack stack : table.getRandomItems(params)) {
				if (!stack.isEmpty() && seen.add(stack.getItem())) {
					icons.putIfAbsent(stack.getItem(), stack.copyWithCount(1));
					hits.merge(stack.getItem(), 1, Integer::sum);
				}
			}
		}
		List<Drop> drops = new ArrayList<>();
		icons.forEach((item, icon) -> drops.add(new Drop(icon, hits.get(item) / (float) SAMPLES)));
		drops.sort(Comparator.comparingDouble(Drop::chance).reversed());
		return List.copyOf(drops);
	}
}
