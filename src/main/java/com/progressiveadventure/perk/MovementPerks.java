package com.progressiveadventure.perk;

import com.progressiveadventure.config.PamConfig;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server side of Shadow Step, Double Jump and Mantle. Double Jump and Mantle move the player on the client (movement
 * is client-driven); the server only checks the perk, allows one of each per jump, resets fall distance and shows the
 * effect to others.
 */
public final class MovementPerks {
	/** Mobs that lost a sneaking player to Shadow Step II: who they lost and until which game tick. */
	private static final Map<Mob, Forgotten> FORGOTTEN = new WeakHashMap<>();
	private static final Set<UUID> DOUBLE_JUMPED = new HashSet<>();
	private static final Set<UUID> MANTLED = new HashSet<>();

	private record Forgotten(UUID player, long untilTick) {
	}

	private MovementPerks() {
	}

	/** Visibility multiplier for a player as seen by a mob (1 = normal). Mobs only notice targets within range × visibility. */
	public static double visibilityMultiplier(Player player, Entity looker) {
		if (!player.isDiscrete() || player.level().isClientSide()) {
			return 1;
		}
		if (looker instanceof Mob mob) {
			Forgotten forgotten = FORGOTTEN.get(mob);
			if (forgotten != null && forgotten.player().equals(player.getUUID())
					&& forgotten.untilTick() > player.level().getGameTime() && Perks.has(player, Perks.SHADOW_STEP_2)) {
				return 0;
			}
		}
		return Perks.has(player, Perks.SHADOW_STEP_1) ? PamConfig.get().shadowStepVisibility : 1;
	}

	public static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.onGround() || player.isInWater() || player.onClimbable()) {
				DOUBLE_JUMPED.remove(player.getUUID());
				MANTLED.remove(player.getUUID());
			}
		}
		if (server.getTickCount() % 10 == 0) {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.isDiscrete() && Perks.has(player, Perks.SHADOW_STEP_2)) {
					shadowStepEscape(player);
				}
			}
		}
	}

	/** Shadow Step II: mobs chasing a sneaking player from further than the escape distance give up. */
	private static void shadowStepEscape(ServerPlayer player) {
		PamConfig config = PamConfig.get();
		ServerLevel level = player.level();
		double escape = config.shadowStepEscapeDistance;
		for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(64), mob -> mob.getTarget() == player)) {
			// A mob the player just hit keeps chasing
			if (mob.distanceToSqr(player) <= escape * escape || mob.getLastHurtByMob() == player
					&& mob.tickCount - mob.getLastHurtByMobTimestamp() < 100) {
				continue;
			}
			mob.setTarget(null);
			mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
			FORGOTTEN.put(mob, new Forgotten(player.getUUID(), level.getGameTime() + config.shadowStepForgetSeconds * 20L));
			player.sendOverlayMessage(Component.translatable("message.progressive_adventure.shadow_step_escape", mob.getName()));
		}
	}

	public static void onDoubleJump(ServerPlayer player) {
		if (!Perks.has(player, Perks.DOUBLE_JUMP) || player.onGround() || !DOUBLE_JUMPED.add(player.getUUID())) {
			return;
		}
		player.resetFallDistance();
		ServerLevel level = player.level();
		level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 8, 0.25, 0.05, 0.25, 0.02);
		level.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.5f, 1.6f);
	}

	public static void onMantle(ServerPlayer player) {
		if (!Perks.has(player, Perks.MANTLE) || player.onGround() || !MANTLED.add(player.getUUID())) {
			return;
		}
		player.resetFallDistance();
		player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
	}
}
