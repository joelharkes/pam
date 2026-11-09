package com.progressiveadventure.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.progressiveadventure.ProgressiveAdventure;
import com.progressiveadventure.client.screen.JournalScreen;
import com.progressiveadventure.journal.ExplorersJournalItem;
import com.progressiveadventure.journal.JournalContent;
import com.progressiveadventure.network.JournalMapPayload;
import com.progressiveadventure.network.JournalMapRequestPayload;
import com.progressiveadventure.network.JournalMarkersPayload;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;

import java.util.Arrays;
import java.util.List;

/**
 * Client side of the Explorer's Journal: the part of the shared map this client has seen (per connection), the markers,
 * asking the server for updates, and the 128×128 texture shown when you hold a journal in your hand.
 */
public final class JournalClient {
	/** The held map shows this many blocks around you, like a vanilla map at its closest zoom. */
	public static final int HELD_BLOCKS = 128;
	private static final Identifier HELD_TEXTURE = ProgressiveAdventure.id("journal_held");
	private static final int REQUEST_INTERVAL_TICKS = 20;
	private static final int MIN_REQUEST_INTERVAL_TICKS = 5;

	private static final Long2ObjectMap<byte[]> CHUNKS = new Long2ObjectOpenHashMap<>();
	private static List<JournalMarkersPayload.Marker> markers = List.of();
	/** Goes up with every map update, so textures know to redraw. */
	private static int changes;

	private static int ticks;
	private static int lastRequestTick = -100;
	private static int[] lastRequestArea = new int[0];

	private static DynamicTexture heldTexture;
	private static int heldX = Integer.MIN_VALUE;
	private static int heldZ = Integer.MIN_VALUE;
	private static int heldChanges = -1;

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
		ClientPlayNetworking.registerGlobalReceiver(JournalMarkersPayload.TYPE, (payload, context) -> markers = List.copyOf(payload.markers()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			CHUNKS.clear();
			markers = List.of();
			changes++;
			lastRequestTick = -100;
			if (heldTexture != null) {
				client.getTextureManager().release(HELD_TEXTURE);
				heldTexture = null;
			}
		});
	}

	/** Every client tick: while a journal is in your hand, keep the area around you up to date. */
	public static void tick(Minecraft minecraft) {
		ticks++;
		LocalPlayer player = minecraft.player;
		if (player != null && minecraft.gui.screen() == null && isHoldingJournal(player) && player.level().dimension() == Level.OVERWORLD) {
			int radius = HELD_BLOCKS / 32 + 1;
			ChunkPos chunk = player.chunkPosition();
			request(chunk.x() - radius, chunk.z() - radius, chunk.x() + radius, chunk.z() + radius);
		}
	}

	/** Asks for this area (in chunks): right away when it's a new area, otherwise once a second. */
	public static void request(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
		int[] area = {minChunkX, minChunkZ, maxChunkX, maxChunkZ};
		int since = ticks - lastRequestTick;
		if (since < MIN_REQUEST_INTERVAL_TICKS || (Arrays.equals(area, lastRequestArea) && since < REQUEST_INTERVAL_TICKS)) {
			return;
		}
		lastRequestTick = ticks;
		lastRequestArea = area;
		ClientPlayNetworking.send(new JournalMapRequestPayload(minChunkX, minChunkZ, maxChunkX, maxChunkZ));
	}

	/** Packed map colours of a drawn chunk, or null. */
	public static byte[] chunk(long pos) {
		return CHUNKS.get(pos);
	}

	public static int changes() {
		return changes;
	}

	public static List<JournalMarkersPayload.Marker> markers() {
		return markers;
	}

	/**
	 * Paints the map into an image: texel (x, y) shows the block at (minX + x * step, minZ + y * step).
	 * Undrawn land stays transparent, so the paper shows through.
	 */
	public static void paint(NativeImage image, int width, int height, int minX, int minZ, int step) {
		for (int y = 0; y < height; y++) {
			int worldZ = minZ + y * step;
			for (int x = 0; x < width; x++) {
				int worldX = minX + x * step;
				byte[] chunk = CHUNKS.get(ChunkPos.pack(worldX >> 4, worldZ >> 4));
				image.setPixel(x, y, chunk == null ? 0 : MapColor.getColorFromPackedId(chunk[(worldZ & 15) * 16 + (worldX & 15)]));
			}
		}
	}

	/** The texture of the map in your hand, centred on you, redrawn when you move a block or the map changes. */
	public static Identifier heldTexture(Player player) {
		Minecraft minecraft = Minecraft.getInstance();
		if (heldTexture == null) {
			heldTexture = new DynamicTexture(() -> "Explorer's Journal (held)", HELD_BLOCKS, HELD_BLOCKS, true);
			minecraft.getTextureManager().register(HELD_TEXTURE, heldTexture);
			heldChanges = -1;
		}
		int x = Mth.floor(player.getX());
		int z = Mth.floor(player.getZ());
		if (x != heldX || z != heldZ || changes != heldChanges) {
			heldX = x;
			heldZ = z;
			heldChanges = changes;
			paint(heldTexture.getPixels(), HELD_BLOCKS, HELD_BLOCKS, x - HELD_BLOCKS / 2, z - HELD_BLOCKS / 2, 1);
			heldTexture.upload();
		}
		return HELD_TEXTURE;
	}

	public static boolean isHoldingJournal(Player player) {
		return player.getMainHandItem().is(JournalContent.EXPLORERS_JOURNAL) || player.getOffhandItem().is(JournalContent.EXPLORERS_JOURNAL);
	}

	/** With a compass in your inventory, the map shows which way you're looking. */
	public static boolean hasCompass(Player player) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).is(Items.COMPASS)) {
				return true;
			}
		}
		return false;
	}

	/** The journal that draws: the first one in the inventory, like on the server. */
	public static ItemStack firstJournal(Player player) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(JournalContent.EXPLORERS_JOURNAL)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	/** A vanilla map decoration sprite, e.g. "player" or "red_banner". */
	public static TextureAtlasSprite sprite(String name) {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.MAP_DECORATIONS)
				.getSprite(Identifier.withDefaultNamespace(name));
	}

	/** Sprite for a marker from the server. */
	public static String spriteOf(JournalMarkersPayload.Marker marker) {
		return switch (marker.kind()) {
			case JournalMarkersPayload.KIND_WAYPOINT -> "blue_marker";
			case JournalMarkersPayload.KIND_BED -> "red_marker";
			default -> marker.icon();
		};
	}

	/** Vanilla's 16-step map rotation for a yaw, as its maps compute it for players. */
	public static byte mapRotation(float yaw) {
		return (byte) ((yaw < 0 ? yaw - 8 : yaw + 8) * 16.0 / 360.0);
	}
}
