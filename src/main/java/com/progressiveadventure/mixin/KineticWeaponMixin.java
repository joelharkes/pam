package com.progressiveadventure.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.progressiveadventure.skill.MeleeMoves;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Running Charge: a player on foot counts faster for the spear charge.
 * <p>
 * UPDATE RISK: this targets the first {@code Vec3.dot} call in {@code damageEntities} by position (ordinal 0), which in
 * 26.2 is {@code attackerLookVector.dot(getMotion(livingEntity))}, the attacker's speed. If a Minecraft update adds
 * another dot product before it, the mixin still applies (no crash) but silently scales the wrong value. Re-read
 * {@code KineticWeapon.damageEntities} on every Minecraft update (listed in AUDIT.md).
 */
@Mixin(KineticWeapon.class)
public class KineticWeaponMixin {
	@ModifyExpressionValue(method = "damageEntities", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/phys/Vec3;dot(Lnet/minecraft/world/phys/Vec3;)D", ordinal = 0))
	private double pam$runningCharge(double attackerSpeed, ItemStack stack, int ticksRemaining, LivingEntity entity, EquipmentSlot slot) {
		return attackerSpeed * MeleeMoves.chargeSpeedMultiplier(entity);
	}
}
