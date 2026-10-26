package com.progressiveadventure.skill;

import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.perk.Perks;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mace and spear perks. Ground Pound is a client move (like Double Jump): the client dives and tells the server, which
 * remembers the dive and handles the landing instead of fall damage. Shockwave hurts the hostile mobs around a mace
 * smash or a Ground Pound landing. Running Charge and Pinning Jab change vanilla's spear charge and jab (mixins).
 */
public final class MeleeMoves {
	// Vanilla's mace smash pushes everything within 3.5 blocks; the landing and Shockwave use the same reach
	private static final double SMASH_RADIUS = 3.5;
	/** How high above the ground a dive must start (the client checks the same), so it can't be sent just before landing. */
	public static final double POUND_MIN_HEIGHT = 2.0;
	/** Players in a Ground Pound dive: game time it started. */
	private static final Map<UUID, Long> POUNDING = new HashMap<>();
	private static boolean shockwaving;

	private MeleeMoves() {
	}

	public static void onServerStopped() {
		POUNDING.clear();
	}

	/** Whether this player may start a Ground Pound now. Runs on client and server. */
	public static boolean canGroundPound(Player player) {
		return Perks.has(player, Perks.GROUND_POUND) && player.getMainHandItem().getItem() instanceof MaceItem
				&& !player.onGround() && !player.isFallFlying() && !player.getAbilities().flying
				&& !player.isInWater() && !player.isPassenger();
	}

	/** Whether there are at least POUND_MIN_HEIGHT blocks of air below the player. Runs on client and server. */
	public static boolean highEnoughToPound(Player player) {
		return player.level().noCollision(player, player.getBoundingBox().move(0, -POUND_MIN_HEIGHT, 0));
	}

	/**
	 * The client started a dive (it already moves itself down). The server checks it too: otherwise a modified client
	 * could send this just before every landing and never take fall damage while holding a mace.
	 */
	public static void onGroundPound(ServerPlayer player) {
		if (!canGroundPound(player) || !highEnoughToPound(player)) {
			return;
		}
		POUNDING.put(player.getUUID(), player.level().getGameTime());
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 1f, 0.8f);
	}

	/**
	 * Every server tick: forget dives that ended without a landing, so they can't cancel the fall damage of a later
	 * fall. On the ground counts too: the landing itself was handled earlier in the tick, when the movement packet
	 * arrived. Ladders, cobwebs and powder snow stop a fall without a landing; a dive that isn't going down any more
	 * (after the first ticks, while the client's dive reaches the server) is over as well.
	 */
	public static void tick(MinecraftServer server) {
		if (POUNDING.isEmpty()) {
			return;
		}
		POUNDING.entrySet().removeIf(entry -> {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				return true;
			}
			long age = player.level().getGameTime() - entry.getValue();
			return player.onGround() || player.isInWater() || player.isFallFlying() || player.onClimbable()
					|| (age > 3 && player.getKnownMovement().y >= 0) || age > 200;
		});
	}

	/**
	 * Called instead of fall damage when a player lands. Returns true if this was a Ground Pound landing: no fall
	 * damage, hostile mobs around are pushed away (and hurt with Shockwave).
	 */
	public static boolean onLanding(ServerPlayer player, double fallDistance) {
		if (POUNDING.remove(player.getUUID()) == null || !(player.level() instanceof ServerLevel level)) {
			return false;
		}
		level.levelEvent(LevelEvent.PARTICLES_SMASH_ATTACK, player.getOnPos(), 750); // vanilla's mace smash dust
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				fallDistance > 5 ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1f, 1f);
		for (Mob mob : hostileMobsAround(player, player, player.position())) {
			Vec3 direction = mob.position().subtract(player.position());
			double power = (SMASH_RADIUS - direction.length()) * 0.7 * (fallDistance > 5 ? 2 : 1);
			if (power > 0) {
				Vec3 push = direction.multiply(1, 0, 1).normalize().scale(power);
				mob.push(push.x, 0.7, push.z);
				mob.hurtMarked = true;
			}
		}
		shockwave(player, null, player.position(), fallDistance);
		return true;
	}

	/** Shockwave: a mace smash that hit a mob also hurts the hostile mobs around it. */
	public static void onSmashHit(ServerPlayer player, LivingEntity victim) {
		shockwave(player, victim, victim.position(), player.fallDistance);
	}

	private static void shockwave(ServerPlayer player, Entity except, Vec3 center, double fallDistance) {
		if (shockwaving || !Perks.has(player, Perks.SHOCKWAVE) || !(player.level() instanceof ServerLevel level)) {
			return;
		}
		float damage = (float) (smashBonus(fallDistance) * PamConfig.get().shockwaveDamageRatio);
		if (damage <= 0) {
			return;
		}
		shockwaving = true; // never set off another shockwave from the shockwave's own hits
		try {
			for (Mob mob : hostileMobsAround(player, except, center)) {
				mob.hurtServer(level, player.damageSources().playerAttack(player), damage);
			}
		} finally {
			shockwaving = false;
		}
	}

	/** Vanilla's extra mace smash damage for a fall (MaceItem.getAttackDamageBonus, without enchantments). */
	private static double smashBonus(double fallDistance) {
		if (fallDistance <= 1.5) {
			return 0;
		}
		if (fallDistance <= 3) {
			return 4 * fallDistance;
		}
		if (fallDistance <= 8) {
			return 12 + 2 * (fallDistance - 3);
		}
		return 22 + fallDistance - 8;
	}

	private static Iterable<Mob> hostileMobsAround(ServerPlayer player, Entity except, Vec3 center) {
		return player.level().getEntitiesOfClass(Mob.class, new AABB(center, center).inflate(SMASH_RADIUS),
				mob -> mob != except && mob.position().distanceToSqr(center) <= SMASH_RADIUS * SMASH_RADIUS && Targets.isHostile(mob, player));
	}

	/** Running Charge: how much faster a player on foot counts for the spear charge (1 = vanilla). */
	public static double chargeSpeedMultiplier(LivingEntity entity) {
		return entity instanceof Player player && !player.isPassenger() && Perks.has(player, Perks.RUNNING_CHARGE)
				? PamConfig.get().runningChargeSpeedMultiplier : 1;
	}

	/** Pinning Jab: a spear jab that hit a hostile mob slows it. */
	public static void onJabHit(LivingEntity attacker, Entity target) {
		if (attacker instanceof ServerPlayer player && target instanceof Mob mob && Perks.has(player, Perks.PINNING_JAB)
				&& Targets.isHostile(mob, player)) {
			mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, PamConfig.get().pinningJabSlownessTicks, 1));
		}
	}
}
