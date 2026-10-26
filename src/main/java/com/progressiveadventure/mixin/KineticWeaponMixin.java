package com.progressiveadventure.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.progressiveadventure.skill.MeleeMoves;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Running Charge: a player on foot counts faster for the spear charge (the first dot product is the attacker's speed). */
@Mixin(KineticWeapon.class)
public class KineticWeaponMixin {
	@ModifyExpressionValue(method = "damageEntities", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/phys/Vec3;dot(Lnet/minecraft/world/phys/Vec3;)D", ordinal = 0))
	private double pam$runningCharge(double attackerSpeed, ItemStack stack, int ticksRemaining, LivingEntity entity, EquipmentSlot slot) {
		return attackerSpeed * MeleeMoves.chargeSpeedMultiplier(entity);
	}
}
