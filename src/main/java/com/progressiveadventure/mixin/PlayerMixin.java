package com.progressiveadventure.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.progressiveadventure.access.PlayerAttackAccess;
import com.progressiveadventure.skill.Melee;
import com.progressiveadventure.skill.Skills;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Player.class)
public abstract class PlayerMixin implements PlayerAttackAccess {
	@Unique
	private float pam$attackStrength;

	@Shadow
	public abstract float getAttackStrengthScale(float partialTick);

	@Override
	public float pam$attackStrength() {
		return pam$attackStrength;
	}

	/** Remember how full the cooldown was: vanilla resets it in onAttack, before the hit and the damage events. */
	@Inject(method = "attack", at = @At("HEAD"))
	private void pam$rememberAttackStrength(Entity target, CallbackInfo ci) {
		pam$attackStrength = getAttackStrengthScale(0.5f);
	}
	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void pam$applySkillSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(cir.getReturnValueF() * Skills.speedMultiplier((Player) (Object) this, state));
	}

	/** Careful Sweep: sword sweeps don't hurt pets, villagers or golems next to the mob you hit. */
	@WrapOperation(method = "doSweepAttack", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean pam$spareFriendsFromSweep(LivingEntity nearby, ServerLevel level, DamageSource source, float damage, Operation<Boolean> original) {
		return !Melee.sparedBySweep((Player) (Object) this, nearby) && original.call(nearby, level, source, damage);
	}

	/** Whirlwind: a sweep while sneaking hits the hostile mobs all around you instead of those next to the target. */
	@WrapOperation(method = "doSweepAttack", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
	private <T extends Entity> List<T> pam$whirlwindTargets(Level level, Class<T> type, AABB box, Operation<List<T>> original) {
		Player self = (Player) (Object) this;
		return Melee.isWhirlwind(self) ? original.call(level, type, Melee.whirlwindArea(self)) : original.call(level, type, box);
	}

	@WrapOperation(method = "doSweepAttack", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z"))
	private boolean pam$whirlwindOnlyHostile(Player self, Entity nearby, Operation<Boolean> original) {
		return original.call(self, nearby) || (Melee.isWhirlwind(self) && !Melee.isWhirlwindTarget(self, nearby));
	}

	@Inject(method = "doSweepAttack", at = @At("TAIL"))
	private void pam$whirlwindRing(CallbackInfo ci) {
		Player self = (Player) (Object) this;
		if (Melee.isWhirlwind(self)) {
			Melee.whirlwindEffects(self);
		}
	}
}
