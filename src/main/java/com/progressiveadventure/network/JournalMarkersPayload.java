package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.UUID;

/** Server sends everything to show on the journal map besides the land: player markers, Waypoint Stones, your bed, treasure. */
public record JournalMarkersPayload(List<Marker> markers) implements CustomPacketPayload {
	public static final Type<JournalMarkersPayload> TYPE = new Type<>(ProgressiveAdventure.id("journal_markers"));

	public static final int KIND_MARKER = 0;
	public static final int KIND_WAYPOINT = 1;
	public static final int KIND_BED = 2;
	public static final int KIND_TREASURE = 3;

	/**
	 * @param id    the marker's id for player markers (to remove it), otherwise random
	 * @param icon  a map decoration sprite for player markers and treasure, otherwise empty
	 * @param mine  a player marker you may remove (yours, or you're an op)
	 */
	public record Marker(UUID id, int kind, String icon, String name, int x, int z, String owner, boolean mine) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Marker> CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, Marker::id,
				ByteBufCodecs.VAR_INT, Marker::kind,
				ByteBufCodecs.STRING_UTF8, Marker::icon,
				ByteBufCodecs.STRING_UTF8, Marker::name,
				ByteBufCodecs.VAR_INT, Marker::x,
				ByteBufCodecs.VAR_INT, Marker::z,
				ByteBufCodecs.STRING_UTF8, Marker::owner,
				ByteBufCodecs.BOOL, Marker::mine,
				Marker::new);
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, JournalMarkersPayload> CODEC =
			Marker.CODEC.apply(ByteBufCodecs.list()).map(JournalMarkersPayload::new, JournalMarkersPayload::markers);

	@Override
	public Type<JournalMarkersPayload> type() {
		return TYPE;
	}
}
