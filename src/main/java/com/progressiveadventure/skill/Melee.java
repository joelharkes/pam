package com.progressiveadventure.skill;

import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.perk.Perks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;

/**
 * Melee skill: xp from hits a player lands by hand (any weapon, also sweeps, mace smashes and spear attacks); the
 * skill bonus is a faster attack cooldown (an attribute modifier applied in {@code Progression}).
 */
public final class Melee {
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

	/** A hit that didn't kill (Fabric's damage event skips killing hits, those go to {@link #onKill}). */
	public static void onDamage(LivingEntity victim, DamageSource source, float damageTaken, boolean blocked) {
		ServerPlayer player = meleeAttacker(source);
		if (player != null && victim instanceof Mob && !blocked && damageTaken >= PamConfig.get().meleeMinHitDamage) {
			Skills.addXp(player, Skill.MELEE, PamConfig.get().meleeHitXp);
		}
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
