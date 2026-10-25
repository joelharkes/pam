package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client did a double jump in the air (it already moved itself). The server checks the perk and resets fall distance. */
public record DoubleJumpPayload() implements CustomPacketPayload {
	public static final Type<DoubleJumpPayload> TYPE = new Type<>(ProgressiveAdventure.id("double_jump"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DoubleJumpPayload> CODEC = StreamCodec.unit(new DoubleJumpPayload());

	@Override
	public Type<DoubleJumpPayload> type() {
		return TYPE;
	}
}
