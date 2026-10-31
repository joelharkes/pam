package com.progressiveadventure.bestiary;

import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.config.PamConfig.FamiliarityTier;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.PlayerProgress;
import com.progressiveadventure.milestone.Milestone;
import com.progressiveadventure.milestone.Milestones;
import com.progressiveadventure.mixin.CombatTrackerAccessor;
import com.progressiveadventure.network.BestiaryDiscoveredPayload;
import com.progressiveadventure.perk.Perks;
import com.progressiveadventure.world.EliteTrait;
import com.progressiveadventure.world.Elites;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mob familiarity: kills per mob type change the damage you deal to that type. Higher tiers also show more in
 * the bestiary (stats and tips, then drops, then exact drop chances), and Master gives a chance of double loot.
 */
public final class Bestiary {
	/** Tier labels (see the familiarityTiers config) that unlock something in the bestiary. */
	public static final String KNOWN = "known";
	public static final String FAMILIAR = "familiar";
	public static final String EXPERT = "expert";
	public static final String MASTER = "master";

	/** Defeat every mob in the list to complete the milestone. */
	public record Collection(Milestone milestone, List<String> mobs) {
	}

	public static final List<Collection> COLLECTIONS = List.of(
			new Collection(Milestones.NIGHT_WATCH, List.of("minecraft:zombie", "minecraft:skeleton", "minecraft:spider", "minecraft:creeper")),
			new Collection(Milestones.OCEAN_DEEP, List.of("minecraft:drowned", "minecraft:guardian", "minecraft:zombie_nautilus")),
			new Collection(Milestones.RAIDERS, List.of("minecraft:pillager", "minecraft:vindicator", "minecraft:evoker",
					"minecraft:ravager", "minecraft:witch")),
			new Collection(Milestones.NETHER_HUNTER, List.of("minecraft:zombified_piglin", "minecraft:ghast", "minecraft:magma_cube",
					"minecraft:blaze", "minecraft:wither_skeleton", "minecraft:hoglin", "minecraft:piglin")),
			new Collection(Milestones.END_HUNTER, List.of("minecraft:enderman", "minecraft:endermite", "minecraft:shulker")));

	private static final Set<String> ALL_TRAIT_IDS = Arrays.stream(EliteTrait.values()).map(trait -> trait.id).collect(Collectors.toSet());

	private Bestiary() {
	}

	public static String key(EntityType<?> type) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
	}

	/** Used when the config has no tier for this few kills (its first tier starts above 0, or the list is empty). */
	private static final FamiliarityTier NO_TIER = new FamiliarityTier(0, 0.0, "unknown");

	/** The highest tier reached with this many kills; never null. */
	public static FamiliarityTier tier(int kills) {
		FamiliarityTier result = NO_TIER;
		for (FamiliarityTier tier : PamConfig.get().familiarityTiers) {
			if (kills >= tier.kills()) {
				result = tier;
			}
		}
		return result;
	}

	/** Kills needed for the first tier with this label, or MAX_VALUE if the config has none. */
	public static int killsFor(String label) {
		for (FamiliarityTier tier : PamConfig.get().familiarityTiers) {
			if (tier.label().equals(label)) {
				return tier.kills();
			}
		}
		return Integer.MAX_VALUE;
	}

	public static boolean reached(int kills, String label) {
		return kills >= killsFor(label);
	}

	public static float damageMultiplier(ServerPlayer player, LivingEntity target) {
		FamiliarityTier tier = tier(PamAttachments.progress(player).killsOf(key(target.getType())));
		return 1f + (float) tier.damageModifier();
	}

	/** A player got the kill. With assistKills, everyone who hurt the mob during the fight also gets a bestiary kill. */
	public static void onKill(ServerPlayer killer, LivingEntity killed, DamageSource source) {
		if (killed instanceof Player) {
			return;
		}
		fighters(killer, killed).forEach(player -> addKill(player, killed));

		// Only the one who landed the last hit, like vanilla loot
		if (reached(PamAttachments.progress(killer).killsOf(key(killed.getType())), MASTER) && killed.level() instanceof ServerLevel level
				&& !killed.isBaby() && level.getGameRules().get(GameRules.MOB_DROPS)
				&& level.getRandom().nextDouble() < PamConfig.get().masterDoubleLootChance) {
			killed.getLootTable().ifPresent(lootTable -> killed.dropFromLootTable(level, source, true, lootTable));
		}
	}

	/** The killer and, with assistKills, everyone who hurt the mob during the fight and is still online in its dimension. */
	public static Set<ServerPlayer> fighters(ServerPlayer killer, LivingEntity killed) {
		Set<ServerPlayer> players = new LinkedHashSet<>();
		players.add(killer);
		if (PamConfig.get().assistKills) {
			for (CombatEntry entry : ((CombatTrackerAccessor) killed.getCombatTracker()).pam$entries()) {
				if (entry.source().getEntity() instanceof ServerPlayer hitter) {
					// The current player object: a hitter who died and respawned since has a new one
					ServerPlayer player = killer.level().getServer().getPlayerList().getPlayer(hitter.getUUID());
					if (player != null && player.level() == killed.level()) {
						players.add(player);
					}
				}
			}
		}
		return players;
	}

	private static void addKill(ServerPlayer player, LivingEntity killed) {
		String key = key(killed.getType());
		PlayerProgress progress = PamAttachments.progress(player);
		int before = progress.killsOf(key);
		boolean elite = Elites.isElite(killed);
		int kills = (Perks.has(player, Perks.QUICK_STUDY) ? 2 : 1) * (elite ? PamConfig.get().eliteBestiaryKills : 1);
		progress = progress.withKills(key, kills);
		if (elite) {
			EliteTrait trait = Elites.trait(killed);
			progress = progress.withEliteKill(key, trait == null ? null : trait.id);
		}
		PamAttachments.setProgress(player, progress);

		FamiliarityTier oldTier = tier(before);
		FamiliarityTier newTier = tier(progress.killsOf(key));
		if (before == 0) {
			ServerPlayNetworking.send(player, new BestiaryDiscoveredPayload(key));
		} else if (newTier != oldTier) {
			player.sendOverlayMessage(Component.translatable("message.progressive_adventure.bestiary_tier",
					killed.getType().getDescription(), Component.translatable("familiarity.progressive_adventure." + newTier.label()),
					formatModifier(newTier.damageModifier())).withStyle(ChatFormatting.YELLOW));
		}

		Milestones.checkThresholds(player, Milestones.MOB_TYPE_THRESHOLDS, progress.kills().size());
		checkCollections(player, progress);
	}

	/** For testing (/pam kills): sets the kill count of a mob type. */
	public static void setKills(ServerPlayer player, EntityType<?> type, int count) {
		String key = key(type);
		PlayerProgress progress = PamAttachments.progress(player);
		progress = progress.withKills(key, count - progress.killsOf(key));
		PamAttachments.setProgress(player, progress);
		Milestones.checkThresholds(player, Milestones.MOB_TYPE_THRESHOLDS, progress.kills().size());
		checkCollections(player, progress);
	}

	/** Collections, first Master and all elite traits. Also checked on join, for kills from before these existed. */
	public static void checkCollections(ServerPlayer player, PlayerProgress progress) {
		for (Collection collection : COLLECTIONS) {
			if (collection.mobs().stream().allMatch(mob -> progress.killsOf(mob) > 0)) {
				Milestones.grant(player, collection.milestone());
			}
		}
		if (progress.kills().values().stream().anyMatch(kills -> reached(kills, MASTER))) {
			Milestones.grant(player, Milestones.FIRST_MASTER);
		}
		Set<String> traits = progress.eliteTraits().stream().map(entry -> entry.substring(entry.indexOf('/') + 1)).collect(Collectors.toSet());
		if (traits.containsAll(ALL_TRAIT_IDS)) {
			Milestones.grant(player, Milestones.ALL_TRAITS);
		}
	}

	public static String formatModifier(double modifier) {
		long percent = Math.round(modifier * 100);
		return (percent >= 0 ? "+" : "") + percent + "%";
	}
}
