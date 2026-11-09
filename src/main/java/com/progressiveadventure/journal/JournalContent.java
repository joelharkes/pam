package com.progressiveadventure.journal;

import com.progressiveadventure.ProgressiveAdventure;
import com.progressiveadventure.config.PamConfig;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.item.v1.ItemComponentTooltipProviderRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** The Explorer's Journal item and its ink component. */
public final class JournalContent {
	public static final DataComponentType<JournalInk> INK = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			ProgressiveAdventure.id("journal_ink"),
			DataComponentType.<JournalInk>builder().persistent(JournalInk.CODEC).networkSynchronized(JournalInk.STREAM_CODEC).build());

	public static final Item EXPLORERS_JOURNAL = registerJournal();

	private JournalContent() {
	}

	private static Item registerJournal() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ProgressiveAdventure.id("explorers_journal"));
		PamConfig config = PamConfig.get();
		// A new journal comes with the ink of the ink sac in its recipe
		Item.Properties properties = new Item.Properties().stacksTo(1)
				.component(INK, new JournalInk(Math.min(config.journalStartInk, config.journalMaxInk), config.journalMaxInk))
				.setId(key);
		return Registry.register(BuiltInRegistries.ITEM, key, new ExplorersJournalItem(properties));
	}

	public static void init() {
		ItemComponentTooltipProviderRegistry.addLast(INK);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.insertAfter(Items.MAP, EXPLORERS_JOURNAL));
	}
}
