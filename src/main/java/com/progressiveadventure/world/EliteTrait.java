package com.progressiveadventure.world;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;

/** Extra ability of a high-rank elite, shown in its name ("Frost Skeleton"). */
public enum EliteTrait {
	SWIFT,
	FROST,
	BLAZING,
	LEAPING,
	SUMMONER;

	public final String id = name().toLowerCase();

	public static EliteTrait byId(String id) {
		for (EliteTrait trait : values()) {
			if (trait.id.equals(id)) {
				return trait;
			}
		}
		return null;
	}

	/** Leaping only makes sense for melee mobs that don't jump around already. */
	public boolean canHave(Mob mob) {
		return this != LEAPING || !(mob instanceof AbstractSkeleton || mob instanceof Slime);
	}
}
