package com.progressiveadventure.skill;

import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.PlayerProgress;
import com.progressiveadventure.milestone.Milestones;
import com.progressiveadventure.perk.PerkEffects;
import com.progressiveadventure.perk.Perks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Farming skill: xp from harvesting grown crops and breeding animals, extra harvests, farming perks. */
public final class Farming {
	/** Set while Harvester breaks the surrounding crops, so those breaks don't trigger it again. */
	private static boolean harvestingArea;

	private Farming() {
	}

	/** Crops whose age proves they grew, so it doesn't matter who planted them. */
	private static boolean isGrownAgeCrop(BlockState state) {
		if (state.getBlock() instanceof CropBlock crop) {
			return crop.isMaxAge(state);
		}
		if (state.getBlock() instanceof NetherWartBlock) {
			return state.getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE;
		}
		if (state.getBlock() instanceof CocoaBlock) {
			return state.getValue(CocoaBlock.AGE) == CocoaBlock.MAX_AGE;
		}
		return false;
	}

	/**
	 * Blocks that belong to Farming, grown or not. They never train a tool skill, even though vanilla lets an axe
	 * mine melons, pumpkins, cocoa and bamboo.
	 */
	public static boolean isFarmBlock(BlockState state) {
		return state.getBlock() instanceof CropBlock || state.getBlock() instanceof NetherWartBlock
				|| state.getBlock() instanceof CocoaBlock || state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN)
				|| state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS) || state.is(Blocks.BAMBOO);
	}

	private static BlockState replantedState(BlockState state) {
		if (state.getBlock() instanceof CropBlock crop) {
			return crop.getStateForAge(0);
		}
		if (state.getBlock() instanceof NetherWartBlock) {
			return state.setValue(NetherWartBlock.AGE, 0);
		}
		return state.setValue(CocoaBlock.AGE, 0);
	}

	/**
	 * Farming xp for breaking this block, or 0. Melons, pumpkins, sugar cane, cactus and bamboo only count if
	 * they grew (weren't placed by a player).
	 */
	private static int harvestXp(BlockState state, boolean placed) {
		if (isGrownAgeCrop(state)) {
			return 1;
		}
		if (placed) {
			return 0;
		}
		if (state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN)) {
			return PamConfig.get().melonPumpkinXp;
		}
		return state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS) || state.is(Blocks.BAMBOO) ? 1 : 0;
	}

	public static void onBlockBroken(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, BlockEntity blockEntity, boolean placed) {
		int xp = harvestXp(state, placed);
		if (xp == 0) {
			return;
		}
		ItemStack tool = player.getMainHandItem();
		onHarvest(player, level, pos, state, xp, Block.getDrops(state, level, pos, blockEntity, player, tool));

		if (tool.is(ItemTags.HOES) && !harvestingArea && Perks.has(player, Perks.HARVESTER)) {
			harvestingArea = true;
			try {
				for (BlockPos neighbor : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
					if (!neighbor.equals(pos) && isGrownAgeCrop(level.getBlockState(neighbor)) && PerkEffects.mayChangeBlock(player, level, neighbor)) {
						player.gameMode.destroyBlock(neighbor.immutable());
					}
				}
			} finally {
				harvestingArea = false;
			}
		}
	}

	/** Right-click on a block: sweet berry picking (xp only, vanilla does the picking) and the Replant perk. */
	public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND || player.isSpectator()) {
			return InteractionResult.PASS;
		}
		BlockPos pos = hit.getBlockPos();
		BlockState state = level.getBlockState(pos);
		ItemStack held = player.getMainHandItem();

		if (state.getBlock() instanceof SweetBerryBushBlock) {
			int age = state.getValue(SweetBerryBushBlock.AGE);
			boolean willPick = age > 1 && !(age < SweetBerryBushBlock.MAX_AGE && held.is(Items.BONE_MEAL))
					&& !(player.isSecondaryUseActive() && !held.isEmpty());
			if (willPick && player instanceof ServerPlayer serverPlayer) {
				onHarvest(serverPlayer, (ServerLevel) level, pos, state, 1, List.of(new ItemStack(Items.SWEET_BERRIES)));
			}
			return InteractionResult.PASS;
		}

		if (!isGrownAgeCrop(state) || !Perks.has(player, Perks.REPLANT)) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			ServerLevel serverLevel = (ServerLevel) level;
			// Replant changes the block directly, so it does the protection checks a normal break would
			if (!PerkEffects.mayChangeBlock(serverPlayer, serverLevel, pos)) {
				return InteractionResult.PASS;
			}
			replant(serverPlayer, serverLevel, pos, state);
			if (held.is(ItemTags.HOES) && Perks.has(player, Perks.HARVESTER)) {
				for (BlockPos neighbor : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 0, 1))) {
					BlockState neighborState = level.getBlockState(neighbor);
					if (!neighbor.equals(pos) && isGrownAgeCrop(neighborState) && PerkEffects.mayChangeBlock(serverPlayer, serverLevel, neighbor)) {
						replant(serverPlayer, serverLevel, neighbor.immutable(), neighborState);
					}
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Harvests a grown crop in place: drops everything except one seed, which goes back in the ground. */
	private static void replant(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
		List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, null, player, player.getMainHandItem()));
		ItemStack seed = state.getCloneItemStack(level, pos, false);
		for (ItemStack drop : drops) {
			if (ItemStack.isSameItem(drop, seed)) {
				drop.shrink(1);
				break;
			}
		}
		drops.removeIf(ItemStack::isEmpty);
		drops.forEach(drop -> Block.popResource(level, pos, drop));
		level.setBlock(pos, replantedState(state), Block.UPDATE_ALL);
		level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1f, 1f);
		onHarvest(player, level, pos, state, 1, drops);
	}

	/** Xp, crop tracking and extra drops for any harvest. {@code drops} are the drops the player already got. */
	private static void onHarvest(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, int xp, List<ItemStack> drops) {
		Skills.addXp(player, Skill.FARMING, xp);

		String crop = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
		PlayerProgress progress = PamAttachments.progress(player);
		if (!progress.crops().contains(crop)) {
			progress = progress.withCrop(crop);
			PamAttachments.setProgress(player, progress);
			Milestones.checkThresholds(player, Milestones.CROP_THRESHOLDS, progress.crops().size());
		}

		if (drops.isEmpty()) {
			return;
		}
		if (level.getRandom().nextDouble() < Skills.bonus(Skill.FARMING, Skills.level(player, Skill.FARMING))) {
			Block.popResource(level, pos, drops.getFirst().copyWithCount(1));
		}
		int tier = Perks.tier(player, Perks.BOUNTIFUL_HARVEST_1, Perks.BOUNTIFUL_HARVEST_2);
		if (tier > 0 && level.getRandom().nextDouble() < PamConfig.listValue(PamConfig.get().bountifulHarvestChances, tier - 1)) {
			drops.forEach(drop -> Block.popResource(level, pos, drop.copy()));
		}
	}

	/** Called when two animals breed, before vanilla resets their love state. */
	public static void onBred(ServerLevel level, Animal animal, Animal partner, AgeableMob offspring) {
		Set<ServerPlayer> breeders = new LinkedHashSet<>();
		if (animal.getLoveCause() != null) {
			breeders.add(animal.getLoveCause());
		}
		if (partner.getLoveCause() != null) {
			breeders.add(partner.getLoveCause());
		}

		String type = BuiltInRegistries.ENTITY_TYPE.getKey(animal.getType()).toString();
		boolean twins = false;
		for (ServerPlayer player : breeders) {
			Skills.addXp(player, Skill.FARMING, PamConfig.get().breedingXp);
			PlayerProgress progress = PamAttachments.progress(player);
			if (!progress.bred().contains(type)) {
				progress = progress.withBred(type);
				PamAttachments.setProgress(player, progress);
				Milestones.checkThresholds(player, Milestones.BREED_THRESHOLDS, progress.bred().size());
			}
			twins |= Perks.has(player, Perks.RANCHER);
		}

		if (twins && offspring != null && level.getRandom().nextDouble() < PamConfig.get().rancherTwinChance) {
			AgeableMob twin = animal.getBreedOffspring(level, partner);
			if (twin != null) {
				twin.setBaby(true);
				twin.snapTo(animal.getX(), animal.getY(), animal.getZ(), 0f, 0f);
				level.addFreshEntityWithPassengers(twin);
			}
		}
	}
}
