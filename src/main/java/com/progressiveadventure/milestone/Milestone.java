package com.progressiveadventure.milestone;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.resources.Identifier;

/**
 * A milestone is an advancement in our namespace plus the reward it grants.
 *
 * @param path       advancement path under data/progressive_adventure/advancement
 * @param halfHearts max health reward in half hearts
 * @param stomach    stomach upgrades granted
 */
public record Milestone(String path, int halfHearts, int stomach) {
	public Identifier id() {
		return ProgressiveAdventure.id(path);
	}
}
