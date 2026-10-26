package com.progressiveadventure.perk;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum PerkCategory {
	MINING("mining", Items.IRON_PICKAXE),
	WOODCUTTING("woodcutting", Items.IRON_AXE),
	DIGGING("digging", Items.IRON_SHOVEL),
	FARMING("farming", Items.IRON_HOE),
	MELEE("melee", Items.IRON_SWORD),
	ARCHERY("archery", Items.BOW),
	FISHING("fishing", Items.FISHING_ROD),
	DEFENSE("defense", Items.IRON_CHESTPLATE),
	SURVIVAL("survival", Items.GOLDEN_APPLE),
	MOVEMENT("movement", Items.IRON_BOOTS);

	public final String id;
	public final Item icon;

	PerkCategory(String id, Item icon) {
		this.id = id;
		this.icon = icon;
	}
}
