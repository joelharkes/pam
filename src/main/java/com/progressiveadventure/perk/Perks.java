package com.progressiveadventure.perk;

import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.PlayerProgress;
import com.progressiveadventure.data.ProgressionSnapshot;
import com.progressiveadventure.perk.Requirement.MobTypes;
import com.progressiveadventure.perk.Requirement.Rank;
import com.progressiveadventure.perk.Requirement.SkillLevel;
import com.progressiveadventure.skill.Skill;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All perks. Costs and requirements live here rather than in the config because the client must see the same
 * values as the server; effect strengths (chances etc.) are server-only and live in the config.
 */
public final class Perks {
	private static final Map<String, Perk> BY_ID = new LinkedHashMap<>();

	public static final Perk STURDY_PICKAXE_1 = register("sturdy_pickaxe_1", PerkCategory.MINING, Items.STONE_PICKAXE, 1, new SkillLevel(Skill.MINING, 5), null);
	public static final Perk STURDY_PICKAXE_2 = register("sturdy_pickaxe_2", PerkCategory.MINING, Items.IRON_PICKAXE, 2, new SkillLevel(Skill.MINING, 15), "sturdy_pickaxe_1");
	public static final Perk STURDY_PICKAXE_3 = register("sturdy_pickaxe_3", PerkCategory.MINING, Items.DIAMOND_PICKAXE, 3, new SkillLevel(Skill.MINING, 30), "sturdy_pickaxe_2");
	public static final Perk PROSPECTOR_1 = register("prospector_1", PerkCategory.MINING, Items.RAW_IRON, 2, new SkillLevel(Skill.MINING, 10), null);
	public static final Perk PROSPECTOR_2 = register("prospector_2", PerkCategory.MINING, Items.DIAMOND, 3, new SkillLevel(Skill.MINING, 25), "prospector_1");
	public static final Perk ORE_SENSE = register("ore_sense", PerkCategory.MINING, Items.SPYGLASS, 4, new SkillLevel(Skill.MINING, 40), null);
	public static final Perk TREASURE_SENSE = register("treasure_sense", PerkCategory.MINING, Items.ANCIENT_DEBRIS, 5, new SkillLevel(Skill.MINING, 50), "ore_sense");
	public static final Perk VEIN_MINER = register("vein_miner", PerkCategory.MINING, Items.IRON_ORE, 4, new SkillLevel(Skill.MINING, 30), null);
	public static final Perk CATS_EYES_1 = register("cats_eyes_1", PerkCategory.MINING, Items.GLOW_INK_SAC, 2, new SkillLevel(Skill.MINING, 15), null);
	public static final Perk CATS_EYES_2 = register("cats_eyes_2", PerkCategory.MINING, Items.GLOWSTONE, 3, new SkillLevel(Skill.MINING, 35), "cats_eyes_1");

	public static final Perk STURDY_AXE_1 = register("sturdy_axe_1", PerkCategory.WOODCUTTING, Items.STONE_AXE, 1, new SkillLevel(Skill.WOODCUTTING, 5), null);
	public static final Perk STURDY_AXE_2 = register("sturdy_axe_2", PerkCategory.WOODCUTTING, Items.IRON_AXE, 2, new SkillLevel(Skill.WOODCUTTING, 15), "sturdy_axe_1");
	public static final Perk STURDY_AXE_3 = register("sturdy_axe_3", PerkCategory.WOODCUTTING, Items.DIAMOND_AXE, 3, new SkillLevel(Skill.WOODCUTTING, 30), "sturdy_axe_2");
	public static final Perk LUMBERJACK = register("lumberjack", PerkCategory.WOODCUTTING, Items.SPRUCE_LOG, 2, new SkillLevel(Skill.WOODCUTTING, 10), null);
	public static final Perk TIMBER = register("timber", PerkCategory.WOODCUTTING, Items.OAK_LOG, 4, new SkillLevel(Skill.WOODCUTTING, 25), "lumberjack");
	public static final Perk FORESTER = register("forester", PerkCategory.WOODCUTTING, Items.OAK_SAPLING, 2, new SkillLevel(Skill.WOODCUTTING, 15), "lumberjack");
	public static final Perk CLEAR_CUT = register("clear_cut", PerkCategory.WOODCUTTING, Items.SHEARS, 2, new SkillLevel(Skill.WOODCUTTING, 30), "timber");
	public static final Perk ORCHARD = register("orchard", PerkCategory.WOODCUTTING, Items.OAK_LEAVES, 2, new SkillLevel(Skill.WOODCUTTING, 5), null);
	public static final Perk BIRDS_NEST = register("birds_nest", PerkCategory.WOODCUTTING, Items.EGG, 2, new SkillLevel(Skill.WOODCUTTING, 15), null);

	public static final Perk STURDY_SHOVEL_1 = register("sturdy_shovel_1", PerkCategory.DIGGING, Items.STONE_SHOVEL, 1, new SkillLevel(Skill.DIGGING, 5), null);
	public static final Perk STURDY_SHOVEL_2 = register("sturdy_shovel_2", PerkCategory.DIGGING, Items.IRON_SHOVEL, 2, new SkillLevel(Skill.DIGGING, 15), "sturdy_shovel_1");
	public static final Perk STURDY_SHOVEL_3 = register("sturdy_shovel_3", PerkCategory.DIGGING, Items.DIAMOND_SHOVEL, 3, new SkillLevel(Skill.DIGGING, 30), "sturdy_shovel_2");
	public static final Perk ARCHAEOLOGIST = register("archaeologist", PerkCategory.DIGGING, Items.BRUSH, 2, new SkillLevel(Skill.DIGGING, 10), null);
	public static final Perk FLINT_FINDER = register("flint_finder", PerkCategory.DIGGING, Items.FLINT, 1, new SkillLevel(Skill.DIGGING, 5), null);
	public static final Perk PATH_MAKER = register("path_maker", PerkCategory.DIGGING, Items.DIRT_PATH, 1, new SkillLevel(Skill.DIGGING, 5), null);
	public static final Perk RELIC_SENSE = register("relic_sense", PerkCategory.DIGGING, Items.SUSPICIOUS_SAND, 2, new SkillLevel(Skill.DIGGING, 15), null);
	public static final Perk EXCAVATOR = register("excavator", PerkCategory.DIGGING, Items.GOLDEN_SHOVEL, 4, new SkillLevel(Skill.DIGGING, 20), null);

	public static final Perk LIGHT_STEP = register("light_step", PerkCategory.FARMING, Items.LEATHER_BOOTS, 1, Requirement.NONE, null);
	public static final Perk REPLANT = register("replant", PerkCategory.FARMING, Items.WHEAT_SEEDS, 2, new SkillLevel(Skill.FARMING, 5), null);
	public static final Perk STURDY_HOE_1 = register("sturdy_hoe_1", PerkCategory.FARMING, Items.STONE_HOE, 1, new SkillLevel(Skill.FARMING, 5), null);
	public static final Perk STURDY_HOE_2 = register("sturdy_hoe_2", PerkCategory.FARMING, Items.IRON_HOE, 2, new SkillLevel(Skill.FARMING, 15), "sturdy_hoe_1");
	public static final Perk STURDY_HOE_3 = register("sturdy_hoe_3", PerkCategory.FARMING, Items.DIAMOND_HOE, 3, new SkillLevel(Skill.FARMING, 30), "sturdy_hoe_2");
	public static final Perk BOUNTIFUL_HARVEST_1 = register("bountiful_harvest_1", PerkCategory.FARMING, Items.WHEAT, 2, new SkillLevel(Skill.FARMING, 10), null);
	public static final Perk BOUNTIFUL_HARVEST_2 = register("bountiful_harvest_2", PerkCategory.FARMING, Items.HAY_BLOCK, 3, new SkillLevel(Skill.FARMING, 25), "bountiful_harvest_1");
	public static final Perk HARVESTER = register("harvester", PerkCategory.FARMING, Items.GOLDEN_HOE, 3, new SkillLevel(Skill.FARMING, 20), null);
	public static final Perk RANCHER = register("rancher", PerkCategory.FARMING, Items.LEAD, 2, new SkillLevel(Skill.FARMING, 10), null);
	public static final Perk GREEN_THUMB = register("green_thumb", PerkCategory.FARMING, Items.BONE_MEAL, 2, new SkillLevel(Skill.FARMING, 15), null);

	public static final Perk THRIFTY_QUIVER_1 = register("thrifty_quiver_1", PerkCategory.ARCHERY, Items.ARROW, 1, new SkillLevel(Skill.ARCHERY, 5), null);
	public static final Perk THRIFTY_QUIVER_2 = register("thrifty_quiver_2", PerkCategory.ARCHERY, Items.BUNDLE, 2, new SkillLevel(Skill.ARCHERY, 15), "thrifty_quiver_1");
	public static final Perk THRIFTY_QUIVER_3 = register("thrifty_quiver_3", PerkCategory.ARCHERY, Items.SPECTRAL_ARROW, 3, new SkillLevel(Skill.ARCHERY, 30), "thrifty_quiver_2");
	public static final Perk FIRE_ARROWS = register("fire_arrows", PerkCategory.ARCHERY, Items.FIRE_CHARGE, 2, new SkillLevel(Skill.ARCHERY, 10), null);
	public static final Perk FROST_ARROWS = register("frost_arrows", PerkCategory.ARCHERY, Items.SNOWBALL, 2, new SkillLevel(Skill.ARCHERY, 10), null);
	public static final Perk PIERCING_SHOT = register("piercing_shot", PerkCategory.ARCHERY, Items.CROSSBOW, 2, new SkillLevel(Skill.ARCHERY, 15), null);
	public static final Perk STEADY_AIM = register("steady_aim", PerkCategory.ARCHERY, Items.TARGET, 3, new SkillLevel(Skill.ARCHERY, 20), null);
	public static final Perk RICOCHET = register("ricochet", PerkCategory.ARCHERY, Items.SLIME_BALL, 5, new SkillLevel(Skill.ARCHERY, 40), null);
	public static final Perk LOYAL_TRIDENT = register("loyal_trident", PerkCategory.ARCHERY, Items.TRIDENT, 2, new SkillLevel(Skill.ARCHERY, 15), null);

	public static final Perk KEEN_EYE = register("keen_eye", PerkCategory.FISHING, Items.BELL, 1, new SkillLevel(Skill.FISHING, 5), null);
	public static final Perk LUCKY = register("lucky", PerkCategory.FISHING, Items.EMERALD, 2, new SkillLevel(Skill.FISHING, 10), null);
	public static final Perk DOUBLE_CATCH = register("double_catch", PerkCategory.FISHING, Items.COD, 3, new SkillLevel(Skill.FISHING, 15), null);
	public static final Perk TREASURE_HUNTER = register("treasure_hunter", PerkCategory.FISHING, Items.NAME_TAG, 4, new SkillLevel(Skill.FISHING, 25), null);

	// Melee: grouped by weapon in the perk list (perks without a weapon first)
	public static final Perk QUICK_STUDY = register("quick_study", PerkCategory.MELEE, "general", Items.WRITABLE_BOOK, 2, new MobTypes(5), null);
	public static final Perk STURDY_WEAPON_1 = register("sturdy_weapon_1", PerkCategory.MELEE, "general", Items.STONE_SWORD, 1, new MobTypes(5), null);
	public static final Perk STURDY_WEAPON_2 = register("sturdy_weapon_2", PerkCategory.MELEE, "general", Items.IRON_SWORD, 2, new MobTypes(15), "sturdy_weapon_1");
	public static final Perk STURDY_WEAPON_3 = register("sturdy_weapon_3", PerkCategory.MELEE, "general", Items.DIAMOND_SWORD, 3, new MobTypes(25), "sturdy_weapon_2");
	public static final Perk CAREFUL_SWEEP = register("careful_sweep", PerkCategory.MELEE, "sword", Items.LEAD, 1, new SkillLevel(Skill.MELEE, 5), null);
	public static final Perk WIDE_SWEEP_1 = register("wide_sweep_1", PerkCategory.MELEE, "sword", Items.GOLDEN_SWORD, 2, new SkillLevel(Skill.MELEE, 10), null);
	public static final Perk WIDE_SWEEP_2 = register("wide_sweep_2", PerkCategory.MELEE, "sword", Items.NETHERITE_SWORD, 3, new SkillLevel(Skill.MELEE, 25), "wide_sweep_1");
	public static final Perk WHIRLWIND = register("whirlwind", PerkCategory.MELEE, "sword", Items.WIND_CHARGE, 3, new SkillLevel(Skill.MELEE, 20), "wide_sweep_1");
	public static final Perk DISARM = register("disarm", PerkCategory.MELEE, "sword", Items.TRIPWIRE_HOOK, 2, new SkillLevel(Skill.MELEE, 15), null);
	public static final Perk HEAVY_SWING = register("heavy_swing", PerkCategory.MELEE, "axe", Items.IRON_AXE, 2, new SkillLevel(Skill.MELEE, 10), null);
	public static final Perk SUNDER = register("sunder", PerkCategory.MELEE, "axe", Items.DIAMOND_AXE, 3, new SkillLevel(Skill.MELEE, 20), null);
	public static final Perk GROUND_POUND = register("ground_pound", PerkCategory.MELEE, "mace", Items.MACE, 3, new SkillLevel(Skill.MELEE, 15), null);
	public static final Perk SHOCKWAVE = register("shockwave", PerkCategory.MELEE, "mace", Items.HEAVY_CORE, 4, new SkillLevel(Skill.MELEE, 30), null);
	public static final Perk RUNNING_CHARGE = register("running_charge", PerkCategory.MELEE, "spear", Items.IRON_SPEAR, 2, new SkillLevel(Skill.MELEE, 10), null);
	public static final Perk PINNING_JAB = register("pinning_jab", PerkCategory.MELEE, "spear", Items.COBWEB, 2, new SkillLevel(Skill.MELEE, 15), null);

	public static final Perk STURDY_ARMOR_1 = register("sturdy_armor_1", PerkCategory.DEFENSE, Items.LEATHER_CHESTPLATE, 1, new MobTypes(5), null);
	public static final Perk STURDY_ARMOR_2 = register("sturdy_armor_2", PerkCategory.DEFENSE, Items.IRON_CHESTPLATE, 2, new MobTypes(10), "sturdy_armor_1");
	public static final Perk STURDY_ARMOR_3 = register("sturdy_armor_3", PerkCategory.DEFENSE, Items.DIAMOND_CHESTPLATE, 3, new MobTypes(15), "sturdy_armor_2");
	public static final Perk BLAST_GUARD_1 = register("blast_guard_1", PerkCategory.DEFENSE, Items.GUNPOWDER, 2, new MobTypes(5), null);
	public static final Perk BLAST_GUARD_2 = register("blast_guard_2", PerkCategory.DEFENSE, Items.TNT, 3, new MobTypes(10), "blast_guard_1");
	public static final Perk ARROW_GUARD_1 = register("arrow_guard_1", PerkCategory.DEFENSE, Items.ARROW, 2, new MobTypes(5), null);
	public static final Perk ARROW_GUARD_2 = register("arrow_guard_2", PerkCategory.DEFENSE, Items.SHIELD, 3, new MobTypes(10), "arrow_guard_1");
	public static final Perk FIRE_GUARD_1 = register("fire_guard_1", PerkCategory.DEFENSE, Items.BLAZE_POWDER, 2, new MobTypes(5), null);
	public static final Perk FIRE_GUARD_2 = register("fire_guard_2", PerkCategory.DEFENSE, Items.MAGMA_CREAM, 3, new MobTypes(10), "fire_guard_1");
	public static final Perk TOUGH_HIDE_1 = register("tough_hide_1", PerkCategory.DEFENSE, Items.LEATHER, 2, new MobTypes(5), null);
	public static final Perk TOUGH_HIDE_2 = register("tough_hide_2", PerkCategory.DEFENSE, Items.ARMADILLO_SCUTE, 3, new MobTypes(10), "tough_hide_1");

	public static final Perk STEADY_STANCE = register("steady_stance", PerkCategory.DEFENSE, Items.ANVIL, 2, new MobTypes(10), null);
	public static final Perk SHIELD_BASH = register("shield_bash", PerkCategory.DEFENSE, "shield", Items.SHIELD, 2, new MobTypes(5), null);
	public static final Perk PARRY = register("parry", PerkCategory.DEFENSE, "shield", Items.IRON_INGOT, 3, new MobTypes(10), null);
	public static final Perk DEFLECT = register("deflect", PerkCategory.DEFENSE, "shield", Items.SPECTRAL_ARROW, 2, new MobTypes(15), "parry");

	public static final Perk BONUS_HEART_1 = register("bonus_heart_1", PerkCategory.SURVIVAL, Items.APPLE, 3, new Rank(5), null);
	public static final Perk BONUS_HEART_2 = register("bonus_heart_2", PerkCategory.SURVIVAL, Items.GOLDEN_APPLE, 4, new Rank(20), "bonus_heart_1");
	public static final Perk BONUS_HEART_3 = register("bonus_heart_3", PerkCategory.SURVIVAL, Items.ENCHANTED_GOLDEN_APPLE, 5, new Rank(40), "bonus_heart_2");
	public static final Perk IRON_STOMACH = register("iron_stomach", PerkCategory.SURVIVAL, Items.ROTTEN_FLESH, 1, Requirement.NONE, null);
	public static final Perk LONG_ARMS = register("long_arms", PerkCategory.SURVIVAL, Items.SCAFFOLDING, 2, new Rank(10), null);
	public static final Perk DEEP_BREATH_1 = register("deep_breath_1", PerkCategory.SURVIVAL, Items.PUFFERFISH, 1, Requirement.NONE, null);
	public static final Perk DEEP_BREATH_2 = register("deep_breath_2", PerkCategory.SURVIVAL, Items.NAUTILUS_SHELL, 2, new Rank(10), "deep_breath_1");
	public static final Perk DEEP_BREATH_3 = register("deep_breath_3", PerkCategory.SURVIVAL, Items.TURTLE_HELMET, 3, new Rank(25), "deep_breath_2");
	public static final Perk DIVER = register("diver", PerkCategory.SURVIVAL, Items.PRISMARINE_SHARD, 2, new Rank(10), null);
	public static final Perk SECOND_WIND = register("second_wind", PerkCategory.SURVIVAL, Items.GLISTERING_MELON_SLICE, 2, Requirement.NONE, null);
	public static final Perk HEARTY_KILL_1 = register("hearty_kill_1", PerkCategory.SURVIVAL, Items.COOKED_BEEF, 2, new MobTypes(10), null);
	public static final Perk HEARTY_KILL_2 = register("hearty_kill_2", PerkCategory.SURVIVAL, Items.RABBIT_STEW, 3, new MobTypes(20), "hearty_kill_1");
	public static final Perk HEARTY_KILL_3 = register("hearty_kill_3", PerkCategory.SURVIVAL, Items.GOLDEN_CARROT, 4, new MobTypes(30), "hearty_kill_2");

	public static final Perk FLEET_FOOT_1 = register("fleet_foot_1", PerkCategory.MOVEMENT, Items.SUGAR, 2, new Rank(10), null);
	public static final Perk FLEET_FOOT_2 = register("fleet_foot_2", PerkCategory.MOVEMENT, Items.RABBIT_FOOT, 3, new Rank(25), "fleet_foot_1");
	public static final Perk FLEET_FOOT_3 = register("fleet_foot_3", PerkCategory.MOVEMENT, Items.DIAMOND_BOOTS, 4, new Rank(40), "fleet_foot_2");
	public static final Perk SOFT_LANDING = register("soft_landing", PerkCategory.MOVEMENT, Items.FEATHER, 2, Requirement.NONE, null);
	public static final Perk SPRING_STEP = register("spring_step", PerkCategory.MOVEMENT, Items.SLIME_BLOCK, 2, new Rank(5), null);
	public static final Perk STEP_UP = register("step_up", PerkCategory.MOVEMENT, Items.OAK_STAIRS, 2, new Rank(5), null);
	public static final Perk SURE_FEET = register("sure_feet", PerkCategory.MOVEMENT, Items.SOUL_SAND, 1, Requirement.NONE, null);
	public static final Perk SNEAKY_FEET = register("sneaky_feet", PerkCategory.MOVEMENT, Items.MOSS_CARPET, 1, Requirement.NONE, null);
	public static final Perk SHADOW_STEP_1 = register("shadow_step_1", PerkCategory.MOVEMENT, Items.INK_SAC, 2, new Rank(10), null);
	public static final Perk SHADOW_STEP_2 = register("shadow_step_2", PerkCategory.MOVEMENT, Items.ECHO_SHARD, 3, new Rank(25), "shadow_step_1");
	public static final Perk MANTLE = register("mantle", PerkCategory.MOVEMENT, Items.LADDER, 3, new Rank(15), null);
	public static final Perk PATHFINDER_1 = register("pathfinder_1", PerkCategory.MOVEMENT, Items.COMPASS, 2, new Rank(15), null);
	public static final Perk PATHFINDER_2 = register("pathfinder_2", PerkCategory.MOVEMENT, Items.RECOVERY_COMPASS, 3, new Rank(35), "pathfinder_1");
	public static final Perk DOUBLE_JUMP = register("double_jump", PerkCategory.MOVEMENT, Items.PHANTOM_MEMBRANE, 4, new Rank(30), null);
	public static final Perk SWIMMER_1 = register("swimmer_1", PerkCategory.MOVEMENT, Items.TROPICAL_FISH, 2, new Rank(5), null);
	public static final Perk SWIMMER_2 = register("swimmer_2", PerkCategory.MOVEMENT, Items.HEART_OF_THE_SEA, 3, new Rank(20), "swimmer_1");

	public enum Status {
		OWNED, BUYABLE, TOO_EXPENSIVE, LOCKED, NEEDS_PREREQUISITE
	}

	private Perks() {
	}

	private static Perk register(String id, PerkCategory category, Item icon, int cost, Requirement requirement, String prerequisite) {
		return register(id, category, null, icon, cost, requirement, prerequisite);
	}

	private static Perk register(String id, PerkCategory category, String group, Item icon, int cost, Requirement requirement, String prerequisite) {
		Perk perk = new Perk(id, category, icon, cost, requirement, prerequisite, group);
		BY_ID.put(id, perk);
		return perk;
	}

	public static Perk byId(String id) {
		return BY_ID.get(id);
	}

	public static List<Perk> inCategory(PerkCategory category) {
		List<Perk> result = new ArrayList<>();
		for (Perk perk : BY_ID.values()) {
			if (perk.category() == category) {
				result.add(perk);
			}
		}
		return result;
	}

	/** The perk with the lowest skill level requirement above the given level, or null when none is left. */
	public static Perk nextForSkill(Skill skill, int level) {
		Perk next = null;
		int nextLevel = Integer.MAX_VALUE;
		for (Perk perk : BY_ID.values()) {
			if (perk.requirement() instanceof SkillLevel(Skill required, int needed)
					&& required == skill && needed > level && needed < nextLevel) {
				next = perk;
				nextLevel = needed;
			}
		}
		return next;
	}

	public static boolean has(Player player, Perk perk) {
		return PamAttachments.progress(player).perks().contains(perk.id());
	}

	public static int pointsSpent(PlayerProgress progress) {
		int spent = 0;
		for (String id : progress.perks()) {
			Perk perk = BY_ID.get(id);
			if (perk != null) {
				spent += perk.cost();
			}
		}
		return spent;
	}

	public static int pointsAvailable(PlayerProgress progress, ProgressionSnapshot snapshot) {
		return snapshot.rank() - pointsSpent(progress);
	}

	public static Status status(Perk perk, PlayerProgress progress, ProgressionSnapshot snapshot) {
		if (progress.perks().contains(perk.id())) {
			return Status.OWNED;
		}
		if (perk.prerequisite() != null && !progress.perks().contains(perk.prerequisite())) {
			return Status.NEEDS_PREREQUISITE;
		}
		if (!perk.requirement().isMet(progress, snapshot)) {
			return Status.LOCKED;
		}
		return pointsAvailable(progress, snapshot) >= perk.cost() ? Status.BUYABLE : Status.TOO_EXPENSIVE;
	}

	/** Highest owned tier among the given perks (1-based), or 0. */
	public static int tier(Player player, Perk... tiers) {
		PlayerProgress progress = PamAttachments.progress(player);
		for (int i = tiers.length - 1; i >= 0; i--) {
			if (progress.perks().contains(tiers[i].id())) {
				return i + 1;
			}
		}
		return 0;
	}

	public static int bonusHearts(Player player) {
		return tier(player, BONUS_HEART_1, BONUS_HEART_2, BONUS_HEART_3);
	}
}
