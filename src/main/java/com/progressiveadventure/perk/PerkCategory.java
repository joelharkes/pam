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
	HEALTH("health", Items.GOLDEN_APPLE),
	SURVIVAL("survival", Items.TOTEM_OF_UNDYING),
	MOVEMENT("movement", Items.IRON_BOOTS),
	EXPLORATION("exploration", Items.FILLED_MAP);

	public final String id;
	public final Item icon;

	PerkCategory(String id, Item icon) {
		this.id = id;
		this.icon = icon;
	}
}
