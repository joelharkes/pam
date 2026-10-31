package com.progressiveadventure.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.progressiveadventure.bestiary.Bestiary;
import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.config.PamConfig.FamiliarityTier;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.PlayerProgress;
import com.progressiveadventure.milestone.Progression;
import com.progressiveadventure.perk.Perks;
import com.progressiveadventure.skill.Skill;
import com.progressiveadventure.skill.Skills;
import com.progressiveadventure.waypoint.Waypoint;
import com.progressiveadventure.waypoint.Waypoints;
import com.progressiveadventure.world.EliteMap;
import com.progressiveadventure.world.EliteTrait;
import com.progressiveadventure.world.Elites;
import com.progressiveadventure.world.MobUnlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** /pam: shows your progress until there is a proper screen for it. */
public final class PamCommand {
	private PamCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("pam")
				.executes(PamCommand::showStats)
				.then(Commands.literal("bestiary").executes(PamCommand::showBestiary))
				// Test helper for ops: elites are rare, so spawn one on demand
				.then(Commands.literal("elite").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("mob", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(
										PamConfig.get().eliteMobs.stream().map(id -> Identifier.parse(id).getPath()), builder))
								.executes(context -> spawnElite(context, null))
								.then(Commands.argument("trait", StringArgumentType.word())
										.suggests((context, builder) -> SharedSuggestionProvider.suggest(
												Arrays.stream(EliteTrait.values()).map(trait -> trait.id), builder))
										.executes(context -> spawnElite(context, StringArgumentType.getString(context, "trait"))))))
				// Test helper for ops: get an elite's map right away; the message shows how long the search took
				.then(Commands.literal("elitemap").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("map", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(
										Arrays.stream(EliteMap.values()).map(map -> map.id), builder))
								.executes(PamCommand::giveEliteMap)))
				// Test helper for ops: set a skill level to try perks without levelling
				.then(Commands.literal("skill").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("skill", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(
										Arrays.stream(Skill.values()).map(skill -> skill.id), builder))
								.then(Commands.argument("level", IntegerArgumentType.integer(0, 100))
										.executes(context -> setSkill(context, context.getSource().getPlayerOrException()))
										.then(Commands.argument("player", EntityArgument.player())
												.executes(context -> setSkill(context, EntityArgument.getPlayer(context, "player")))))))
				// Test helper for ops: set bestiary kills to try familiarity tiers without grinding
				.then(Commands.literal("kills").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("mob", StringArgumentType.word())
								.then(Commands.argument("count", IntegerArgumentType.integer(0, 10000))
										.executes(context -> setKills(context, context.getSource().getPlayerOrException()))
										.then(Commands.argument("player", EntityArgument.player())
												.executes(context -> setKills(context, EntityArgument.getPlayer(context, "player")))))))
				// Waypoint tools for ops: list all, travel for free, remove one (turns the stone off, drops its shards)
				.then(Commands.literal("waypoint").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("list").executes(PamCommand::listWaypoints))
						.then(Commands.literal("tp")
								.then(Commands.argument("name", StringArgumentType.greedyString()).suggests(PamCommand::suggestWaypoints)
										.executes(context -> waypointCommand(context, false))))
						.then(Commands.literal("remove")
								.then(Commands.argument("name", StringArgumentType.greedyString()).suggests(PamCommand::suggestWaypoints)
										.executes(context -> waypointCommand(context, true))))));
	}

	private static CompletableFuture<Suggestions> suggestWaypoints(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(Waypoints.all(context.getSource().getServer()).stream().map(Waypoint::name), builder);
	}

	private static int listWaypoints(CommandContext<CommandSourceStack> context) {
		List<Waypoint> waypoints = Waypoints.all(context.getSource().getServer());
		context.getSource().sendSuccess(() -> Component.translatable("command.progressive_adventure.waypoint.list_header", waypoints.size())
				.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
		for (Waypoint waypoint : waypoints) {
			BlockPos pos = waypoint.pos();
			context.getSource().sendSuccess(() -> Component.translatable("command.progressive_adventure.waypoint.list_entry", waypoint.name(),
					waypoint.ownerName(), waypoint.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(), waypoint.shards()), false);
		}
		return waypoints.size();
	}

	private static int waypointCommand(CommandContext<CommandSourceStack> context, boolean remove) throws CommandSyntaxException {
		String name = StringArgumentType.getString(context, "name");
		Optional<Waypoint> waypoint = Waypoints.byName(context.getSource().getServer(), name);
		if (waypoint.isEmpty()) {
			context.getSource().sendFailure(Component.translatable("command.progressive_adventure.waypoint.unknown", name));
			return 0;
		}
		if (remove) {
			Waypoints.remove(context.getSource().getServer(), waypoint.get());
			context.getSource().sendSuccess(() -> Component.translatable("command.progressive_adventure.waypoint.removed", waypoint.get().name()), true);
			return 1;
		}
		return Waypoints.teleport(context.getSource().getPlayerOrException(), waypoint.get()) ? 1 : 0;
	}

	private static int setSkill(CommandContext<CommandSourceStack> context, ServerPlayer target) {
		String id = StringArgumentType.getString(context, "skill");
		Skill skill = Arrays.stream(Skill.values()).filter(s -> s.id.equals(id)).findFirst().orElse(null);
		if (skill == null) {
			context.getSource().sendFailure(Component.translatable("command.progressive_adventure.skill_set.unknown", id));
			return 0;
		}
		int level = IntegerArgumentType.getInteger(context, "level");
		// Via addXp, so raising a level shows the usual level-up message
		Skills.addXp(target, skill, Skills.xpForLevel(level) - PamAttachments.progress(target).xpOf(skill.id));
		Component name = Component.translatable("skill.progressive_adventure." + skill.id);
		context.getSource().sendSuccess(() -> Component.translatable("command.progressive_adventure.skill_set",
				target.getDisplayName(), name, level), true);
		return 1;
	}

	private static int setKills(CommandContext<CommandSourceStack> context, ServerPlayer target) {
		String mob = StringArgumentType.getString(context, "mob");
		Identifier id = Identifier.tryParse(mob);
		if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
			context.getSource().sendFailure(Component.translatable("command.progressive_adventure.kills_set.unknown", mob));
			return 0;
		}
		EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
		int count = IntegerArgumentType.getInteger(context, "count");
		Bestiary.setKills(target, type, count);
		context.getSource().sendSuccess(() -> Component.translatable("command.progressive_adventure.kills_set",
				target.getDisplayName(), type.getDescription(), count), true);
		return 1;
	}

	private static int spawnElite(CommandContext<CommandSourceStack> context, String traitId) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		Identifier id = Identifier.tryParse(StringArgumentType.getString(context, "mob"));
		EliteTrait trait = traitId == null ? null : EliteTrait.byId(traitId);
		if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id) || (traitId != null && trait == null)
				|| !Elites.spawnTestElite(player, BuiltInRegistries.ENTITY_TYPE.getValue(id), trait)) {
			context.getSource().sendFailure(Component.translatable("command.progressive_adventure.elite.unknown"));
			return 0;
		}
		return 1;
	}

	private static int giveEliteMap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		EliteMap map = EliteMap.byId(StringArgumentType.getString(context, "map"));
		if (map == null) {
			context.getSource().sendFailure(Component.translatable("command.progressive_adventure.elitemap.unknown"));
			return 0;
		}
		long start = System.nanoTime();
		ItemStack stack = map.create(player.level(), player.blockPosition(), PamConfig.get().eliteMapSearchRadiusChunks);
		long millis = (System.nanoTime() - start) / 1_000_000;
		if (stack.isEmpty()) {
			context.getSource().sendFailure(Component.translatable("command.progressive_adventure.elitemap.none", millis));
			return 0;
		}
		player.getInventory().placeItemBackInInventory(stack);
		context.getSource().sendSuccess(() -> Component.translatable("command.progressive_adventure.elitemap.found", millis), false);
		return 1;
	}

	private static int showStats(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		Progression.Status status = Progression.status(player);
		PlayerProgress progress = PamAttachments.progress(player);
		PamConfig config = PamConfig.get();

		send(player, Component.translatable("command.progressive_adventure.header").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		send(player, Component.translatable("command.progressive_adventure.rank", status.rank()));
		MobUnlocks.next(status.rank()).ifPresent(next -> send(player, Component.translatable("command.progressive_adventure.next_mobs",
				MobUnlocks.groupName(next.name()), next.rank())));
		send(player, Component.translatable("command.progressive_adventure.hearts",
				Progression.formatHearts(Progression.maxHealth(player, status)), config.maxHearts));
		send(player, Component.translatable("command.progressive_adventure.perk_points",
				Perks.pointsAvailable(progress, PamAttachments.snapshot(player)), progress.perks().size()));
		send(player, Component.translatable("command.progressive_adventure.stomach", status.stomach(),
				Math.round(PamConfig.listValue(config.stomachExhaustionMultipliers, status.stomach()) * 100)));
		for (Skill skill : Skill.values()) {
			int xp = progress.xpOf(skill.id);
			int level = Skills.level(xp);
			send(player, Component.translatable("command.progressive_adventure.skill",
					Component.translatable("skill.progressive_adventure." + skill.id), level,
					xp, Skills.xpForLevel(level + 1), Skills.describeBonus(skill, level)));
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
