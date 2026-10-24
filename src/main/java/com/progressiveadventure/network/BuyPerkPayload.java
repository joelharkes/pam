package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client asks to buy a perk. The server re-checks everything. */
public record BuyPerkPayload(String perkId) implements CustomPacketPayload {
	public static final Type<BuyPerkPayload> TYPE = new Type<>(ProgressiveAdventure.id("buy_perk"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BuyPerkPayload> CODEC =
			ByteBufCodecs.STRING_UTF8.map(BuyPerkPayload::new, BuyPerkPayload::perkId).cast();

	@Override
	public Type<BuyPerkPayload> type() {
		return TYPE;
	}
}
