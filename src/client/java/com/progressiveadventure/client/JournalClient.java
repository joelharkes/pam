package com.progressiveadventure.client;

import com.progressiveadventure.client.screen.JournalScreen;
import com.progressiveadventure.journal.ExplorersJournalItem;
import com.progressiveadventure.network.JournalMapPayload;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;

/** The part of the shared journal map this client has seen, per connection. Filled by the server on request. */
public final class JournalClient {
	private static final Long2ObjectMap<byte[]> CHUNKS = new Long2ObjectOpenHashMap<>();
	/** Goes up with every update, so the screen knows to redraw its texture. */
	private static int changes;

	private JournalClient() {
	}

	public static void init() {
		ExplorersJournalItem.openScreen = () -> Minecraft.getInstance().gui.setScreen(new JournalScreen());
		ClientPlayNetworking.registerGlobalReceiver(JournalMapPayload.TYPE, (payload, context) -> {
			for (JournalMapPayload.Chunk chunk : payload.chunks()) {
				CHUNKS.put(chunk.pos(), chunk.colors());
			}
			changes++;
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			CHUNKS.clear();
			changes++;
		});
	}

	/** Packed map colours of a drawn chunk, or null. */
	public static byte[] chunk(long pos) {
		return CHUNKS.get(pos);
	}

	public static int changes() {
		return changes;
	}
}
