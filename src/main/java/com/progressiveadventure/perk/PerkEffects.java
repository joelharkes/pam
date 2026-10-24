package com.progressiveadventure.perk;

import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.skill.PlacedBlocks;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/** Server-side behaviour of perks that aren't plain attribute modifiers. */
public final class PerkEffects {
	private static final List<Item> ARCHAEOLOGIST_FINDS = List.of(
			Items.FLINT, Items.CLAY_BALL, Items.BONE, Items.STICK, Items.WHEAT_SEEDS,
			Items.IRON_NUGGET, Items.GOLD_NUGGET, Items.BOWL, Items.STRING);

	private static final Map<UUID, Long> SECOND_WIND_COOLDOWNS = new HashMap<>();

	/** Set while Timber breaks logs, so the extra breaks don't trigger Timber again. */
	private static boolean fellingTree;

	private PerkEffects() {
	}

	/** Chance that this item takes no durability damage, from Sturdy perks. */
	public static double durabilitySaveChance(ServerPlayer player, ItemStack stack) {
		int tier;
		if (stack.is(ItemTags.PICKAXES)) {
			tier = Perks.tier(player, Perks.STURDY_PICKAXE_1, Perks.STURDY_PICKAXE_2, Perks.STURDY_PICKAXE_3);
		} else if (stack.is(ItemTags.AXES)) {
			tier = Perks.tier(player, Perks.STURDY_AXE_1, Perks.STURDY_AXE_2, Perks.STURDY_AXE_3);
		} else if (stack.is(ItemTags.SHOVELS)) {
			tier = Perks.tier(player, Perks.STURDY_SHOVEL_1, Perks.STURDY_SHOVEL_2, Perks.STURDY_SHOVEL_3);
		} else if (stack.is(ItemTags.HOES)) {
			tier = Perks.tier(player, Perks.STURDY_HOE_1, Perks.STURDY_HOE_2, Perks.STURDY_HOE_3);
		} else if (stack.is(ItemTags.SWORDS)) {
			tier = Perks.tier(player, Perks.STURDY_WEAPON_1, Perks.STURDY_WEAPON_2);
		} else if (stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.CHEST_ARMOR) || stack.is(ItemTags.LEG_ARMOR) || stack.is(ItemTags.FOOT_ARMOR)) {
			tier = Perks.tier(player, Perks.STURDY_ARMOR_1, Perks.STURDY_ARMOR_2, Perks.STURDY_ARMOR_3);
		} else {
			return 0;
		}
		return tier == 0 ? 0 : PamConfig.listValue(PamConfig.get().sturdyChances, tier - 1);
	}

	/** Damage multiplier for damage the player takes, from Defense perks. Only the first matching guard counts. */
	public static float damageTakenMultiplier(ServerPlayer player, DamageSource source) {
		PamConfig config = PamConfig.get();
		int tier;
		List<Double> reductions = config.guardDamageReductions;
		if (source.is(DamageTypeTags.IS_EXPLOSION)) {
			tier = Perks.tier(player, Perks.BLAST_GUARD_1, Perks.BLAST_GUARD_2);
		} else if (source.is(DamageTypeTags.IS_PROJECTILE)) {
			tier = Perks.tier(player, Perks.ARROW_GUARD_1, Perks.ARROW_GUARD_2);
		} else if (source.is(DamageTypeTags.IS_FIRE)) {
			tier = Perks.tier(player, Perks.FIRE_GUARD_1, Perks.FIRE_GUARD_2);
		} else if (source.getDirectEntity() instanceof Mob) {
			tier = Perks.tier(player, Perks.TOUGH_HIDE_1, Perks.TOUGH_HIDE_2);
			reductions = config.toughHideDamageReductions;
		} else {
			return 1f;
		}
		return tier == 0 ? 1f : (float) (1 - PamConfig.listValue(reductions, tier - 1));
	}

	/** Called after a player breaks a block that they didn't place themselves. */
	public static void onNaturalBlockBroken(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		ItemStack tool = player.getMainHandItem();
		if (tool.is(ItemTags.PICKAXES) && state.is(ConventionalBlockTags.ORES)) {
			prospect(player, level, pos, state, blockEntity, tool);
		}
		if (tool.is(ItemTags.SHOVELS) && isArchaeologyBlock(state) && Perks.has(player, Perks.ARCHAEOLOGIST)
				&& level.getRandom().nextDouble() < PamConfig.get().archaeologistChance) {
			Item find = ARCHAEOLOGIST_FINDS.get(level.getRandom().nextInt(ARCHAEOLOGIST_FINDS.size()));
			Block.popResource(level, pos, new ItemStack(find));
		}
		if (tool.is(ItemTags.AXES) && state.is(BlockTags.LOGS) && !fellingTree && !player.isShiftKeyDown() && Perks.has(player, Perks.TIMBER)) {
			fellTree(player, level, pos);
		}
	}

	private static void prospect(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
		int tier = Perks.tier(player, Perks.PROSPECTOR_1, Perks.PROSPECTOR_2);
		if (tier == 0 || level.getRandom().nextDouble() >= PamConfig.listValue(PamConfig.get().prospectorChances, tier - 1)) {
			return;
		}
		List<ItemStack> drops = Block.getDrops(state, level, pos, blockEntity, player, tool);
		// Silk Touch drops the ore block itself; doubling that would be an exploit
		if (drops.stream().anyMatch(stack -> stack.is(state.getBlock().asItem()))) {
			return;
		}
		drops.forEach(stack -> Block.popResource(level, pos, stack));
	}

	private static boolean isArchaeologyBlock(BlockState state) {
		return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL);
	}

	/** Breaks all logs connected to the broken one, if they form a natural tree (non-persistent leaves nearby). */
	private static void fellTree(ServerPlayer player, ServerLevel level, BlockPos origin) {
		int maxLogs = PamConfig.get().timberMaxLogs;
		List<BlockPos> logs = new ArrayList<>();
		Set<BlockPos> visited = new HashSet<>();
		Queue<BlockPos> queue = new ArrayDeque<>();
		queue.add(origin);
		visited.add(origin);
		boolean hasLeaves = false;

		while (!queue.isEmpty() && logs.size() < maxLogs) {
			BlockPos current = queue.poll();
			for (BlockPos next : BlockPos.betweenClosed(current.offset(-1, 0, -1), current.offset(1, 1, 1))) {
				if (visited.contains(next)) {
					continue;
				}
				BlockState state = level.getBlockState(next);
				if (state.getBlock() instanceof LeavesBlock && !state.getValue(LeavesBlock.PERSISTENT)) {
					hasLeaves = true;
				}
				if (state.is(BlockTags.LOGS)) {
					BlockPos log = next.immutable();
					visited.add(log);
					queue.add(log);
					logs.add(log);
				}
			}
		}
		if (!hasLeaves) {
			return;
		}

		fellingTree = true;
		try {
			for (BlockPos log : logs) {
				if (!player.getMainHandItem().is(ItemTags.AXES)) {
					break; // axe broke
				}
				if (!PlacedBlocks.isPlaced(level, log)) {
					player.gameMode.destroyBlock(log);
				}
			}
		} finally {
			fellingTree = false;
		}
	}

	public static void onDamaged(ServerPlayer player) {
		PamConfig config = PamConfig.get();
		if (!player.isAlive() || player.getHealth() >= config.secondWindHealthThreshold || !Perks.has(player, Perks.SECOND_WIND)) {
			return;
		}
		long now = player.level().getGameTime();
		Long readyAt = SECOND_WIND_COOLDOWNS.get(player.getUUID());
		if (readyAt != null && now < readyAt) {
			return;
		}
		SECOND_WIND_COOLDOWNS.put(player.getUUID(), now + config.secondWindCooldownSeconds * 20L);
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
	}
}
