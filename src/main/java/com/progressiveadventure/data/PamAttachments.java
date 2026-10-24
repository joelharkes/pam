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

	/** Per chunk: packed positions of blocks placed by players, which give no skill xp when broken. */
	public static final AttachmentType<Set<Long>> PLACED_BLOCKS = AttachmentRegistry.<Set<Long>>builder()
			.persistent(Codec.LONG.listOf().xmap(Set::copyOf, List::copyOf))
			.initializer(Set::of)
			.buildAndRegister(ProgressiveAdventure.id("placed_blocks"));

	private PamAttachments() {
	}

	public static PlayerProgress progress(Player player) {
		return player.getAttachedOrCreate(PLAYER_PROGRESS);
	}

	public static void setProgress(Player player, PlayerProgress progress) {
		player.setAttached(PLAYER_PROGRESS, progress);
	}

	public static void init() {
		// Loads the class so the attachment types register during mod init
	}
}
