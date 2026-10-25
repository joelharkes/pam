package com.progressiveadventure.skill;

import com.progressiveadventure.access.ArrowAccess;
import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.perk.Perks;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Archery skill and perks. Xp comes from arrows a player shot that hit a mob; the skill bonus is a faster bow and
 * crossbow draw. Perks act when the player shoots (ammo, fire, frost, pierce) or while the arrow flies (Steady Aim).
 */
public final class Archery {
	private Archery() {
	}

	public static boolean isDrawWeapon(ItemStack stack) {
		return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
	}

	/** Extra draw progress per tick as a fraction. Runs on client and server, so it reads the synced attachment. */
	public static double drawSpeedBonus(Player player) {
		return Skills.bonus(Skill.ARCHERY, Skills.level(player, Skill.ARCHERY));
	}

	/** The arrow behind this damage if a player shot it (tridents don't count), or null. */
	private static AbstractArrow playerArrow(DamageSource source) {
		if (source.getDirectEntity() instanceof AbstractArrow arrow && !(arrow instanceof ThrownTrident)
				&& source.getEntity() instanceof ServerPlayer) {
			return arrow;
		}
		return null;
	}

	public static void onDamage(LivingEntity victim, DamageSource source, float damageTaken, boolean blocked) {
		AbstractArrow arrow = playerArrow(source);
		if (arrow == null || !(victim instanceof Mob) || blocked || damageTaken <= 0) {
			return;
		}
		Skills.addXp((ServerPlayer) source.getEntity(), Skill.ARCHERY, PamConfig.get().archeryHitXp);
		if (arrow.hasAttached(PamAttachments.FROST_ARROW)) {
			victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, PamConfig.get().frostArrowSlownessSeconds * 20, 1));
		}
	}

	public static void onKill(LivingEntity victim, DamageSource source) {
		if (playerArrow(source) != null && victim instanceof Mob) {
			Skills.addXp((ServerPlayer) source.getEntity(), Skill.ARCHERY, PamConfig.get().archeryKillXp);
		}
	}

	/** Thrifty Quiver: true if this shot shouldn't use up the arrow. */
	public static boolean saveArrow(ServerPlayer player, ItemStack projectile) {
		int tier = Perks.tier(player, Perks.THRIFTY_QUIVER_1, Perks.THRIFTY_QUIVER_2, Perks.THRIFTY_QUIVER_3);
		return tier > 0 && projectile.is(ItemTags.ARROWS)
				&& player.getRandom().nextDouble() < PamConfig.listValue(PamConfig.get().thriftyQuiverChances, tier - 1);
	}

	/** Called for every arrow a player shoots from a bow or crossbow, before it's added to the world. */
	public static void onArrowCreated(ServerPlayer player, AbstractArrow arrow) {
		PamConfig config = PamConfig.get();
		RandomSource random = player.getRandom();
		if (Perks.has(player, Perks.FIRE_ARROWS) && random.nextDouble() < config.fireArrowChance) {
			arrow.igniteForSeconds(100);
		}
		if (Perks.has(player, Perks.FROST_ARROWS) && random.nextDouble() < config.frostArrowChance) {
			arrow.setAttached(PamAttachments.FROST_ARROW, true);
		}
		if (Perks.has(player, Perks.PIERCING_SHOT)) {
			((ArrowAccess) arrow).pam$addPierceLevel(config.piercingShotExtraMobs);
		}
		if (Perks.has(player, Perks.STEADY_AIM)) {
			arrow.setAttached(PamAttachments.STEADY_AIM_ARROW, true);
		}
	}

	/** Every server tick of an arrow that is still flying. */
	public static void tickFlyingArrow(ServerLevel level, AbstractArrow arrow) {
		if (arrow.hasAttached(PamAttachments.FROST_ARROW)) {
			level.sendParticles(ParticleTypes.SNOWFLAKE, arrow.getX(), arrow.getY(), arrow.getZ(), 1, 0, 0, 0, 0);
		}
		if (arrow.hasAttached(PamAttachments.STEADY_AIM_ARROW)) {
			steer(level, arrow);
		}
	}

	/** Steady Aim: turn the arrow a little toward the hostile mob closest to its path. Speed stays the same. */
	private static void steer(ServerLevel level, AbstractArrow arrow) {
		PamConfig config = PamConfig.get();
		Vec3 motion = arrow.getDeltaMovement();
		double speed = motion.length();
		if (speed < 0.5) {
			return; // a weak shot or one falling down: leave it alone
		}
		Vec3 direction = motion.scale(1 / speed);
		Entity owner = arrow.getOwner();

		Mob best = null;
		Vec3 bestDirection = null;
		double bestCos = Math.cos(Math.toRadians(config.steadyAimConeDegrees));
		for (Mob mob : level.getEntitiesOfClass(Mob.class, arrow.getBoundingBox().inflate(config.steadyAimRange), mob -> isAimTarget(mob, owner))) {
			Vec3 toMob = mob.getBoundingBox().getCenter().subtract(arrow.position());
			double distance = toMob.length();
			if (distance < 0.5 || distance > config.steadyAimRange) {
				continue;
			}
			double cos = direction.dot(toMob) / distance;
			if (cos > bestCos) {
				best = mob;
				bestCos = cos;
				bestDirection = toMob.scale(1 / distance);
			}
		}
		if (best == null || level.clip(new ClipContext(arrow.position(), best.getBoundingBox().getCenter(),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, arrow)).getType() != HitResult.Type.MISS) {
			return;
		}

		double angle = Math.acos(Math.min(1, bestCos));
		double maxTurn = Math.toRadians(config.steadyAimTurnDegrees);
		Vec3 newDirection = angle <= maxTurn ? bestDirection : direction.lerp(bestDirection, maxTurn / angle).normalize();
		arrow.setDeltaMovement(newDirection.scale(speed));
		arrow.needsSync = true; // the client would otherwise keep flying the arrow straight
	}

	/** Hostile mobs only; neutral ones (endermen, zombified piglins) only once they target the shooter. */
	private static boolean isAimTarget(Mob mob, Entity owner) {
		return mob instanceof Enemy && mob.isAlive() && !mob.isInvisible()
				&& (!(mob instanceof NeutralMob) || (owner != null && mob.getTarget() == owner));
	}
}
