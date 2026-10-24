package com.progressiveadventure.mixin;

import com.progressiveadventure.skill.Farming;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
public class AnimalMixin {
	// HEAD: the love cause is still known here; vanilla resets it further down
	@Inject(method = "finalizeSpawnChildFromBreeding", at = @At("HEAD"))
	private void pam$onBred(ServerLevel level, Animal partner, AgeableMob offspring, CallbackInfo ci) {
		Farming.onBred(level, (Animal) (Object) this, partner, offspring);
	}
}
