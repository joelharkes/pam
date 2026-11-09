package com.progressiveadventure.challenge;

import com.progressiveadventure.ProgressiveAdventure;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.milestone.Milestones;
import com.progressiveadventure.world.Elites;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * The Challenges tab: advancements for doing something well rather than often. Progress is per player and in memory
 * (a night, a fall, a streak), except creepers defused and attacks parried, statistics so they survive logging out.
 */
public final class Challenges {
	private static final int UNTOUCHABLE_KILLS = 10;
	private static final double LANDING_HEIGHT = 30;
	/** A bigger drop in one tick than any fall (about 4 blocks at full speed) is a teleport, not a fall. */
	private static final double TELEPORT_DROP = 5;
	private static final int DEFUSED_CREEPERS = 5;
	private static final int SHARPSHOOTER_HITS = 10;
	private static final int PERFECT_PARRIES = 25;
	/** Seconds outside, of the roughly 545 a night lasts (dark = when beds work): sleeping through it or joining late doesn't count. */
	private static final int GHOST_SECONDS = 400;
	/** Sky light at the feet from this level counts as outside: open sky gives 15, under a roof it drops off fast. */
	private static final int GHOST_SKY_LIGHT = 8;

	public static final Identifier CREEPERS_DEFUSED = ProgressiveAdventure.id("creepers_defused");
	public static final Identifier ATTACKS_PARRIED = ProgressiveAdventure.id("attacks_parried");

	private static final Map<UUID, State> STATES = new HashMap<>();
	/** Arrows that hit a mob: a piercing arrow landing in the ground afterwards isn't a miss. */
	private static final Set<AbstractArrow> HIT_ARROWS = Collections.newSetFromMap(new WeakHashMap<>());
	/** Raids where a villager died. Forgotten on restart (raids are loaded as new objects), which only makes it easier. */
	private static final Set<Raid> VILLAGER_DIED = Collections.newSetFromMap(new WeakHashMap<>());
	private static boolean wasDark;

	private static final class State {
		int nightKills;
		boolean hurtTonight;
		int ghostSeconds;
		boolean ghostFailed;
		/** Highest point since last standing, swimming, climbing, riding or flying; NaN when unknown. */
		double fallPeakY = Double.NaN;
		/** Hurt or slow falling during this fall. */
		boolean fallSpoiled;
		double lastY;
		ResourceKey<Level> lastLevel;
		int arrowStreak;
		/** Ender Dragons and Withers this player hit, and whether they've been unhurt since their first hit. */
		final Map<UUID, Boolean> bosses = new HashMap<>();
	}

	private Challenges() {
	}

	public static void init() {
		for (Identifier stat : List.of(CREEPERS_DEFUSED, ATTACKS_PARRIED)) {
			Registry.register(BuiltInRegistries.CUSTOM_STAT, stat, stat);
			Stats.CUSTOM.get(stat, StatFormatter.DEFAULT);
		}
	}

	public static void onServerStopped() {
		STATES.clear();
		VILLAGER_DIED.clear();
		wasDark = false;
	}

	public static void onDisconnect(ServerPlayer player) {
		STATES.remove(player.getUUID());
	}

	private static State state(ServerPlayer player) {
		return STATES.computeIfAbsent(player.getUUID(), uuid -> new State());
	}

	/** Creative and spectators can't get hurt, so nothing here counts for them. */
	private static boolean canEarn(ServerPlayer player) {
		return !player.isCreative() && !player.isSpectator();
	}

	private static boolean isNight(ServerPlayer player) {
		return player.level().getServer().overworld().isDarkOutside();
	}

	public static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			trackFall(player);
		}
		if (server.getTickCount() % 20 == 0) {
			trackNight(server);
		}
	}

	private static void trackNight(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		boolean dark = overworld.isDarkOutside();
		if (dark && !wasDark) {
			for (State state : STATES.values()) {
				state.nightKills = 0;
				state.hurtTonight = false;
				state.ghostSeconds = 0;
				state.ghostFailed = false;
			}
		} else if (!dark && wasDark) {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				State state = state(player);
				if (!state.ghostFailed && state.ghostSeconds >= GHOST_SECONDS) {
					Milestones.grant(player, Milestones.GHOST);
				}
			}
		}
		wasDark = dark;
		if (!dark || overworld.getDifficulty() == Difficulty.PEACEFUL) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.level() == overworld && player.isAlive() && canEarn(player) && !player.isSleeping()
					&& overworld.getBrightness(LightLayer.SKY, player.blockPosition()) >= GHOST_SKY_LIGHT) {
				state(player).ghostSeconds++;
			}
		}
	}

	/** Nailed the Landing: a fall of 30+ blocks that ends standing or in water, unhurt. */
	private static void trackFall(ServerPlayer player) {
		State state = state(player);
		double y = player.getY();
		boolean teleported = player.level().dimension() != state.lastLevel || state.lastY - y > TELEPORT_DROP;
		state.lastLevel = player.level().dimension();
		state.lastY = y;
		if (teleported || !player.isAlive() || !canEarn(player)) {
			state.fallPeakY = Double.NaN;
			return;
		}
		// Elytra and flying reset the start too, so only the drop after them counts
		boolean supported = player.onGround() || player.isInWater() || player.onClimbable() || player.isPassenger()
				|| player.isFallFlying() || player.getAbilities().flying;
		if (supported) {
			if (!state.fallSpoiled && state.fallPeakY - y >= LANDING_HEIGHT) {
				Milestones.grant(player, Milestones.NAILED_THE_LANDING);
			}
			state.fallPeakY = y;
			state.fallSpoiled = false;
		} else {
			state.fallPeakY = Double.isNaN(state.fallPeakY) ? y : Math.max(state.fallPeakY, y);
			if (player.hasEffect(MobEffects.SLOW_FALLING)) {
				state.fallSpoiled = true;
			}
		}
	}

	/** Damage that got through (or death). Fall damage arrives before the landing is seen, so it spoils the fall. */
	public static void onPlayerHurt(ServerPlayer player) {
		State state = state(player);
		state.fallSpoiled = true;
		if (isNight(player)) {
			state.hurtTonight = true;
			state.ghostFailed = true;
		}
		state.bosses.replaceAll((boss, unhurt) -> false);
	}

	/** Any hit a player lands, blocked or not. */
	public static void onAttack(ServerPlayer player, LivingEntity victim) {
		State state = state(player);
		if (isNight(player)) {
			state.ghostFailed = true;
		}
		if ((victim instanceof EnderDragon || victim instanceof WitherBoss) && canEarn(player)) {
			state.bosses.putIfAbsent(victim.getUUID(), true);
		}
	}

	public static void onKill(ServerPlayer player, LivingEntity victim) {
		onAttack(player, victim);
		if (!canEarn(player) || !(victim instanceof Enemy)) {
			return;
		}
		State state = state(player);
		if (player.getHealth() <= 2) {
			Milestones.grant(player, Milestones.COMEBACK);
		}
		if (Elites.isElite(victim) && wearsNoArmor(player)) {
			Milestones.grant(player, Milestones.BRAVE);
		}
		if (victim instanceof Creeper creeper && creeper.getSwelling(1) > 0) {
			player.awardStat(CREEPERS_DEFUSED);
			if (player.getStats().getValue(Stats.CUSTOM.get(CREEPERS_DEFUSED)) >= DEFUSED_CREEPERS) {
				Milestones.grant(player, Milestones.DEFUSED);
			}
		}
		if (player.level().dimension() == Level.OVERWORLD && isNight(player) && !state.hurtTonight
				&& ++state.nightKills >= UNTOUCHABLE_KILLS) {
			Milestones.grant(player, Milestones.UNTOUCHABLE);
		}
	}

	private static boolean wearsNoArmor(ServerPlayer player) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !player.getItemBySlot(slot).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/** Any death: bosses for Not a Scratch, villagers for Safe and Sound. */
	public static void onDeath(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return;
		}
		if (entity instanceof Villager) {
			Raid raid = level.getRaidAt(entity.blockPosition());
			if (raid != null) {
				VILLAGER_DIED.add(raid);
			}
		} else if (entity instanceof EnderDragon || entity instanceof WitherBoss) {
			for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
				if (Boolean.TRUE.equals(state(player).bosses.remove(entity.getUUID()))) {
					Milestones.grant(player, Milestones.NOT_A_SCRATCH);
				}
			}
		}
	}

	/** From RaidMixin, for each hero when a raid is won. */
	public static void onRaidWon(Raid raid, ServerPlayer player) {
		if (!VILLAGER_DIED.contains(raid) && canEarn(player)) {
			Milestones.grant(player, Milestones.SAFE_AND_SOUND);
		}
	}

	/** From ShieldPerks: a melee attack parried (needs the Parry perk). */
	public static void onParry(ServerPlayer player) {
		player.awardStat(ATTACKS_PARRIED);
		if (player.getStats().getValue(Stats.CUSTOM.get(ATTACKS_PARRIED)) >= PERFECT_PARRIES) {
			Milestones.grant(player, Milestones.PERFECT_PARRY);
		}
	}

	public static void onArrowHit(ServerPlayer player, AbstractArrow arrow) {
		HIT_ARROWS.add(arrow);
		if (++state(player).arrowStreak >= SHARPSHOOTER_HITS) {
			Milestones.grant(player, Milestones.SHARPSHOOTER);
		}
	}

	/** From AbstractArrowMixin. Ricochet's extra arrows don't count: the player didn't aim them. */
	public static void onArrowHitBlock(AbstractArrow arrow) {
		if (arrow.getOwner() instanceof ServerPlayer player && !HIT_ARROWS.contains(arrow)
				&& !arrow.hasAttached(PamAttachments.RICOCHET_ARROW)) {
			state(player).arrowStreak = 0;
		}
	}
}
