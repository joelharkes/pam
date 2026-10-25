package com.progressiveadventure.waypoint;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Crafted stone that becomes a waypoint once turned on with shards. ACTIVE is only a look (particles, glow);
 * the waypoint itself (name, owner) is stored in {@link Waypoints}.
 */
public class WaypointStoneBlock extends Block {
	public static final MapCodec<WaypointStoneBlock> CODEC = simpleCodec(WaypointStoneBlock::new);
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	public WaypointStoneBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
	}

	@Override
	protected MapCodec<WaypointStoneBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (player instanceof ServerPlayer serverPlayer) {
			Waypoints.onUse(serverPlayer, (ServerLevel) level, pos, state);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		if (state.getValue(ACTIVE)) {
			Waypoints.onStoneRemoved(level, pos);
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(ACTIVE) && random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.05,
					pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.02 + random.nextDouble() * 0.03, 0);
		}
	}
}
