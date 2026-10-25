package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.UUID;

/** Server sends the waypoints you can travel to from the stone at {@code from}, with their XP cost (already worked out). */
public record OpenWaypointListPayload(BlockPos from, String fromName, List<Entry> entries) implements CustomPacketPayload {
	public static final Type<OpenWaypointListPayload> TYPE = new Type<>(ProgressiveAdventure.id("open_waypoint_list"));

	/**
	 * @param dimension dimension id, e.g. "minecraft:the_nether"
	 * @param distance  in blocks, -1 when it's in another dimension
	 */
	public record Entry(UUID id, String name, String owner, String dimension, int distance, int cost) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, Entry::id,
				ByteBufCodecs.STRING_UTF8, Entry::name,
				ByteBufCodecs.STRING_UTF8, Entry::owner,
				ByteBufCodecs.STRING_UTF8, Entry::dimension,
				ByteBufCodecs.VAR_INT, Entry::distance,
				ByteBufCodecs.VAR_INT, Entry::cost,
				Entry::new);
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, OpenWaypointListPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, OpenWaypointListPayload::from,
			ByteBufCodecs.STRING_UTF8, OpenWaypointListPayload::fromName,
			Entry.CODEC.apply(ByteBufCodecs.list()), OpenWaypointListPayload::entries,
			OpenWaypointListPayload::new);

	@Override
	public Type<OpenWaypointListPayload> type() {
		return TYPE;
	}
}
