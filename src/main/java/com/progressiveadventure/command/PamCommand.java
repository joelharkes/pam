package com.progressiveadventure.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.progressiveadventure.bestiary.Bestiary;
import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.config.PamConfig.FamiliarityTier;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.PlayerProgress;
import com.progressiveadventure.milestone.Milestones;
import com.progressiveadventure.milestone.Progression;
import com.progressiveadventure.skill.Skill;
import com.progressiveadventure.skill.Skills;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.Map;

/** /pam: shows your progress until there is a proper screen for it. */
public final class PamCommand {
	private PamCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("pam")
				.executes(PamCommand::showStats)
				.then(Commands.literal("bestiary").executes(PamCommand::showBestiary)));
	}

	private static int showStats(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		Progression.Status status = Progression.status(player);
		PlayerProgress progress = PamAttachments.progress(player);
		PamConfig config = PamConfig.get();

		send(player, Component.translatable("command.progressive_adventure.header").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		send(player, Component.translatable("command.progressive_adventure.rank", status.rank(), Milestones.ALL.size()));
		send(player, Component.translatable("command.progressive_adventure.hearts",
				Progression.formatHearts(Progression.maxHealth(status)), config.maxHearts));
		send(player, Component.translatable("command.progressive_adventure.stomach", status.stomach(),
				Math.round(PamConfig.listValue(config.stomachExhaustionMultipliers, status.stomach()) * 100)));
		for (Skill skill : Skill.values()) {
			int xp = progress.xpOf(skill.id);
			int level = Skills.level(xp);
			send(player, Component.translatable("command.progressive_adventure.skill",
					Component.translatable("skill.progressive_adventure." + skill.id), level,
					xp, Skills.xpForLevel(level + 1), Math.round(Skills.speedBonus(level) * 100)));
		}
		send(player, Component.translatable("command.progressive_adventure.explore",
				progress.foodsEaten().size(), progress.biomes().size(), progress.kills().size()));
		return 1;
	}

	private static int showBestiary(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		Map<String, Integer> kills = PamAttachments.progress(player).kills();
		send(player, Component.translatable("command.progressive_adventure.bestiary_header").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		if (kills.isEmpty()) {
			send(player, Component.translatable("command.progressive_adventure.bestiary_empty"));
		}
		kills.entrySet().stream()
				.sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
				.forEach(entry -> {
					FamiliarityTier tier = Bestiary.tier(entry.getValue());
					Component name = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(entry.getKey()))
							.<Component>map(type -> type.getDescription())
							.orElse(Component.literal(entry.getKey()));
					send(player, Component.translatable("command.progressive_adventure.bestiary_entry", name, entry.getValue(),
							Component.translatable("familiarity.progressive_adventure." + tier.label()),
							Bestiary.formatModifier(tier.damageModifier())));
				});
		return 1;
	}

	private static void send(ServerPlayer player, Component message) {
		player.sendSystemMessage(message);
	}
}
