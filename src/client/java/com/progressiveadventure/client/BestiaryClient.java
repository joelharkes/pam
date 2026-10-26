package com.progressiveadventure.client;

import com.progressiveadventure.bestiary.Bestiary;
import com.progressiveadventure.network.BestiaryDiscoveredPayload;
import com.progressiveadventure.network.BestiaryInfoPayload;
import com.progressiveadventure.network.BestiaryInfoRequestPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Bestiary info from the server (asked once per mob type per connection) and the discovery toast. */
public final class BestiaryClient {
	private static final Map<String, BestiaryInfoPayload> INFO = new HashMap<>();
	private static final Set<String> REQUESTED = new HashSet<>();

	private BestiaryClient() {
	}

	static void init() {
		ClientPlayNetworking.registerGlobalReceiver(BestiaryInfoPayload.TYPE, (payload, context) -> INFO.put(payload.entityType(), payload));
		ClientPlayNetworking.registerGlobalReceiver(BestiaryDiscoveredPayload.TYPE, (payload, context) -> {
			Identifier id = Identifier.tryParse(payload.entityType());
			if (id != null) {
				BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(type -> context.client().gui.toastManager().addToast(new BestiaryToast(type)));
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			INFO.clear();
			REQUESTED.clear();
		});
	}

	/** The server's info for this mob, or null while it's on its way (the first call asks for it). */
	public static BestiaryInfoPayload info(EntityType<?> type) {
		String key = Bestiary.key(type);
		if (REQUESTED.add(key)) {
			ClientPlayNetworking.send(new BestiaryInfoRequestPayload(key));
		}
		return INFO.get(key);
	}
}
