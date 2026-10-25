package com.progressiveadventure.perk;

import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.skill.PlacedBlocks;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
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
import java.util.function.Predicate;

/** Server-side behaviour of perks that aren't plain attribute modifiers. */
public final class PerkEffects {
	private static final List<Item> ARCHAEOLOGIST_FINDS = List.of(
			Items.FLINT, Items.CLAY_BALL, Items.BONE, Items.STICK, Items.WHEAT_SEEDS,
			Items.IRON_NUGGET, Items.GOLD_NUGGET, Items.BOWL, Items.STRING);
	private static final List<Item> BIRDS_NEST_FINDS = List.of(
			Items.FEATHER, Items.FEATHER, Items.EGG, Items.STRING, Items.STICK, Items.WHEAT_SEEDS);
	/** Ores that count as the same vein for Vein Miner (so a vein crossing into deepslate stays one vein). */
	private static final List<TagKey<Block>> ORE_GROUPS = List.of(
			ConventionalBlockTags.COAL_ORES, ConventionalBlockTags.COPPER_ORES, ConventionalBlockTags.IRON_ORES,
			ConventionalBlockTags.GOLD_ORES, ConventionalBlockTags.REDSTONE_ORES, ConventionalBlockTags.LAPIS_ORES,
			ConventionalBlockTags.DIAMOND_ORES, ConventionalBlockTags.EMERALD_ORES, ConventionalBlockTags.QUARTZ_ORES,
			ConventionalBlockTags.NETHERITE_SCRAP_ORES);

	private static final Map<UUID, Long> SECOND_WIND_COOLDOWNS = new HashMap<>();

	/** Set while Timber, Vein Miner or Excavator break extra blocks, so those breaks don't trigger the perk again. */
	private static boolean fellingTree;
	private static boolean miningVein;
	private static boolean excavating;

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
			tier = Perks.tier(player, Perks.STURDY_WEAPON_1, Perks.STURDY_WEAPON_2, Perks.STURDY_WEAPON_3);
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
		if (tool.is(ItemTags.AXES) && state.is(BlockTags.LOGS) && !fellingTree && !player.isShiftKeyDown()) {
			if (Perks.has(player, Perks.TIMBER)) {
				fellTree(player, level, pos, PamConfig.get().timberMaxLogs);
			} else if (Perks.has(player, Perks.LUMBERJACK)) {
				fellTree(player, level, pos, PamConfig.get().lumberjackMaxLogs);
			}
		}
		if (tool.is(ItemTags.AXES) && state.is(BlockTags.LOGS) && Perks.has(player, Perks.BIRDS_NEST)) {
			birdsNest(player, level, pos);
		}
		if (state.getBlock() instanceof LeavesBlock && !state.getValue(LeavesBlock.PERSISTENT) && Perks.has(player, Perks.ORCHARD)) {
			orchard(level, pos);
		}
		if (tool.is(ItemTags.PICKAXES) && state.is(ConventionalBlockTags.ORES) && !miningVein && !player.isShiftKeyDown()
				&& Perks.has(player, Perks.VEIN_MINER)) {
			mineVein(player, level, pos, state);
		}
		if (tool.is(ItemTags.SHOVELS) && canExcavate(state) && !excavating && !player.isShiftKeyDown() && Perks.has(player, Perks.EXCAVATOR)) {
			excavate(player, level, pos);
		}
	}

	private static void birdsNest(ServerPlayer player, ServerLevel level, BlockPos pos) {
		PamConfig config = PamConfig.get();
		if (level.getRandom().nextDouble() >= config.birdsNestChance) {
			return;
		}
		int finds = 1 + level.getRandom().nextInt(2);
		for (int i = 0; i < finds; i++) {
			Block.popResource(level, pos, new ItemStack(BIRDS_NEST_FINDS.get(level.getRandom().nextInt(BIRDS_NEST_FINDS.size()))));
		}
		if (level.getRandom().nextDouble() < config.birdsNestEmeraldChance) {
			Block.popResource(level, pos, new ItemStack(Items.EMERALD));
		}
		player.sendOverlayMessage(Component.translatable("message.progressive_adventure.birds_nest"));
		level.playSound(null, pos, SoundEvents.PARROT_AMBIENT, SoundSource.PLAYERS, 0.8f, 1.2f);
	}

	private static void orchard(ServerLevel level, BlockPos pos) {
		PamConfig config = PamConfig.get();
		double roll = level.getRandom().nextDouble();
		if (roll < config.orchardGoldenAppleChance) {
			Block.popResource(level, pos, new ItemStack(Items.GOLDEN_APPLE));
		} else if (roll < config.orchardGoldenAppleChance + config.orchardAppleChance) {
			Block.popResource(level, pos, new ItemStack(Items.APPLE));
		}
	}

	/** Mines ore blocks connected to the broken one (also diagonally) that belong to the same ore. */
	private static void mineVein(ServerPlayer player, ServerLevel level, BlockPos origin, BlockState ore) {
		TagKey<Block> group = ORE_GROUPS.stream().filter(ore::is).findFirst().orElse(null);
		Predicate<BlockState> sameOre = state -> group != null ? state.is(group) : state.is(ore.getBlock());
		List<BlockPos> vein = new ArrayList<>();
		Set<BlockPos> visited = new HashSet<>();
		Queue<BlockPos> queue = new ArrayDeque<>();
		queue.add(origin);
		visited.add(origin);
		int max = PamConfig.get().veinMinerMaxBlocks;
		while (!queue.isEmpty() && vein.size() < max) {
			BlockPos current = queue.poll();
			for (BlockPos next : BlockPos.betweenClosed(current.offset(-1, -1, -1), current.offset(1, 1, 1))) {
				if (vein.size() >= max || visited.contains(next)) {
					continue;
				}
				BlockPos pos = next.immutable();
				visited.add(pos);
				if (sameOre.test(level.getBlockState(pos)) && !PlacedBlocks.isPlaced(level, pos)) {
					queue.add(pos);
					vein.add(pos);
				}
			}
		}

		miningVein = true;
		try {
			for (BlockPos pos : vein) {
				if (!player.getMainHandItem().is(ItemTags.PICKAXES)) {
					break; // pickaxe broke
				}
				player.gameMode.destroyBlock(pos);
			}
		} finally {
			miningVein = false;
		}
	}

	private static boolean canExcavate(BlockState state) {
		// Suspicious sand and gravel hide treasure for the brush, so never dig those up by accident
		return state.is(BlockTags.MINEABLE_WITH_SHOVEL) && !state.hasBlockEntity();
	}

	/** Digs the 3x3 square around the broken block, facing the player (flat when looking up or down). */
	private static void excavate(ServerPlayer player, ServerLevel level, BlockPos center) {
		Direction.Axis axis = Math.abs(player.getXRot()) > 45 ? Direction.Axis.Y : player.getDirection().getAxis();
		excavating = true;
		try {
			for (int a = -1; a <= 1; a++) {
				for (int b = -1; b <= 1; b++) {
					if (a == 0 && b == 0) {
						continue;
					}
					if (!player.getMainHandItem().is(ItemTags.SHOVELS)) {
						return; // shovel broke
					}
					BlockPos pos = switch (axis) {
						case Y -> center.offset(a, 0, b);
						case X -> center.offset(0, a, b);
						case Z -> center.offset(a, b, 0);
					};
					if (canExcavate(level.getBlockState(pos)) && !PlacedBlocks.isPlaced(level, pos)) {
						player.gameMode.destroyBlock(pos);
					}
				}
			}
		} finally {
			excavating = false;
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

	/** Breaks up to maxLogs logs connected to the broken one, if they form a natural tree (non-persistent leaves nearby). */
	private static void fellTree(ServerPlayer player, ServerLevel level, BlockPos origin, int maxLogs) {
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
