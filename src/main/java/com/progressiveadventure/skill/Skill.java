package com.progressiveadventure.skill;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Skills level up with use. Tool skills are trained by breaking matching blocks with the matching tool and
 * rewarded with faster breaking. Farming rewards extra harvests (see {@link Farming}), Melee faster attacks (see
 * {@link Melee}), Archery a faster bow, crossbow and trident draw (see {@link Archery}), Fishing faster bites (see
 * {@link Fishing}). The order is the order on the Character tab; the enum name matches its {@code PerkCategory}.
 */
public enum Skill {
	MINING("mining", ItemTags.PICKAXES, BlockTags.MINEABLE_WITH_PICKAXE),
	WOODCUTTING("woodcutting", ItemTags.AXES, BlockTags.MINEABLE_WITH_AXE),
	DIGGING("digging", ItemTags.SHOVELS, BlockTags.MINEABLE_WITH_SHOVEL),
	FARMING("farming", null, null),
	MELEE("melee", null, null),
	ARCHERY("archery", null, null),
	FISHING("fishing", null, null);

	public final String id;
	public final TagKey<Item> tools;
	public final TagKey<Block> blocks;

	Skill(String id, TagKey<Item> tools, TagKey<Block> blocks) {
		this.id = id;
		this.tools = tools;
		this.blocks = blocks;
	}

	public boolean isToolSkill() {
		return tools != null;
	}
}
