package com.progressiveadventure.milestone;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * All milestones and their rewards. The advancement JSON defines how a milestone is completed; milestones
 * with an "impossible" criterion are granted from code via {@link #grant}.
 */
public final class Milestones {
	public static final Milestone FIRST_NIGHT = new Milestone("survivor/first_night", 1, 0);
	public static final Milestone SLEEP_IN_BED = new Milestone("survivor/sleep_in_bed", 1, 0);
	public static final Milestone IRON_ARMOR = new Milestone("survivor/iron_armor", 1, 0);

	public static final Milestone FIRST_FOOD = new Milestone("gourmet/first_food", 0, 1);
	public static final Milestone FOODS_3 = new Milestone("gourmet/foods_3", 0, 1);
	public static final Milestone FOODS_8 = new Milestone("gourmet/foods_8", 0, 1);
	public static final Milestone FOODS_15 = new Milestone("gourmet/foods_15", 0, 1);
	public static final Milestone FOODS_25 = new Milestone("gourmet/foods_25", 2, 0);

	public static final Milestone CROPS_3 = new Milestone("farmer/crops_3", 0, 1);
	public static final Milestone CROPS_10 = new Milestone("farmer/crops_10", 1, 0);
	public static final Milestone BREED_5 = new Milestone("farmer/breed_5", 1, 0);

	public static final Milestone BIOMES_5 = new Milestone("explorer/biomes_5", 1, 0);
	public static final Milestone BIOMES_15 = new Milestone("explorer/biomes_15", 1, 0);
	public static final Milestone BIOMES_30 = new Milestone("explorer/biomes_30", 2, 0);
	public static final Milestone FIND_VILLAGE = new Milestone("explorer/find_village", 1, 0);
	public static final Milestone ENTER_NETHER = new Milestone("explorer/enter_nether", 2, 0);
	public static final Milestone ENTER_END = new Milestone("explorer/enter_end", 2, 0);

	public static final Milestone MOB_TYPES_5 = new Milestone("hunter/mob_types_5", 1, 0);
	public static final Milestone MOB_TYPES_15 = new Milestone("hunter/mob_types_15", 1, 0);
	public static final Milestone MOB_TYPES_30 = new Milestone("hunter/mob_types_30", 2, 0);
	public static final Milestone ELDER_GUARDIAN = new Milestone("hunter/elder_guardian", 4, 0);
	public static final Milestone WITHER = new Milestone("hunter/wither", 4, 0);
	public static final Milestone DRAGON = new Milestone("hunter/dragon", 4, 0);

	public static final Milestone IRON_TOOLS = new Milestone("crafter/iron_tools", 1, 0);
	public static final Milestone DIAMOND_TOOLS = new Milestone("crafter/diamond_tools", 1, 0);
	public static final Milestone ENCHANT = new Milestone("crafter/enchant", 1, 0);
	public static final Milestone BREW = new Milestone("crafter/brew", 1, 0);

	public static final Milestone TRADE = new Milestone("social/trade", 1, 0);

	public static final List<Milestone> ALL = List.of(
			FIRST_NIGHT, SLEEP_IN_BED, IRON_ARMOR,
			FIRST_FOOD, FOODS_3, FOODS_8, FOODS_15, FOODS_25,
			CROPS_3, CROPS_10, BREED_5,
			BIOMES_5, BIOMES_15, BIOMES_30, FIND_VILLAGE, ENTER_NETHER, ENTER_END,
			MOB_TYPES_5, MOB_TYPES_15, MOB_TYPES_30, ELDER_GUARDIAN, WITHER, DRAGON,
			IRON_TOOLS, DIAMOND_TOOLS, ENCHANT, BREW,
			TRADE);

	private static final Map<Identifier, Milestone> BY_ID = ALL.stream().collect(Collectors.toMap(Milestone::id, Function.identity()));

	/** Code-driven count milestones, checked whenever the matching counter grows. */
	public static final List<Threshold> FOOD_THRESHOLDS = List.of(
			new Threshold(1, FIRST_FOOD), new Threshold(3, FOODS_3), new Threshold(8, FOODS_8),
			new Threshold(15, FOODS_15), new Threshold(25, FOODS_25));
	public static final List<Threshold> CROP_THRESHOLDS = List.of(
			new Threshold(3, CROPS_3), new Threshold(10, CROPS_10));
	public static final List<Threshold> BREED_THRESHOLDS = List.of(new Threshold(5, BREED_5));
	public static final List<Threshold> BIOME_THRESHOLDS = List.of(
			new Threshold(5, BIOMES_5), new Threshold(15, BIOMES_15), new Threshold(30, BIOMES_30));
	public static final List<Threshold> MOB_TYPE_THRESHOLDS = List.of(
			new Threshold(5, MOB_TYPES_5), new Threshold(15, MOB_TYPES_15), new Threshold(30, MOB_TYPES_30));

	public record Threshold(int count, Milestone milestone) {
	}

	private Milestones() {
	}

	public static Milestone byId(Identifier id) {
		return BY_ID.get(id);
	}

	public static boolean isDone(ServerPlayer player, Milestone milestone) {
		AdvancementHolder holder = player.level().getServer().getAdvancements().get(milestone.id());
		return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	/** Completes a code-driven milestone (its advancement uses a single "impossible" criterion named "granted"). */
	public static void grant(ServerPlayer player, Milestone milestone) {
		AdvancementHolder holder = player.level().getServer().getAdvancements().get(milestone.id());
		if (holder != null) {
			player.getAdvancements().award(holder, "granted");
		}
	}

	public static void checkThresholds(ServerPlayer player, List<Threshold> thresholds, int count) {
		for (Threshold threshold : thresholds) {
			if (count >= threshold.count()) {
				grant(player, threshold.milestone());
			}
		}
	}
}
