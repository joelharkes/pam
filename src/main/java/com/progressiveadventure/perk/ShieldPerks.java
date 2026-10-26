package com.progressiveadventure.perk;

import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.skill.Archery;
import com.progressiveadventure.skill.Skill;
import com.progressiveadventure.skill.Skills;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shield perks (Defense). Parry and Deflect act when a shield blocks a hit right after it was raised; Shield Bash is
 * an attack click while blocking (vanilla ignores those, so the client sends {@code ShieldBashPayload}).
 */
public final class ShieldPerks {
	private static final Map<UUID, Long> BASH_COOLDOWNS = new HashMap<>();

	private ShieldPerks() {
	}

	public static void onServerStopped() {
		BASH_COOLDOWNS.clear();
	}

	/**
	 * Called when the player's shield blocked some damage. Within the parry window (just after the shield starts
	 * blocking): Parry sends a melee hit back to the attacker, Deflect marks an arrow so it flies back to the shooter.
	 */
	public static void onBlocked(ServerPlayer player, ServerLevel level, DamageSource source, float damage) {
		if (!inParryWindow(player)) {
			return;
		}
		PamConfig config = PamConfig.get();
		if (source.getDirectEntity() instanceof AbstractArrow arrow) {
			if (Perks.has(player, Perks.DEFLECT) && arrow.getOwner() instanceof LivingEntity) {
				arrow.setAttached(PamAttachments.DEFLECTED_ARROW, true); // aimed after vanilla's bounce, see redirectArrow
				parryEffects(player, level, arrow.position());
			}
			return;
		}
		// Explosions have the creeper as direct entity: those can't be parried
		if (!Perks.has(player, Perks.PARRY) || source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypeTags.IS_EXPLOSION)
				|| !(source.getDirectEntity() instanceof LivingEntity attacker) || attacker == player) {
			return;
		}
		// Thorns damage: credited to the player (bestiary, loot) without counting as a Melee hit
		attacker.hurtServer(level, player.damageSources().thorns(player), Math.min(damage, config.parryMaxDamage));
		attacker.knockback(config.parryKnockback, player.getX() - attacker.getX(), player.getZ() - attacker.getZ(), source, damage);
		attacker.hurtMarked = true;
		parryEffects(player, level, attacker.position().add(0, attacker.getBbHeight() * 0.6, 0));
		Skills.addXp(player, Skill.MELEE, config.parryXp);
	}

	/** Whether the shield started blocking at most parryWindowTicks ago (it only blocks after its block delay). */
	private static boolean inParryWindow(ServerPlayer player) {
		ItemStack shield = player.getItemBlockingWith();
		BlocksAttacks blocksAttacks = shield == null ? null : shield.get(DataComponents.BLOCKS_ATTACKS);
		return blocksAttacks != null
				&& player.getTicksUsingItem() <= blocksAttacks.blockDelayTicks() + PamConfig.get().parryWindowTicks;
	}

	private static void parryEffects(ServerPlayer player, ServerLevel level, Vec3 at) {
		level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 10, 0.3, 0.3, 0.3, 0.3);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.4f, 1.8f);
		player.sendOverlayMessage(Component.translatable("message.progressive_adventure.parry").withStyle(ChatFormatting.GOLD));
	}

	/**
	 * Deflect: after vanilla bounced a marked arrow off the shield, aim it at whoever shot it. It now belongs to the
	 * player, so the hit counts as theirs.
	 */
	public static void redirectArrow(AbstractArrow arrow, ServerPlayer player) {
		arrow.removeAttached(PamAttachments.DEFLECTED_ARROW);
		if (arrow.isRemoved() || !(arrow.getOwner() instanceof LivingEntity shooter) || !shooter.isAlive() || shooter == player) {
			return;
		}
		double speed = PamConfig.get().deflectArrowSpeed;
		Vec3 target = shooter.getBoundingBox().getCenter();
		Vec3 start = arrow.position();
		double flightTicks = target.distanceTo(start) / speed;
		// Aim a little high to make up for gravity (0.05 blocks/tick²)
		Vec3 aim = target.add(0, 0.5 * 0.05 * flightTicks * flightTicks, 0).subtract(start).normalize();
		arrow.setOwner(player);
		arrow.setDeltaMovement(aim.scale(speed));
		arrow.needsSync = true;
	}

	/** Shield Bash: the client saw an attack click while blocking. Pushes hostile mobs in front of the player away. */
	public static void onBash(ServerPlayer player) {
		if (!Perks.has(player, Perks.SHIELD_BASH) || !player.isBlocking() || !(player.level() instanceof ServerLevel level)) {
			return;
		}
		PamConfig config = PamConfig.get();
		long now = level.getGameTime();
		Long readyAt = BASH_COOLDOWNS.get(player.getUUID());
		if (readyAt != null && now < readyAt) {
			return;
		}
		BASH_COOLDOWNS.put(player.getUUID(), now + config.shieldBashCooldownTicks);

		Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
		for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(config.shieldBashRange), mob -> Archery.isAimTarget(mob, player))) {
			Vec3 toMob = mob.position().subtract(player.position()).multiply(1, 0, 1);
			double distance = toMob.length();
			if (distance > config.shieldBashRange || (distance > 0.5 && look.dot(toMob.scale(1 / distance)) < 0.5)) {
				continue; // only in front of the player (60° to each side)
			}
			Vec3 push = distance > 0.01 ? toMob.scale(config.shieldBashKnockback / distance) : look.scale(config.shieldBashKnockback);
			mob.push(push.x, 0.3, push.z);
			mob.hurtMarked = true;
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 1f, 0.7f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1f, 1f);
	}
}
