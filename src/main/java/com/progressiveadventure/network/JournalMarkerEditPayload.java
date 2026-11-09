package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/** Client adds a marker on the journal map (remove = false), or removes the marker with this id (remove = true). */
public record JournalMarkerEditPayload(boolean remove, UUID id, int x, int z, String name, String icon) implements CustomPacketPayload {
	public static final Type<JournalMarkerEditPayload> TYPE = new Type<>(ProgressiveAdventure.id("journal_marker_edit"));

	public static final StreamCodec<RegistryFriendlyByteBuf, JournalMarkerEditPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, JournalMarkerEditPayload::remove,
			UUIDUtil.STREAM_CODEC, JournalMarkerEditPayload::id,
			ByteBufCodecs.VAR_INT, JournalMarkerEditPayload::x,
			ByteBufCodecs.VAR_INT, JournalMarkerEditPayload::z,
			ByteBufCodecs.STRING_UTF8, JournalMarkerEditPayload::name,
			ByteBufCodecs.STRING_UTF8, JournalMarkerEditPayload::icon,
			JournalMarkerEditPayload::new);

	@Override
	public Type<JournalMarkerEditPayload> type() {
		return TYPE;
	}
}
