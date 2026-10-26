package com.progressiveadventure.skill;

import com.progressiveadventure.ProgressiveAdventure;
import com.progressiveadventure.access.PlayerAttackAccess;
import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.perk.Perks;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Melee skill: xp from hits a player lands by hand (any weapon, also sweeps, mace smashes and spear attacks); the
 * skill bonus is a faster attack cooldown (an attribute modifier applied in {@code Progression}).
 */
public final class Melee {
	private static final Identifier SUNDER_MODIFIER = ProgressiveAdventure.id("sunder");
	// Vanilla's sweep only hits mobs within 3 blocks of the player
	private static final double WHIRLWIND_RADIUS = 3.0;
	private static final Map<Mob, Long> SUNDERED = new HashMap<>();

	private Melee() {
	}

	public static double attackSpeedBonus(Player player) {
		return Skills.bonus(Skill.MELEE, Skills.level(player, Skill.MELEE));
	}

	/** The player behind this damage if they dealt it by hand (not arrows, thorns or explosions), or null. */
	private static ServerPlayer meleeAttacker(DamageSource source) {
		if (source.getEntity() instanceof ServerPlayer player && source.getDirectEntity() == player
				&& !source.is(DamageTypes.THORNS) && !source.is(DamageTypeTags.IS_EXPLOSION)) {
			return player;
		}
		return null;
	}

	/**
	 * A hit that didn't kill (Fabric's damage event skips killing hits, those go to {@link #onKill}). Also runs the
	 * on-hit perks: Disarm (sword), Heavy Swing and Sunder (fully charged axe).
	 */
	public static void onDamage(LivingEntity victim, DamageSource source, float damageTaken, boolean blocked) {
		ServerPlayer player = meleeAttacker(source);
		PamConfig config = PamConfig.get();
		if (player == null || !(victim instanceof Mob mob) || blocked || damageTaken < config.meleeMinHitDamage) {
			return;
		}
		Skills.addXp(player, Skill.MELEE, config.meleeHitXp);

		ItemStack weapon = player.getMainHandItem();
		if (weapon.is(ItemTags.SWORDS) && Perks.has(player, Perks.DISARM) && player.getRandom().nextDouble() < config.disarmChance) {
			disarm(player, mob);
		}
		if (weapon.is(ItemTags.AXES) && isFullStrength(player)) {
			if (Perks.has(player, Perks.HEAVY_SWING)) {
				heavySwing(player, mob, source, damageTaken);
			}
			if (Perks.has(player, Perks.SUNDER)) {
				sunder(mob);
			}
		}
	}

	/** Whether the player's current melee attack had a (nearly) full cooldown, like vanilla's sweep and crit check. */
	public static boolean isFullStrength(Player player) {
		return ((PlayerAttackAccess) player).pam$attackStrength() > 0.9f;
	}

	/** Disarm: the weapon flies out of the mob's hand and lands next to it (a skeleton without a bow has to come close). */
	private static void disarm(ServerPlayer player, Mob mob) {
		ItemStack held = mob.getMainHandItem();
		if (held.isEmpty() || !(mob.level() instanceof ServerLevel level)) {
			return;
		}
		mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY); // skeletons switch to melee on their own
		mob.spawnAtLocation(level, held, mob.getBbHeight() * 0.6f);
		level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
		player.sendOverlayMessage(Component.translatable("message.progressive_adventure.disarmed", mob.getDisplayName()));
	}

	/** Heavy Swing: extra knockback away from the player and a short slow. */
	private static void heavySwing(ServerPlayer player, Mob mob, DamageSource source, float damage) {
		PamConfig config = PamConfig.get();
		double dx = player.getX() - mob.getX();
		double dz = player.getZ() - mob.getZ();
		mob.knockback(config.heavySwingKnockback, dx, dz, source, damage);
		mob.hurtMarked = true; // send the new motion to clients now
		mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, config.heavySwingSlownessTicks, 1));
	}

	/** Sunder: the mob's armor counts for less for a few seconds (a transient armor modifier, removed by {@link #tick}). */
	private static void sunder(Mob mob) {
		AttributeInstance armor = mob.getAttribute(Attributes.ARMOR);
		if (armor == null || mob.getArmorValue() <= 0 || !(mob.level() instanceof ServerLevel level)) {
			return;
		}
		PamConfig config = PamConfig.get();
		if (!armor.hasModifier(SUNDER_MODIFIER)) {
			level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.SHIELD_BREAK.value(), SoundSource.PLAYERS, 0.8f, 0.8f);
			level.sendParticles(ParticleTypes.CRIT, mob.getX(), mob.getY(0.6), mob.getZ(), 12, 0.3, 0.4, 0.3, 0.2);
		}
		armor.addOrUpdateTransientModifier(new AttributeModifier(SUNDER_MODIFIER, -config.sunderArmorReduction,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		SUNDERED.put(mob, level.getGameTime() + config.sunderSeconds * 20L);
	}

	/** Every server tick: lifts Sunder from mobs whose time is up. */
	public static void tick() {
		if (SUNDERED.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<Mob, Long>> it = SUNDERED.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Mob, Long> entry = it.next();
			Mob mob = entry.getKey();
			if (mob.isRemoved() || mob.level().getGameTime() >= entry.getValue()) {
				AttributeInstance armor = mob.getAttribute(Attributes.ARMOR);
				if (armor != null) {
					armor.removeModifier(SUNDER_MODIFIER);
				}
				it.remove();
			}
		}
	}

	public static void onServerStopped() {
		SUNDERED.clear();
	}

	/** Whirlwind: the player owns it, sneaks and holds a sword (only called while a sweep is happening). */
	public static boolean isWhirlwind(Player player) {
		return !player.level().isClientSide() && player.isShiftKeyDown() && Perks.has(player, Perks.WHIRLWIND);
	}

	/**
	 * Where Whirlwind looks for mobs: all around the player. Vanilla's sweep also needs the mob within 3 blocks of the
	 * player, so a bigger radius would change nothing.
	 */
	public static AABB whirlwindArea(Player player) {
		return player.getBoundingBox().inflate(WHIRLWIND_RADIUS, 0.5, WHIRLWIND_RADIUS);
	}

	/** Only hostile mobs (neutral ones that target the player); never pets, villagers, animals or players. */
	public static boolean isWhirlwindTarget(Player player, Entity entity) {
		return entity instanceof Mob mob && Archery.isAimTarget(mob, player);
	}

	/** A ring of sweep particles around the player. */
	public static void whirlwindEffects(Player player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		for (int i = 0; i < 8; i++) {
			double angle = i * Math.PI / 4;
			double dx = Math.cos(angle) * 1.8;
			double dz = Math.sin(angle) * 1.8;
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + dx, player.getY(0.5), player.getZ() + dz, 0, dx, 0, dz, 0);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
	}

	public static void onKill(LivingEntity victim, DamageSource source) {
		ServerPlayer player = meleeAttacker(source);
		if (player != null && victim instanceof Mob) {
			Skills.addXp(player, Skill.MELEE, PamConfig.get().meleeHitXp + PamConfig.get().meleeKillXp);
		}
	}

	/**
	 * Careful Sweep: the player's sword sweep leaves pets, villagers and golems next to the mob alone (vanilla sweeps
	 * hit everything that isn't on your team).
	 */
	public static boolean sparedBySweep(Player player, LivingEntity entity) {
		return Perks.has(player, Perks.CAREFUL_SWEEP) && isFriendly(entity);
	}

	/** Tamed pets, villagers and golems: what Careful Sweep and the area moves leave alone. */
	public static boolean isFriendly(LivingEntity entity) {
		return (entity instanceof OwnableEntity pet && pet.getOwnerReference() != null)
				|| entity instanceof AbstractVillager
				|| (entity instanceof AbstractGolem && !(entity instanceof Enemy)); // shulkers are golems too
	}
}
