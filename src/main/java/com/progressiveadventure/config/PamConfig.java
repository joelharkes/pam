package com.progressiveadventure.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.progressiveadventure.ProgressiveAdventure;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * All balance numbers. Loaded from config/progressive_adventure.json; missing fields keep their defaults.
 */
public class PamConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(ProgressiveAdventure.MOD_ID + ".json");

	private static PamConfig instance = new PamConfig();

	// Vitality
	public int startingHearts = 5;
	public int maxHearts = 20;

	// Metabolism: index = stomach level. Values past the end reuse the last entry.
	public List<Double> stomachExhaustionMultipliers = List.of(1.5, 1.35, 1.2, 1.0, 0.9, 0.8, 0.7);
	public List<Double> stomachSaturationMultipliers = List.of(0.6, 0.7, 0.8, 1.0, 1.1, 1.2, 1.3);

	// Proficiency: level = floor(sqrt(xp / xpPerLevelFactor));
	// speed bonus = minSpeedBonus + (maxSpeedBonus - minSpeedBonus) * (1 - e^(-level / speedBonusCurve))
	public double xpPerLevelFactor = 4.0;
	public double minSpeedBonus = -0.2;
	public double maxSpeedBonus = 0.6;
	public double speedBonusCurve = 15.0;
	public int oreXp = 3;

	// Farming: extra harvest chance uses the same curve as speedBonusCurve, but starts at 0
	public double farmingMaxHarvestBonus = 0.5;
	public int melonPumpkinXp = 2;
	public int breedingXp = 5;

	// Archery: draw speed bonus uses the same curve as speedBonusCurve, but starts at 0. Xp only for arrows a
	// player shot that hit a mob (not players or armor stands); a kill gives archeryKillXp on top.
	public double archeryMaxDrawBonus = 0.5;
	public int archeryHitXp = 3;
	public int archeryKillXp = 2;

	// Bestiary: damage modifier per kill threshold, sorted by kills
	public List<FamiliarityTier> familiarityTiers = List.of(
			new FamiliarityTier(0, -0.15, "unknown"),
			new FamiliarityTier(1, -0.10, "unfamiliar"),
			new FamiliarityTier(3, -0.05, "unfamiliar"),
			new FamiliarityTier(5, 0.0, "known"),
			new FamiliarityTier(15, 0.10, "familiar"),
			new FamiliarityTier(30, 0.20, "familiar"),
			new FamiliarityTier(60, 0.35, "expert"),
			new FamiliarityTier(100, 0.50, "master"),
			new FamiliarityTier(250, 0.55, "master"));

	// Perks: points per completed advancement by frame
	public int perkPointsTask = 1;
	public int perkPointsGoal = 2;
	public int perkPointsChallenge = 3;

	// Perk effects. Index = perk tier - 1.
	public List<Double> sturdyChances = List.of(0.1, 0.2, 0.3);
	public List<Double> prospectorChances = List.of(0.1, 0.2);
	public int timberMaxLogs = 64;
	// Lumberjack: chopping a log also chops up to this many connected logs (Timber I)
	public int lumberjackMaxLogs = 6;
	// Orchard: chance per natural leaf block you break
	public double orchardAppleChance = 0.05;
	public double orchardGoldenAppleChance = 0.0004;
	// Bird's Nest: chance per natural log you chop with an axe; a nest has 1-2 finds, emeralds are rarer
	public double birdsNestChance = 0.02;
	public double birdsNestEmeraldChance = 0.1;
	// Flint Finder: chance of an extra flint per natural gravel block (vanilla's own 10% still applies)
	public double flintFinderChance = 0.3;
	// Vein Miner: most extra ore blocks mined with one ore
	public int veinMinerMaxBlocks = 16;
	public double archaeologistChance = 0.03;
	public float secondWindHealthThreshold = 6f;
	public int secondWindCooldownSeconds = 120;
	// Oxygen bonus works like Respiration: each point adds ~15s of air
	public List<Double> deepBreathOxygenBonuses = List.of(1.0, 2.0, 3.0);
	public double softLandingBlocks = 3.0;
	public List<Double> fleetFootSpeedBonuses = List.of(0.05, 0.10, 0.15);
	// Jump strength is 0.42 by default (1.25 blocks); +0.08 jumps about 1.7 blocks, over fences
	public double springStepJumpBonus = 0.08;
	// Step height is 0.6 by default; 1.0 walks up full blocks
	public double stepUpHeightBonus = 0.4;
	// Movement efficiency 1 = soul sand and honey don't slow you down
	public double sureFeetEfficiency = 1.0;
	// Sneaking speed is 0.3 of walking speed by default
	public double sneakyFeetSpeedBonus = 0.2;
	// Shadow Step I: while sneaking, mobs notice you from this share of their normal distance (vanilla sneaking
	// already gives 0.8, this multiplies on top)
	public double shadowStepVisibility = 0.5;
	// Shadow Step II: a mob chasing you gives up when you sneak further away than this, and then can't see you
	// (while you sneak) for this long unless you hit it
	public double shadowStepEscapeDistance = 8.0;
	public int shadowStepForgetSeconds = 10;
	// Water movement efficiency 1 = Depth Strider III
	public List<Double> swimmerEfficiencies = List.of(0.33, 0.66);
	// Submerged mining speed is 0.2 by default; +0.8 = Aqua Affinity
	public double diverMiningBonus = 0.8;
	// Block reach is 4.5 by default
	public double longArmsReachBonus = 1.5;
	public double steadyStanceKnockbackResistance = 0.3;
	// Defense perks: share of damage taken away, by tier
	public List<Double> guardDamageReductions = List.of(0.15, 0.30);
	public List<Double> toughHideDamageReductions = List.of(0.10, 0.20);
	public List<Double> bountifulHarvestChances = List.of(0.1, 0.2);
	public double rancherTwinChance = 0.15;
	public double greenThumbChance = 0.25;
	public List<Double> thriftyQuiverChances = List.of(0.1, 0.2, 0.3);
	public double fireArrowChance = 0.15;
	public double frostArrowChance = 0.15;
	public int frostArrowSlownessSeconds = 3;
	public int piercingShotExtraMobs = 1;
	// Steady Aim: a flying arrow turns at most this many degrees per tick toward the hostile mob closest to its
	// path, if that mob is within the cone and range and in sight
	public double steadyAimTurnDegrees = 2.0;
	public double steadyAimConeDegrees = 15.0;
	public double steadyAimRange = 20.0;
	// Ricochet: after hitting a mob, the arrow bounces once to the nearest hostile mob within this range
	public double ricochetRange = 8.0;
	public double ricochetMinSpeed = 1.5;

	// Mob unlocks: an Overworld hostile mob in this list only spawns naturally once the lowest Adventure Rank
	// among players within mobUnlockPlayerRadius reaches its rank. Unlisted mobs (zombies, other mods) always spawn.
	public boolean mobUnlocksEnabled = true;
	public List<MobUnlock> mobUnlocks = List.of(
			new MobUnlock("skeletons", 5, List.of("minecraft:skeleton", "minecraft:stray", "minecraft:bogged", "minecraft:parched")),
			new MobUnlock("creepers", 12, List.of("minecraft:creeper")),
			new MobUnlock("spiders", 20, List.of("minecraft:spider", "minecraft:cave_spider")),
			new MobUnlock("drowned", 20, List.of("minecraft:drowned")),
			new MobUnlock("endermen", 30, List.of("minecraft:enderman")),
			new MobUnlock("slimes", 40, List.of("minecraft:slime", "minecraft:sulfur_cube")),
			new MobUnlock("witches", 50, List.of("minecraft:witch")),
			new MobUnlock("patrols", 50, List.of("minecraft:pillager")));
	// Chance that a locked mob is replaced by an unlocked one instead of not spawning (0 = fewer mobs early)
	public double lockedMobReplacementChance = 0.0;
	public int mobUnlockPlayerRadius = 128;

	// Elites and mob gear (Overworld natural spawns, by the lowest Adventure Rank among nearby players)
	public boolean elitesEnabled = true;
	public List<String> eliteMobs = List.of("minecraft:zombie", "minecraft:husk", "minecraft:zombie_villager", "minecraft:drowned",
			"minecraft:skeleton", "minecraft:stray", "minecraft:bogged", "minecraft:parched",
			"minecraft:spider", "minecraft:cave_spider", "minecraft:enderman", "minecraft:slime");
	// Elites of a mob type start this many ranks after the type unlocks, then get more common
	public int eliteRankDelay = 5;
	public double eliteChanceStart = 0.02;
	public double eliteChancePerRank = 0.001;
	public double eliteChanceMax = 0.10;
	public double eliteHealthBonus = 0.5;
	public double eliteDamageBonus = 0.25;
	public double eliteScaleBonus = 0.2;
	// Elite gear: every armor slot is rolled on its own (at least 1 piece), and every piece and the weapon roll
	// their own material. Material odds are weights per rank; between two rows they shift smoothly.
	public Ramp eliteArmorPieceChance = new Ramp(0, 0.35, 160, 0.95);
	public List<GearWeights> gearMaterialWeights = List.of(
			new GearWeights(0, Map.of("leather", 9.0, "chainmail", 1.0)),
			new GearWeights(15, Map.of("leather", 7.0, "chainmail", 3.0)),
			new GearWeights(30, Map.of("leather", 4.0, "chainmail", 6.0, "iron", 1.0)),
			new GearWeights(60, Map.of("leather", 2.0, "chainmail", 6.0, "iron", 3.0)),
			new GearWeights(90, Map.of("chainmail", 4.0, "iron", 6.0, "diamond", 1.0)),
			new GearWeights(120, Map.of("chainmail", 1.0, "iron", 6.0, "diamond", 4.0)),
			new GearWeights(160, Map.of("iron", 1.0, "diamond", 9.0)));
	// Sword for elite zombies, by the material rolled for it
	public Map<String, String> weaponByMaterial = Map.of(
			"leather", "minecraft:stone_sword", "chainmail", "minecraft:stone_sword",
			"iron", "minecraft:iron_sword", "diamond", "minecraft:diamond_sword");
	public Ramp eliteEnchantChance = new Ramp(30, 0.1, 150, 0.8);
	public Ramp eliteEnchantLevel = new Ramp(30, 5, 150, 30);
	// Normal zombies/skeletons: chance of 1-2 extra armor pieces, on top of vanilla's own. Their material is
	// rolled like an elite's at rank * normalGearRankFactor, so they stay behind the elites.
	public Ramp mobGearChance = new Ramp(10, 0.0, 100, 0.25);
	public double normalGearRankFactor = 0.5;
	public Ramp eliteXpMultiplier = new Ramp(0, 3.0, 100, 6.0);
	public int eliteBestiaryKills = 3;
	// Bonus loot, only when a player kills the elite
	public Ramp eliteEmeraldChance = new Ramp(0, 0.10, 100, 0.5);
	public Ramp eliteEmeraldMaxCount = new Ramp(0, 1, 100, 3);
	public Ramp eliteBookChance = new Ramp(40, 0.05, 100, 0.25);
	public Ramp eliteBookLevel = new Ramp(40, 5, 100, 30);
	public Ramp eliteGoldenAppleChance = new Ramp(80, 0.03, 100, 0.08);
	// Traits for high-rank elites: Swift, Frost, Blazing, Leaping, Summoner
	public Ramp eliteTraitChance = new Ramp(100, 0.25, 160, 0.75);
	public double traitLootMultiplier = 1.5;
	public double swiftSpeedBonus = 0.3;
	public int frostSlownessSeconds = 3;
	public float blazingFireSeconds = 4f;
	public int summonerHelpers = 2;

	// Waypoints: elites drop shards (player kills only; the first elite a player kills always drops one).
	public Ramp eliteShardChance = new Ramp(0, 0.25, 100, 0.5);
	// Turning on a stone costs base + step × (stones you already own) shards; breaking it gives them back
	public int waypointShardCostBase = 1;
	public int waypointShardCostStep = 1;
	// Teleport cost in XP points (not levels): base + per 1000 blocks, capped. Distance is in Overworld blocks
	// (Nether ×8). Changing dimension adds the fee on top of the cap; trips to or from the End cost cap + fee.
	public int teleportXpBase = 15;
	public int teleportXpPer1000Blocks = 10;
	public int teleportXpCap = 50;
	public int teleportXpDimensionFee = 20;
	// Charge-up before the teleport; getting hurt or walking away cancels it
	public int teleportChargeTicks = 40;
	// Tamed pets that follow you (not sitting, not on a lead) within this range come along
	public double teleportPetRange = 16.0;
	// Pathfinder perk: share of the teleport cost taken away, by tier
	public List<Double> pathfinderCostReductions = List.of(0.25, 0.5);

	public record FamiliarityTier(int kills, double damageModifier, String label) {
	}

	/** Armor material weights (e.g. "iron" for iron_helmet) at a rank. */
	public record GearWeights(int rank, Map<String, Double> weights) {
	}

	/** A value that grows linearly from fromRank to toRank and stays at "to" after it; 0 below fromRank. */
	public record Ramp(int fromRank, double from, int toRank, double to) {
		public double at(int rank) {
			if (rank < fromRank) {
				return 0;
			}
			if (toRank <= fromRank) {
				return to;
			}
			double t = Math.min(1, (rank - fromRank) / (double) (toRank - fromRank));
			return from + (to - from) * t;
		}
	}

	public record MobUnlock(String name, int rank, List<String> mobs) {
	}

	public static PamConfig get() {
		return instance;
	}

	public static void load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH)) {
				PamConfig loaded = GSON.fromJson(reader, PamConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (IOException | RuntimeException e) {
				ProgressiveAdventure.LOGGER.error("Failed to read {}, using defaults", PATH, e);
			}
		}
		// Write back so new options show up in existing config files
		try (Writer writer = Files.newBufferedWriter(PATH)) {
			GSON.toJson(instance, writer);
		} catch (IOException e) {
			ProgressiveAdventure.LOGGER.error("Failed to write {}", PATH, e);
		}
	}

	public static double listValue(List<Double> values, int index) {
		return values.get(Math.clamp(index, 0, values.size() - 1));
	}
}
