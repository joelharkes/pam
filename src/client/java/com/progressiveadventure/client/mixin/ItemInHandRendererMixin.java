package com.progressiveadventure.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.progressiveadventure.client.JournalClient;
import com.progressiveadventure.journal.JournalContent;
import com.progressiveadventure.network.JournalMarkersPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Holding an Explorer's Journal shows its map in first person, like a vanilla map: the "is this a map?" check also
 * accepts the journal, and the map drawing itself uses the journal's texture and markers instead of map data.
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
	@Unique
	private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/map/map_background.png");
	@Unique
	private final MapRenderState pam$journalState = new MapRenderState();

	@WrapOperation(method = "submitArmWithItem", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z"))
	private boolean pam$journalIsMap(ItemStack stack, DataComponentType<?> type, Operation<Boolean> original) {
		return original.call(stack, type) || stack.is(JournalContent.EXPLORERS_JOURNAL);
	}

	@Inject(method = "renderMap", at = @At("HEAD"), cancellable = true)
	private void pam$renderJournal(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, ItemStack stack, CallbackInfo ci) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (!stack.is(JournalContent.EXPLORERS_JOURNAL) || player == null) {
			return;
		}
		ci.cancel();
		// Same placement as vanilla's renderMap
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
		poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
		poseStack.scale(0.38F, 0.38F, 0.38F);
		poseStack.translate(-0.5F, -0.5F, 0.0F);
		poseStack.scale(0.0078125F, 0.0078125F, 0.0078125F);
		submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.text(BACKGROUND), (pose, buffer) -> {
			buffer.addVertex(pose, -7.0F, 135.0F, 0.0F).setColor(-1).setUv(0.0F, 1.0F).setLight(lightCoords);
			buffer.addVertex(pose, 135.0F, 135.0F, 0.0F).setColor(-1).setUv(1.0F, 1.0F).setLight(lightCoords);
			buffer.addVertex(pose, 135.0F, -7.0F, 0.0F).setColor(-1).setUv(1.0F, 0.0F).setLight(lightCoords);
			buffer.addVertex(pose, -7.0F, -7.0F, 0.0F).setColor(-1).setUv(0.0F, 0.0F).setLight(lightCoords);
		});
		if (player.level().dimension() != Level.OVERWORLD) {
			return;
		}
		MapRenderState state = pam$journalState;
		state.texture = JournalClient.heldTexture(player);
		state.decorations.clear();
		double half = JournalClient.HELD_BLOCKS / 2.0;
		for (JournalMarkersPayload.Marker marker : JournalClient.markers()) {
			double dx = marker.x() + 0.5 - player.getX();
			double dz = marker.z() + 0.5 - player.getZ();
			if (Math.abs(dx) < half && Math.abs(dz) < half) {
				state.decorations.add(pam$decoration(JournalClient.spriteOf(marker), dx, dz, (byte) 0));
			}
		}
		boolean compass = JournalClient.hasCompass(player);
		state.decorations.add(pam$decoration(compass ? "player" : "player_off_map", 0, 0,
				compass ? JournalClient.mapRotation(player.getYRot()) : 0));
		Minecraft.getInstance().getMapRenderer().render(state, poseStack, submitNodeCollector, false, lightCoords);
	}

	/** dx/dz in blocks from the middle; map decorations use 2 units per map pixel. */
	@Unique
	private static MapRenderState.MapDecorationRenderState pam$decoration(String sprite, double dx, double dz, byte rotation) {
		MapRenderState.MapDecorationRenderState decoration = new MapRenderState.MapDecorationRenderState();
		decoration.atlasSprite = JournalClient.sprite(sprite);
		decoration.x = (byte) Math.clamp(Math.round(dx * 2), -128, 127);
		decoration.y = (byte) Math.clamp(Math.round(dz * 2), -128, 127);
		decoration.rot = rotation;
		return decoration;
	}
}
