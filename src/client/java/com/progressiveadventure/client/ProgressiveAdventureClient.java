package com.progressiveadventure.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.progressiveadventure.ProgressiveAdventure;
import com.progressiveadventure.client.screen.AdventureScreen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

import net.minecraft.client.KeyMapping;

import org.lwjgl.glfw.GLFW;

public class ProgressiveAdventureClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(ProgressiveAdventure.id("main"));

	@Override
	public void onInitializeClient() {
		KeyMapping openScreen = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.progressive_adventure.open_screen", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			while (openScreen.consumeClick()) {
				if (minecraft.player != null && minecraft.gui.screen() == null) {
					minecraft.gui.setScreen(new AdventureScreen());
				}
			}
		});
	}
}
