package com.progressiveadventure.client;

import com.progressiveadventure.network.DoubleJumpPayload;
import com.progressiveadventure.network.GroundPoundPayload;
import com.progressiveadventure.network.MantlePayload;
import com.progressiveadventure.perk.Perks;
import com.progressiveadventure.skill.MeleeMoves;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Double Jump, Mantle and Ground Pound. Player movement is client-driven, so the client moves itself right away and
 * tells the server, which checks the perk and resets fall distance (Ground Pound: handles the landing). One of each
 * per time in the air.
 */
final class AirMoves {
	/** Lowest and highest ledge (above the feet) Mantle grabs; lower ones a normal jump already reaches. */
	private static final double MIN_LEDGE = 0.6;
	private static final double MAX_LEDGE = 1.9;
	/** Ground Pound: how fast the dive goes down (blocks/tick). */
	private static final double POUND_SPEED = 1.5;

	private static boolean wasJumpDown;
	private static boolean wasAirborne;
	private static boolean doubleJumped;
	private static boolean mantled;
	private static boolean wasSneakDown;
	private static boolean pounded;

	private AirMoves() {
	}

	static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null) {
			return;
		}
		boolean jumpDown = player.input.keyPresses.jump();
		boolean jumpPressed = jumpDown && !wasJumpDown;
		wasJumpDown = jumpDown;
		boolean sneakDown = player.input.keyPresses.shift();
		boolean sneakPressed = sneakDown && !wasSneakDown;
		wasSneakDown = sneakDown;
		// The normal jump from the ground already happened this tick, so only count presses that start in the air
		boolean airborne = wasAirborne;
		wasAirborne = !player.onGround();

		if (player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable()) {
			doubleJumped = false;
			mantled = false;
			pounded = false;
			return;
		}
		if (player.getAbilities().flying || player.isFallFlying() || player.isPassenger()) {
			return;
		}

		if (sneakPressed && !pounded && MeleeMoves.canGroundPound(player) && MeleeMoves.highEnoughToPound(player)) {
			pounded = true;
			player.setDeltaMovement(player.getDeltaMovement().x * 0.2, -POUND_SPEED, player.getDeltaMovement().z * 0.2);
			ClientPlayNetworking.send(new GroundPoundPayload());
		} else if (jumpDown && !mantled && Perks.has(player, Perks.MANTLE) && tryMantle(player)) {
			mantled = true;
			ClientPlayNetworking.send(new MantlePayload());
		} else if (jumpPressed && airborne && !doubleJumped && Perks.has(player, Perks.DOUBLE_JUMP) && !wearsGlider(player)) {
			doubleJumped = true;
			// Vanilla's jump (so Spring Step and Jump Boost count), but without its sprint push: a second push in the
			// air, where there's little drag, sent players flying forward
			Vec3 before = player.getDeltaMovement();
			player.jumpFromGround();
			player.setDeltaMovement(before.x, player.getDeltaMovement().y, before.z);
			player.resetFallDistance();
			ClientPlayNetworking.send(new DoubleJumpPayload());
		}
	}

	private static boolean wearsGlider(LocalPlayer player) {
		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			if (LivingEntity.canGlideUsing(player.getItemBySlot(slot), slot)) {
				return true;
			}
		}
		return false;
	}

	/** Pulls the player up when they push forward into a wall whose top is within reach and has room above it. */
	private static boolean tryMantle(LocalPlayer player) {
		// Only near the top of a jump or while falling, so it doesn't fire on every hop up a block
		if (!player.horizontalCollision || !player.input.hasForwardImpulse() || player.getDeltaMovement().y > 0.1) {
			return false;
		}
		Direction facing = player.getDirection();
		Level level = player.level();
		double feet = player.getY();
		int x = Mth.floor(player.getX() + facing.getStepX() * 0.5);
		int z = Mth.floor(player.getZ() + facing.getStepZ() * 0.5);

		// Top of the highest block in front of the player within reach
		double ledgeTop = Double.NaN;
		for (int y = Mth.floor(feet + MAX_LEDGE); y >= Mth.floor(feet); y--) {
			BlockPos pos = new BlockPos(x, y, z);
			VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
			if (!shape.isEmpty()) {
				// Fences and walls are taller than a block and are meant to keep you out
				if (shape.max(Direction.Axis.Y) > 1) {
					return false;
				}
				ledgeTop = y + shape.max(Direction.Axis.Y);
				break;
			}
		}
		double rise = ledgeTop - feet;
		if (Double.isNaN(rise) || rise < MIN_LEDGE || rise > MAX_LEDGE) {
			return false;
		}
		// Room to rise in place and to stand on the ledge
		AABB raised = player.getBoundingBox().move(0, rise + 0.05, 0);
		if (!level.noCollision(player, raised) || !level.noCollision(player, raised.move(facing.getStepX() * 0.6, 0, facing.getStepZ() * 0.6))) {
			return false;
		}

		double lift = Math.sqrt(2 * player.getGravity() * (rise + 0.25));
		player.setDeltaMovement(facing.getStepX() * 0.1, lift, facing.getStepZ() * 0.1);
		player.resetFallDistance();
		return true;
	}
}
