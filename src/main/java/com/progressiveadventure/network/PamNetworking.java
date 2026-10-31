package com.progressiveadventure.network;

import com.progressiveadventure.achievement.Achievements;
import com.progressiveadventure.bestiary.BestiaryInfo;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.PlayerProgress;
import com.progressiveadventure.milestone.Progression;
import com.progressiveadventure.perk.MovementPerks;
import com.progressiveadventure.perk.Perk;
import com.progressiveadventure.perk.Perks;
import com.progressiveadventure.perk.ShieldPerks;
import com.progressiveadventure.skill.MeleeMoves;
import com.progressiveadventure.waypoint.Waypoints;

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
		PayloadTypeRegistry.serverboundPlay().register(DoubleJumpPayload.TYPE, DoubleJumpPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ShieldBashPayload.TYPE, ShieldBashPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(GroundPoundPayload.TYPE, GroundPoundPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(MantlePayload.TYPE, MantlePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(NameWaypointPayload.TYPE, NameWaypointPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(WaypointTeleportPayload.TYPE, WaypointTeleportPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenWaypointNamePayload.TYPE, OpenWaypointNamePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(BestiaryInfoRequestPayload.TYPE, BestiaryInfoRequestPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(AchievementListRequestPayload.TYPE, AchievementListRequestPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(BuyTipPayload.TYPE, BuyTipPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(AchievementListPayload.TYPE, AchievementListPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenWaypointListPayload.TYPE, OpenWaypointListPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(BestiaryInfoPayload.TYPE, BestiaryInfoPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(BestiaryDiscoveredPayload.TYPE, BestiaryDiscoveredPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(BuyPerkPayload.TYPE, (payload, context) -> buy(context.player(), payload.perkId()));
		ServerPlayNetworking.registerGlobalReceiver(ResetPerksPayload.TYPE, (payload, context) -> reset(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(DoubleJumpPayload.TYPE, (payload, context) -> MovementPerks.onDoubleJump(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(MantlePayload.TYPE, (payload, context) -> MovementPerks.onMantle(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(ShieldBashPayload.TYPE, (payload, context) -> ShieldPerks.onBash(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(GroundPoundPayload.TYPE, (payload, context) -> MeleeMoves.onGroundPound(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(NameWaypointPayload.TYPE, (payload, context) -> Waypoints.onName(context.player(), payload.pos(), payload.name()));
		ServerPlayNetworking.registerGlobalReceiver(WaypointTeleportPayload.TYPE,
				(payload, context) -> Waypoints.onTeleportRequest(context.player(), payload.from(), payload.target()));
		ServerPlayNetworking.registerGlobalReceiver(BestiaryInfoRequestPayload.TYPE,
				(payload, context) -> BestiaryInfo.onRequest(context.player(), payload.entityType()));
		ServerPlayNetworking.registerGlobalReceiver(AchievementListRequestPayload.TYPE, (payload, context) -> Achievements.onRequest(context.player()));
		ServerPlayNetworking.registerGlobalReceiver(BuyTipPayload.TYPE, (payload, context) -> Achievements.onBuyTip(context.player(), payload.advancement(), payload.tip()));
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
