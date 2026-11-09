package com.progressiveadventure.achievement;

import com.progressiveadventure.ProgressiveAdventure;
import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.PlayerProgress;
import com.progressiveadventure.milestone.Milestones;
import com.progressiveadventure.milestone.Progression;
import com.progressiveadventure.network.AchievementListPayload;
import com.progressiveadventure.network.AchievementListPayload.Entry;
import com.progressiveadventure.network.AchievementListPayload.Group;
import com.progressiveadventure.skill.Skill;
import com.progressiveadventure.waypoint.Waypoints;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/** Server side of the Achievements tab: the list of all advancements with the player's progress, and buying tips. */
public final class Achievements {
	/** Heading order; other mods' tabs come last. */
	private static final List<Identifier> TAB_ORDER = List.of(
			ProgressiveAdventure.id("root"),
			ProgressiveAdventure.id("skills/root"),
			ProgressiveAdventure.id("challenges/root"),
			Identifier.withDefaultNamespace("story/root"),
			Identifier.withDefaultNamespace("nether/root"),
			Identifier.withDefaultNamespace("end/root"),
			Identifier.withDefaultNamespace("adventure/root"),
			Identifier.withDefaultNamespace("husbandry/root"));

	/**
	 * Order of our advancements that share a parent: earlier ones first, related ones together. A streak (foods, mob
	 * types) is a chain of parents, so it stays together anyway (the list is depth first). Unlisted ones (vanilla, other
	 * mods, single children) come after, normal before goal before challenge, then by id.
	 */
	private static final List<Identifier> SIBLING_ORDER = Stream.of(Stream.of(
			// Adventure tab, by category
			"survivor/first_night", "gourmet/first_food", "farmer/crops_3", "farmer/breed_5", "explorer/biomes_5",
			"explorer/walk_5", "explorer/journal", "hunter/mob_types_5", "crafter/diamond_tools", "angler/treasure", "angler/all_fish",
			// After Wanderer: villages and waypoints first, then structures from common to rare
			"explorer/find_village", "explorer/waypoint", "explorer/mineshaft", "explorer/ruined_portal", "explorer/shipwreck",
			"explorer/desert_pyramid", "explorer/jungle_temple", "explorer/witch_hut", "explorer/igloo", "explorer/trail_ruins",
			"explorer/biomes_15", "explorer/five_professions", "explorer/pillager_outpost", "explorer/spelunker", "explorer/biomes_30",
			"explorer/network", "explorer/long_jump", "explorer/first_marker", "explorer/mapmaker",
			"hunter/night_watch", "hunter/elite", "hunter/mob_types_15",
			"hunter/raiders", "hunter/ocean_deep", "hunter/nether_hunter", "hunter/end_hunter",
			"hunter/mob_types_30", "hunter/first_master", "hunter/elder_guardian", "hunter/wither",
			"hunter/elite_types", "hunter/all_traits",
			// Challenges tab: one branch per kind (movement, melee, danger, archery, shield, village), each a chain from easy to hard
			"challenges/nailed_the_landing", "challenges/bare_knuckles", "challenges/defused", "challenges/trick_shot",
			"challenges/perfect_parry", "challenges/safe_and_sound"),
			// Skills tab: in Character tab order, all skills last
			Arrays.stream(Skill.values()).map(skill -> "skills/" + skill.id + "_" + Milestones.SKILL_LEVELS.getFirst()),
			Stream.of("skills/all_10"))
			.flatMap(paths -> paths).map(ProgressiveAdventure::id).toList();

	/** Only a count on these: naming the missing mobs would spoil what's still to discover. */
	private static final Set<Identifier> COUNT_ONLY = Set.of(Identifier.withDefaultNamespace("adventure/kill_all_mobs"));

	private Achievements() {
	}

	public static void onRequest(ServerPlayer player) {
		List<AdvancementNode> roots = new ArrayList<>();
		player.level().getServer().getAdvancements().tree().roots().forEach(roots::add);
		roots.sort(Comparator.<AdvancementNode>comparingInt(root -> tabOrder(root.holder().id())).thenComparing(root -> root.holder().id().toString()));

		List<Group> groups = new ArrayList<>();
		for (AdvancementNode root : roots) {
			List<Entry> entries = new ArrayList<>();
			addChildren(player, root, entries);
			if (!entries.isEmpty()) {
				groups.add(new Group(groupTitle(root), List.copyOf(entries)));
			}
		}
		ServerPlayNetworking.send(player, new AchievementListPayload(List.copyOf(groups), PamConfig.get().achievementTipCosts));
	}

	private static int tabOrder(Identifier root) {
		int index = TAB_ORDER.indexOf(root);
		return index < 0 ? TAB_ORDER.size() : index;
	}

	/** Our own tab has the same name as vanilla's Adventure tab, so it gets its own heading. */
	private static Component groupTitle(AdvancementNode root) {
		if (root.holder().id().equals(ProgressiveAdventure.id("root"))) {
			return Component.translatable("screen.progressive_adventure.achievements.group.mod");
		}
		return root.advancement().display().map(DisplayInfo::getTitle).orElse(Component.literal(root.holder().id().getNamespace()));
	}

	/** Depth first, so an achievement sits next to the ones it leads to. Tab roots aren't listed: they give no Rank. */
	private static void addChildren(ServerPlayer player, AdvancementNode node, List<Entry> entries) {
		List<AdvancementNode> children = new ArrayList<>();
		node.children().forEach(children::add);
		children.sort(Comparator.<AdvancementNode>comparingInt(child -> siblingOrder(child.holder().id()))
				.thenComparingInt(child -> child.advancement().display().map(display -> difficulty(display.getType())).orElse(0))
				.thenComparing(child -> child.holder().id().toString()));
		for (AdvancementNode child : children) {
			child.advancement().display().ifPresent(display -> entries.add(entry(player, child, display)));
			addChildren(player, child, entries);
		}
	}

	/** Not the enum order: that's normal, challenge, goal. */
	private static int difficulty(AdvancementType type) {
		return switch (type) {
			case TASK -> 0;
			case GOAL -> 1;
			case CHALLENGE -> 2;
		};
	}

	private static int siblingOrder(Identifier id) {
		int index = SIBLING_ORDER.indexOf(id);
		return index < 0 ? SIBLING_ORDER.size() : index;
	}

	private static Entry entry(ServerPlayer player, AdvancementNode node, DisplayInfo display) {
		AdvancementHolder holder = node.holder();
		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
		boolean parentDone = node.parent() == null || player.getAdvancements().getOrStartProgress(node.parent().holder()).isDone();

		// One part per requirement, done when any of its criteria is: "kill any of these 41 mobs" is one part
		List<List<String>> requirements = holder.value().requirements().requirements();
		List<String> missing = new ArrayList<>();
		for (List<String> requirement : requirements) {
			if (requirement.stream().noneMatch(criterion -> isDone(progress, criterion))) {
				missing.add(requirement.getFirst());
			}
		}
		int total = requirements.size();
		int completed = total - missing.size();
		if (COUNT_ONLY.contains(holder.id())) {
			missing.clear();
		}
		return new Entry(holder.id().toString(), display.getTitle(), display.getDescription(), display.getIcon().create(),
				display.isHidden() && !progress.isDone(), progress.isDone(), parentDone, Progression.perkPoints(holder), completed, total,
				List.copyOf(missing));
	}

	private static boolean isDone(AdvancementProgress progress, String criterion) {
		CriterionProgress criterionProgress = progress.getCriterion(criterion);
		return criterionProgress != null && criterionProgress.isDone();
	}

	/**
	 * Tips are bought in order. The server doesn't know how many an achievement has (their text is client-side lang), so a
	 * made-up tip number only wastes the sender's own XP. Done achievements show their tips for free, so there's nothing to buy.
	 */
	public static void onBuyTip(ServerPlayer player, String advancement, int tip) {
		Identifier id = Identifier.tryParse(advancement);
		AdvancementHolder holder = id == null ? null : player.level().getServer().getAdvancements().get(id);
		if (holder == null || holder.value().display().isEmpty()) {
			return;
		}
		boolean done = player.getAdvancements().getOrStartProgress(holder).isDone();
		PlayerProgress progress = PamAttachments.progress(player);
		if (done || holder.value().display().get().isHidden() || tip != progress.tipsBoughtOf(advancement) + 1) {
			return;
		}
		List<Integer> costs = PamConfig.get().achievementTipCosts;
		int cost = costs.isEmpty() ? 0 : costs.get(Math.min(tip - 1, costs.size() - 1));
		if (Waypoints.totalXp(player) < cost) {
			player.sendOverlayMessage(Component.translatable("message.progressive_adventure.tip.needs_xp", cost).withStyle(ChatFormatting.YELLOW));
			return;
		}
		if (cost > 0) {
			player.giveExperiencePoints(-cost);
		}
		PamAttachments.setProgress(player, progress.withTipBought(advancement));
		player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4f, 1.5f);
	}
}
