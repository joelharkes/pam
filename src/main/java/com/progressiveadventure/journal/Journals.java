package com.progressiveadventure.journal;

import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.network.JournalMapPayload;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server side of the Explorer's Journal. Every second, each player in the Overworld who carries a journal draws the
 * loaded chunks around them into the one shared map: a new chunk costs ink from the first journal in their inventory,
 * redrawing a drawn one is free (one per pass, plus the chunk they stand in, so builds show up). Colours follow
 * vanilla's map (MapItem.update) at 1 pixel per block.
 */
public final class Journals {
	/** Chunks around the requested centre a client gets; must cover what the journal screen shows (128 blocks). */
	public static final int VIEW_RADIUS_CHUNKS = 5;
	private static final int DRAW_INTERVAL_TICKS = 20;
	private static final int MIN_REQUEST_INTERVAL_TICKS = 5;

	/** Per player: version of each chunk last sent, so a request only gets what changed. */
	private static final Map<UUID, Long2IntOpenHashMap> SENT = new HashMap<>();
	private static final Map<UUID, Integer> LAST_REQUEST = new HashMap<>();
	/** Players who were told their journal is out of ink; told again only after it drew something. */
	private static final Set<UUID> WARNED_EMPTY = new HashSet<>();
	/** Draw radius → its chunk offsets, nearest first. */
	private static final Map<Integer, int[][]> OFFSETS = new HashMap<>();
	private static int pass;

	private Journals() {
	}

	public static void onServerStopped() {
		SENT.clear();
		LAST_REQUEST.clear();
		WARNED_EMPTY.clear();
	}

	public static void onDisconnect(ServerPlayer player) {
		SENT.remove(player.getUUID());
		LAST_REQUEST.remove(player.getUUID());
		WARNED_EMPTY.remove(player.getUUID());
	}

	public static JournalMap map(MinecraftServer server) {
		return server.overworld().getAttachedOrCreate(PamAttachments.JOURNAL_MAP);
	}

	/** The first journal in the inventory (only that one draws and pays), or empty. */
	public static ItemStack findJournal(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(JournalContent.EXPLORERS_JOURNAL)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % DRAW_INTERVAL_TICKS != 0) {
			return;
		}
		pass++;
		ServerLevel overworld = server.overworld();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.level() == overworld && !player.isSpectator()) {
				draw(player, overworld);
			}
		}
	}

	private static void draw(ServerPlayer player, ServerLevel level) {
		ItemStack journal = findJournal(player);
		if (journal.isEmpty()) {
			return;
		}
		PamConfig config = PamConfig.get();
		JournalMap map = map(level.getServer());
		ChunkPos center = player.chunkPosition();
		int[][] offsets = offsets(config.journalDrawRadius);
		boolean free = player.hasInfiniteMaterials();
		JournalInk ink = ExplorersJournalItem.ink(journal);
		int inkLeft = ink.ink();
		boolean ranOut = false;

		for (int[] offset : offsets) {
			int chunkX = center.x() + offset[0];
			int chunkZ = center.z() + offset[1];
			long pos = ChunkPos.pack(chunkX, chunkZ);
			if (map.isDrawn(pos)) {
				continue;
			}
			LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
			if (chunk == null) {
				continue;
			}
			if (!free && inkLeft < config.journalInkPerChunk) {
				ranOut = true;
				break;
			}
			map.put(pos, drawChunk(level, chunk));
			if (!free) {
				inkLeft -= config.journalInkPerChunk;
			}
			WARNED_EMPTY.remove(player.getUUID());
		}
		if (inkLeft != ink.ink()) {
			journal.set(JournalContent.INK, new JournalInk(inkLeft, ink.max()));
		}
		if (ranOut && WARNED_EMPTY.add(player.getUUID())) {
			player.sendOverlayMessage(Component.translatable("message.progressive_adventure.journal.out_of_ink").withStyle(ChatFormatting.YELLOW));
		}

		// Free redraws: where you stand, and one other drawn chunk in reach per pass
		redraw(level, map, center.x(), center.z());
		int[] other = offsets[pass % offsets.length];
		redraw(level, map, center.x() + other[0], center.z() + other[1]);
	}

	private static void redraw(ServerLevel level, JournalMap map, int chunkX, int chunkZ) {
		long pos = ChunkPos.pack(chunkX, chunkZ);
		LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
		if (chunk != null && map.isDrawn(pos)) {
			map.put(pos, drawChunk(level, chunk));
		}
	}

	/** Chunk offsets within the radius (a square), nearest first, so a nearly empty journal draws where you are. */
	private static int[][] offsets(int radius) {
		return OFFSETS.computeIfAbsent(radius, r -> {
			List<int[]> list = new ArrayList<>();
			for (int dx = -r; dx <= r; dx++) {
				for (int dz = -r; dz <= r; dz++) {
					list.add(new int[] {dx, dz});
				}
			}
			list.sort(Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
			return list.toArray(new int[0][]);
		});
	}

	/** 16×16 packed map colours of a loaded chunk, the way vanilla's map draws them at scale 1:1. */
	static byte[] drawChunk(ServerLevel level, LevelChunk chunk) {
		byte[] colors = new byte[JournalMap.CHUNK_PIXELS];
		int minX = chunk.getPos().getMinBlockX();
		int minZ = chunk.getPos().getMinBlockZ();
		LevelChunk north = level.getChunkSource().getChunkNow(chunk.getPos().x(), chunk.getPos().z() - 1);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = 0; x < 16; x++) {
			int worldX = minX + x;
			// Shading compares each block with the one north of it; without the north chunk, the first row is flat
			double previousHeight = north != null ? surface(level, north, worldX, minZ - 1, pos).height : -1;
			for (int z = 0; z < 16; z++) {
				int worldZ = minZ + z;
				Column column = surface(level, chunk, worldX, worldZ, pos);
				if (previousHeight < 0) {
					previousHeight = column.height;
				}
				MapColor.Brightness brightness;
				int checker = (worldX + worldZ) & 1;
				if (column.color == MapColor.WATER) {
					double diff = column.waterDepth * 0.1 + checker * 0.2;
					brightness = diff < 0.5 ? MapColor.Brightness.HIGH : diff > 0.9 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
				} else {
					double diff = (column.height - previousHeight) * 4.0 / 5.0 + (checker - 0.5) * 0.4;
					brightness = diff > 0.6 ? MapColor.Brightness.HIGH : diff < -0.6 ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
				}
				previousHeight = column.height;
				colors[z * 16 + x] = column.color.getPackedId(brightness);
			}
		}
		return colors;
	}

	private record Column(MapColor color, int height, int waterDepth) {
	}

	/** The top block with a map colour (vanilla's map rules: fluids show as water/lava unless a solid top covers them). */
	private static Column surface(ServerLevel level, LevelChunk chunk, int x, int z, BlockPos.MutableBlockPos pos) {
		int minY = level.getMinY();
		int y = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1;
		if (y <= minY) {
			return new Column(Blocks.BEDROCK.defaultBlockState().getMapColor(level, pos.set(x, minY, z)), minY, 0);
		}
		BlockState state;
		do {
			pos.set(x, --y, z);
			state = chunk.getBlockState(pos);
		} while (state.getMapColor(level, pos) == MapColor.NONE && y > minY);

		int waterDepth = 0;
		if (y > minY && !state.getFluidState().isEmpty()) {
			int belowY = y - 1;
			BlockState below;
			do {
				below = chunk.getBlockState(new BlockPos(x, belowY--, z));
				waterDepth++;
			} while (belowY > minY && !below.getFluidState().isEmpty());
			if (!state.isFaceSturdy(level, pos, Direction.UP)) {
				state = state.getFluidState().createLegacyBlock();
			}
		}
		return new Column(state.getMapColor(level, pos), y, waterDepth);
	}

	/** A client looks at its journal: send the drawn chunks around that it doesn't have in their latest version. */
	public static void onMapRequest(ServerPlayer player, int chunkX, int chunkZ) {
		MinecraftServer server = player.level().getServer();
		Integer last = LAST_REQUEST.get(player.getUUID());
		if (last != null && server.getTickCount() - last < MIN_REQUEST_INTERVAL_TICKS) {
			return;
		}
		LAST_REQUEST.put(player.getUUID(), server.getTickCount());
		// Only with a journal, and only near where you are (no peeking at the whole map from elsewhere)
		ChunkPos here = player.chunkPosition();
		if (findJournal(player).isEmpty() || player.level() != server.overworld()
				|| Math.abs(chunkX - here.x()) > VIEW_RADIUS_CHUNKS || Math.abs(chunkZ - here.z()) > VIEW_RADIUS_CHUNKS) {
			return;
		}
		JournalMap map = map(server);
		Long2IntOpenHashMap sent = SENT.computeIfAbsent(player.getUUID(), id -> new Long2IntOpenHashMap());
		List<JournalMapPayload.Chunk> chunks = new ArrayList<>();
		for (int dx = -VIEW_RADIUS_CHUNKS; dx <= VIEW_RADIUS_CHUNKS; dx++) {
			for (int dz = -VIEW_RADIUS_CHUNKS; dz <= VIEW_RADIUS_CHUNKS; dz++) {
				long pos = ChunkPos.pack(chunkX + dx, chunkZ + dz);
				int version = map.version(pos);
				if (version != 0 && sent.get(pos) != version) {
					sent.put(pos, version);
					chunks.add(new JournalMapPayload.Chunk(pos, Arrays.copyOf(map.get(pos), JournalMap.CHUNK_PIXELS)));
				}
			}
		}
		if (!chunks.isEmpty()) {
			ServerPlayNetworking.send(player, new JournalMapPayload(chunks));
		}
	}
}
