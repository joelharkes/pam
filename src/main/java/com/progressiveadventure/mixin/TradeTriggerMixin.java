package com.progressiveadventure.mixin;

import com.progressiveadventure.milestone.Trackers;

import net.minecraft.advancements.triggers.TradeTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla fires this trigger on every trade with a villager or wandering trader: Five Professions and Master Trader. */
@Mixin(TradeTrigger.class)
public class TradeTriggerMixin {
	@Inject(method = "trigger", at = @At("HEAD"))
	private void pam$onTrade(ServerPlayer player, AbstractVillager villager, ItemStack stack, CallbackInfo ci) {
		Trackers.onTrade(player, villager);
	}
}
