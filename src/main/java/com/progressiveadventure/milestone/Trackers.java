package com.progressiveadventure.milestone;

import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.PlayerProgress;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Feeds code-driven milestones: distinct foods, distinct biomes, surviving the first night and distance on foot. */
public final class Trackers {
	/** Spelunker: all three cave biomes. */
	private static final Set<String> CAVE_BIOMES = Set.of("minecraft:lush_caves", "minecraft:dripstone_caves", "minecraft:deep_dark");

	/** On-foot travel; riding, boats and elytra don't count. Climbing and falling neither (they're not travelling). */
	private static final List<Identifier> ON_FOOT_STATS = List.of(Stats.WALK_ONE_CM, Stats.SPRINT_ONE_CM, Stats.CROUCH_ONE_CM,
			Stats.SWIM_ONE_CM, Stats.WALK_ON_WATER_ONE_CM, Stats.WALK_UNDER_WATER_ONE_CM);

	/** Players who have been alive in the overworld while it was dark since their last death. */
	private static final Set<UUID> SAW_NIGHT = new HashSet<>();

	private Trackers() {
	}

	/** Forgets everything when the server stops: in singleplayer, the next world has the same player UUID. */
	public static void onServerStopped() {
		SAW_NIGHT.clear();
	}

	/** Logged out during the night: that night doesn't count as survived. */
	public static void onDisconnect(ServerPlayer player) {
		SAW_NIGHT.remove(player.getUUID());
	}

	public static void onFoodEaten(ServerPlayer player, ItemStack stack) {
		String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		PlayerProgress progress = PamAttachments.progress(player);
		if (!progress.foodsEaten().contains(item)) {
			progress = progress.withFood(item);
			PamAttachments.setProgress(player, progress);
		}
		Milestones.checkThresholds(player, Milestones.FOOD_THRESHOLDS, progress.foodsEaten().size());
	}

	/** From TradeTriggerMixin. Wandering traders have no profession, so only villagers count. */
	public static void onTrade(ServerPlayer player, AbstractVillager merchant) {
		if (!(merchant instanceof Villager villager)) {
			return;
		}
		VillagerData data = villager.getVillagerData();
		if (data.level() >= VillagerData.MAX_VILLAGER_LEVEL) {
			Milestones.grant(player, Milestones.MASTER_TRADER);
		}
		data.profession().unwrapKey().filter(key -> key != VillagerProfession.NONE && key != VillagerProfession.NITWIT).ifPresent(key -> {
			PlayerProgress progress = PamAttachments.progress(player);
			String profession = key.identifier().toString();
			if (!progress.tradedProfessions().contains(profession)) {
				progress = progress.withTradedProfession(profession);
				PamAttachments.setProgress(player, progress);
				Milestones.checkThresholds(player, Milestones.PROFESSION_THRESHOLDS, progress.tradedProfessions().size());
			}
		});
	}

	public static void onDeath(ServerPlayer player) {
		SAW_NIGHT.remove(player.getUUID());
	}

	/** Runs once per second. */
	public static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			trackBiome(player);
			trackNight(player);
			trackWalking(player);
		}
	}

	private static void trackBiome(ServerPlayer player) {
		player.level().getBiome(player.blockPosition()).unwrapKey().ifPresent(key -> {
			String biome = key.identifier().toString();
			PlayerProgress progress = PamAttachments.progress(player);
			if (!progress.biomes().contains(biome)) {
				progress = progress.withBiome(biome);
				PamAttachments.setProgress(player, progress);
				Milestones.checkThresholds(player, Milestones.BIOME_THRESHOLDS, progress.biomes().size());
			}
		});
		// Every second rather than on a new biome, so caves visited before Spelunker existed count
		if (PamAttachments.progress(player).biomes().containsAll(CAVE_BIOMES)) {
			Milestones.grant(player, Milestones.SPELUNKER);
		}
	}

	/** Vanilla statistics, so distance from before these advancements existed counts too. */
	private static void trackWalking(ServerPlayer player) {
		long centimetres = 0;
		for (Identifier stat : ON_FOOT_STATS) {
			centimetres += player.getStats().getValue(Stats.CUSTOM.get(stat));
		}
		Milestones.checkThresholds(player, Milestones.WALK_KM_THRESHOLDS, (int) (centimetres / 100_000));
	}

	private static void trackNight(ServerPlayer player) {
		Level level = player.level();
		if (level.dimension() != Level.OVERWORLD || !player.isAlive()) {
			return;
		}
		if (level.isDarkOutside()) {
			SAW_NIGHT.add(player.getUUID());
		} else if (SAW_NIGHT.remove(player.getUUID())) {
			Milestones.grant(player, Milestones.FIRST_NIGHT);
		}
	}
}
