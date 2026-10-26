package com.progressiveadventure.perk;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/**
 * A choosable upgrade. Name and description come from the lang file:
 * {@code perk.progressive_adventure.<id>} and {@code perk.progressive_adventure.<id>.description}.
 *
 * @param prerequisite id of a perk that must be owned first (tier II needs tier I), or null
 * @param group        sub-heading in its category's list ({@code perk_group.progressive_adventure.<group>}), or null
 */
public record Perk(String id, PerkCategory category, Item icon, int cost, Requirement requirement, String prerequisite, String group) {
	public Component name() {
		return Component.translatable("perk.progressive_adventure." + id);
	}

	public Component description() {
		return Component.translatable("perk.progressive_adventure." + id + ".description");
	}
}
