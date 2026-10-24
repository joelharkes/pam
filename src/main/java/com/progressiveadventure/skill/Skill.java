package com.progressiveadventure.skill;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Tool skills: trained by breaking matching blocks with the matching tool, rewarded with faster breaking. */
public enum Skill {
	MINING("mining", ItemTags.PICKAXES, BlockTags.MINEABLE_WITH_PICKAXE),
	WOODCUTTING("woodcutting", ItemTags.AXES, BlockTags.MINEABLE_WITH_AXE),
	DIGGING("digging", ItemTags.SHOVELS, BlockTags.MINEABLE_WITH_SHOVEL);

	public final String id;
	public final TagKey<Item> tools;
	public final TagKey<Block> blocks;

	Skill(String id, TagKey<Item> tools, TagKey<Block> blocks) {
		this.id = id;
		this.tools = tools;
		this.blocks = blocks;
	}
}
