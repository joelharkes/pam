package com.progressiveadventure.mixin;

import com.progressiveadventure.milestone.Trackers;

import net.minecraft.advancements.triggers.ConsumeItemTrigger;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla fires this trigger whenever a player finishes consuming an item, which makes it a handy food hook. */
@Mixin(ConsumeItemTrigger.class)
public class ConsumeItemTriggerMixin {
	@Inject(method = "trigger", at = @At("HEAD"))
	private void pam$onConsume(ServerPlayer player, ItemStack stack, CallbackInfo ci) {
		if (stack.has(DataComponents.FOOD)) {
			Trackers.onFoodEaten(player, stack);
		}
	}
}
