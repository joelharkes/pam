package com.progressiveadventure.milestone;

import com.progressiveadventure.ProgressiveAdventure;
import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.access.FoodDataAccess;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Derives hearts, stomach level and Adventure Rank from completed milestones and applies them to the player.
 * Everything is recomputed from advancement progress, so rewards can never be granted twice and balance
 * changes apply to existing players.
 */
public final class Progression {
	private static final Identifier HEALTH_MODIFIER = ProgressiveAdventure.id("milestone_hearts");

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

	public static int maxHealth(Status status) {
		PamConfig config = PamConfig.get();
		return Math.min(config.startingHearts * 2 + status.halfHearts(), config.maxHearts * 2);
	}

	/** Applies max health and stomach multipliers. Call on join, respawn and milestone completion. */
	public static void apply(ServerPlayer player) {
		Status status = status(player);
		PamConfig config = PamConfig.get();

		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		double before = player.getMaxHealth();
		maxHealth.addOrUpdateTransientModifier(new AttributeModifier(HEALTH_MODIFIER,
				maxHealth(status) - maxHealth.getBaseValue(), AttributeModifier.Operation.ADD_VALUE));
		double gained = player.getMaxHealth() - before;
		if (gained > 0) {
			player.heal((float) gained);
		} else if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}

		((FoodDataAccess) player.getFoodData()).pam$setMultipliers(
				(float) PamConfig.listValue(config.stomachExhaustionMultipliers, status.stomach()),
				(float) PamConfig.listValue(config.stomachSaturationMultipliers, status.stomach()));
	}

	public static void onMilestoneCompleted(ServerPlayer player, Milestone milestone) {
		apply(player);
		Status status = status(player);
		if (milestone.halfHearts() > 0) {
			player.sendSystemMessage(Component.translatable("message.progressive_adventure.hearts_gained",
					formatHearts(milestone.halfHearts()), formatHearts(maxHealth(status))).withStyle(ChatFormatting.RED));
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
