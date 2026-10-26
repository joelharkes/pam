package com.progressiveadventure.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

/** "New bestiary entry!" with the mob's spawn egg, styled like an advancement toast. */
public class BestiaryToast implements Toast {
	private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/advancement");
	private static final Component TITLE = Component.translatable("toast.progressive_adventure.bestiary_new");
	private static final long DISPLAY_TIME = 5000L;

	private final Component name;
	private final ItemStack icon;
	private Visibility wantedVisibility = Visibility.HIDE;

	public BestiaryToast(EntityType<?> type) {
		this.name = type.getDescription();
		this.icon = SpawnEggItem.byId(type).map(ItemStack::new).orElseGet(() -> new ItemStack(Items.BOOK));
	}

	@Override
	public Visibility getWantedVisibility() {
		return wantedVisibility;
	}

	@Override
	public void update(ToastManager manager, long fullyVisibleForMs) {
		wantedVisibility = fullyVisibleForMs >= DISPLAY_TIME * manager.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, width(), height());
		graphics.text(font, TITLE, 30, 7, 0xFFFFFF00, false);
		graphics.text(font, name, 30, 18, 0xFFFFFFFF, false);
		graphics.fakeItem(icon, 8, 8);
	}
}
