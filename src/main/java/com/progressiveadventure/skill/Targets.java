package com.progressiveadventure.skill;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;

/**
 * Who the combat perks may pick as a target. Perks that choose mobs themselves (Steady Aim, Ricochet, Whirlwind,
 * Shockwave, Pinning Jab, Shield Bash, Ground Pound) only go for hostile ones; Careful Sweep spares friendly ones
 * from vanilla's sweep. One place, so "never hurt pets or villagers" is changed in one spot.
 */
public final class Targets {
	private Targets() {
	}

	/** Hostile mobs only; neutral ones (endermen, zombified piglins) only once they target this player or shooter. */
	public static boolean isHostile(Mob mob, Entity player) {
		return mob instanceof Enemy && mob.isAlive() && !mob.isInvisible()
				&& (!(mob instanceof NeutralMob) || (player != null && mob.getTarget() == player));
	}

	/** Tamed pets, villagers and golems (not shulkers, which are golems too). */
	public static boolean isFriendly(LivingEntity entity) {
		return (entity instanceof OwnableEntity pet && pet.getOwnerReference() != null)
				|| entity instanceof AbstractVillager
				|| (entity instanceof AbstractGolem && !(entity instanceof Enemy));
	}
}
