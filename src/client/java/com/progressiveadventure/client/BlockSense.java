package com.progressiveadventure.client;

import com.progressiveadventure.perk.Perks;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Sense perks: while holding the right tool, a short particle trail periodically points at the nearest matching block.
 * Ore Sense (pickaxe, any ore, coloured by ore type, bigger when closer), Treasure Sense (pickaxe, diamonds and
 * ancient debris further away, white sparkles and a quiet chime once per block when close) and Relic Sense (brush or
 * shovel, suspicious sand and gravel). Purely client-side; the client already knows the blocks around it.
 */
public final class BlockSense {
	private static final int ORE_RADIUS = 12;
	private static final int ORE_INTERVAL_TICKS = 30;
	private static final int TREASURE_RADIUS = 24;
	private static final int TREASURE_INTERVAL_TICKS = 50;
	private static final int TREASURE_CHIME_DISTANCE = 6;
	private static final int RELIC_RADIUS = 16;
	private static final int RELIC_INTERVAL_TICKS = 40;
	private static final int RELIC_COLOR = 0xE3C08C;
	private static final int TRAIL_PARTICLES = 6;
	private static final double TRAIL_SPACING = 0.35;

	private record OreColor(TagKey<Block> tag, int color) {
	}

	private static final List<OreColor> COLORS = List.of(
			new OreColor(ConventionalBlockTags.DIAMOND_ORES, 0x4AEDD9),
			new OreColor(ConventionalBlockTags.EMERALD_ORES, 0x17DD62),
			new OreColor(ConventionalBlockTags.GOLD_ORES, 0xFCEE4B),
			new OreColor(ConventionalBlockTags.IRON_ORES, 0xD8AF93),
			new OreColor(ConventionalBlockTags.REDSTONE_ORES, 0xFF0000),
			new OreColor(ConventionalBlockTags.LAPIS_ORES, 0x345EC3),
			new OreColor(ConventionalBlockTags.COPPER_ORES, 0xE77C56),
			new OreColor(ConventionalBlockTags.COAL_ORES, 0x3A3A3A),
			new OreColor(ConventionalBlockTags.NETHERITE_SCRAP_ORES, 0x6B4A3A),
			new OreColor(ConventionalBlockTags.QUARTZ_ORES, 0xEDE6DD));
	private static final int DEFAULT_COLOR = 0xFFFFFF;

	private static final Predicate<BlockState> ORE = state -> state.is(ConventionalBlockTags.ORES);
	private static final Predicate<BlockState> TREASURE = state -> state.is(ConventionalBlockTags.DIAMOND_ORES)
			|| state.is(ConventionalBlockTags.NETHERITE_SCRAP_ORES);
	private static final Predicate<BlockState> RELIC = state -> state.is(Blocks.SUSPICIOUS_SAND) || state.is(Blocks.SUSPICIOUS_GRAVEL);

	/** Treasure blocks that already chimed, so each one chimes only once. */
	private static final Set<BlockPos> CHIMED = new HashSet<>();

	private static int ticks;

	private BlockSense() {
	}

	public static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		ClientLevel level = minecraft.level;
		if (player == null || level == null || minecraft.isPaused()) {
			return;
		}
		ticks++;
		ItemStack held = player.getMainHandItem();
		BlockPos origin = player.blockPosition();

		if (ticks % ORE_INTERVAL_TICKS == 0 && held.is(ItemTags.PICKAXES) && Perks.has(player, Perks.ORE_SENSE)) {
			BlockPos ore = nearest(level, origin, ORE_RADIUS, ORE);
			if (ore != null) {
				// Bigger particles when the ore is close, so you can feel when you're getting warm
				double distance = Math.sqrt(ore.distSqr(origin));
				float scale = (float) (0.6 + 1.0 * (1 - Math.min(distance, ORE_RADIUS) / ORE_RADIUS));
				showTrail(level, player, ore, new DustParticleOptions(colorOf(level.getBlockState(ore)), scale));
			}
		}
		// Offset so the treasure trail doesn't appear on the same tick as the ore trail
		if ((ticks + TREASURE_INTERVAL_TICKS / 2) % TREASURE_INTERVAL_TICKS == 0 && held.is(ItemTags.PICKAXES)
				&& Perks.has(player, Perks.TREASURE_SENSE)) {
			BlockPos treasure = nearest(level, origin, TREASURE_RADIUS, TREASURE);
			if (treasure != null) {
				showTrail(level, player, treasure, ParticleTypes.END_ROD);
				if (treasure.distSqr(origin) <= TREASURE_CHIME_DISTANCE * TREASURE_CHIME_DISTANCE && CHIMED.add(treasure)) {
					level.playLocalSound(treasure, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5f, 1.0f, false);
				}
			}
		}
		if (ticks % RELIC_INTERVAL_TICKS == 0 && (held.is(Items.BRUSH) || held.is(ItemTags.SHOVELS)) && Perks.has(player, Perks.RELIC_SENSE)) {
			BlockPos relic = nearest(level, origin, RELIC_RADIUS, RELIC);
			if (relic != null) {
				showTrail(level, player, relic, new DustParticleOptions(RELIC_COLOR, 1.0f));
			}
		}
		if (CHIMED.size() > 256) {
			CHIMED.clear();
		}
	}

	/**
	 * Nearest matching block within the radius. Chunk sections whose palette can't contain a match are skipped
	 * without looking at their blocks, which keeps big radii for rare blocks cheap.
	 */
	private static BlockPos nearest(ClientLevel level, BlockPos origin, int radius, Predicate<BlockState> target) {
		BlockPos nearest = null;
		double nearestDistance = (double) radius * radius;
		int minY = Math.max(origin.getY() - radius, level.getMinY());
		int maxY = Math.min(origin.getY() + radius, level.getMaxY());
		for (int sectionX = SectionPos.blockToSectionCoord(origin.getX() - radius); sectionX <= SectionPos.blockToSectionCoord(origin.getX() + radius); sectionX++) {
			for (int sectionZ = SectionPos.blockToSectionCoord(origin.getZ() - radius); sectionZ <= SectionPos.blockToSectionCoord(origin.getZ() + radius); sectionZ++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(sectionX, sectionZ);
				if (chunk == null) {
					continue;
				}
				for (int sectionY = SectionPos.blockToSectionCoord(minY); sectionY <= SectionPos.blockToSectionCoord(maxY); sectionY++) {
					LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
					if (section.hasOnlyAir() || !section.maybeHas(target)) {
						continue;
					}
					int baseX = SectionPos.sectionToBlockCoord(sectionX);
					int baseY = SectionPos.sectionToBlockCoord(sectionY);
					int baseZ = SectionPos.sectionToBlockCoord(sectionZ);
					for (int x = Math.max(baseX, origin.getX() - radius); x <= Math.min(baseX + 15, origin.getX() + radius); x++) {
						for (int y = Math.max(baseY, minY); y <= Math.min(baseY + 15, maxY); y++) {
							for (int z = Math.max(baseZ, origin.getZ() - radius); z <= Math.min(baseZ + 15, origin.getZ() + radius); z++) {
								double distance = origin.distSqr(new Vec3i(x, y, z));
								if (distance <= nearestDistance && target.test(section.getBlockState(x - baseX, y - baseY, z - baseZ))) {
									nearest = new BlockPos(x, y, z);
									nearestDistance = distance;
								}
							}
						}
					}
				}
			}
		}
		return nearest;
	}

	private static void showTrail(ClientLevel level, LocalPlayer player, BlockPos target, ParticleOptions particle) {
		Vec3 eyes = player.getEyePosition();
		Vec3 toTarget = Vec3.atCenterOf(target).subtract(eyes);
		double distance = toTarget.length();
		Vec3 direction = toTarget.normalize();
		int particles = Math.min(TRAIL_PARTICLES, (int) (distance / TRAIL_SPACING));
		for (int i = 0; i < particles; i++) {
			Vec3 point = eyes.add(direction.scale(0.8 + i * TRAIL_SPACING));
			level.addParticle(particle, point.x, point.y - 0.3, point.z, 0, 0, 0);
		}
	}

	private static int colorOf(BlockState state) {
		for (OreColor oreColor : COLORS) {
			if (state.is(oreColor.tag())) {
				return oreColor.color();
			}
		}
		return DEFAULT_COLOR;
	}
}
