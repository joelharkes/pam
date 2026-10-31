package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client asks to buy a tip of an achievement. @param tip number of the tip, from 1 */
public record BuyTipPayload(String advancement, int tip) implements CustomPacketPayload {
	public static final Type<BuyTipPayload> TYPE = new Type<>(ProgressiveAdventure.id("buy_tip"));

	public static final StreamCodec<RegistryFriendlyByteBuf, BuyTipPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, BuyTipPayload::advancement,
			ByteBufCodecs.VAR_INT, BuyTipPayload::tip,
			BuyTipPayload::new);

	@Override
	public Type<BuyTipPayload> type() {
		return TYPE;
	}
}
