package com.progressiveadventure.access;

/** Implemented on AbstractArrow by mixin: vanilla's pierce level setter and base damage are private. */
public interface ArrowAccess {
	void pam$addPierceLevel(int extra);

	double pam$getBaseDamage();
}
