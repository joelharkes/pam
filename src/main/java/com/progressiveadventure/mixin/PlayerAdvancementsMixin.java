package com.progressiveadventure.mixin;

import com.progressiveadventure.milestone.Progression;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
	@Shadow
	private ServerPlayer player;

	@Shadow
	public abstract AdvancementProgress getOrStartProgress(AdvancementHolder holder);

	@Inject(method = "award", at = @At("RETURN"))
	private void pam$onAward(AdvancementHolder holder, String criterion, CallbackInfoReturnable<Boolean> cir) {
		// Recipe unlocks are advancements too; they give no points but are cheap to skip
		if (cir.getReturnValueZ() && holder.value().display().isPresent() && getOrStartProgress(holder).isDone()) {
			Progression.onAdvancementCompleted(player, holder);
		}
	}

	/** /advancement revoke: hearts, stomach and Rank are derived, so recompute them right away instead of on the next login. */
	@Inject(method = "revoke", at = @At("RETURN"))
	private void pam$onRevoke(AdvancementHolder holder, String criterion, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && holder.value().display().isPresent()) {
			Progression.apply(player);
		}
	}
}
