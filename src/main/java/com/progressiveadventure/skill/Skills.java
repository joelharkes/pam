package com.progressiveadventure.skill;

import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.PlayerProgress;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class Skills {
	private Skills() {
	}

	public static int level(int xp) {
		return (int) Math.floor(Math.sqrt(xp / PamConfig.get().xpPerLevelFactor));
	}

	public static int xpForLevel(int level) {
		return (int) Math.ceil(level * level * PamConfig.get().xpPerLevelFactor);
	}

	/**
	 * Fraction added to break speed, e.g. 0.25 = 25% faster. Starts below vanilla at minSpeedBonus, rises fast
	 * early and flattens towards maxSpeedBonus.
	 */
	public static double speedBonus(int level) {
		PamConfig config = PamConfig.get();
		return config.minSpeedBonus + (config.maxSpeedBonus - config.minSpeedBonus) * (1 - Math.exp(-level / config.speedBonusCurve));
	}

	public static String formatBonus(int level) {
		long percent = Math.round(speedBonus(level) * 100);
		return (percent >= 0 ? "+" : "") + percent + "%";
	}

	/** The skill that applies when breaking this block with this tool, or null. */
	public static Skill skillFor(ItemStack tool, BlockState state) {
		for (Skill skill : Skill.values()) {
			if (tool.is(skill.tools) && state.is(skill.blocks)) {
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
		return 1f + (float) speedBonus(level(PamAttachments.progress(player).xpOf(skill.id)));
	}

	public static void onBlockBroken(ServerPlayer player, BlockState state) {
		Skill skill = skillFor(player.getMainHandItem(), state);
		if (skill == null) {
			return;
		}
		int xp = state.is(ConventionalBlockTags.ORES) ? PamConfig.get().oreXp : 1;
		PlayerProgress progress = PamAttachments.progress(player);
		int levelBefore = level(progress.xpOf(skill.id));
		progress = progress.withSkillXp(skill.id, xp);
		PamAttachments.setProgress(player, progress);

		int levelAfter = level(progress.xpOf(skill.id));
		if (levelAfter > levelBefore) {
			player.sendOverlayMessage(Component.translatable("message.progressive_adventure.skill_level_up",
					Component.translatable("skill.progressive_adventure." + skill.id), levelAfter,
					formatBonus(levelAfter)).withStyle(ChatFormatting.GREEN));
			player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.5f);
		}
	}
}
