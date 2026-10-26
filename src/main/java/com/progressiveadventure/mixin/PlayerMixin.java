package com.progressiveadventure.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.progressiveadventure.skill.Melee;
import com.progressiveadventure.skill.Skills;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {
	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void pam$applySkillSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(cir.getReturnValueF() * Skills.speedMultiplier((Player) (Object) this, state));
	}

	/** Sword sweeps don't hurt pets, villagers or golems next to the mob you hit (Wide Sweep makes sweeps strong). */
	@WrapOperation(method = "doSweepAttack", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean pam$spareFriendsFromSweep(LivingEntity nearby, ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
		return !Melee.sparedBySweep(nearby) && original.call(nearby, level, source, damage);
	}
}
