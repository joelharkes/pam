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
	public double archaeologistChance = 0.03;
	public float secondWindHealthThreshold = 6f;
	public int secondWindCooldownSeconds = 120;
	// Oxygen bonus works like Respiration: each point adds ~15s of air
	public List<Double> deepBreathOxygenBonuses = List.of(1.0, 2.0, 3.0);
	public double softLandingBlocks = 3.0;
	public double fleetFootSpeedBonus = 0.05;
	public List<Double> bountifulHarvestChances = List.of(0.1, 0.2);
	public double rancherTwinChance = 0.15;
	public double greenThumbChance = 0.25;

	public record FamiliarityTier(int kills, double damageModifier, String label) {
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
