package com.progressiveadventure.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.progressiveadventure.world.Elites;
import com.progressiveadventure.world.MobUnlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.chunk.ChunkGenerator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(NaturalSpawner.class)
public class NaturalSpawnerMixin {
	// Runs once per spawn group, after vanilla picked the mob type; an empty result skips the group
	@Inject(method = "getRandomSpawnMobAt", at = @At("RETURN"), cancellable = true)
	private static void pam$mobUnlocks(ServerLevel level, StructureManager structureManager, ChunkGenerator generator, MobCategory category,
			RandomSource random, BlockPos pos, CallbackInfoReturnable<Optional<SpawnerData>> cir) {
		Optional<SpawnerData> picked = cir.getReturnValue();
		if (picked.isPresent()) {
			cir.setReturnValue(MobUnlocks.filterNaturalSpawn(level, structureManager, generator, category, random, pos, picked.get()));
		}
	}

	// Only the regular spawn loop, not chunk generation (which only spawns animals anyway)
	@WrapOperation(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
	private static void pam$elites(ServerLevel level, Entity entity, Operation<Void> original) {
		if (entity instanceof Mob mob) {
			Elites.onNaturalSpawn(level, mob);
		}
		original.call(level, entity);
	}
}
