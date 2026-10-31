package com.progressiveadventure.mixin;

import com.progressiveadventure.access.FoodDataAccess;

import net.minecraft.world.food.FoodData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FoodData.class)
public class FoodDataMixin implements FoodDataAccess {
	@Unique
	private float pam$exhaustionMultiplier = 1f;
	@Unique
	private float pam$saturationMultiplier = 1f;
	@Unique
	private float pam$healingMultiplier = 1f;
	@Unique
	private int pam$healInterval = VANILLA_HEAL_INTERVAL;
	@Unique
	private int pam$healMinFood = VANILLA_HEAL_MIN_FOOD;

	@Override
	public void pam$setMultipliers(float exhaustion, float saturation, float healing) {
		pam$exhaustionMultiplier = exhaustion;
		pam$saturationMultiplier = saturation;
		pam$healingMultiplier = healing;
	}

	@Override
	public void pam$setHealing(int intervalTicks, int minFood) {
		pam$healInterval = intervalTicks;
		pam$healMinFood = minFood;
	}

	@ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true)
	private float pam$scaleExhaustion(float exhaustion) {
		return exhaustion * pam$exhaustionMultiplier;
	}

	// Inside tick, vanilla only adds exhaustion for natural healing (the saturation heal and the normal one)
	@ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V"))
	private float pam$scaleHealingExhaustion(float exhaustion) {
		return exhaustion * pam$healingMultiplier;
	}

	// The 1st 80 in tick is the healing timer, the 2nd is starving, which stays as it is
	@ModifyConstant(method = "tick", constant = @Constant(intValue = VANILLA_HEAL_INTERVAL, ordinal = 0))
	private int pam$healInterval(int interval) {
		return pam$healInterval;
	}

	@ModifyConstant(method = "tick", constant = @Constant(intValue = VANILLA_HEAL_MIN_FOOD))
	private int pam$healMinFood(int minFood) {
		return pam$healMinFood;
	}

	// Both eat(...) overloads funnel into add(int nutrition, float saturation)
	@ModifyVariable(method = "add", at = @At("HEAD"), argsOnly = true)
	private float pam$scaleSaturation(float saturation) {
		return saturation * pam$saturationMultiplier;
	}
}
