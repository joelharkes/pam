package com.progressiveadventure.skill;

import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.PlayerProgress;
import com.progressiveadventure.milestone.Milestones;
import com.progressiveadventure.milestone.Progression;
import com.progressiveadventure.perk.Perk;
import com.progressiveadventure.perk.Perks;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public final class Skills {
	private Skills() {
	}

	public static int level(int xp) {
		return (int) Math.floor(Math.sqrt(xp / PamConfig.get().xpPerLevelFactor));
	}

	public static int level(Player player, Skill skill) {
		return level(PamAttachments.progress(player).xpOf(skill.id));
	}

	public static int xpForLevel(int level) {
		return (int) Math.ceil(level * level * PamConfig.get().xpPerLevelFactor);
	}

	/**
	 * The skill's bonus as a fraction. Tool skills: break speed, starting below vanilla at minSpeedBonus and
	 * flattening towards maxSpeedBonus. Farming: extra harvest chance, Melee: attack speed, Archery: draw speed,
	 * Fishing: bite speed, all starting at 0 (food and combat already have their own early penalties, fishing is
	 * slow already).
	 */
	public static double bonus(Skill skill, int level) {
		PamConfig config = PamConfig.get();
		double curve = 1 - Math.exp(-level / config.speedBonusCurve);
		return switch (skill) {
			case FARMING -> config.farmingMaxHarvestBonus * curve;
			case MELEE -> config.meleeMaxAttackSpeedBonus * curve;
			case ARCHERY -> config.archeryMaxDrawBonus * curve;
			case FISHING -> config.fishingMaxBiteBonus * curve;
			default -> config.minSpeedBonus + (config.maxSpeedBonus - config.minSpeedBonus) * curve;
		};
	}

	/** E.g. "+19% speed", "+12% extra harvest" or "+8% draw speed". */
	public static Component describeBonus(Skill skill, int level) {
		String key = switch (skill) {
			case FARMING -> "harvest";
			case MELEE -> "attack";
			case ARCHERY -> "draw";
			case FISHING -> "bite";
			default -> "speed";
		};
		return Component.translatable("skill_bonus.progressive_adventure." + key, formatBonus(skill, level));
	}

	/** E.g. "+19%" or "-20%". */
	public static String formatBonus(Skill skill, int level) {
		long percent = Math.round(bonus(skill, level) * 100);
		return (percent >= 0 ? "+" : "") + percent + "%";
	}

	/** The tool skill that applies when breaking this block with this tool, or null. Farm blocks never count. */
	public static Skill skillFor(ItemStack tool, BlockState state) {
		if (Farming.isFarmBlock(state)) {
			return null;
		}
		for (Skill skill : Skill.values()) {
			if (skill.isToolSkill() && tool.is(skill.tools) && state.is(skill.blocks)) {
				return skill;
			}
		}
		return null;
	}

	/** Break speed multiplier. Runs on client and server, so it reads the synced attachment. */
	public static float speedMultiplier(Player player, BlockState state) {
		Skill skill = skillFor(player.getMainHandItem(), state);
		if (skill == null) {
			return 1f;
		}
		return 1f + (float) bonus(skill, level(player, skill));
	}

	public static void onBlockBroken(ServerPlayer player, BlockState state) {
		Skill skill = skillFor(player.getMainHandItem(), state);
		if (skill != null) {
			addXp(player, skill, blockXp(state));
		}
	}

	/** Xp for breaking a natural block with its skill's tool: rarer ores give more (see PamConfig.miningXpByBlock). */
	static int blockXp(BlockState state) {
		PamConfig config = PamConfig.get();
		Integer byBlock = config.miningXpByBlock.get(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
		if (byBlock != null) {
			return byBlock;
		}
		for (Map.Entry<String, Integer> entry : config.miningXpByBlock.entrySet()) {
			if (entry.getKey().startsWith("#")
					&& state.is(TagKey.create(Registries.BLOCK, Identifier.parse(entry.getKey().substring(1))))) {
				return entry.getValue();
			}
		}
		return state.is(ConventionalBlockTags.ORES) ? config.oreXp : 1;
	}

	public static void addXp(ServerPlayer player, Skill skill, int xp) {
		PlayerProgress progress = PamAttachments.progress(player);
		int levelBefore = level(progress.xpOf(skill.id));
		progress = progress.withSkillXp(skill.id, xp);
		PamAttachments.setProgress(player, progress);

		int levelAfter = level(progress.xpOf(skill.id));
		if (skill == Skill.MELEE && levelAfter != levelBefore) {
			Progression.apply(player); // attack speed is an attribute modifier
		}
		if (levelAfter > levelBefore) {
			checkAdvancements(player, progress);
			MutableComponent message = Component.translatable("message.progressive_adventure.skill_level_up",
					Component.translatable("skill.progressive_adventure." + skill.id), levelAfter,
					describeBonus(skill, levelAfter)).withStyle(ChatFormatting.GREEN);
			for (Perk perk : Perks.newlyAvailable(skill, levelBefore, levelAfter, progress, PamAttachments.snapshot(player))) {
				message.append(Component.translatable("message.progressive_adventure.skill_new_perk", perk.name()).withStyle(ChatFormatting.GOLD));
			}
			player.sendOverlayMessage(message);
			player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.5f);
		}
	}

	/** Skills tab advancements. Also checked on join, for levels from before these existed. */
	public static void checkAdvancements(ServerPlayer player, PlayerProgress progress) {
		int lowest = Integer.MAX_VALUE;
		for (Skill skill : Skill.values()) {
			int level = level(progress.xpOf(skill.id));
			lowest = Math.min(lowest, level);
			for (int milestoneLevel : Milestones.SKILL_LEVELS) {
				if (level >= milestoneLevel) {
					Milestones.grant(player, Milestones.skillLevel(skill, milestoneLevel));
				}
			}
		}
		Milestones.checkThresholds(player, Milestones.ALL_SKILLS_THRESHOLDS, lowest);
	}
}
