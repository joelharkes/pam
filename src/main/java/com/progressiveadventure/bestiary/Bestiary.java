package com.progressiveadventure.bestiary;

import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.config.PamConfig.FamiliarityTier;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.PlayerProgress;
import com.progressiveadventure.milestone.Milestones;
import com.progressiveadventure.perk.Perks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Mob familiarity: kills per mob type change the damage you deal to that type. */
public final class Bestiary {
	private Bestiary() {
	}

	public static String key(EntityType<?> type) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
	}

	public static FamiliarityTier tier(int kills) {
		FamiliarityTier result = null;
		for (FamiliarityTier tier : PamConfig.get().familiarityTiers) {
			if (kills >= tier.kills()) {
				result = tier;
			}
		}
		return result;
	}

	public static float damageMultiplier(ServerPlayer player, LivingEntity target) {
		FamiliarityTier tier = tier(PamAttachments.progress(player).killsOf(key(target.getType())));
		return tier == null ? 1f : 1f + (float) tier.damageModifier();
	}

	public static void onKill(ServerPlayer player, LivingEntity killed) {
		if (killed instanceof Player) {
			return;
		}
		String key = key(killed.getType());
		PlayerProgress progress = PamAttachments.progress(player);
		int before = progress.killsOf(key);
		progress = progress.withKills(key, Perks.has(player, Perks.QUICK_STUDY) ? 2 : 1);
		PamAttachments.setProgress(player, progress);

		FamiliarityTier oldTier = tier(before);
		FamiliarityTier newTier = tier(progress.killsOf(key));
		Component mobName = killed.getType().getDescription();
		if (before == 0) {
			player.sendOverlayMessage(Component.translatable("message.progressive_adventure.bestiary_new", mobName)
					.withStyle(ChatFormatting.YELLOW));
		} else if (newTier != null && newTier != oldTier) {
			player.sendOverlayMessage(Component.translatable("message.progressive_adventure.bestiary_tier",
					mobName, Component.translatable("familiarity.progressive_adventure." + newTier.label()),
					formatModifier(newTier.damageModifier())).withStyle(ChatFormatting.YELLOW));
		}

		Milestones.checkThresholds(player, Milestones.MOB_TYPE_THRESHOLDS, progress.kills().size());
	}

	public static String formatModifier(double modifier) {
		long percent = Math.round(modifier * 100);
		return (percent >= 0 ? "+" : "") + percent + "%";
	}
}
