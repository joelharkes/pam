package com.progressiveadventure.mixin;

import com.progressiveadventure.perk.Perks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(ShovelItem.class)
public class ShovelItemMixin {
	@Shadow
	@Final
	protected static Map<Block, BlockState> FLATTENABLES;

	/** Path Maker: making a path also flattens the 8 blocks around it (sneak for a single block). */
	@Inject(method = "useOn", at = @At("RETURN"))
	private void pam$pathMaker(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		BlockPos center = context.getClickedPos();
		if (level.isClientSide() || player == null || player.isShiftKeyDown() || cir.getReturnValue() != InteractionResult.SUCCESS
				|| !level.getBlockState(center).is(Blocks.DIRT_PATH) || !Perks.has(player, Perks.PATH_MAKER)) {
			return;
		}
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
			BlockState path = FLATTENABLES.get(level.getBlockState(pos).getBlock());
			if (path != null && level.getBlockState(pos.above()).isAir()) {
				level.setBlock(pos, path, Block.UPDATE_ALL_IMMEDIATE);
			}
		}
	}
}
