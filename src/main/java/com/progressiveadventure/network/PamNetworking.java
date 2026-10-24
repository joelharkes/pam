package com.progressiveadventure.network;

import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.PlayerProgress;
import com.progressiveadventure.milestone.Progression;
import com.progressiveadventure.perk.Perk;
import com.progressiveadventure.perk.Perks;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class PamNetworking {
	private PamNetworking() {
	}

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(BuyPerkPayload.TYPE, BuyPerkPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ResetPerksPayload.TYPE, ResetPerksPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(BuyPerkPayload.TYPE, (payload, context) -> buy(context.player(), payload.perkId()));
		ServerPlayNetworking.registerGlobalReceiver(ResetPerksPayload.TYPE, (payload, context) -> reset(context.player()));
	}

	private static void buy(ServerPlayer player, String perkId) {
		Perk perk = Perks.byId(perkId);
		PlayerProgress progress = PamAttachments.progress(player);
		if (perk == null || Perks.status(perk, progress, PamAttachments.snapshot(player)) != Perks.Status.BUYABLE) {
			return;
		}
		PamAttachments.setProgress(player, progress.withPerk(perk.id()));
		Progression.apply(player);
		player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.2f);
	}

	private static void reset(ServerPlayer player) {
		PamAttachments.setProgress(player, PamAttachments.progress(player).withoutPerks());
		Progression.apply(player);
	}
}
