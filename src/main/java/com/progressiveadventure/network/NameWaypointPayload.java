package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client picked a name: turns the stone on (paying shards) or renames it. The server re-checks everything. */
public record NameWaypointPayload(BlockPos pos, String name) implements CustomPacketPayload {
	public static final Type<NameWaypointPayload> TYPE = new Type<>(ProgressiveAdventure.id("name_waypoint"));
	public static final StreamCodec<RegistryFriendlyByteBuf, NameWaypointPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, NameWaypointPayload::pos,
			ByteBufCodecs.STRING_UTF8, NameWaypointPayload::name,
			NameWaypointPayload::new);

	@Override
	public Type<NameWaypointPayload> type() {
		return TYPE;
	}
}
