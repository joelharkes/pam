package com.progressiveadventure.milestone;

import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.PlayerProgress;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Feeds code-driven milestones: distinct foods, distinct biomes and surviving the first night. */
public final class Trackers {
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

	public static void onDeath(ServerPlayer player) {
		SAW_NIGHT.remove(player.getUUID());
	}

	/** Runs once per second. */
	public static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			trackBiome(player);
			trackNight(player);
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
