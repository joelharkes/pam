package com.progressiveadventure.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.progressiveadventure.ProgressiveAdventure;
import com.progressiveadventure.client.screen.AdventureScreen;
import com.progressiveadventure.client.screen.WaypointListScreen;
import com.progressiveadventure.client.screen.WaypointNameScreen;
import com.progressiveadventure.network.OpenWaypointListPayload;
import com.progressiveadventure.network.OpenWaypointNamePayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.KeyMapping;

import org.lwjgl.glfw.GLFW;

public class ProgressiveAdventureClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(ProgressiveAdventure.id("main"));

	@Override
	public void onInitializeClient() {
		KeyMapping openScreen = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.progressive_adventure.open_screen", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY));

		ClientPlayNetworking.registerGlobalReceiver(OpenWaypointNamePayload.TYPE, (payload, context) ->
				context.client().gui.setScreen(new WaypointNameScreen(payload.pos(), payload.shardCost(), payload.currentName())));
		ClientPlayNetworking.registerGlobalReceiver(OpenWaypointListPayload.TYPE, (payload, context) ->
				context.client().gui.setScreen(new WaypointListScreen(payload)));
		BestiaryClient.init();
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> AdventureScreen.onDisconnect());

		// Before vanilla's key handling, which drops attack clicks while blocking
		ClientTickEvents.START_CLIENT_TICK.register(ShieldBash::tick);
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			BlockSense.tick(minecraft);
			AirMoves.tick(minecraft);
			while (openScreen.consumeClick()) {
				if (minecraft.player != null && minecraft.gui.screen() == null) {
					minecraft.gui.setScreen(new AdventureScreen());
				}
			}
		});
	}
}
