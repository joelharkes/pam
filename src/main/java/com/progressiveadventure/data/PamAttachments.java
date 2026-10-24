package com.progressiveadventure.data;

import com.mojang.serialization.Codec;
import com.progressiveadventure.ProgressiveAdventure;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Set;

public final class PamAttachments {
	/** Synced to the owning player only: the client needs skill levels to predict block breaking speed. */
	public static final AttachmentType<PlayerProgress> PLAYER_PROGRESS = AttachmentRegistry.<PlayerProgress>builder()
			.persistent(PlayerProgress.CODEC)
			.copyOnDeath()
			.initializer(() -> PlayerProgress.EMPTY)
			.syncWith(ByteBufCodecs.fromCodecWithRegistries(PlayerProgress.CODEC), AttachmentSyncPredicate.targetOnly())
			.buildAndRegister(ProgressiveAdventure.id("player_progress"));

	/** Server-derived numbers the client can't compute itself (it doesn't know all advancements or the server config). */
	public static final AttachmentType<ProgressionSnapshot> PROGRESSION = AttachmentRegistry.<ProgressionSnapshot>builder()
			.initializer(() -> ProgressionSnapshot.EMPTY)
			.syncWith(ByteBufCodecs.fromCodecWithRegistries(ProgressionSnapshot.CODEC), AttachmentSyncPredicate.targetOnly())
			.buildAndRegister(ProgressiveAdventure.id("progression"));

	/** Per chunk: packed positions of blocks placed by players, which give no skill xp when broken. */
	public static final AttachmentType<Set<Long>> PLACED_BLOCKS = AttachmentRegistry.<Set<Long>>builder()
			.persistent(Codec.LONG.listOf().xmap(Set::copyOf, List::copyOf))
			.initializer(Set::of)
			.buildAndRegister(ProgressiveAdventure.id("placed_blocks"));

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
