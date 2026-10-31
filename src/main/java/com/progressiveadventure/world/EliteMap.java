package com.progressiveadventure.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/** Treasure and explorer maps elites can drop. Same maps as vanilla's loot tables and cartographer trades. */
public enum EliteMap {
	BURIED_TREASURE(StructureTags.ON_TREASURE_MAPS, MapDecorationTypes.RED_X, 1, "filled_map.buried_treasure"),
	TRIAL_CHAMBERS(StructureTags.ON_TRIAL_CHAMBERS_MAPS, MapDecorationTypes.TRIAL_CHAMBERS, 2, "filled_map.trial_chambers"),
	OCEAN_MONUMENT(StructureTags.ON_OCEAN_EXPLORER_MAPS, MapDecorationTypes.OCEAN_MONUMENT, 2, "filled_map.monument"),
	WOODLAND_MANSION(StructureTags.ON_WOODLAND_EXPLORER_MAPS, MapDecorationTypes.WOODLAND_MANSION, 2, "filled_map.mansion");

	public final String id = name().toLowerCase();
	private final TagKey<Structure> destination;
	private final Holder<MapDecorationType> decoration;
	private final int zoom;
	private final String nameKey;

	EliteMap(TagKey<Structure> destination, Holder<MapDecorationType> decoration, int zoom, String nameKey) {
		this.destination = destination;
		this.decoration = decoration;
		this.zoom = zoom;
		this.nameKey = nameKey;
	}

	public static EliteMap byId(String id) {
		for (EliteMap map : values()) {
			if (map.id.equals(id)) {
				return map;
			}
		}
		return null;
	}

	/**
	 * A map to the nearest structure no map points to yet, or empty when there is none in range. The search runs on
	 * the server thread, like vanilla's exploration_map loot function this copies. These structures only exist in the
	 * Overworld, so the search is skipped elsewhere.
	 */
	public ItemStack create(ServerLevel level, BlockPos from, int searchRadiusChunks) {
		if (level.dimension() != Level.OVERWORLD) {
			return ItemStack.EMPTY;
		}
		BlockPos target = level.findNearestMapStructure(destination, from, searchRadiusChunks, true);
		if (target == null) {
			return ItemStack.EMPTY;
		}
		ItemStack map = MapItem.create(level, target.getX(), target.getZ(), (byte) zoom, true, true);
		MapItem.renderBiomePreviewMap(level, map);
		MapItemSavedData.addTargetDecoration(map, target, "+", decoration);
		map.set(DataComponents.ITEM_NAME, Component.translatable(nameKey));
		return map;
	}
}
