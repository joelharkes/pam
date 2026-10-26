package com.progressiveadventure.perk;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A per-player perk cooldown in game time (Second Wind, Hearty Kill, Shield Bash). All cooldowns are forgotten when the
 * server stops (game time differs per world); on logout only finished ones are dropped, so relogging doesn't reset one.
 */
public final class Cooldown {
	private static final List<Cooldown> ALL = new ArrayList<>();

	private final Map<UUID, Long> readyAt = new HashMap<>();

	private Cooldown() {
	}

	public static Cooldown create() {
		Cooldown cooldown = new Cooldown();
		ALL.add(cooldown);
		return cooldown;
	}

	/** If the player's cooldown is over, starts a new one of this many ticks and returns true. */
	public boolean tryUse(ServerPlayer player, long ticks) {
		long now = player.level().getGameTime();
		Long ready = readyAt.get(player.getUUID());
		if (ready != null && now < ready) {
			return false;
		}
		readyAt.put(player.getUUID(), now + ticks);
		return true;
	}

	public static void onServerStopped() {
		for (Cooldown cooldown : ALL) {
			cooldown.readyAt.clear();
		}
	}

	public static void onDisconnect(ServerPlayer player) {
		long now = player.level().getGameTime();
		for (Cooldown cooldown : ALL) {
			Long ready = cooldown.readyAt.get(player.getUUID());
			if (ready != null && now >= ready) {
				cooldown.readyAt.remove(player.getUUID());
			}
		}
	}
}
