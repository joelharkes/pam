package com.progressiveadventure.journal;

import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.PlayerProgress;
import com.progressiveadventure.milestone.Milestones;
import com.progressiveadventure.network.JournalMapPayload;
import com.progressiveadventure.network.JournalMarkersPayload;
import com.progressiveadventure.perk.Perks;
import com.progressiveadventure.waypoint.Waypoint;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Server side of the Explorer's Journal. Every second, each player in the Overworld who carries a journal draws the
 * loaded chunks around them into the one shared map: a new chunk costs ink from the first journal in their inventory,
 * redrawing a drawn one is free (one per pass, plus the chunk they stand in, so builds show up). Colours follow
 * vanilla's map (MapItem.update) at 1 pixel per block.
 */
public final class Journals {
	/** Largest area (in chunks per side) one request may ask for; the journal screen at its widest zoom stays below it. */
	public static final int MAX_REQUEST_CHUNKS = 96;
	/** Chunks per map packet, so a big first look is split into packets well below the size limit. */
	private static final int CHUNKS_PER_PACKET = 512;
	private static final int DRAW_INTERVAL_TICKS = 20;
	private static final int MIN_REQUEST_INTERVAL_TICKS = 5;

	/** Per player: version of each chunk last sent, so a request only gets what changed. */
	private static final Map<UUID, Long2IntOpenHashMap> SENT = new HashMap<>();
	private static final Map<UUID, Integer> LAST_REQUEST = new HashMap<>();
	/** Players who were told their journal is out of ink; told again only after it drew something. */
	private static final Set<UUID> WARNED_EMPTY = new HashSet<>();
	/** Per player: ink owed below one whole ink (Steady Hand makes a chunk cost less than 1). */
	private static final Map<UUID, Double> INK_OWED = new HashMap<>();
	/** Per player: hash of the markers last sent, so unchanged markers aren't sent every second. */
	private static final Map<UUID, Integer> SENT_MARKERS = new HashMap<>();
	/** Draw radius → its chunk offsets, nearest first. */
	private static final Map<Integer, int[][]> OFFSETS = new HashMap<>();
	private static int pass;

	private Journals() {
	}

	public static void onServerStopped() {
		SENT.clear();
		LAST_REQUEST.clear();
		WARNED_EMPTY.clear();
		INK_OWED.clear();
		SENT_MARKERS.clear();
	}

	public static void onDisconnect(ServerPlayer player) {
		SENT.remove(player.getUUID());
		LAST_REQUEST.remove(player.getUUID());
		WARNED_EMPTY.remove(player.getUUID());
		INK_OWED.remove(player.getUUID());
		SENT_MARKERS.remove(player.getUUID());
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
		int[][] offsets = offsets(config.journalDrawRadius + (Perks.has(player, Perks.WIDE_VIEW) ? config.wideViewRadiusBonus : 0));
		boolean free = player.hasInfiniteMaterials();
		int steadyHand = Perks.tier(player, Perks.STEADY_HAND_1, Perks.STEADY_HAND_2, Perks.STEADY_HAND_3);
		double costPerChunk = config.journalInkPerChunk * (steadyHand > 0 ? 1 - PamConfig.listValue(config.steadyHandInkReductions, steadyHand - 1) : 1);
		JournalInk ink = ExplorersJournalItem.ink(journal);
		int inkLeft = ink.ink();
		double owed = INK_OWED.getOrDefault(player.getUUID(), 0.0);
		int drawn = 0;
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
			if (!free) {
				// Ink goes in whole units: a chunk that costs 0.75 takes 1 and leaves 0.25 credit (negative owed) for the next
				int pay = (int) Math.ceil(owed + costPerChunk - 1e-9);
				if (pay > inkLeft) {
					ranOut = true;
					break;
				}
				owed = owed + costPerChunk - pay;
				inkLeft -= pay;
			}
			map.put(pos, drawChunk(level, chunk));
			drawn++;
			WARNED_EMPTY.remove(player.getUUID());
		}
		INK_OWED.put(player.getUUID(), owed);
		if (inkLeft != ink.ink()) {
			journal.set(JournalContent.INK, new JournalInk(inkLeft, ink.max()));
		}
		if (drawn > 0) {
			PlayerProgress progress = PamAttachments.progress(player).withChunksDrawn(drawn);
			PamAttachments.setProgress(player, progress);
			Milestones.checkThresholds(player, Milestones.CHUNKS_DRAWN_THRESHOLDS, progress.chunksDrawn());
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

	/** A client looks at this area of its journal: send the drawn chunks there it doesn't have in their latest version, and the markers. */
	public static void onMapRequest(ServerPlayer player, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
		MinecraftServer server = player.level().getServer();
		Integer last = LAST_REQUEST.get(player.getUUID());
		if (last != null && server.getTickCount() - last < MIN_REQUEST_INTERVAL_TICKS) {
			return;
		}
		LAST_REQUEST.put(player.getUUID(), server.getTickCount());
		// Only with a journal; the whole shared map may be looked at, but not all at once
		if (findJournal(player).isEmpty() || maxChunkX < minChunkX || maxChunkZ < minChunkZ
				|| maxChunkX - minChunkX >= MAX_REQUEST_CHUNKS || maxChunkZ - minChunkZ >= MAX_REQUEST_CHUNKS) {
			return;
		}
		JournalMap map = map(server);
		Long2IntOpenHashMap sent = SENT.computeIfAbsent(player.getUUID(), id -> new Long2IntOpenHashMap());
		List<JournalMapPayload.Chunk> chunks = new ArrayList<>();
		for (int x = minChunkX; x <= maxChunkX; x++) {
			for (int z = minChunkZ; z <= maxChunkZ; z++) {
				long pos = ChunkPos.pack(x, z);
				int version = map.version(pos);
				if (version != 0 && sent.get(pos) != version) {
					sent.put(pos, version);
					chunks.add(new JournalMapPayload.Chunk(pos, Arrays.copyOf(map.get(pos), JournalMap.CHUNK_PIXELS)));
					if (chunks.size() == CHUNKS_PER_PACKET) {
						ServerPlayNetworking.send(player, new JournalMapPayload(chunks));
						chunks = new ArrayList<>();
					}
				}
			}
		}
		if (!chunks.isEmpty()) {
			ServerPlayNetworking.send(player, new JournalMapPayload(chunks));
		}
		sendMarkers(player);
	}

	private static void sendMarkers(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		ServerLevel overworld = server.overworld();
		boolean op = player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
		List<JournalMarkersPayload.Marker> markers = new ArrayList<>();
		for (JournalMarker marker : overworld.getAttachedOrElse(PamAttachments.JOURNAL_MARKERS, List.of())) {
			markers.add(new JournalMarkersPayload.Marker(marker.id(), JournalMarkersPayload.KIND_MARKER, marker.icon(), marker.name(),
					marker.x(), marker.z(), marker.ownerName(), op || marker.owner().equals(player.getUUID())));
		}
		for (Waypoint waypoint : overworld.getAttachedOrElse(PamAttachments.WAYPOINTS, List.<Waypoint>of())) {
			if (waypoint.dimension().equals(Level.OVERWORLD)) {
				markers.add(new JournalMarkersPayload.Marker(waypoint.id(), JournalMarkersPayload.KIND_WAYPOINT, "", waypoint.name(),
						waypoint.pos().getX(), waypoint.pos().getZ(), waypoint.ownerName(), false));
			}
		}
		ServerPlayer.RespawnConfig respawn = player.getRespawnConfig();
		if (respawn != null && respawn.respawnData().dimension().equals(Level.OVERWORLD)) {
			BlockPos bed = respawn.respawnData().pos();
			markers.add(new JournalMarkersPayload.Marker(new UUID(0, 0), JournalMarkersPayload.KIND_BED, "", "", bed.getX(), bed.getZ(), "", false));
		}
		if (Perks.has(player, Perks.TREASURE_MARKS)) {
			addTreasureMarks(player, markers);
		}
		// Ids of automatic markers are fixed, so the hash only changes when something really changed
		int hash = markers.hashCode();
		Integer lastHash = SENT_MARKERS.put(player.getUUID(), hash);
		if (lastHash == null || lastHash != hash) {
			ServerPlayNetworking.send(player, new JournalMarkersPayload(markers));
		}
	}

	/** Targets of treasure and explorer maps in the inventory (Treasure Marks perk). */
	private static void addTreasureMarks(ServerPlayer player, List<JournalMarkersPayload.Marker> markers) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.has(DataComponents.MAP_ID)) {
				continue;
			}
			MapItemSavedData data = MapItem.getSavedData(stack, player.level());
			if (data == null || !data.dimension.equals(Level.OVERWORLD)) {
				continue;
			}
			int scale = 1 << data.scale;
			for (MapDecoration decoration : data.getDecorations()) {
				if (!decoration.type().value().explorationMapElement() && !decoration.type().value().equals(MapDecorationTypes.RED_X.value())) {
					continue;
				}
				// Decoration x/y are 2 units per map pixel, centred on the map
				int x = data.centerX + decoration.x() * scale / 2;
				int z = data.centerZ + decoration.y() * scale / 2;
				String icon = decoration.type().value().assetId().getPath();
				markers.add(new JournalMarkersPayload.Marker(new UUID(1, ((long) x << 32) ^ (z & 0xFFFFFFFFL)), JournalMarkersPayload.KIND_TREASURE,
						icon, stack.getHoverName().getString(), x, z, "", false));
			}
		}
	}

	/** Adds or removes a marker on the shared map (from the journal screen). */
	public static void onMarkerEdit(ServerPlayer player, boolean remove, UUID id, int x, int z, String name, String icon) {
		if (findJournal(player).isEmpty()) {
			return;
		}
		ServerLevel overworld = player.level().getServer().overworld();
		List<JournalMarker> markers = overworld.getAttachedOrElse(PamAttachments.JOURNAL_MARKERS, List.of());
		if (remove) {
			boolean op = player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
			List<JournalMarker> kept = markers.stream().filter(m -> !(m.id().equals(id) && (op || m.owner().equals(player.getUUID())))).toList();
			if (kept.size() != markers.size()) {
				overworld.setAttached(PamAttachments.JOURNAL_MARKERS, kept);
				sendMarkers(player);
			}
			return;
		}
		String cleanName = JournalMarker.cleanName(name);
		long owned = markers.stream().filter(m -> m.owner().equals(player.getUUID())).count();
		// Only on drawn land, so markers stay where the map is
		if (cleanName.isEmpty() || !JournalMarker.ICONS.contains(icon) || !map(player.level().getServer()).isDrawn(ChunkPos.pack(x >> 4, z >> 4))) {
			return;
		}
		if (owned >= PamConfig.get().journalMaxMarkersPerPlayer) {
			player.sendOverlayMessage(Component.translatable("message.progressive_adventure.journal.too_many_markers",
					PamConfig.get().journalMaxMarkersPerPlayer).withStyle(ChatFormatting.YELLOW));
			return;
		}
		JournalMarker marker = new JournalMarker(UUID.randomUUID(), cleanName, x, z, icon, player.getUUID(), player.getName().getString());
		overworld.setAttached(PamAttachments.JOURNAL_MARKERS, Stream.concat(markers.stream(), Stream.of(marker)).toList());
		Milestones.grant(player, Milestones.FIRST_MARKER);
		sendMarkers(player);
	}
}
