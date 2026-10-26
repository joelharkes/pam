package com.progressiveadventure.access;

/** Implemented on Player by mixin: vanilla resets the attack strength before the hit lands, so it's kept here. */
public interface PlayerAttackAccess {
	/** The attack strength (0-1, how full the cooldown was) of the player's current or last melee attack. */
	float pam$attackStrength();
}
