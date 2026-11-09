package com.progressiveadventure.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.progressiveadventure.challenge.Challenges;

import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.raid.Raid;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Safe and Sound: vanilla tells each hero of a won raid through the raid_win trigger; we also need the raid. */
@Mixin(Raid.class)
public abstract class RaidMixin {
	@WrapOperation(method = "tick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/advancements/triggers/PlayerTrigger;trigger(Lnet/minecraft/server/level/ServerPlayer;)V"))
	private void pam$raidWon(PlayerTrigger trigger, ServerPlayer player, Operation<Void> original) {
		original.call(trigger, player);
		Challenges.onRaidWon((Raid) (Object) this, player);
	}
}
