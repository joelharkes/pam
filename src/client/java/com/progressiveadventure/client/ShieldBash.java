package com.progressiveadventure.client;

import com.progressiveadventure.network.ShieldBashPayload;
import com.progressiveadventure.perk.Perks;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Shield Bash: vanilla throws away attack clicks while you use an item, so this runs at the start of the client tick
 * (before vanilla's key handling) and turns an attack click while blocking into a bash.
 */
final class ShieldBash {
	private ShieldBash() {
	}

	static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.gui.screen() != null || !player.isBlocking() || !Perks.has(player, Perks.SHIELD_BASH)) {
			return;
		}
		if (minecraft.options.keyAttack.consumeClick()) {
			player.swing(player.getUsedItemHand());
			ClientPlayNetworking.send(new ShieldBashPayload());
		}
	}
}
