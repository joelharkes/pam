package com.progressiveadventure.access;

/** Implemented on FoodData by mixin: per-player hunger multipliers from the stomach level and Steady Healing. */
public interface FoodDataAccess {
	void pam$setMultipliers(float exhaustion, float saturation, float healing);
}
