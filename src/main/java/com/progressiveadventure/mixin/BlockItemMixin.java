package com.progressiveadventure.mixin;

import com.progressiveadventure.skill.PlacedBlocks;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {
	@Inject(method = "place", at = @At("RETURN"))
	private void pam$markPlaced(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (!context.getLevel().isClientSide() && context.getPlayer() != null && cir.getReturnValue().consumesAction()) {
			PlacedBlocks.mark(context.getLevel(), context.getClickedPos());
		}
	}
}
