package com.progressiveadventure.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.progressiveadventure.skill.Archery;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bows and crossbows: Thrifty Quiver (like Infinity, the saved arrow can't be picked up) and the arrow perks. */
@Mixin(ProjectileWeaponItem.class)
public class ProjectileWeaponItemMixin {
	@ModifyExpressionValue(method = "useAmmo", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processAmmoUse(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;I)I"))
	private static int pam$thriftyQuiver(int ammoToUse, @Local(argsOnly = true, ordinal = 1) ItemStack projectile,
			@Local(argsOnly = true) LivingEntity holder) {
		if (ammoToUse > 0 && holder instanceof ServerPlayer player && Archery.saveArrow(player, projectile)) {
			return 0;
		}
		return ammoToUse;
	}

	@Inject(method = "createProjectile", at = @At("RETURN"))
	private void pam$arrowPerks(Level level, LivingEntity shooter, ItemStack weapon, ItemStack projectile, boolean isCrit,
			CallbackInfoReturnable<Projectile> cir) {
		if (shooter instanceof ServerPlayer player && cir.getReturnValue() instanceof AbstractArrow arrow) {
			Archery.onArrowCreated(player, arrow);
		}
	}
}
