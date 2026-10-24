package com.progressiveadventure.mixin;

import com.progressiveadventure.bestiary.Bestiary;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float pam$applyFamiliarity(float amount, ServerLevel level, DamageSource source) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (source.getEntity() instanceof ServerPlayer player && !(self instanceof Player)) {
			return amount * Bestiary.damageMultiplier(player, self);
		}
		return amount;
	}
}
