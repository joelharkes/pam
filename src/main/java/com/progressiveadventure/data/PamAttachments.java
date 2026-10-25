package com.progressiveadventure.data;

import com.mojang.serialization.Codec;
import com.progressiveadventure.ProgressiveAdventure;
import com.progressiveadventure.waypoint.Waypoint;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class PamAttachments {
	/** Synced to the owning player only: the client needs skill levels to predict block breaking speed. */
	public static final AttachmentType<PlayerProgress> PLAYER_PROGRESS = AttachmentRegistry.create(
			ProgressiveAdventure.id("player_progress"), builder -> builder
					.persistent(PlayerProgress.CODEC)
					.copyOnDeath()
					.initializer(() -> PlayerProgress.EMPTY)
					.syncWith(ByteBufCodecs.fromCodecWithRegistries(PlayerProgress.CODEC), AttachmentSyncPredicate.targetOnly()));

	/** Server-derived numbers the client can't compute itself (it doesn't know all advancements or the server config). */
	public static final AttachmentType<ProgressionSnapshot> PROGRESSION = AttachmentRegistry.create(
			ProgressiveAdventure.id("progression"), builder -> builder
					.initializer(() -> ProgressionSnapshot.EMPTY)
					.syncWith(ByteBufCodecs.fromCodecWithRegistries(ProgressionSnapshot.CODEC), AttachmentSyncPredicate.targetOnly()));

	/**
	 * Per chunk: packed positions of blocks placed by players, which give no skill xp when broken. Mutable and
	 * oldest first (see PlacedBlocks); not synced, so the immutable-value rule doesn't apply.
	 */
	public static final AttachmentType<LongLinkedOpenHashSet> PLACED_BLOCKS = AttachmentRegistry.create(
			ProgressiveAdventure.id("placed_blocks"), builder -> builder
					.persistent(Codec.LONG.listOf().xmap(LongLinkedOpenHashSet::new, LongArrayList::new))
					.initializer(LongLinkedOpenHashSet::new));

	/** On elite mobs: the Adventure Rank they spawned at (decides their XP and loot). */
	public static final AttachmentType<Integer> ELITE_RANK = AttachmentRegistry.create(
			ProgressiveAdventure.id("elite_rank"), builder -> builder.persistent(Codec.INT));

	/** On elites with a trait: the trait id. */
	public static final AttachmentType<String> ELITE_TRAIT = AttachmentRegistry.create(
			ProgressiveAdventure.id("elite_trait"), builder -> builder.persistent(Codec.STRING));

	/** On Summoner elites that already called their helpers. */
	public static final AttachmentType<Boolean> ELITE_SUMMONED = AttachmentRegistry.create(
			ProgressiveAdventure.id("elite_summoned"), builder -> builder.persistent(Codec.BOOL));

	/** On arrows shot with a Frost Arrows proc: slow the mob they hit. */
	public static final AttachmentType<Boolean> FROST_ARROW = AttachmentRegistry.create(
			ProgressiveAdventure.id("frost_arrow"), builder -> builder.persistent(Codec.BOOL));

	/** On arrows shot by a player with Steady Aim: they curve toward hostile mobs. */
	public static final AttachmentType<Boolean> STEADY_AIM_ARROW = AttachmentRegistry.create(
			ProgressiveAdventure.id("steady_aim_arrow"), builder -> builder.persistent(Codec.BOOL));

	/** On arrows that already bounced (or were bounced) with Ricochet: they don't bounce again. */
	public static final AttachmentType<Boolean> RICOCHET_ARROW = AttachmentRegistry.create(
			ProgressiveAdventure.id("ricochet_arrow"), builder -> builder.persistent(Codec.BOOL));

	/** On the Overworld: all turned-on Waypoint Stones of every dimension (shared by all players). */
	public static final AttachmentType<List<Waypoint>> WAYPOINTS = AttachmentRegistry.create(
			ProgressiveAdventure.id("waypoints"), builder -> builder
					.persistent(Waypoint.CODEC.listOf())
					.initializer(List::of));

	private PamAttachments() {
	}

	public static PlayerProgress progress(Player player) {
		return player.getAttachedOrElse(PLAYER_PROGRESS, PlayerProgress.EMPTY);
	}

	public static void setProgress(Player player, PlayerProgress progress) {
		player.setAttached(PLAYER_PROGRESS, progress);
	}

	public static ProgressionSnapshot snapshot(Player player) {
		return player.getAttachedOrElse(PROGRESSION, ProgressionSnapshot.EMPTY);
	}

	public static void init() {
		// Loads the class so the attachment types register during mod init
	}
}
