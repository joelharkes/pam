package com.progressiveadventure.waypoint;

import com.progressiveadventure.ProgressiveAdventure;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

/** The mod's own items and blocks. */
public final class WaypointContent {
	public static final Block WAYPOINT_STONE = registerBlock("waypoint_stone", BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			// Creepers and TNT can't break it; only the owner (or an op) can
			.strength(3.5f, 3_600_000f)
			.sound(SoundType.LODESTONE)
			.pushReaction(PushReaction.BLOCK)
			.lightLevel(state -> state.getValue(WaypointStoneBlock.ACTIVE) ? 7 : 0));

	public static final Item WAYPOINT_STONE_ITEM = registerItem("waypoint_stone",
			properties -> new BlockItem(WAYPOINT_STONE, properties.useBlockDescriptionPrefix()), new Item.Properties());
	public static final Item WAYPOINT_SHARD = registerItem("waypoint_shard", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));

	private WaypointContent() {
	}

	private static Block registerBlock(String name, BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ProgressiveAdventure.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, new WaypointStoneBlock(properties.setId(key)));
	}

	private static Item registerItem(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ProgressiveAdventure.id(name));
		Item item = factory.apply(properties.setId(key));
		if (item instanceof BlockItem blockItem) {
			blockItem.registerBlocks(Item.BY_BLOCK, item);
		}
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.insertAfter(Items.LODESTONE, WAYPOINT_STONE_ITEM));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.insertAfter(Items.ECHO_SHARD, WAYPOINT_SHARD));
	}
}
