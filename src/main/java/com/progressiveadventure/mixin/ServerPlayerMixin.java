package com.progressiveadventure.mixin;

import com.progressiveadventure.perk.DeathPerks;

import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
	/** Keepsake: vanilla only moves the inventory to the new player when keepInventory is on. */
	@Inject(method = "restoreFrom", at = @At("TAIL"))
	private void pam$restoreKept(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo ci) {
		if (!restoreAll) {
			DeathPerks.restoreKept((ServerPlayer) (Object) this, oldPlayer);
		}
	}
}
