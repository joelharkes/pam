package com.progressiveadventure.perk;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum PerkCategory {
	MINING("mining", Items.IRON_PICKAXE),
	WOODCUTTING("woodcutting", Items.IRON_AXE),
	DIGGING("digging", Items.IRON_SHOVEL),
	FARMING("farming", Items.IRON_HOE),
	COMBAT("combat", Items.IRON_SWORD),
	SURVIVAL("survival", Items.GOLDEN_APPLE);

	public final String id;
	public final Item icon;

	PerkCategory(String id, Item icon) {
		this.id = id;
		this.icon = icon;
	}
}
