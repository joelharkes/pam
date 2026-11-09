package com.progressiveadventure.skill;

import com.progressiveadventure.ProgressiveAdventure;
import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.milestone.Milestones;
import com.progressiveadventure.perk.Perks;
import com.progressiveadventure.world.Elites;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Fishing skill: xp per catch, faster bites as the skill bonus (starting at vanilla speed, waiting is already the
 * slow part), and the fishing perks. Lucky is an attribute perk (vanilla luck), applied in Progression. Sunken Riches
 * adds an extra find to some catches, from a list in the config that grows with each tier.
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

	/** Ticks the fish takes to swim up to the hook once it has noticed it. Sea's Favor makes it faster. */
	public static int swimUpTicks(ServerPlayer player, int ticks) {
		if (!Perks.has(player, Perks.SEAS_FAVOR)) {
			return ticks;
		}
		return Math.max(1, (int) Math.round(ticks * PamConfig.get().seasFavorSwimUpMultiplier));
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

	/**
	 * A catch was reeled in: skill xp, Double Catch, Sunken Riches and the treasure milestone. Returns the (maybe
	 * bigger) catch. openWater already counts Treasure Hunter. A riches find gives treasure xp and counts as treasure.
	 */
	public static List<ItemStack> onCatch(ServerPlayer player, List<ItemStack> items, boolean openWater) {
		PamConfig config = PamConfig.get();
		List<ItemStack> result = new ArrayList<>(items);
		ItemStack riches = openWater ? rollRiches(player) : ItemStack.EMPTY;
		boolean treasure = !riches.isEmpty() || items.stream().anyMatch(Fishing::isTreasure);
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
		if (!riches.isEmpty()) {
			result.add(riches);
		}
		return result;
	}

	/** Campfire Catch: cod and salmon come out cooked. Runs after the advancement trigger, so Fish Collector still counts the raw fish. */
	public static ItemStack cook(ServerPlayer player, ItemStack stack) {
		if (!Perks.has(player, Perks.CAMPFIRE_CATCH)) {
			return stack;
		}
		if (stack.is(Items.COD)) {
			return stack.transmuteCopy(Items.COOKED_COD);
		}
		if (stack.is(Items.SALMON)) {
			return stack.transmuteCopy(Items.COOKED_SALMON);
		}
		return stack;
	}

	/** Sunken Riches: maybe a find from the riches list, by weight among the entries of the tiers owned. */
	private static ItemStack rollRiches(ServerPlayer player) {
		int tier = Perks.has(player, Perks.SUNKEN_RICHES_3) ? 3 : Perks.has(player, Perks.SUNKEN_RICHES_2) ? 2 : Perks.has(player, Perks.SUNKEN_RICHES_1) ? 1 : 0;
		PamConfig config = PamConfig.get();
		RandomSource random = player.getRandom();
		double chance = Perks.has(player, Perks.SEAS_FAVOR) ? config.seasFavorRichesChance : config.sunkenRichesChance;
		if (tier == 0 || random.nextDouble() >= chance) {
			return ItemStack.EMPTY;
		}
		List<PamConfig.RichesDrop> options = new ArrayList<>(config.sunkenRiches.stream()
				.filter(drop -> drop.tier() <= tier && drop.weight() > 0).toList());
		// A map with no structure in range (or an unknown item) gives one other entry a try
		for (int attempt = 0; attempt < 2 && !options.isEmpty(); attempt++) {
			PamConfig.RichesDrop drop = pickByWeight(options, random);
			options.remove(drop);
			ItemStack stack = createRiches(player, drop, random);
			if (!stack.isEmpty()) {
				boolean map = drop.item().equals("map");
				player.sendOverlayMessage(Component.translatable(map ? "message.progressive_adventure.message_in_a_bottle" : "message.progressive_adventure.sunken_riches",
						stack.getHoverName()).withStyle(ChatFormatting.AQUA));
				player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static ItemStack createRiches(ServerPlayer player, PamConfig.RichesDrop drop, RandomSource random) {
		if (drop.item().equals("map")) {
			return Elites.rollMap((ServerLevel) player.level(), player.blockPosition(), PamAttachments.snapshot(player).rank(), random);
		}
		int count = drop.min() + random.nextInt(Math.max(1, drop.max() - drop.min() + 1));
		boolean tag = drop.item().startsWith("#");
		Identifier id = Identifier.tryParse(tag ? drop.item().substring(1) : drop.item());
		if (id == null) {
			return ItemStack.EMPTY;
		}
		if (tag) {
			return BuiltInRegistries.ITEM.getRandomElementOf(TagKey.create(Registries.ITEM, id), random)
					.map(item -> new ItemStack(item, count)).orElse(ItemStack.EMPTY);
		}
		return BuiltInRegistries.ITEM.getOptional(id).map(item -> new ItemStack(item, count)).orElse(ItemStack.EMPTY);
	}

	private static PamConfig.RichesDrop pickByWeight(List<PamConfig.RichesDrop> options, RandomSource random) {
		int roll = random.nextInt(options.stream().mapToInt(PamConfig.RichesDrop::weight).sum());
		for (PamConfig.RichesDrop drop : options) {
			roll -= drop.weight();
			if (roll < 0) {
				return drop;
			}
		}
		throw new IllegalStateException("Roll is below the total weight");
	}
}
