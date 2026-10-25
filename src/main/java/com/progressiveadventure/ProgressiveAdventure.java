package com.progressiveadventure;

import com.progressiveadventure.bestiary.Bestiary;
import com.progressiveadventure.command.PamCommand;
import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.milestone.Progression;
import com.progressiveadventure.milestone.Trackers;
import com.progressiveadventure.network.PamNetworking;
import com.progressiveadventure.perk.MovementPerks;
import com.progressiveadventure.perk.PerkEffects;
import com.progressiveadventure.skill.Archery;
import com.progressiveadventure.skill.Farming;
import com.progressiveadventure.skill.PlacedBlocks;
import com.progressiveadventure.skill.Skills;
import com.progressiveadventure.waypoint.WaypointContent;
import com.progressiveadventure.waypoint.Waypoints;
import com.progressiveadventure.world.Elites;
import com.progressiveadventure.world.MobUnlocks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgressiveAdventure implements ModInitializer {
	public static final String MOD_ID = "progressive_adventure";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PamConfig.load();
		WaypointContent.init();
		PamAttachments.init();
		PamNetworking.init();

		// After all mods registered their mobs, so modded ids in the config are known
		ServerLifecycleEvents.SERVER_STARTED.register(server -> MobUnlocks.validateConfig());
		ServerPlayerEvents.JOIN.register(Progression::apply);
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> Progression.apply(newPlayer));

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer player) {
				Trackers.onDeath(player);
			} else if (source.getEntity() instanceof ServerPlayer player) {
				Bestiary.onKill(player, entity);
				Archery.onKill(entity, source);
			}
			Elites.onDeath(entity, source);
		});

		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (entity instanceof ServerPlayer player) {
				PerkEffects.onDamaged(player);
				if (damageTaken > 0) {
					Waypoints.onDamaged(player);
				}
			}
			Elites.onDamage(entity, source, damageTaken);
			Archery.onDamage(entity, source, damageTaken, blocked);
		});
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			Elites.onLoad(entity);
			Waypoints.onEntityLoad(entity, level);
		});

		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> Waypoints.canBreak(player, level, pos, state));

		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (!(player instanceof ServerPlayer serverPlayer)) {
				return;
			}
			boolean placed = PlacedBlocks.remove(level, pos);
			if (!placed) {
				Skills.onBlockBroken(serverPlayer, state);
				PerkEffects.onNaturalBlockBroken(serverPlayer, (ServerLevel) level, pos, state, blockEntity);
			}
			// Grown crops count even when planted by a player; Farming checks placement itself
			Farming.onBlockBroken(serverPlayer, (ServerLevel) level, pos, state, blockEntity, placed);
		});

		UseBlockCallback.EVENT.register(Farming::onUseBlock);

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) {
				Trackers.tick(server);
			}
			MovementPerks.tick(server);
			PerkEffects.tick();
			Waypoints.tick(server);
			// Last, so changes from the handlers above go out in the same packet
			PamAttachments.flushAllProgress();
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> PamCommand.register(dispatcher));

		LOGGER.info("PAM: Progressive Adventure Mod loaded");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
