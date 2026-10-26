package com.progressiveadventure.mixin;

import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.perk.Perks;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin extends AbstractArrow {
	@Shadow
	@Final
	private static EntityDataAccessor<Byte> ID_LOYALTY;

	protected ThrownTridentMixin(EntityType<? extends AbstractArrow> type, Level level) {
		super(type, level);
	}

	/** Loyal Trident: a trident thrown by a player with the perk comes back as if it had Loyalty. */
	@Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V", at = @At("TAIL"))
	private void pam$loyalTrident(Level level, LivingEntity owner, ItemStack tridentItem, CallbackInfo ci) {
		if (owner instanceof ServerPlayer player && entityData.get(ID_LOYALTY) == 0 && Perks.has(player, Perks.LOYAL_TRIDENT)) {
			entityData.set(ID_LOYALTY, (byte) PamConfig.get().loyalTridentLoyalty);
		}
	}
}
