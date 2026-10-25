package com.progressiveadventure.mixin;

import com.progressiveadventure.data.PamAttachments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Progress changes are stored once per tick; store them early when the player is saved (disconnect, autosave, stop) or respawns. */
@Mixin(PlayerList.class)
public class PlayerListMixin {
	@Inject(method = "save", at = @At("HEAD"))
	private void pam$flushOnSave(ServerPlayer player, CallbackInfo ci) {
		PamAttachments.flushProgress(player);
	}

	@Inject(method = "respawn", at = @At("HEAD"))
	private void pam$flushOnRespawn(ServerPlayer player, boolean keepAllPlayerData, Entity.RemovalReason removalReason,
			CallbackInfoReturnable<ServerPlayer> cir) {
		PamAttachments.flushProgress(player);
	}
}
