package com.progressiveadventure.mixin;

import com.progressiveadventure.bestiary.Bestiary;
import com.progressiveadventure.perk.MovementPerks;
import com.progressiveadventure.perk.PerkEffects;
import com.progressiveadventure.perk.Perks;
import com.progressiveadventure.skill.Archery;
import com.progressiveadventure.world.Elites;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
	@Shadow
	protected int useItemRemaining;

	/** Archery draw speed carried over between ticks, since the bonus is a fraction of a tick. */
	@Unique
	private double pam$drawCarry;

	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float pam$applyFamiliarity(float amount, ServerLevel level, DamageSource source) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (source.getEntity() instanceof ServerPlayer player && !(self instanceof Player)) {
			return amount * Bestiary.damageMultiplier(player, self);
		}
		if (self instanceof ServerPlayer player) {
			return amount * PerkEffects.damageTakenMultiplier(player, source);
		}
		return amount;
	}

	@Inject(method = "canBeAffected", at = @At("RETURN"), cancellable = true)
	private void pam$ironStomach(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Player player && effect.is(MobEffects.HUNGER) && Perks.has(player, Perks.IRON_STOMACH)) {
			cir.setReturnValue(false);
		}
	}

	/**
	 * Archery draw speed: bows, crossbows and tridents count extra use ticks. Runs on client and server, so the pull
	 * animation, zoom and crossbow charge match the arrow's real power.
	 */
	@Inject(method = "updateUsingItem", at = @At("TAIL"))
	private void pam$drawSpeed(ItemStack useItem, CallbackInfo ci) {
		if (!((Object) this instanceof Player player) || !Archery.isDrawWeapon(useItem)) {
			return;
		}
		pam$drawCarry += Archery.drawSpeedBonus(player);
		while (pam$drawCarry >= 1 && useItemRemaining > 1) {
			pam$drawCarry--;
			useItemRemaining--;
		}
	}

	@Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
	private void pam$shadowStep(Entity looker, CallbackInfoReturnable<Double> cir) {
		if ((Object) this instanceof Player player) {
			double multiplier = MovementPerks.visibilityMultiplier(player, looker);
			if (multiplier != 1) {
				cir.setReturnValue(cir.getReturnValueD() * multiplier);
			}
		}
	}

	@Inject(method = "getExperienceReward", at = @At("RETURN"), cancellable = true)
	private void pam$eliteXp(ServerLevel level, Entity killer, CallbackInfoReturnable<Integer> cir) {
		double multiplier = Elites.xpMultiplier((LivingEntity) (Object) this);
		if (multiplier != 1) {
			cir.setReturnValue((int) Math.round(cir.getReturnValueI() * multiplier));
		}
	}
}
