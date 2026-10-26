package com.progressiveadventure.mixin;

import com.progressiveadventure.access.ArrowAccess;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.perk.ShieldPerks;
import com.progressiveadventure.skill.Archery;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Piercing Shot and Ricochet (pierce level setter and base damage are private), the in-flight arrow perks (Frost particles, Steady Aim) and Deflect. */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin implements ArrowAccess {
	@Shadow
	protected abstract boolean isInGround();

	@Shadow
	private double baseDamage;

	@Shadow
	public abstract byte getPierceLevel();

	@Shadow
	private void setPierceLevel(byte pierceLevel) {
	}

	@Override
	public void pam$addPierceLevel(int extra) {
		setPierceLevel((byte) Math.min(Byte.MAX_VALUE, getPierceLevel() + extra));
	}

	@Override
	public double pam$getBaseDamage() {
		return baseDamage;
	}

	/** Deflect: after vanilla bounced the arrow off a parrying shield, aim it back at the shooter. */
	@Inject(method = "onHitEntity", at = @At("TAIL"))
	private void pam$deflect(EntityHitResult hitResult, CallbackInfo ci) {
		AbstractArrow self = (AbstractArrow) (Object) this;
		if (self.hasAttached(PamAttachments.DEFLECTED_ARROW) && hitResult.getEntity() instanceof ServerPlayer player) {
			ShieldPerks.redirectArrow(self, player);
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void pam$tickFlying(CallbackInfo ci) {
		AbstractArrow self = (AbstractArrow) (Object) this;
		if (self.level() instanceof ServerLevel level && !isInGround()) {
			Archery.tickFlyingArrow(level, self);
		}
	}
}
