package com.progressiveadventure.milestone;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * All milestones and their rewards: our own advancements plus vanilla advancements that grant a reward (so the
 * same achievement never shows up twice). The advancement JSON defines how a milestone is completed; ours
 * with an "impossible" criterion are granted from code via {@link #grant}.
 */
public final class Milestones {
	public static final Milestone FIRST_NIGHT = pam("survivor/first_night", 1, 0);
	public static final Milestone IRON_ARMOR = pam("survivor/iron_armor", 1, 0);

	public static final Milestone FIRST_FOOD = pam("gourmet/first_food", 0, 1);
	public static final Milestone FOODS_3 = pam("gourmet/foods_3", 0, 1);
	public static final Milestone FOODS_8 = pam("gourmet/foods_8", 0, 1);
	public static final Milestone FOODS_15 = pam("gourmet/foods_15", 0, 1);
	public static final Milestone FOODS_25 = pam("gourmet/foods_25", 2, 0);

	public static final Milestone CROPS_3 = pam("farmer/crops_3", 0, 1);
	public static final Milestone CROPS_10 = pam("farmer/crops_10", 1, 0);
	public static final Milestone BREED_5 = pam("farmer/breed_5", 1, 0);

	public static final Milestone BIOMES_5 = pam("explorer/biomes_5", 1, 0);
	public static final Milestone BIOMES_15 = pam("explorer/biomes_15", 1, 0);
	public static final Milestone BIOMES_30 = pam("explorer/biomes_30", 2, 0);
	public static final Milestone FIND_VILLAGE = pam("explorer/find_village", 1, 0);
	public static final Milestone WAYPOINT = pam("explorer/waypoint", 1, 0);
	public static final Milestone FIRST_TRIP = pam("explorer/first_trip", 0, 1);
	public static final Milestone NETWORK = pam("explorer/network", 2, 0);

	public static final Milestone MOB_TYPES_5 = pam("hunter/mob_types_5", 1, 0);
	public static final Milestone MOB_TYPES_15 = pam("hunter/mob_types_15", 1, 0);
	public static final Milestone MOB_TYPES_30 = pam("hunter/mob_types_30", 2, 0);
	public static final Milestone ELDER_GUARDIAN = pam("hunter/elder_guardian", 4, 0);
	public static final Milestone WITHER = pam("hunter/wither", 4, 0);
	public static final Milestone ELITE = pam("hunter/elite", 1, 0);

	public static final Milestone DIAMOND_TOOLS = pam("crafter/diamond_tools", 1, 0);

	// Vanilla advancements with a reward. Their descriptions are overridden in our lang file to show it.
	public static final List<Milestone> VANILLA = List.of(
			vanilla("adventure/sleep_in_bed", 1, 0),
			vanilla("adventure/trade", 1, 0),
			vanilla("adventure/hero_of_the_village", 2, 0),
			vanilla("story/iron_tools", 1, 0),
			vanilla("story/mine_diamond", 1, 0),
			vanilla("story/deflect_arrow", 1, 0),
			vanilla("story/enchant_item", 1, 0),
			vanilla("story/cure_zombie_villager", 2, 0),
			vanilla("story/enter_the_nether", 2, 0),
			vanilla("story/enter_the_end", 2, 0),
			vanilla("husbandry/tame_an_animal", 1, 0),
			vanilla("husbandry/fishy_business", 1, 0),
			vanilla("husbandry/balanced_diet", 0, 1),
			vanilla("nether/brew_potion", 1, 0),
			vanilla("nether/find_fortress", 1, 0),
			vanilla("nether/find_bastion", 1, 0),
			vanilla("end/find_end_city", 2, 0),
			vanilla("end/kill_dragon", 4, 0));

	public static final List<Milestone> ALL = Stream.concat(Stream.of(
			FIRST_NIGHT, IRON_ARMOR,
			FIRST_FOOD, FOODS_3, FOODS_8, FOODS_15, FOODS_25,
			CROPS_3, CROPS_10, BREED_5,
			BIOMES_5, BIOMES_15, BIOMES_30, FIND_VILLAGE, WAYPOINT, FIRST_TRIP, NETWORK,
			MOB_TYPES_5, MOB_TYPES_15, MOB_TYPES_30, ELDER_GUARDIAN, WITHER, ELITE,
			DIAMOND_TOOLS), VANILLA.stream()).toList();

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
	/** Turned-on stones you own. */
	public static final List<Threshold> WAYPOINT_THRESHOLDS = List.of(new Threshold(5, NETWORK));

	public record Threshold(int count, Milestone milestone) {
	}

	private Milestones() {
	}

	private static Milestone pam(String path, int halfHearts, int stomach) {
		return new Milestone(ProgressiveAdventure.id(path), halfHearts, stomach);
	}

	private static Milestone vanilla(String path, int halfHearts, int stomach) {
		return new Milestone(Identifier.withDefaultNamespace(path), halfHearts, stomach);
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
