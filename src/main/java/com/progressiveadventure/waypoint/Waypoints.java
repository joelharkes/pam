package com.progressiveadventure.waypoint;

import com.progressiveadventure.ProgressiveAdventure;
import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.milestone.Milestones;
import com.progressiveadventure.network.OpenWaypointListPayload;
import com.progressiveadventure.network.OpenWaypointNamePayload;
import com.progressiveadventure.perk.Perks;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Waypoint Stones: turn one on with shards (the Nth stone you own costs more), then travel between any two stones
 * for XP points. The list of waypoints is shared by all players and stored on the Overworld.
 */
public final class Waypoints {
	private static final String LABEL_TAG = ProgressiveAdventure.MOD_ID + ".waypoint_label";
	private static final double LABEL_HEIGHT = 1.6;
	// How far from the stone's center you may move during the charge-up
	private static final double CHARGE_RANGE = 3.0;
	private static final Map<UUID, Pending> PENDING = new HashMap<>();

	private record Pending(ResourceKey<Level> fromDimension, BlockPos from, UUID target, int cost, int ticksLeft) {
		Pending tick() {
			return new Pending(fromDimension, from, target, cost, ticksLeft - 1);
		}
	}

	private Waypoints() {
	}

	/** Forgets pending trips when the server stops (singleplayer: the next world has the same player UUID). */
	public static void onServerStopped() {
		PENDING.clear();
	}

	// --- Stored list ---

	public static List<Waypoint> all(MinecraftServer server) {
		return server.overworld().getAttachedOrElse(PamAttachments.WAYPOINTS, List.of());
	}

	private static void save(MinecraftServer server, List<Waypoint> waypoints) {
		server.overworld().setAttached(PamAttachments.WAYPOINTS, List.copyOf(waypoints));
	}

	private static void put(MinecraftServer server, Waypoint waypoint) {
		List<Waypoint> list = new ArrayList<>(all(server));
		list.removeIf(w -> w.id().equals(waypoint.id()));
		list.add(waypoint);
		save(server, list);
	}

	private static void delete(MinecraftServer server, Waypoint waypoint) {
		List<Waypoint> list = new ArrayList<>(all(server));
		list.removeIf(w -> w.id().equals(waypoint.id()));
		save(server, list);
	}

	public static Optional<Waypoint> at(MinecraftServer server, ResourceKey<Level> dimension, BlockPos pos) {
		return all(server).stream().filter(w -> w.isAt(dimension, pos)).findFirst();
	}

	public static Optional<Waypoint> byId(MinecraftServer server, UUID id) {
		return all(server).stream().filter(w -> w.id().equals(id)).findFirst();
	}

	/** For commands: case doesn't matter. */
	public static Optional<Waypoint> byName(MinecraftServer server, String name) {
		return all(server).stream().filter(w -> w.name().equalsIgnoreCase(name.strip())).findFirst();
	}

	public static int ownedCount(MinecraftServer server, UUID owner) {
		return (int) all(server).stream().filter(w -> w.owner().equals(owner)).count();
	}

	/** Shards needed to turn on one more stone: grows with the stones you already own. */
	public static int shardCost(ServerPlayer player) {
		PamConfig config = PamConfig.get();
		return config.waypointShardCostBase + config.waypointShardCostStep * ownedCount(player.level().getServer(), player.getUUID());
	}

	// --- Using and breaking stones ---

	public static void onUse(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
		if (PENDING.containsKey(player.getUUID())) {
			return;
		}
		MinecraftServer server = level.getServer();
		Optional<Waypoint> waypoint = state.getValue(WaypointStoneBlock.ACTIVE) ? at(server, level.dimension(), pos) : Optional.empty();
		if (waypoint.isEmpty()) {
			if (state.getValue(WaypointStoneBlock.ACTIVE)) {
				// Turned on without a stored waypoint (e.g. placed by a command): treat it as a new stone
				level.setBlockAndUpdate(pos, state.setValue(WaypointStoneBlock.ACTIVE, false));
			}
			int cost = shardCost(player);
			if (!player.hasInfiniteMaterials() && countShards(player) < cost) {
				player.sendOverlayMessage(Component.translatable("message.progressive_adventure.waypoint.needs_shards", cost)
						.withStyle(ChatFormatting.YELLOW));
				return;
			}
			ServerPlayNetworking.send(player, new OpenWaypointNamePayload(pos, cost, ""));
		} else if (player.isShiftKeyDown() && canManage(player, waypoint.get())) {
			ServerPlayNetworking.send(player, new OpenWaypointNamePayload(pos, 0, waypoint.get().name()));
		} else {
			sendList(player, waypoint.get());
		}
	}

	/** Name chosen in the client screen: turns the stone on, or renames it. */
	public static void onName(ServerPlayer player, BlockPos pos, String rawName) {
		ServerLevel level = player.level();
		String name = Waypoint.cleanName(rawName);
		BlockState state = level.getBlockState(pos);
		if (name.isEmpty() || !state.is(WaypointContent.WAYPOINT_STONE) || !player.isWithinBlockInteractionRange(pos, 1.0)) {
			return;
		}
		MinecraftServer server = level.getServer();
		Optional<Waypoint> existing = at(server, level.dimension(), pos);
		if (existing.isPresent()) {
			if (canManage(player, existing.get())) {
				put(server, existing.get().withName(name));
				removeLabels(level, pos);
				spawnLabel(level, pos, name);
			}
			return;
		}

		int cost = shardCost(player);
		if (!player.hasInfiniteMaterials()) {
			if (countShards(player) < cost) {
				player.sendOverlayMessage(Component.translatable("message.progressive_adventure.waypoint.needs_shards", cost)
						.withStyle(ChatFormatting.YELLOW));
				return;
			}
			removeShards(player, cost);
		}
		put(server, new Waypoint(UUID.randomUUID(), name, level.dimension(), pos.immutable(), player.getUUID(), player.getPlainTextName(),
				player.hasInfiniteMaterials() ? 0 : cost));
		level.setBlockAndUpdate(pos, state.setValue(WaypointStoneBlock.ACTIVE, true));
		spawnLabel(level, pos, name);
		level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1f, 1.2f);
		player.sendSystemMessage(Component.translatable("message.progressive_adventure.waypoint.activated", name).withStyle(ChatFormatting.AQUA));

		Milestones.grant(player, Milestones.WAYPOINT);
		Milestones.checkThresholds(player, Milestones.WAYPOINT_THRESHOLDS, ownedCount(server, player.getUUID()));
	}

	/** Owners and ops may rename or break a stone; everyone may use it. */
	public static boolean canManage(Player player, Waypoint waypoint) {
		return waypoint.owner().equals(player.getUUID()) || player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	/** Before a player breaks a block: only the owner (or an op) may break a turned-on stone. */
	public static boolean canBreak(Player player, Level level, BlockPos pos, BlockState state) {
		if (!state.is(WaypointContent.WAYPOINT_STONE) || !state.getValue(WaypointStoneBlock.ACTIVE) || !(level instanceof ServerLevel serverLevel)) {
			return true;
		}
		Optional<Waypoint> waypoint = at(serverLevel.getServer(), serverLevel.dimension(), pos);
		if (waypoint.isEmpty() || canManage(player, waypoint.get())) {
			return true;
		}
		player.sendOverlayMessage(Component.translatable("message.progressive_adventure.waypoint.not_yours", waypoint.get().ownerName())
				.withStyle(ChatFormatting.RED));
		return false;
	}

	/** A turned-on stone was removed (broken, replaced by a command): forget it and give the shards back. */
	public static void onStoneRemoved(ServerLevel level, BlockPos pos) {
		at(level.getServer(), level.dimension(), pos).ifPresent(waypoint -> forget(level, waypoint));
	}

	/** For /pam waypoint remove: turns the stone off (it stays as a block) and drops its shards there. */
	public static void remove(MinecraftServer server, Waypoint waypoint) {
		ServerLevel level = server.getLevel(waypoint.dimension());
		if (level == null) {
			delete(server, waypoint);
			return;
		}
		BlockState state = level.getBlockState(waypoint.pos());
		forget(level, waypoint);
		if (state.is(WaypointContent.WAYPOINT_STONE)) {
			level.setBlockAndUpdate(waypoint.pos(), state.setValue(WaypointStoneBlock.ACTIVE, false));
		}
	}

	private static void forget(ServerLevel level, Waypoint waypoint) {
		delete(level.getServer(), waypoint);
		removeLabels(level, waypoint.pos());
		if (waypoint.shards() > 0) {
			Vec3 center = Vec3.atCenterOf(waypoint.pos());
			level.addFreshEntity(new ItemEntity(level, center.x, center.y, center.z, new ItemStack(WaypointContent.WAYPOINT_SHARD, waypoint.shards())));
		}
	}

	private static int countShards(ServerPlayer player) {
		return player.getInventory().clearOrCountMatchingItems(stack -> stack.is(WaypointContent.WAYPOINT_SHARD), 0, player.inventoryMenu.getCraftSlots());
	}

	private static void removeShards(ServerPlayer player, int count) {
		player.getInventory().clearOrCountMatchingItems(stack -> stack.is(WaypointContent.WAYPOINT_SHARD), count, player.inventoryMenu.getCraftSlots());
		player.inventoryMenu.broadcastChanges();
	}

	// --- Floating name ---

	private static void spawnLabel(ServerLevel level, BlockPos pos, String name) {
		Display.TextDisplay label = EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		if (label == null) {
			return;
		}
		label.setPos(pos.getX() + 0.5, pos.getY() + LABEL_HEIGHT, pos.getZ() + 0.5);
		label.setText(Component.literal(name).withStyle(ChatFormatting.AQUA));
		label.setBillboardConstraints(Display.BillboardConstraints.CENTER);
		label.setBrightnessOverride(Brightness.FULL_BRIGHT);
		label.addTag(LABEL_TAG);
		level.addFreshEntity(label);
	}

	private static void removeLabels(ServerLevel level, BlockPos pos) {
		AABB box = new AABB(pos.above()).inflate(0.5, 1, 0.5);
		level.getEntitiesOfClass(Display.TextDisplay.class, box, entity -> entity.entityTags().contains(LABEL_TAG)).forEach(Entity::discard);
	}

	/** Removes labels whose stone is gone (e.g. removed while the label's chunk wasn't loaded). */
	public static void onEntityLoad(Entity entity, ServerLevel level) {
		if (entity instanceof Display.TextDisplay && entity.entityTags().contains(LABEL_TAG)) {
			BlockPos stone = BlockPos.containing(entity.getX(), entity.getY() - LABEL_HEIGHT, entity.getZ());
			if (at(level.getServer(), level.dimension(), stone).isEmpty()) {
				entity.discard();
			}
		}
	}

	// --- Travel ---

	private static void sendList(ServerPlayer player, Waypoint from) {
		MinecraftServer server = player.level().getServer();
		List<OpenWaypointListPayload.Entry> entries = new ArrayList<>();
		for (Waypoint to : all(server)) {
			if (to.id().equals(from.id())) {
				continue;
			}
			boolean sameDimension = to.dimension().equals(from.dimension());
			int distance = sameDimension ? (int) Math.round(horizontalDistance(from.pos(), to.pos(), 1, 1)) : -1;
			entries.add(new OpenWaypointListPayload.Entry(to.id(), to.name(), to.ownerName(), to.dimension().identifier().toString(),
					distance, xpCost(player, from.dimension(), from.pos(), to)));
		}
		entries.sort(Comparator.comparingInt(OpenWaypointListPayload.Entry::cost).thenComparing(OpenWaypointListPayload.Entry::name));
		ServerPlayNetworking.send(player, new OpenWaypointListPayload(from.pos(), from.name(), entries));
	}

	/**
	 * XP points (not levels) for a trip. Distance is measured in Overworld blocks (Nether ×8), so the same trip costs
	 * the same in every dimension. A dimension jump adds a fee on top of the cap; the End has no matching
	 * coordinates, so trips to or from it always cost the cap plus the fee.
	 */
	public static int xpCost(ServerPlayer player, ResourceKey<Level> fromDimension, BlockPos from, Waypoint to) {
		PamConfig config = PamConfig.get();
		MinecraftServer server = player.level().getServer();
		boolean otherDimension = !fromDimension.equals(to.dimension());
		double cost;
		if (otherDimension && (fromDimension.equals(Level.END) || to.dimension().equals(Level.END))) {
			cost = config.teleportXpCap;
		} else {
			double blocks = horizontalDistance(from, to.pos(), scale(server, fromDimension), scale(server, to.dimension()));
			cost = Math.min(config.teleportXpCap, config.teleportXpBase + config.teleportXpPer1000Blocks * blocks / 1000);
		}
		if (otherDimension) {
			cost += config.teleportXpDimensionFee;
		}
		int pathfinder = Perks.tier(player, Perks.PATHFINDER_1, Perks.PATHFINDER_2);
		if (pathfinder > 0) {
			cost *= 1 - PamConfig.listValue(config.pathfinderCostReductions, pathfinder - 1);
		}
		return Math.max(1, (int) Math.round(cost));
	}

	private static double scale(MinecraftServer server, ResourceKey<Level> dimension) {
		ServerLevel level = server.getLevel(dimension);
		return level == null ? 1 : level.dimensionType().coordinateScale();
	}

	private static double horizontalDistance(BlockPos a, BlockPos b, double scaleA, double scaleB) {
		double dx = a.getX() * scaleA - b.getX() * scaleB;
		double dz = a.getZ() * scaleA - b.getZ() * scaleB;
		return Math.sqrt(dx * dx + dz * dz);
	}

	/** All XP points the player has, from their level and progress bar. Works on both sides. */
	public static int totalXp(Player player) {
		int total = 0;
		for (int level = 0; level < player.experienceLevel; level++) {
			total += xpNeededFor(level);
		}
		return total + Math.round(player.experienceProgress * xpNeededFor(player.experienceLevel));
	}

	/** Same as vanilla's Player.getXpNeededForNextLevel, for any level. */
	private static int xpNeededFor(int level) {
		if (level >= 30) {
			return 112 + (level - 30) * 9;
		}
		return level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
	}

	/** Destination picked in the list: checks everything again and starts the charge-up. */
	public static void onTeleportRequest(ServerPlayer player, BlockPos fromPos, UUID targetId) {
		ServerLevel level = player.level();
		MinecraftServer server = level.getServer();
		Optional<Waypoint> from = at(server, level.dimension(), fromPos);
		Optional<Waypoint> target = byId(server, targetId);
		if (from.isEmpty() || target.isEmpty() || from.get().id().equals(targetId) || PENDING.containsKey(player.getUUID())
				|| !player.isWithinBlockInteractionRange(fromPos, 1.0)) {
			return;
		}
		int cost = player.hasInfiniteMaterials() ? 0 : xpCost(player, level.dimension(), fromPos, target.get());
		if (totalXp(player) < cost) {
			player.sendOverlayMessage(Component.translatable("message.progressive_adventure.waypoint.needs_xp", cost).withStyle(ChatFormatting.YELLOW));
			return;
		}
		PENDING.put(player.getUUID(), new Pending(level.dimension(), fromPos.immutable(), targetId, cost, PamConfig.get().teleportChargeTicks));
		level.playSound(null, fromPos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1f, 1.4f);
		player.sendOverlayMessage(Component.translatable("message.progressive_adventure.waypoint.charging", target.get().name())
				.withStyle(ChatFormatting.AQUA));
	}

	/** Getting hurt during the charge-up cancels the trip (no free escape from a fight). */
	public static void onDamaged(ServerPlayer player) {
		if (PENDING.remove(player.getUUID()) != null) {
			player.sendOverlayMessage(Component.translatable("message.progressive_adventure.waypoint.cancelled").withStyle(ChatFormatting.RED));
		}
	}

	public static void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Pending> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			Pending pending = entry.getValue().tick();
			if (player == null || !player.isAlive() || !player.level().dimension().equals(pending.fromDimension())
					|| player.position().distanceTo(Vec3.atBottomCenterOf(pending.from())) > CHARGE_RANGE) {
				it.remove();
				if (player != null) {
					player.sendOverlayMessage(Component.translatable("message.progressive_adventure.waypoint.cancelled").withStyle(ChatFormatting.RED));
				}
				continue;
			}
			if (pending.ticksLeft() % 4 == 0) {
				player.level().sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(), 12, 0.4, 0.8, 0.4, 0.3);
			}
			if (pending.ticksLeft() > 0) {
				entry.setValue(pending);
				continue;
			}
			it.remove();
			byId(server, pending.target()).ifPresentOrElse(target -> arrive(player, target, pending.cost()),
					() -> player.sendOverlayMessage(Component.translatable("message.progressive_adventure.waypoint.gone").withStyle(ChatFormatting.RED)));
		}
	}

	private static void arrive(ServerPlayer player, Waypoint target, int cost) {
		if (totalXp(player) < cost) {
			player.sendOverlayMessage(Component.translatable("message.progressive_adventure.waypoint.needs_xp", cost).withStyle(ChatFormatting.YELLOW));
			return;
		}
		if (!teleport(player, target)) {
			return;
		}
		if (cost > 0) {
			player.giveExperiencePoints(-cost);
		}
		Milestones.grant(player, Milestones.FIRST_TRIP);
	}

	/**
	 * Moves the player (and their following pets) next to the target stone. False, with a message, when the stone
	 * is gone or there's no safe spot; then nothing is paid. Also used by /pam waypoint tp (free).
	 */
	public static boolean teleport(ServerPlayer player, Waypoint target) {
		MinecraftServer server = player.level().getServer();
		ServerLevel level = server.getLevel(target.dimension());
		BlockState state = level == null ? null : level.getBlockState(target.pos()); // loads the chunk
		if (state == null || !state.is(WaypointContent.WAYPOINT_STONE) || !state.getValue(WaypointStoneBlock.ACTIVE)) {
			delete(server, target);
			player.sendOverlayMessage(Component.translatable("message.progressive_adventure.waypoint.gone").withStyle(ChatFormatting.RED));
			return false;
		}
		Optional<Vec3> spot = safeSpot(level, target.pos());
		if (spot.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.progressive_adventure.waypoint.blocked", target.name()).withStyle(ChatFormatting.RED));
			return false;
		}

		double petRange = PamConfig.get().teleportPetRange;
		List<TamableAnimal> pets = player.level().getEntitiesOfClass(TamableAnimal.class, player.getBoundingBox().inflate(petRange),
				pet -> pet.isOwnedBy(player) && !pet.isOrderedToSit() && !pet.isLeashed() && !pet.isPassenger());
		player.stopRiding();
		Vec3 pos = spot.get();
		// Face the stone
		float yRot = (float) Math.toDegrees(Math.atan2(-(target.pos().getX() + 0.5 - pos.x), target.pos().getZ() + 0.5 - pos.z));
		player.teleport(new TeleportTransition(level, pos, Vec3.ZERO, yRot, 0, TeleportTransition.DO_NOTHING));
		for (TamableAnimal pet : pets) {
			pet.teleport(new TeleportTransition(level, pos, Vec3.ZERO, pet.getYRot(), pet.getXRot(), TeleportTransition.DO_NOTHING));
		}
		level.playSound(null, BlockPos.containing(pos), SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 1, pos.z, 30, 0.4, 0.8, 0.4, 0.05);
		return true;
	}

	/** A spot next to the stone where the player fits and stands on something solid; on top of it as a last resort. */
	private static Optional<Vec3> safeSpot(ServerLevel level, BlockPos stone) {
		List<BlockPos> candidates = new ArrayList<>();
		for (int dy : new int[] {0, 1, -1}) {
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				candidates.add(stone.relative(direction).above(dy));
			}
			for (int dx = -1; dx <= 1; dx += 2) {
				for (int dz = -1; dz <= 1; dz += 2) {
					candidates.add(stone.offset(dx, dy, dz));
				}
			}
		}
		candidates.add(stone.above());
		for (int dy : new int[] {0, 1, -1}) {
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
						candidates.add(stone.offset(dx, dy, dz));
					}
				}
			}
		}
		for (BlockPos feet : candidates) {
			if (fits(level, feet) && fits(level, feet.above()) && !level.getFluidState(feet.above()).is(FluidTags.WATER)
					&& level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) {
				return Optional.of(Vec3.atBottomCenterOf(feet));
			}
		}
		return Optional.empty();
	}

	private static boolean fits(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && !level.getFluidState(pos).is(FluidTags.LAVA);
	}
}
