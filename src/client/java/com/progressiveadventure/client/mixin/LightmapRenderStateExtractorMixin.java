package com.progressiveadventure.client.mixin;

import com.progressiveadventure.perk.Perks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cat's Eyes: a weak, always-on share of the Night Vision brightening, so dark places are less dark. */
@Mixin(LightmapRenderStateExtractor.class)
public class LightmapRenderStateExtractorMixin {
	/** Night vision strength by Cat's Eyes tier (vanilla Night Vision = 1). Client-side, so not in the config. */
	@Unique
	private static final float[] CATS_EYES_INTENSITY = {0.25f, 0.5f};

	@Inject(method = "extract", at = @At("TAIL"))
	private void pam$catsEyes(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (!renderState.needsUpdate || player == null) {
			return;
		}
		int tier = Perks.tier(player, Perks.CATS_EYES_1, Perks.CATS_EYES_2);
		if (tier > 0) {
			renderState.nightVisionEffectIntensity = Math.max(renderState.nightVisionEffectIntensity, CATS_EYES_INTENSITY[tier - 1]);
		}
	}
}
