package com.progressiveadventure;

import com.progressiveadventure.bestiary.Bestiary;
import com.progressiveadventure.command.PamCommand;
import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.milestone.Progression;
import com.progressiveadventure.milestone.Trackers;
import com.progressiveadventure.skill.PlacedBlocks;
import com.progressiveadventure.skill.Skills;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgressiveAdventure implements ModInitializer {
	public static final String MOD_ID = "progressive_adventure";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PamConfig.load();
		PamAttachments.init();

		ServerPlayerEvents.JOIN.register(Progression::apply);
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> Progression.apply(newPlayer));

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) {
				Trackers.onDeath(player);
			} else if (source.getEntity() instanceof ServerPlayer player) {
				Bestiary.onKill(player, entity);
			}
		});

		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (!PlacedBlocks.remove(level, pos) && player instanceof ServerPlayer serverPlayer) {
				Skills.onBlockBroken(serverPlayer, state);
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) {
				Trackers.tick(server);
			}
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> PamCommand.register(dispatcher));

		LOGGER.info("PAM: Progressive Adventure Mod loaded");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
