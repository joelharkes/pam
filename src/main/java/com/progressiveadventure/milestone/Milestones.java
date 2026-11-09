package com.progressiveadventure.milestone;

import com.progressiveadventure.ProgressiveAdventure;
import com.progressiveadventure.skill.Skill;

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
	// Structure finds: only the three hard ones give a reward, the others add Adventure Rank.
	public static final Milestone MONUMENT = pam("explorer/monument", 1, 0);
	public static final Milestone ANCIENT_CITY = pam("explorer/ancient_city", 1, 0);
	public static final Milestone MANSION = pam("explorer/mansion", 1, 0);
	// Explorer's Journal: Cartographer is the hard one with a reward; Mapmaker and First Marker only add Adventure Rank
	public static final Milestone CARTOGRAPHER = pam("explorer/cartographer", 1, 0);
	public static final Milestone MAPMAKER = pam("explorer/mapmaker", 0, 0);
	public static final Milestone FIRST_MARKER = pam("explorer/first_marker", 0, 0);
	// Harder Explorer advancements, rank only
	public static final Milestone SPELUNKER = pam("explorer/spelunker", 0, 0);
	public static final Milestone FIVE_PROFESSIONS = pam("explorer/five_professions", 0, 0);
	public static final Milestone MASTER_TRADER = pam("explorer/master_trader", 0, 0);
	public static final Milestone WORLD_ATLAS = pam("explorer/world_atlas", 0, 0);
	public static final Milestone GRAND_NETWORK = pam("explorer/grand_network", 0, 0);
	public static final Milestone LONG_JUMP = pam("explorer/long_jump", 0, 0);

	public static final Milestone MOB_TYPES_5 = pam("hunter/mob_types_5", 1, 0);
	public static final Milestone MOB_TYPES_15 = pam("hunter/mob_types_15", 1, 0);
	public static final Milestone MOB_TYPES_30 = pam("hunter/mob_types_30", 2, 0);
	public static final Milestone ELDER_GUARDIAN = pam("hunter/elder_guardian", 4, 0);
	public static final Milestone WITHER = pam("hunter/wither", 4, 0);
	public static final Milestone ELITE = pam("hunter/elite", 1, 0);

	public static final Milestone DIAMOND_TOOLS = pam("crafter/diamond_tools", 1, 0);

	public static final Milestone TREASURE = pam("angler/treasure", 1, 0);
	public static final Milestone ALL_FISH = pam("angler/all_fish", 1, 0);

	// Bestiary collections: no hearts or stomach, they only add Adventure Rank. Not in ALL (that's the reward list).
	public static final Milestone NIGHT_WATCH = pam("hunter/night_watch", 0, 0);
	public static final Milestone OCEAN_DEEP = pam("hunter/ocean_deep", 0, 0);
	public static final Milestone RAIDERS = pam("hunter/raiders", 0, 0);
	public static final Milestone NETHER_HUNTER = pam("hunter/nether_hunter", 0, 0);
	public static final Milestone END_HUNTER = pam("hunter/end_hunter", 0, 0);
	public static final Milestone FIRST_MASTER = pam("hunter/first_master", 0, 0);
	public static final Milestone ALL_TRAITS = pam("hunter/all_traits", 0, 0);
	public static final Milestone ELITE_TYPES = pam("hunter/elite_types", 0, 0);

	// Walking (Explorer): on-foot distance from vanilla statistics, rank only, see WALK_KM_THRESHOLDS
	public static final Milestone WALK_5 = pam("explorer/walk_5", 0, 0);
	public static final Milestone WALK_25 = pam("explorer/walk_25", 0, 0);
	public static final Milestone WALK_50 = pam("explorer/walk_50", 0, 0);
	public static final Milestone WALK_100 = pam("explorer/walk_100", 0, 0);
	public static final Milestone WALK_250 = pam("explorer/walk_250", 0, 0);

	// Challenges tab: rank only, granted from Challenges
	public static final Milestone UNTOUCHABLE = pam("challenges/untouchable", 0, 0);
	public static final Milestone NAILED_THE_LANDING = pam("challenges/nailed_the_landing", 0, 0);
	public static final Milestone DEFUSED = pam("challenges/defused", 0, 0);
	public static final Milestone SHARPSHOOTER = pam("challenges/sharpshooter", 0, 0);
	public static final Milestone COMEBACK = pam("challenges/comeback", 0, 0);
	public static final Milestone GHOST = pam("challenges/ghost", 0, 0);
	public static final Milestone BRAVE = pam("challenges/brave", 0, 0);
	public static final Milestone NOT_A_SCRATCH = pam("challenges/not_a_scratch", 0, 0);
	public static final Milestone SAFE_AND_SOUND = pam("challenges/safe_and_sound", 0, 0);
	public static final Milestone PERFECT_PARRY = pam("challenges/perfect_parry", 0, 0);
	public static final Milestone RIGHT_BACK_AT_YOU = pam("challenges/right_back_at_you", 0, 0);
	public static final Milestone PAYBACK = pam("challenges/payback", 0, 0);
	public static final Milestone ARROW_WALL = pam("challenges/arrow_wall", 0, 0);
	public static final Milestone BARE_KNUCKLES = pam("challenges/bare_knuckles", 0, 0);
	public static final Milestone TWO_FOR_ONE = pam("challenges/two_for_one", 0, 0);
	public static final Milestone QUICK_HANDS = pam("challenges/quick_hands", 0, 0);
	public static final Milestone SHOCKWAVE_COMBO = pam("challenges/shockwave_combo", 0, 0);
	public static final Milestone TRICK_SHOT = pam("challenges/trick_shot", 0, 0);
	public static final Milestone SKEWERED = pam("challenges/skewered", 0, 0);

	// Skills tab: rank only, like the collections. Per skill "skills/<skill>_<level>" at SKILL_LEVELS, see skillLevel.
	public static final Milestone ALL_SKILLS_10 = pam("skills/all_10", 0, 0);
	public static final Milestone ALL_SKILLS_25 = pam("skills/all_25", 0, 0);

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
			vanilla("story/follow_ender_eye", 1, 0),
			vanilla("adventure/minecraft_trials_edition", 1, 0),
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
			BIOMES_5, BIOMES_15, BIOMES_30, FIND_VILLAGE, WAYPOINT, FIRST_TRIP, NETWORK, MONUMENT, ANCIENT_CITY, MANSION, CARTOGRAPHER,
			MOB_TYPES_5, MOB_TYPES_15, MOB_TYPES_30, ELDER_GUARDIAN, WITHER, ELITE,
			DIAMOND_TOOLS,
			TREASURE, ALL_FISH), VANILLA.stream()).toList();

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
	public static final List<Threshold> WAYPOINT_THRESHOLDS = List.of(new Threshold(5, NETWORK), new Threshold(10, GRAND_NETWORK));
	/** Explorer's Journal chunks you drew first. */
	/** Kilometres on foot. */
	public static final List<Threshold> WALK_KM_THRESHOLDS = List.of(
			new Threshold(5, WALK_5), new Threshold(25, WALK_25), new Threshold(50, WALK_50),
			new Threshold(100, WALK_100), new Threshold(250, WALK_250));
	/** Per-skill levels. Never the same as an ALL_SKILLS level, so two never unlock at once. */
	public static final List<Integer> SKILL_LEVELS = List.of(15, 30, 50);
	/** The lowest of all skill levels. */
	public static final List<Threshold> ALL_SKILLS_THRESHOLDS = List.of(new Threshold(10, ALL_SKILLS_10), new Threshold(25, ALL_SKILLS_25));
	public static final List<Threshold> CHUNKS_DRAWN_THRESHOLDS = List.of(new Threshold(100, MAPMAKER), new Threshold(1000, CARTOGRAPHER),
			new Threshold(5000, WORLD_ATLAS));
	/** Villager professions traded with. */
	public static final List<Threshold> PROFESSION_THRESHOLDS = List.of(new Threshold(5, FIVE_PROFESSIONS));
	/** Mob types with an elite defeated. Not all of them: cave spiders are elites only in the config, they never spawn naturally. */
	public static final List<Threshold> ELITE_TYPE_THRESHOLDS = List.of(new Threshold(8, ELITE_TYPES));

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

	public static Milestone skillLevel(Skill skill, int level) {
		return pam("skills/" + skill.id + "_" + level, 0, 0);
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
