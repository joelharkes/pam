package com.progressiveadventure.client;

import com.progressiveadventure.network.AchievementListPayload;
import com.progressiveadventure.network.AchievementListRequestPayload;
import com.progressiveadventure.network.BuyTipPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** The server's achievement list, asked for each time the Achievements tab opens. */
public final class AchievementsClient {
	private static AchievementListPayload list;

	private AchievementsClient() {
	}

	static void init() {
		ClientPlayNetworking.registerGlobalReceiver(AchievementListPayload.TYPE, (payload, context) -> list = payload);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> list = null);
	}

	public static void request() {
		ClientPlayNetworking.send(new AchievementListRequestPayload());
	}

	public static void buyTip(String advancement, int tip) {
		ClientPlayNetworking.send(new BuyTipPayload(advancement, tip));
	}

	/** The last list from the server, or null before the first one arrives. */
	public static AchievementListPayload list() {
		return list;
	}
}
