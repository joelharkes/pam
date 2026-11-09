package com.progressiveadventure.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.progressiveadventure.skill.Fishing;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Fishing skill: bite speed, Keen Eye, Sea's Favor swim-up, Treasure Hunter, xp / Double Catch / Sunken Riches when a catch is reeled in, Campfire Catch. */
@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
	@Shadow
	public abstract Player getPlayerOwner();

	@Shadow
	public abstract boolean isOpenWaterFishing();

	// catchingFish calls Mth.nextInt three times: the bite's reel window (0), the fish swimming up (1) and the wait (2)
	@ModifyExpressionValue(method = "catchingFish", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/util/Mth;nextInt(Lnet/minecraft/util/RandomSource;II)I", ordinal = 0))
	private int pam$reelWindow(int ticks) {
		return getPlayerOwner() instanceof ServerPlayer player ? Fishing.reelTicks(player, ticks) : ticks;
	}

	@ModifyExpressionValue(method = "catchingFish", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/util/Mth;nextInt(Lnet/minecraft/util/RandomSource;II)I", ordinal = 1))
	private int pam$swimUp(int ticks) {
		return getPlayerOwner() instanceof ServerPlayer player ? Fishing.swimUpTicks(player, ticks) : ticks;
	}

	@ModifyExpressionValue(method = "catchingFish", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/util/Mth;nextInt(Lnet/minecraft/util/RandomSource;II)I", ordinal = 2))
	private int pam$biteSpeed(int ticks) {
		return getPlayerOwner() instanceof ServerPlayer player ? Fishing.waitTicks(player, ticks) : ticks;
	}

	@ModifyExpressionValue(method = "retrieve", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"))
	private ObjectArrayList<ItemStack> pam$onCatch(ObjectArrayList<ItemStack> items) {
		return getPlayerOwner() instanceof ServerPlayer player ? new ObjectArrayList<>(Fishing.onCatch(player, items, isOpenWaterFishing())) : items;
	}

	// The items are spawned after the advancement trigger, so cooked fish don't hide the raw catch from Fish Collector
	@ModifyArg(method = "retrieve", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/item/ItemEntity;<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"), index = 4)
	private ItemStack pam$campfireCatch(ItemStack stack) {
		return getPlayerOwner() instanceof ServerPlayer player ? Fishing.cook(player, stack) : stack;
	}

	@ModifyReturnValue(method = "isOpenWaterFishing", at = @At("RETURN"))
	private boolean pam$treasureHunter(boolean openWater) {
		return openWater || getPlayerOwner() instanceof ServerPlayer player && Fishing.treasureAnywhere(player);
	}
}
