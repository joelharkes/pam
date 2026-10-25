package com.progressiveadventure.mixin;

import com.progressiveadventure.access.ArrowAccess;
import com.progressiveadventure.skill.Archery;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Piercing Shot and Ricochet (pierce level setter and base damage are private) and the in-flight arrow perks (Frost particles, Steady Aim). */
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

	@Inject(method = "tick", at = @At("HEAD"))
	private void pam$tickFlying(CallbackInfo ci) {
		AbstractArrow self = (AbstractArrow) (Object) this;
		if (self.level() instanceof ServerLevel level && !isInGround()) {
			Archery.tickFlyingArrow(level, self);
		}
	}
}
