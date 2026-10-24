package com.progressiveadventure.mixin;

import com.progressiveadventure.access.FoodDataAccess;

import net.minecraft.world.food.FoodData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FoodData.class)
public class FoodDataMixin implements FoodDataAccess {
	@Unique
	private float pam$exhaustionMultiplier = 1f;
	@Unique
	private float pam$saturationMultiplier = 1f;

	@Override
	public void pam$setMultipliers(float exhaustion, float saturation) {
		pam$exhaustionMultiplier = exhaustion;
		pam$saturationMultiplier = saturation;
	}

	@ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true)
	private float pam$scaleExhaustion(float exhaustion) {
		return exhaustion * pam$exhaustionMultiplier;
	}

	// Both eat(...) overloads funnel into add(int nutrition, float saturation)
	@ModifyVariable(method = "add", at = @At("HEAD"), argsOnly = true)
	private float pam$scaleSaturation(float saturation) {
		return saturation * pam$saturationMultiplier;
	}
}
