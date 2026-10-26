package com.progressiveadventure.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.progressiveadventure.skill.MeleeMoves;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.PiercingWeapon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Pinning Jab: every mob a spear jab hits gets slowed. */
@Mixin(PiercingWeapon.class)
public class PiercingWeaponMixin {
	@WrapOperation(method = "attack", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;stabAttack(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/Entity;FZZZ)Z"))
	private boolean pam$pinningJab(LivingEntity attacker, EquipmentSlot hand, Entity target, float damage, boolean dealsDamage,
			boolean dealsKnockback, boolean dismounts, Operation<Boolean> original) {
		boolean hit = original.call(attacker, hand, target, damage, dealsDamage, dealsKnockback, dismounts);
		if (hit) {
			MeleeMoves.onJabHit(attacker, target);
		}
		return hit;
	}
}
