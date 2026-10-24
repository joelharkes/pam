package com.progressiveadventure.mixin;

import com.progressiveadventure.perk.Perks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmlandBlock.class)
public class FarmlandBlockMixin {
	@Inject(method = "turnToDirt", at = @At("HEAD"), cancellable = true)
	private static void pam$lightStep(Entity sourceEntity, BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
		if (sourceEntity instanceof Player player && Perks.has(player, Perks.LIGHT_STEP)) {
			ci.cancel();
		}
	}
}
