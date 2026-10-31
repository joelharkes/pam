package com.progressiveadventure.milestone;

import com.progressiveadventure.ProgressiveAdventure;
import com.progressiveadventure.access.FoodDataAccess;
import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.ProgressionSnapshot;
import com.progressiveadventure.perk.Perk;
import com.progressiveadventure.perk.Perks;
import com.progressiveadventure.skill.Melee;
import com.progressiveadventure.world.MobUnlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Optional;

/**
 * Derives hearts, stomach level and Adventure Rank (= perk points earned) from completed advancements and applies them
 * (plus attribute-based perks) to the player. Everything is recomputed from advancement progress, so rewards
 * can never be granted twice and balance changes apply to existing players.
 */
public final class Progression {
	private static final Identifier HEALTH_MODIFIER = ProgressiveAdventure.id("milestone_hearts");
	private static final Identifier DEEP_BREATH_MODIFIER = ProgressiveAdventure.id("perk_deep_breath");
	private static final Identifier SOFT_LANDING_MODIFIER = ProgressiveAdventure.id("perk_soft_landing");
	private static final Identifier FLEET_FOOT_MODIFIER = ProgressiveAdventure.id("perk_fleet_foot");
	private static final Identifier SPRING_STEP_MODIFIER = ProgressiveAdventure.id("perk_spring_step");
	private static final Identifier STEP_UP_MODIFIER = ProgressiveAdventure.id("perk_step_up");
	private static final Identifier SURE_FEET_MODIFIER = ProgressiveAdventure.id("perk_sure_feet");
	private static final Identifier SNEAKY_FEET_MODIFIER = ProgressiveAdventure.id("perk_sneaky_feet");
	private static final Identifier SWIMMER_MODIFIER = ProgressiveAdventure.id("perk_swimmer");
	private static final Identifier DIVER_MODIFIER = ProgressiveAdventure.id("perk_diver");
	private static final Identifier LONG_ARMS_MODIFIER = ProgressiveAdventure.id("perk_long_arms");
	private static final Identifier LUCKY_MODIFIER = ProgressiveAdventure.id("perk_lucky");
	private static final Identifier STEADY_STANCE_MODIFIER = ProgressiveAdventure.id("perk_steady_stance");
	private static final Identifier MELEE_SPEED_MODIFIER = ProgressiveAdventure.id("skill_melee");
	private static final Identifier WIDE_SWEEP_MODIFIER = ProgressiveAdventure.id("perk_wide_sweep");

	/** @param rank Adventure Rank = perk points earned from all advancements */
	public record Status(int rank, int halfHearts, int stomach) {
	}

	private Progression() {
	}

	public static Status status(ServerPlayer player) {
		int halfHearts = 0;
		int stomach = 0;
		for (Milestone milestone : Milestones.ALL) {
			if (Milestones.isDone(player, milestone)) {
				halfHearts += milestone.halfHearts();
				stomach += milestone.stomach();
			}
		}
		return new Status(perkPointsEarned(player), halfHearts, stomach);
	}

	/** Max health in half hearts: milestone hearts (capped) plus bonus heart perks (uncapped). */
	public static int maxHealth(ServerPlayer player, Status status) {
		PamConfig config = PamConfig.get();
		return Math.min(config.startingHearts * 2 + status.halfHearts(), config.maxHearts * 2) + Perks.bonusHearts(player) * 2;
	}

	/** Perk points for one advancement, or 0 if it doesn't count (tab roots and recipes without display). */
	public static int perkPoints(AdvancementHolder holder) {
		if (holder.value().parent().isEmpty() || holder.value().display().isEmpty()) {
			return 0;
		}
		DisplayInfo display = holder.value().display().get();
		PamConfig config = PamConfig.get();
		return switch (display.getType()) {
			case TASK -> config.perkPointsTask;
			case GOAL -> config.perkPointsGoal;
			case CHALLENGE -> config.perkPointsChallenge;
		};
	}

	public static int perkPointsEarned(ServerPlayer player) {
		int points = 0;
		for (AdvancementHolder holder : player.level().getServer().getAdvancements().getAllAdvancements()) {
			int value = perkPoints(holder);
			if (value > 0 && player.getAdvancements().getOrStartProgress(holder).isDone()) {
				points += value;
			}
		}
		return points;
	}

	/** Applies max health, stomach and attribute perks, and syncs the snapshot. Call whenever progress changes. */
	public static void apply(ServerPlayer player) {
		Status status = status(player);
		PamConfig config = PamConfig.get();
		float hungerDrain = (float) PamConfig.listValue(config.stomachExhaustionMultipliers, status.stomach());

		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		double before = player.getMaxHealth();
		// Permanent, so it's saved and already in place when health loads on login; a transient one let health load
		// capped at vanilla's 20, and the heal below then refilled it on every relog. Vanilla drops it on death
		// (so the respawn heal still works) and keeps it, with health, when leaving the End.
		maxHealth.addOrReplacePermanentModifier(new AttributeModifier(HEALTH_MODIFIER,
				maxHealth(player, status) - maxHealth.getBaseValue(), AttributeModifier.Operation.ADD_VALUE));
		double gained = player.getMaxHealth() - before;
		if (gained > 0) {
			player.heal((float) gained);
		} else if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}

		int steadyHealing = Perks.tier(player, Perks.STEADY_HEALING_1, Perks.STEADY_HEALING_2);
		((FoodDataAccess) player.getFoodData()).pam$setMultipliers(hungerDrain,
				(float) PamConfig.listValue(config.stomachSaturationMultipliers, status.stomach()),
				steadyHealing > 0 ? 1f - (float) PamConfig.listValue(config.steadyHealingReductions, steadyHealing - 1) : 1f);

		int deepBreath = Perks.tier(player, Perks.DEEP_BREATH_1, Perks.DEEP_BREATH_2, Perks.DEEP_BREATH_3);
		applyModifier(player, deepBreath > 0, Attributes.OXYGEN_BONUS, DEEP_BREATH_MODIFIER,
				deepBreath > 0 ? PamConfig.listValue(config.deepBreathOxygenBonuses, deepBreath - 1) : 0, AttributeModifier.Operation.ADD_VALUE);
		applyPerkModifier(player, Perks.SOFT_LANDING, Attributes.SAFE_FALL_DISTANCE, SOFT_LANDING_MODIFIER,
				config.softLandingBlocks, AttributeModifier.Operation.ADD_VALUE);
		int fleetFoot = Perks.tier(player, Perks.FLEET_FOOT_1, Perks.FLEET_FOOT_2, Perks.FLEET_FOOT_3);
		applyModifier(player, fleetFoot > 0, Attributes.MOVEMENT_SPEED, FLEET_FOOT_MODIFIER,
				fleetFoot > 0 ? PamConfig.listValue(config.fleetFootSpeedBonuses, fleetFoot - 1) : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		applyPerkModifier(player, Perks.SPRING_STEP, Attributes.JUMP_STRENGTH, SPRING_STEP_MODIFIER,
				config.springStepJumpBonus, AttributeModifier.Operation.ADD_VALUE);
		applyPerkModifier(player, Perks.STEP_UP, Attributes.STEP_HEIGHT, STEP_UP_MODIFIER,
				config.stepUpHeightBonus, AttributeModifier.Operation.ADD_VALUE);
		applyPerkModifier(player, Perks.SURE_FEET, Attributes.MOVEMENT_EFFICIENCY, SURE_FEET_MODIFIER,
				config.sureFeetEfficiency, AttributeModifier.Operation.ADD_VALUE);
		applyPerkModifier(player, Perks.SNEAKY_FEET, Attributes.SNEAKING_SPEED, SNEAKY_FEET_MODIFIER,
				config.sneakyFeetSpeedBonus, AttributeModifier.Operation.ADD_VALUE);
		int swimmer = Perks.tier(player, Perks.SWIMMER_1, Perks.SWIMMER_2);
		applyModifier(player, swimmer > 0, Attributes.WATER_MOVEMENT_EFFICIENCY, SWIMMER_MODIFIER,
				swimmer > 0 ? PamConfig.listValue(config.swimmerEfficiencies, swimmer - 1) : 0, AttributeModifier.Operation.ADD_VALUE);
		applyPerkModifier(player, Perks.DIVER, Attributes.SUBMERGED_MINING_SPEED, DIVER_MODIFIER,
				config.diverMiningBonus, AttributeModifier.Operation.ADD_VALUE);
		applyPerkModifier(player, Perks.LONG_ARMS, Attributes.BLOCK_INTERACTION_RANGE, LONG_ARMS_MODIFIER,
				config.longArmsReachBonus, AttributeModifier.Operation.ADD_VALUE);
		applyPerkModifier(player, Perks.LUCKY, Attributes.LUCK, LUCKY_MODIFIER,
				config.luckyLuck, AttributeModifier.Operation.ADD_VALUE);
		applyPerkModifier(player, Perks.STEADY_STANCE, Attributes.KNOCKBACK_RESISTANCE, STEADY_STANCE_MODIFIER,
				config.steadyStanceKnockbackResistance, AttributeModifier.Operation.ADD_VALUE);
		// Multiplies the total (weapon included): on the base it would be +0.3 x 4 = +1.2 attacks/s for every weapon
		double meleeSpeed = Melee.attackSpeedBonus(player);
		applyModifier(player, meleeSpeed > 0, Attributes.ATTACK_SPEED, MELEE_SPEED_MODIFIER,
				meleeSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		int wideSweep = Perks.tier(player, Perks.WIDE_SWEEP_1, Perks.WIDE_SWEEP_2);
		applyModifier(player, wideSweep > 0, Attributes.SWEEPING_DAMAGE_RATIO, WIDE_SWEEP_MODIFIER,
				wideSweep > 0 ? PamConfig.listValue(config.wideSweepRatios, wideSweep - 1) : 0, AttributeModifier.Operation.ADD_VALUE);

		Optional<PamConfig.MobUnlock> nextMobs = MobUnlocks.next(status.rank());
		player.setAttached(PamAttachments.PROGRESSION, new ProgressionSnapshot(status.rank(),
				maxHealth(player, status), config.maxHearts, status.stomach(), hungerDrain,
				nextMobs.map(PamConfig.MobUnlock::name).orElse(""), nextMobs.map(PamConfig.MobUnlock::rank).orElse(0)));
	}

	private static void applyPerkModifier(ServerPlayer player, Perk perk, Holder<Attribute> attribute, Identifier id,
			double amount, AttributeModifier.Operation operation) {
		applyModifier(player, Perks.has(player, perk), attribute, id, amount, operation);
	}

	private static void applyModifier(ServerPlayer player, boolean active, Holder<Attribute> attribute, Identifier id,
			double amount, AttributeModifier.Operation operation) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (active) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
		} else {
			instance.removeModifier(id);
		}
	}

	/** Called for every completed advancement, vanilla or ours. */
	public static void onAdvancementCompleted(ServerPlayer player, AdvancementHolder holder) {
		int oldRank = PamAttachments.snapshot(player).rank();
		apply(player);
		Status status = status(player);
		int points = perkPoints(holder);
		if (points > 0) {
			player.sendSystemMessage(Component.translatable("message.progressive_adventure.perk_points", points, status.rank())
					.withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		MobUnlocks.announce(player, oldRank, status.rank());

		Milestone milestone = Milestones.byId(holder.id());
		if (milestone == null) {
			return;
		}
		if (milestone.halfHearts() > 0) {
			player.sendSystemMessage(Component.translatable("message.progressive_adventure.hearts_gained",
					formatHearts(milestone.halfHearts()), formatHearts(maxHealth(player, status))).withStyle(ChatFormatting.RED));
		}
		if (milestone.stomach() > 0) {
			player.sendSystemMessage(Component.translatable("message.progressive_adventure.stomach_upgraded",
					status.stomach()).withStyle(ChatFormatting.GOLD));
		}
	}

	public static String formatHearts(int halfHearts) {
		return halfHearts % 2 == 0 ? Integer.toString(halfHearts / 2) : (halfHearts / 2) + ".5";
	}
}
