package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client opened the Achievements tab. It only knows the advancements close to ones the player already did, so it asks for all of them. */
public record AchievementListRequestPayload() implements CustomPacketPayload {
	public static final Type<AchievementListRequestPayload> TYPE = new Type<>(ProgressiveAdventure.id("achievement_list_request"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AchievementListRequestPayload> CODEC = StreamCodec.unit(new AchievementListRequestPayload());

	@Override
	public Type<AchievementListRequestPayload> type() {
		return TYPE;
	}
}
