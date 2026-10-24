package com.progressiveadventure.mixin;

import com.progressiveadventure.skill.Skills;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {
	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void pam$applySkillSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(cir.getReturnValueF() * Skills.speedMultiplier((Player) (Object) this, state));
	}
}
