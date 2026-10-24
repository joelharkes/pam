package com.progressiveadventure.client;

import com.progressiveadventure.perk.Perks;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Ore Sense perk: while holding a pickaxe, periodically shows a short particle trail pointing at the nearest ore,
 * coloured by ore type and bigger when closer. Purely client-side; the client already knows the blocks around it.
 */
public final class OreSense {
	private static final int RADIUS = 12;
	private static final int INTERVAL_TICKS = 30;
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

	private static int ticks;

	private OreSense() {
	}

	public static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		ClientLevel level = minecraft.level;
		if (player == null || level == null || minecraft.isPaused() || ++ticks % INTERVAL_TICKS != 0) {
			return;
		}
		if (!player.getMainHandItem().is(ItemTags.PICKAXES) || !Perks.has(player, Perks.ORE_SENSE)) {
			return;
		}

		BlockPos origin = player.blockPosition();
		BlockPos nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-RADIUS, -RADIUS, -RADIUS), origin.offset(RADIUS, RADIUS, RADIUS))) {
			double distance = pos.distSqr(origin);
			if (distance < nearestDistance && distance <= RADIUS * RADIUS && level.getBlockState(pos).is(ConventionalBlockTags.ORES)) {
				nearest = pos.immutable();
				nearestDistance = distance;
			}
		}
		if (nearest != null) {
			showTrail(level, player, nearest);
		}
	}

	private static void showTrail(ClientLevel level, LocalPlayer player, BlockPos ore) {
		Vec3 eyes = player.getEyePosition();
		Vec3 toOre = Vec3.atCenterOf(ore).subtract(eyes);
		double distance = toOre.length();
		Vec3 direction = toOre.normalize();
		// Bigger particles when the ore is close, so you can feel when you're getting warm
		float scale = (float) (0.6 + 1.0 * (1 - Math.min(distance, RADIUS) / RADIUS));
		DustParticleOptions particle = new DustParticleOptions(colorOf(level.getBlockState(ore)), scale);

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
