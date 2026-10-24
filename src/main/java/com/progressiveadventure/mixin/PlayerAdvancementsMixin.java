package com.progressiveadventure.mixin;

import com.progressiveadventure.milestone.Milestone;
import com.progressiveadventure.milestone.Milestones;
import com.progressiveadventure.milestone.Progression;

import net.minecraft.advancements.AdvancementHolder;
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
	public abstract net.minecraft.advancements.AdvancementProgress getOrStartProgress(AdvancementHolder holder);

	@Inject(method = "award", at = @At("RETURN"))
	private void pam$onAward(AdvancementHolder holder, String criterion, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ()) {
			return;
		}
		Milestone milestone = Milestones.byId(holder.id());
		if (milestone != null && getOrStartProgress(holder).isDone()) {
			Progression.onMilestoneCompleted(player, milestone);
		}
	}
}
