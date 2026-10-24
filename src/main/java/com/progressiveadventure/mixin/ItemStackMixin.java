package com.progressiveadventure.mixin;

import com.progressiveadventure.perk.PerkEffects;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {
	// Runs after Unbreaking, so Sturdy perks stack with it
	@Inject(method = "processDurabilityChange", at = @At("RETURN"), cancellable = true)
	private void pam$sturdyPerks(int amount, ServerLevel level, ServerPlayer player, CallbackInfoReturnable<Integer> cir) {
		if (player != null && cir.getReturnValueI() > 0
				&& level.getRandom().nextDouble() < PerkEffects.durabilitySaveChance(player, (ItemStack) (Object) this)) {
			cir.setReturnValue(0);
		}
	}
}
