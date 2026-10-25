package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/** Client picked a destination at the stone at {@code from}. The server re-checks the cost and starts the charge-up. */
public record WaypointTeleportPayload(BlockPos from, UUID target) implements CustomPacketPayload {
	public static final Type<WaypointTeleportPayload> TYPE = new Type<>(ProgressiveAdventure.id("waypoint_teleport"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WaypointTeleportPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, WaypointTeleportPayload::from,
			UUIDUtil.STREAM_CODEC, WaypointTeleportPayload::target,
			WaypointTeleportPayload::new);

	@Override
	public Type<WaypointTeleportPayload> type() {
		return TYPE;
	}
}
