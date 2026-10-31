package com.progressiveadventure.access;

/** Implemented on FoodData by mixin: per-player hunger multipliers from the stomach level and Steady Healing, healing speed from perks. */
public interface FoodDataAccess {
	// Vanilla's FoodData.tick: a half heart every 80 ticks from food level 18, without saturation
	int VANILLA_HEAL_INTERVAL = 80;
	int VANILLA_HEAL_MIN_FOOD = 18;

	void pam$setMultipliers(float exhaustion, float saturation, float healing);

	void pam$setHealing(int intervalTicks, int minFood);
}
