package com.progressiveadventure.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;

import java.util.List;
import java.util.UUID;

/**
 * A marker a player put on the shared journal map. Shared like the map: everyone sees it, only the owner (or an op)
 * removes it.
 *
 * @param icon one of {@link #ICONS}: a vanilla map decoration sprite
 */
public record JournalMarker(UUID id, String name, int x, int z, String icon, UUID owner, String ownerName) {
	/** Client and server agree on these, so they're not in the config. */
	public static final int MAX_NAME_LENGTH = 24;
	public static final List<String> ICONS = List.of("target_x", "red_banner", "orange_banner", "yellow_banner", "lime_banner",
			"light_blue_banner", "blue_banner", "purple_banner", "white_banner");

	public static final Codec<JournalMarker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("id").forGetter(JournalMarker::id),
			Codec.STRING.fieldOf("name").forGetter(JournalMarker::name),
			Codec.INT.fieldOf("x").forGetter(JournalMarker::x),
			Codec.INT.fieldOf("z").forGetter(JournalMarker::z),
			Codec.STRING.fieldOf("icon").forGetter(JournalMarker::icon),
			UUIDUtil.CODEC.fieldOf("owner").forGetter(JournalMarker::owner),
			Codec.STRING.fieldOf("owner_name").forGetter(JournalMarker::ownerName)
	).apply(instance, JournalMarker::new));

	/** Trims and shortens a name typed by a player; empty if nothing is left. */
	public static String cleanName(String name) {
		String trimmed = name.strip();
		return trimmed.length() > MAX_NAME_LENGTH ? trimmed.substring(0, MAX_NAME_LENGTH).strip() : trimmed;
	}
}
