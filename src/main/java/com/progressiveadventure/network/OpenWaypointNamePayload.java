package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server tells the client to ask for a waypoint name: to turn a stone on (shardCost > 0, empty name) or to
 * rename one (shardCost 0, the current name).
 */
public record OpenWaypointNamePayload(BlockPos pos, int shardCost, String currentName) implements CustomPacketPayload {
	public static final Type<OpenWaypointNamePayload> TYPE = new Type<>(ProgressiveAdventure.id("open_waypoint_name"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenWaypointNamePayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, OpenWaypointNamePayload::pos,
			ByteBufCodecs.VAR_INT, OpenWaypointNamePayload::shardCost,
			ByteBufCodecs.STRING_UTF8, OpenWaypointNamePayload::currentName,
			OpenWaypointNamePayload::new);

	@Override
	public Type<OpenWaypointNamePayload> type() {
		return TYPE;
	}
}
