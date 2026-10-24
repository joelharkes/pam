package com.progressiveadventure.world;

import com.progressiveadventure.ProgressiveAdventure;
import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.config.PamConfig.GearTier;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.milestone.Milestones;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Hostile Overworld mobs get stronger with the Adventure Rank of nearby players: normal zombies and skeletons
 * wear more gear, and rarely an elite spawns (bigger, tougher, named, full gear) that drops its gear and bonus loot.
 * At high rank, elites can also get a trait (see {@link EliteTrait}).
 */
public final class Elites {
	private static final Identifier HEALTH_MODIFIER = ProgressiveAdventure.id("elite_health");
	private static final Identifier DAMAGE_MODIFIER = ProgressiveAdventure.id("elite_damage");
	private static final Identifier SCALE_MODIFIER = ProgressiveAdventure.id("elite_scale");
	private static final Identifier SWIFT_MODIFIER = ProgressiveAdventure.id("elite_swift");
	private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final String[] ARMOR_PIECES = {"helmet", "chestplate", "leggings", "boots"};

	private Elites() {
	}

	public static boolean isElite(LivingEntity entity) {
		return entity.hasAttached(PamAttachments.ELITE_RANK);
	}

	public static EliteTrait trait(LivingEntity entity) {
		String id = entity.getAttached(PamAttachments.ELITE_TRAIT);
		return id == null ? null : EliteTrait.byId(id);
	}

	/** Rank the elite spawned at; 0 for normal mobs. */
	public static int eliteRank(LivingEntity entity) {
		return entity.getAttachedOrElse(PamAttachments.ELITE_RANK, 0);
	}

	/** Called for every natural spawn, after vanilla equipped the mob and just before it's added to the world. */
	public static void onNaturalSpawn(ServerLevel level, Mob mob) {
		PamConfig config = PamConfig.get();
		if (!config.elitesEnabled || mob.getType().getCategory() != MobCategory.MONSTER || level.dimension() != Level.OVERWORLD) {
			return;
		}
		int rank = MobUnlocks.lowestRankNear(level, mob.blockPosition());
		if (rank == Integer.MAX_VALUE) {
			return;
		}
		RandomSource random = level.getRandom();
		if (config.eliteMobs.contains(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString())) {
			int start = MobUnlocks.requiredRank(mob.getType()) + config.eliteRankDelay;
			double chance = Math.min(config.eliteChanceMax, config.eliteChanceStart + (rank - start) * config.eliteChancePerRank);
			if (rank >= start && random.nextDouble() < chance) {
				makeElite(level, mob, rank, random, null);
				return;
			}
		}
		if (wearsGear(mob) && random.nextDouble() < config.mobGearChance.at(rank)) {
			addNormalGear(mob, tierFor(config.normalGearTiers, rank), random);
		}
	}

	/** For testing (/pam elite): spawns an elite of this type next to the player, at the player's rank. */
	public static boolean spawnTestElite(ServerPlayer player, EntityType<?> type, EliteTrait trait) {
		ServerLevel level = player.level();
		if (!(type.create(level, EntitySpawnReason.COMMAND) instanceof Mob mob)) {
			return false;
		}
		Vec3 pos = player.position().add(player.getLookAngle().multiply(3, 0, 3));
		mob.snapTo(pos.x, pos.y, pos.z, player.getYRot() + 180, 0);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.COMMAND, null);
		if (trait != null && !trait.canHave(mob)) {
			return false;
		}
		makeElite(level, mob, PamAttachments.snapshot(player).rank(), level.getRandom(), trait);
		level.addFreshEntityWithPassengers(mob);
		return true;
	}

	private static boolean wearsGear(Mob mob) {
		return mob instanceof Zombie || mob instanceof AbstractSkeleton;
	}

	private static GearTier tierFor(List<GearTier> tiers, int rank) {
		GearTier result = tiers.getFirst();
		for (GearTier tier : tiers) {
			if (rank >= tier.rank()) {
				result = tier;
			}
		}
		return result;
	}

	/** One or two armor pieces in empty slots; vanilla drop chances, so this gear rarely drops. */
	private static void addNormalGear(Mob mob, GearTier tier, RandomSource random) {
		List<Integer> empty = new ArrayList<>();
		for (int i = 0; i < ARMOR_SLOTS.length; i++) {
			if (mob.getItemBySlot(ARMOR_SLOTS[i]).isEmpty()) {
				empty.add(i);
			}
		}
		int pieces = 1 + random.nextInt(2);
		for (int n = 0; n < pieces && !empty.isEmpty(); n++) {
			int i = empty.remove(random.nextInt(empty.size()));
			mob.setItemSlot(ARMOR_SLOTS[i], armorPiece(tier.armor(), i));
		}
	}

	/** @param trait forced trait (testing), or null to roll one by rank */
	private static void makeElite(ServerLevel level, Mob mob, int rank, RandomSource random, EliteTrait trait) {
		PamConfig config = PamConfig.get();
		mob.setAttached(PamAttachments.ELITE_RANK, rank);
		addModifier(mob, Attributes.MAX_HEALTH, HEALTH_MODIFIER, config.eliteHealthBonus);
		addModifier(mob, Attributes.ATTACK_DAMAGE, DAMAGE_MODIFIER, config.eliteDamageBonus);
		addModifier(mob, Attributes.SCALE, SCALE_MODIFIER, config.eliteScaleBonus);
		mob.setHealth(mob.getMaxHealth());

		if (trait == null && random.nextDouble() < config.eliteTraitChance.at(rank)) {
			List<EliteTrait> possible = Arrays.stream(EliteTrait.values()).filter(t -> t.canHave(mob)).toList();
			trait = possible.get(random.nextInt(possible.size()));
		}
		if (trait != null) {
			mob.setAttached(PamAttachments.ELITE_TRAIT, trait.id);
			if (trait == EliteTrait.SWIFT) {
				addModifier(mob, Attributes.MOVEMENT_SPEED, SWIFT_MODIFIER, config.swiftSpeedBonus);
			}
			mob.setCustomName(Component.translatable("entity.progressive_adventure.elite." + trait.id, mob.getType().getDescription()));
		} else {
			mob.setCustomName(Component.translatable("entity.progressive_adventure.elite", mob.getType().getDescription()));
		}

		if (!wearsGear(mob)) {
			return;
		}
		GearTier tier = tierFor(config.eliteGearTiers, rank);
		for (int i = 0; i < ARMOR_SLOTS.length; i++) {
			mob.setItemSlot(ARMOR_SLOTS[i], enchant(level, armorPiece(tier.armor(), i), tier.enchantLevel(), random));
		}
		ItemStack weapon = mob.getMainHandItem();
		if (mob instanceof Zombie && weapon.isEmpty() && tier.weapon() != null) {
			weapon = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(tier.weapon())));
		}
		mob.setItemSlot(EquipmentSlot.MAINHAND, enchant(level, weapon, tier.enchantLevel(), random));
		// 1.0 = always drops, but only for player kills and with random wear (vanilla rules below 1.0)
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			mob.setDropChance(slot, 1.0f);
		}
		mob.setDropChance(EquipmentSlot.MAINHAND, 1.0f);
	}

	private static void addModifier(Mob mob, Holder<Attribute> attribute, Identifier id, double amount) {
		AttributeInstance instance = mob.getAttribute(attribute);
		if (instance != null && amount != 0) {
			instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
	}

	private static ItemStack armorPiece(String material, int index) {
		return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(material + "_" + ARMOR_PIECES[index])));
	}

	private static ItemStack enchant(ServerLevel level, ItemStack stack, int enchantLevel, RandomSource random) {
		if (stack.isEmpty() || enchantLevel <= 0) {
			return stack;
		}
		return EnchantmentHelper.enchantItem(random, stack, enchantLevel, level.registryAccess(), Optional.empty());
	}

	/** Elite killed: milestone and bonus loot, only when a player got the kill (no loot from mob farms). */
	public static void onDeath(LivingEntity entity, DamageSource source) {
		if (!isElite(entity) || !(source.getEntity() instanceof ServerPlayer player) || !(entity.level() instanceof ServerLevel level)) {
			return;
		}
		Milestones.grant(player, Milestones.ELITE);

		PamConfig config = PamConfig.get();
		int rank = eliteRank(entity);
		RandomSource random = level.getRandom();
		double lootBoost = trait(entity) != null ? config.traitLootMultiplier : 1;
		if (random.nextDouble() < config.eliteEmeraldChance.at(rank) * lootBoost) {
			int maxCount = Math.max(1, (int) Math.round(config.eliteEmeraldMaxCount.at(rank)));
			entity.spawnAtLocation(level, new ItemStack(Items.EMERALD, 1 + random.nextInt(maxCount)));
		}
		if (random.nextDouble() < config.eliteBookChance.at(rank) * lootBoost) {
			int enchantLevel = Math.max(1, (int) Math.round(config.eliteBookLevel.at(rank)));
			entity.spawnAtLocation(level, enchant(level, new ItemStack(Items.BOOK), enchantLevel, random));
		}
		if (random.nextDouble() < config.eliteGoldenAppleChance.at(rank) * lootBoost) {
			entity.spawnAtLocation(level, new ItemStack(Items.GOLDEN_APPLE));
		}
	}

	/** Goals aren't saved, so Leaping is (re)added whenever an elite is loaded, including right after spawning. */
	public static void onLoad(Entity entity) {
		if (entity instanceof Mob mob && trait(mob) == EliteTrait.LEAPING) {
			mob.getGoalSelector().addGoal(3, new LeapAtTargetGoal(mob, 0.4f));
		}
	}

	/** Frost and Blazing hit effects (melee and the elite's own arrows), and Summoner calling helpers once. */
	public static void onDamage(LivingEntity victim, DamageSource source, float damageTaken) {
		if (damageTaken <= 0 || !(victim.level() instanceof ServerLevel level)) {
			return;
		}
		PamConfig config = PamConfig.get();
		if (source.getEntity() instanceof LivingEntity attacker && trait(attacker) != null) {
			switch (trait(attacker)) {
				case FROST -> victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, config.frostSlownessSeconds * 20, 1));
				case BLAZING -> victim.igniteForSeconds(config.blazingFireSeconds);
				default -> {
				}
			}
		}
		if (victim instanceof Mob mob && trait(mob) == EliteTrait.SUMMONER && source.getEntity() instanceof ServerPlayer player
				&& !mob.hasAttached(PamAttachments.ELITE_SUMMONED)) {
			mob.setAttached(PamAttachments.ELITE_SUMMONED, true);
			summonHelpers(level, mob, player);
		}
	}

	private static void summonHelpers(ServerLevel level, Mob summoner, ServerPlayer target) {
		RandomSource random = level.getRandom();
		level.playSound(null, summoner.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, summoner.getSoundSource(), 1f, 1f);
		for (int i = 0; i < PamConfig.get().summonerHelpers; i++) {
			if (!(summoner.getType().create(level, EntitySpawnReason.REINFORCEMENT) instanceof Mob helper)) {
				return;
			}
			helper.snapTo(summoner.getX() + random.nextInt(5) - 2, summoner.getY(), summoner.getZ() + random.nextInt(5) - 2, random.nextFloat() * 360, 0);
			helper.finalizeSpawn(level, level.getCurrentDifficultyAt(helper.blockPosition()), EntitySpawnReason.REINFORCEMENT, null);
			helper.setTarget(target);
			level.addFreshEntityWithPassengers(helper);
		}
	}

	/** XP multiplier for an elite, growing with the rank it spawned at. */
	public static double xpMultiplier(LivingEntity entity) {
		return isElite(entity) ? PamConfig.get().eliteXpMultiplier.at(eliteRank(entity)) : 1;
	}
}
