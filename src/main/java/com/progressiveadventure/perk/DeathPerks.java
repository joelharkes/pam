package com.progressiveadventure.perk;

import com.progressiveadventure.config.PamConfig;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Perks that soften a death. Keepsake: what the tier keeps stays in the dead player's inventory, and is moved to the new
 * player on respawn. Way Back: a trail to where you died. Faithful Friend: a pet that would die runs back to you instead.
 */
public final class DeathPerks {
	private static final int TRAIL_COLOR = 0xFF5555;
	private static final int TRAIL_PARTICLES = 10;
	private static final double TRAIL_SPACING = 0.7;
	private static final double ARRIVED_DISTANCE = 4;
	private static final double PET_MAX_DISTANCE = 16;

	/** Game time at which each player's Way Back trail ends. */
	private static final Map<UUID, Long> TRAIL_ENDS = new HashMap<>();

	private DeathPerks() {
	}

	private static int keepsakeTier(ServerPlayer player) {
		return Perks.tier(player, Perks.KEEPSAKE_1, Perks.KEEPSAKE_2, Perks.KEEPSAKE_3);
	}

	/** Slots are 0-8 hotbar, 9-35 backpack, 36-39 armor, 40 offhand. */
	private static boolean keeps(int tier, int slot) {
		boolean armor = slot >= 36 && slot <= 39;
		boolean hotbar = slot < Inventory.SELECTION_SIZE || slot == Inventory.SLOT_OFFHAND;
		return switch (tier) {
			case 1 -> armor;
			case 2 -> armor || hotbar;
			default -> tier >= 3;
		};
	}

	/** Replaces vanilla's dropAll for a player with Keepsake. Returns false when vanilla's should run. */
	public static boolean dropAllButKept(ServerPlayer player) {
		int tier = keepsakeTier(player);
		if (tier == 0) {
			return false;
		}
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (!keeps(tier, slot)) {
				ItemStack stack = inventory.removeItemNoUpdate(slot);
				if (!stack.isEmpty()) {
					player.drop(stack, true, false);
				}
			}
		}
		return true;
	}

	public static boolean keepsXp(ServerPlayer player) {
		return keepsakeTier(player) >= 3;
	}

	/** On respawn after a death. Everything still in the old inventory was kept by Keepsake. */
	public static void restoreKept(ServerPlayer newPlayer, ServerPlayer oldPlayer) {
		if (newPlayer.level().getGameRules().get(GameRules.KEEP_INVENTORY) || oldPlayer.isSpectator()) {
			return; // vanilla already moved everything
		}
		Inventory oldInventory = oldPlayer.getInventory();
		Inventory newInventory = newPlayer.getInventory();
		for (int slot = 0; slot < oldInventory.getContainerSize(); slot++) {
			ItemStack stack = oldInventory.getItem(slot);
			if (!stack.isEmpty()) {
				newInventory.setItem(slot, stack);
			}
		}
		if (keepsXp(oldPlayer)) {
			newPlayer.experienceLevel = oldPlayer.experienceLevel;
			newPlayer.totalExperience = oldPlayer.totalExperience;
			newPlayer.experienceProgress = oldPlayer.experienceProgress;
		}
	}

	public static void onRespawn(ServerPlayer player) {
		if (!Perks.has(player, Perks.WAY_BACK) || player.getLastDeathLocation().isEmpty()) {
			return;
		}
		int seconds = PamConfig.get().wayBackSeconds;
		TRAIL_ENDS.put(player.getUUID(), player.level().getServer().overworld().getGameTime() + seconds * 20L);
		GlobalPos death = player.getLastDeathLocation().get();
		String key = death.dimension() == player.level().dimension() ? "way_back" : "way_back_other_dimension";
		player.sendSystemMessage(Component.translatable("message.progressive_adventure." + key,
				death.pos().getX(), death.pos().getY(), death.pos().getZ(), Math.round(seconds / 60f)));
	}

	/** Runs once per second. */
	public static void tick(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		Iterator<Map.Entry<UUID, Long>> entries = TRAIL_ENDS.entrySet().iterator();
		while (entries.hasNext()) {
			Map.Entry<UUID, Long> entry = entries.next();
			if (now >= entry.getValue()) {
				entries.remove();
				continue;
			}
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player != null && player.isAlive() && showTrail(player)) {
				entries.remove();
			}
		}
	}

	/** Shows the trail to this player only. Returns true when they arrived. */
	private static boolean showTrail(ServerPlayer player) {
		GlobalPos death = player.getLastDeathLocation().orElse(null);
		if (death == null || death.dimension() != player.level().dimension()) {
			return false;
		}
		Vec3 eyes = player.getEyePosition();
		Vec3 toDeath = Vec3.atCenterOf(death.pos()).subtract(eyes);
		if (toDeath.length() < ARRIVED_DISTANCE) {
			return true;
		}
		Vec3 direction = toDeath.normalize();
		DustParticleOptions particle = new DustParticleOptions(TRAIL_COLOR, 1.2f);
		for (int i = 0; i < TRAIL_PARTICLES; i++) {
			Vec3 point = eyes.add(direction.scale(1 + i * TRAIL_SPACING));
			player.level().sendParticles(player, particle, false, false, point.x, point.y - 0.3, point.z, 1, 0, 0, 0, 0);
		}
		return false;
	}

	/**
	 * Faithful Friend, where vanilla checks for a totem. The owner must be online in the same dimension: only they can
	 * bring the pet somewhere safe, and a pet left in lava would otherwise faint over and over. /kill still kills.
	 */
	public static boolean savePet(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof TamableAnimal pet) || !(pet.getOwner() instanceof ServerPlayer owner)
				|| owner.level() != pet.level() || source.is(DamageTypes.GENERIC_KILL) || !Perks.has(owner, Perks.FAITHFUL_FRIEND)) {
			return false;
		}
		PamConfig config = PamConfig.get();
		int ticks = config.faithfulFriendProtectSeconds * 20;
		pet.removeAllEffects();
		pet.clearFire();
		pet.setHealth(config.faithfulFriendHealth);
		pet.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 1));
		pet.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, 3));
		pet.setTarget(null);
		pet.getNavigation().stop();
		pet.setOrderedToSit(true);
		pet.setInSittingPose(true);

		pet.tryToTeleportToOwner();
		if (pet.distanceTo(owner) > PET_MAX_DISTANCE) {
			pet.teleportTo(owner.getX(), owner.getY(), owner.getZ()); // no safe spot found next to the owner
		}
		pet.resetFallDistance();
		ServerLevel level = owner.level();
		level.sendParticles(ParticleTypes.HEART, pet.getX(), pet.getEyeY(), pet.getZ(), 5, 0.4, 0.3, 0.4, 0);
		owner.sendSystemMessage(Component.translatable("message.progressive_adventure.faithful_friend", pet.getDisplayName()));
		return true;
	}

	public static void onServerStopped() {
		TRAIL_ENDS.clear();
	}
}
