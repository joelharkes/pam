package com.progressiveadventure.skill;

import com.progressiveadventure.ProgressiveAdventure;
import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.milestone.Milestones;
import com.progressiveadventure.perk.Perks;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Fishing skill: xp per catch, faster bites as the skill bonus (starting at vanilla speed, waiting is already the
 * slow part), and the fishing perks. Lucky is an attribute perk (vanilla luck), applied in Progression.
 */
public final class Fishing {
	/** Vanilla's treasure loot. Bows and rods only count when enchanted (the junk rod is a damaged plain one). */
	private static final TagKey<Item> TREASURE = TagKey.create(Registries.ITEM, ProgressiveAdventure.id("fishing_treasure"));

	private Fishing() {
	}

	/** Ticks until a fish swims up, shortened by the skill: +50% bite speed = two thirds of the wait. */
	public static int waitTicks(ServerPlayer player, int ticks) {
		return (int) Math.round(ticks / (1 + Skills.bonus(Skill.FISHING, Skills.level(player, Skill.FISHING))));
	}

	/** Ticks the fish stays on the hook. Keen Eye rings a bell when it bites and gives more time to reel in. */
	public static int reelTicks(ServerPlayer player, int ticks) {
		if (!Perks.has(player, Perks.KEEN_EYE)) {
			return ticks;
		}
		player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.7f, 1.6f);
		return ticks + PamConfig.get().keenEyeExtraTicks;
	}

	/** Treasure needs open water unless the player has Treasure Hunter. */
	public static boolean treasureAnywhere(ServerPlayer player) {
		return Perks.has(player, Perks.TREASURE_HUNTER);
	}

	public static boolean isTreasure(ItemStack stack) {
		return stack.is(TREASURE) || stack.isEnchanted();
	}

	/** A catch was reeled in: skill xp, Double Catch and the treasure milestone. Returns the (maybe bigger) catch. */
	public static List<ItemStack> onCatch(ServerPlayer player, List<ItemStack> items) {
		PamConfig config = PamConfig.get();
		List<ItemStack> result = new ArrayList<>(items);
		boolean treasure = items.stream().anyMatch(Fishing::isTreasure);
		boolean fish = items.stream().anyMatch(stack -> stack.is(ItemTags.FISHES));
		Skills.addXp(player, Skill.FISHING, treasure ? config.fishingTreasureXp : fish ? config.fishingFishXp : config.fishingJunkXp);
		if (treasure) {
			Milestones.grant(player, Milestones.TREASURE);
		}
		if (fish && Perks.has(player, Perks.DOUBLE_CATCH) && player.getRandom().nextDouble() < config.doubleCatchChance) {
			for (ItemStack stack : items) {
				if (stack.is(ItemTags.FISHES)) {
					result.add(stack.copy());
				}
			}
		}
		return result;
	}
}
