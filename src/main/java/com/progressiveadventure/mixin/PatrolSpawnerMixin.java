package com.progressiveadventure.mixin;

import com.progressiveadventure.world.MobUnlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.levelgen.PatrolSpawner;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PatrolSpawner.class)
public class PatrolSpawnerMixin {
	// Pillager patrols unlock like the natural spawns, via the pillager entry in the mob unlock config
	@Inject(method = "spawnPatrolMember", at = @At("HEAD"), cancellable = true)
	private void pam$lockPatrols(ServerLevel level, BlockPos pos, RandomSource random, boolean isLeader, CallbackInfoReturnable<Boolean> cir) {
		if (MobUnlocks.isLockedAt(level, EntityTypes.PILLAGER, pos)) {
			cir.setReturnValue(false);
		}
	}
}
