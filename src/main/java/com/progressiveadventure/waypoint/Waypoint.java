package com.progressiveadventure.waypoint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * A turned-on Waypoint Stone. Shared: every player can see and use every waypoint.
 *
 * @param ownerName shown in the list, so it works while the owner is offline
 * @param shards    shards paid to turn it on, given back when it's broken
 */
public record Waypoint(UUID id, String name, ResourceKey<Level> dimension, BlockPos pos, UUID owner, String ownerName, int shards) {
	/** Client and server agree on this, so it's not in the config. */
	public static final int MAX_NAME_LENGTH = 24;

	public static final Codec<Waypoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("id").forGetter(Waypoint::id),
			Codec.STRING.fieldOf("name").forGetter(Waypoint::name),
			ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Waypoint::dimension),
			BlockPos.CODEC.fieldOf("pos").forGetter(Waypoint::pos),
			UUIDUtil.CODEC.fieldOf("owner").forGetter(Waypoint::owner),
			Codec.STRING.fieldOf("owner_name").forGetter(Waypoint::ownerName),
			Codec.INT.fieldOf("shards").forGetter(Waypoint::shards)
	).apply(instance, Waypoint::new));

	public Waypoint withName(String newName) {
		return new Waypoint(id, newName, dimension, pos, owner, ownerName, shards);
	}

	public boolean isAt(ResourceKey<Level> level, BlockPos blockPos) {
		return dimension.equals(level) && pos.equals(blockPos);
	}

	/** Trims and shortens a name typed by a player; empty if nothing is left. */
	public static String cleanName(String name) {
		String trimmed = name.strip();
		return trimmed.length() > MAX_NAME_LENGTH ? trimmed.substring(0, MAX_NAME_LENGTH).strip() : trimmed;
	}
}
