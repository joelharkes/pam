package com.progressiveadventure.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.perk.Perks;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(BoneMealItem.class)
public class BoneMealItemMixin {
	/** Green Thumb: sometimes give the used bone meal back. */
	@WrapMethod(method = "useOn")
	private InteractionResult pam$greenThumb(UseOnContext context, Operation<InteractionResult> original) {
		ItemStack stack = context.getItemInHand();
		int countBefore = stack.getCount();
		InteractionResult result = original.call(context);
		Player player = context.getPlayer();
		if (!context.getLevel().isClientSide() && player != null && stack.getCount() < countBefore
				&& Perks.has(player, Perks.GREEN_THUMB) && context.getLevel().getRandom().nextDouble() < PamConfig.get().greenThumbChance) {
			stack.grow(1);
		}
		return result;
	}
}
