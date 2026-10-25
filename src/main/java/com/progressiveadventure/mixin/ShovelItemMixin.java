package com.progressiveadventure.mixin;

import com.progressiveadventure.perk.PerkEffects;
import com.progressiveadventure.perk.Perks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.context.UseOnContext;
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

	/**
	 * Path Maker: making a path also flattens the 8 blocks around it (sneak for a single block). Each extra block
	 * uses durability like the first one (Sturdy Shovel applies), and it stops when the shovel breaks.
	 */
	@Inject(method = "useOn", at = @At("RETURN"))
	private void pam$pathMaker(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		BlockPos center = context.getClickedPos();
		if (!(context.getLevel() instanceof ServerLevel level) || !(context.getPlayer() instanceof ServerPlayer player) || player.isShiftKeyDown()
				|| cir.getReturnValue() != InteractionResult.SUCCESS || !level.getBlockState(center).is(Blocks.DIRT_PATH)
				|| !Perks.has(player, Perks.PATH_MAKER)) {
			return;
		}
		ItemStack shovel = context.getItemInHand();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 0, 1))) {
			if (shovel.isEmpty()) {
				return; // shovel broke
			}
			BlockState path = FLATTENABLES.get(level.getBlockState(pos).getBlock());
			if (path != null && level.getBlockState(pos.above()).isAir() && PerkEffects.mayChangeBlock(player, level, pos)) {
				level.setBlock(pos, path, Block.UPDATE_ALL_IMMEDIATE);
				shovel.hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
			}
		}
	}
}
