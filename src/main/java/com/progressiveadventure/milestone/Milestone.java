package com.progressiveadventure.milestone;

import net.minecraft.resources.Identifier;

/**
 * A milestone is an advancement (ours or vanilla) plus the reward it grants.
 *
 * @param id         advancement id
 * @param halfHearts max health reward in half hearts
 * @param stomach    stomach upgrades granted
 */
public record Milestone(Identifier id, int halfHearts, int stomach) {
}
