package com.progressiveadventure.milestone;

import com.progressiveadventure.ProgressiveAdventure;
import com.progressiveadventure.access.FoodDataAccess;
import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.ProgressionSnapshot;
import com.progressiveadventure.perk.Perk;
import com.progressiveadventure.perk.Perks;

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

/**
 * Derives hearts, stomach level, Adventure Rank and perk points from completed advancements and applies them
 * (plus attribute-based perks) to the player. Everything is recomputed from advancement progress, so rewards
 * can never be granted twice and balance changes apply to existing players.
 */
public final class Progression {
	private static final Identifier HEALTH_MODIFIER = ProgressiveAdventure.id("milestone_hearts");
	private static final Identifier DEEP_BREATH_MODIFIER = ProgressiveAdventure.id("perk_deep_breath");
	private static final Identifier SOFT_LANDING_MODIFIER = ProgressiveAdventure.id("perk_soft_landing");
	private static final Identifier FLEET_FOOT_MODIFIER = ProgressiveAdventure.id("perk_fleet_foot");

	public record Status(int rank, int halfHearts, int stomach) {
	}

	private Progression() {
	}

	public static Status status(ServerPlayer player) {
		int rank = 0;
		int halfHearts = 0;
		int stomach = 0;
		for (Milestone milestone : Milestones.ALL) {
			if (Milestones.isDone(player, milestone)) {
				rank++;
				halfHearts += milestone.halfHearts();
				stomach += milestone.stomach();
			}
		}
		return new Status(rank, halfHearts, stomach);
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
		maxHealth.addOrUpdateTransientModifier(new AttributeModifier(HEALTH_MODIFIER,
				maxHealth(player, status) - maxHealth.getBaseValue(), AttributeModifier.Operation.ADD_VALUE));
		double gained = player.getMaxHealth() - before;
		if (gained > 0) {
			player.heal((float) gained);
		} else if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}

		((FoodDataAccess) player.getFoodData()).pam$setMultipliers(hungerDrain,
				(float) PamConfig.listValue(config.stomachSaturationMultipliers, status.stomach()));

		int deepBreath = Perks.tier(player, Perks.DEEP_BREATH_1, Perks.DEEP_BREATH_2, Perks.DEEP_BREATH_3);
		applyModifier(player, deepBreath > 0, Attributes.OXYGEN_BONUS, DEEP_BREATH_MODIFIER,
				deepBreath > 0 ? PamConfig.listValue(config.deepBreathOxygenBonuses, deepBreath - 1) : 0, AttributeModifier.Operation.ADD_VALUE);
		applyPerkModifier(player, Perks.SOFT_LANDING, Attributes.SAFE_FALL_DISTANCE, SOFT_LANDING_MODIFIER,
				config.softLandingBlocks, AttributeModifier.Operation.ADD_VALUE);
		int fleetFoot = Perks.tier(player, Perks.FLEET_FOOT_1, Perks.FLEET_FOOT_2, Perks.FLEET_FOOT_3);
		applyModifier(player, fleetFoot > 0, Attributes.MOVEMENT_SPEED, FLEET_FOOT_MODIFIER,
				fleetFoot > 0 ? PamConfig.listValue(config.fleetFootSpeedBonuses, fleetFoot - 1) : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

		player.setAttached(PamAttachments.PROGRESSION, new ProgressionSnapshot(status.rank(), Milestones.ALL.size(),
				maxHealth(player, status), config.maxHearts, status.stomach(), hungerDrain, perkPointsEarned(player)));
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
		apply(player);
		int points = perkPoints(holder);
		if (points > 0) {
			player.sendSystemMessage(Component.translatable("message.progressive_adventure.perk_points", points)
					.withStyle(ChatFormatting.LIGHT_PURPLE));
		}

		Milestone milestone = Milestones.byId(holder.id());
		if (milestone == null) {
			return;
		}
		Status status = status(player);
		if (milestone.halfHearts() > 0) {
			player.sendSystemMessage(Component.translatable("message.progressive_adventure.hearts_gained",
					formatHearts(milestone.halfHearts()), formatHearts(maxHealth(player, status))).withStyle(ChatFormatting.RED));
		}
		if (milestone.stomach() > 0) {
			player.sendSystemMessage(Component.translatable("message.progressive_adventure.stomach_upgraded",
					status.stomach()).withStyle(ChatFormatting.GOLD));
		}
		player.sendSystemMessage(Component.translatable("message.progressive_adventure.rank",
				status.rank()).withStyle(ChatFormatting.AQUA));
	}

	public static String formatHearts(int halfHearts) {
		return halfHearts % 2 == 0 ? Integer.toString(halfHearts / 2) : (halfHearts / 2) + ".5";
	}
}
