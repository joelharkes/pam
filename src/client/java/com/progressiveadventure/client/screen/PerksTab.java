package com.progressiveadventure.client.screen;

import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.PlayerProgress;
import com.progressiveadventure.data.ProgressionSnapshot;
import com.progressiveadventure.network.BuyPerkPayload;
import com.progressiveadventure.network.ResetPerksPayload;
import com.progressiveadventure.perk.Perk;
import com.progressiveadventure.perk.PerkCategory;
import com.progressiveadventure.perk.Perks;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Categories on the left, the selected category's perks on the right (scrollable). Click a perk to buy it.
 * Rows stay short (an icon or the cost); the details, including what blocks a perk, are in the row tooltip.
 */
class PerksTab implements AdventureTab {
	private static final Identifier LOCK = Identifier.withDefaultNamespace("container/cartography_table/locked");
	private static final Identifier CHECKMARK = Identifier.withDefaultNamespace("icon/checkmark");
	private static final int HEADER_HEIGHT = 22;
	private static final int CATEGORY_WIDTH = 96;
	private static final int ROW_HEIGHT = 22;

	private static PerkCategory selectedCategory = PerkCategory.MINING;
	private static int scroll = 0;

	// Layout of the last render, used for click handling
	private int listLeft;
	private int listTop;
	private int listWidth;
	private int listHeight;
	private int categoryLeft;

	@Override
	public void init(AdventureScreen screen, int left, int top, int width, int height) {
		screen.addTabWidget(Button.builder(Component.translatable("screen.progressive_adventure.reset_perks"), b -> ClientPlayNetworking.send(new ResetPerksPayload()))
				.bounds(left + width - 90, top, 90, 18)
				.tooltip(Tooltip.create(Component.translatable("screen.progressive_adventure.reset_perks.tooltip")))
				.build());
	}

	@Override
	public void render(GuiGraphicsExtractor graphics, int left, int top, int width, int height, int mouseX, int mouseY) {
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		PlayerProgress progress = PamAttachments.progress(minecraft.player);
		ProgressionSnapshot snapshot = PamAttachments.snapshot(minecraft.player);

		graphics.text(font, Component.translatable("screen.progressive_adventure.perk_points",
				Perks.pointsAvailable(progress, snapshot)), left, top + 5, AdventureScreen.TEXT_PURPLE);

		categoryLeft = left;
		listLeft = left + CATEGORY_WIDTH + 6;
		listTop = top + HEADER_HEIGHT;
		listWidth = width - CATEGORY_WIDTH - 6;
		listHeight = height - HEADER_HEIGHT;
		List<Perk> perks = Perks.inCategory(selectedCategory);
		int maxScroll = maxScroll(perks.size());
		scroll = Math.clamp(scroll, 0, maxScroll);
		if (maxScroll > 0) {
			listWidth -= 5; // room for the scrollbar
		}

		PerkCategory[] categories = PerkCategory.values();
		for (int i = 0; i < categories.length; i++) {
			PerkCategory category = categories[i];
			int y = listTop + i * ROW_HEIGHT;
			boolean hovered = isInside(mouseX, mouseY, categoryLeft, y, CATEGORY_WIDTH, ROW_HEIGHT - 2);
			int background = category == selectedCategory ? 0xFF3C3C5A : hovered ? 0xFF2A2A3A : 0xFF1C1C26;
			graphics.fill(categoryLeft, y, categoryLeft + CATEGORY_WIDTH, y + ROW_HEIGHT - 2, background);
			graphics.item(new ItemStack(category.icon), categoryLeft + 2, y + 2);
			graphics.text(font, Component.translatable("perk_category.progressive_adventure." + category.id),
					categoryLeft + 22, y + 6, category == selectedCategory ? AdventureScreen.TEXT_GOLD : AdventureScreen.TEXT);
		}

		boolean mouseInList = isInside(mouseX, mouseY, listLeft, listTop, listWidth, listHeight);
		graphics.enableScissor(listLeft, listTop, listLeft + listWidth, listTop + listHeight);
		for (int i = 0; i < perks.size(); i++) {
			Perk perk = perks.get(i);
			int y = listTop + i * ROW_HEIGHT - scroll;
			Perks.Status status = Perks.status(perk, progress, snapshot);
			boolean hovered = mouseInList && isInside(mouseX, mouseY, listLeft, y, listWidth, ROW_HEIGHT - 2);
			int background = switch (status) {
				case OWNED -> 0xFF1E3A1E;
				case BUYABLE -> hovered ? 0xFF4A3F1A : 0xFF33301C;
				default -> 0xFF1C1C26;
			};
			graphics.fill(listLeft, y, listLeft + listWidth, y + ROW_HEIGHT - 2, background);
			graphics.item(new ItemStack(perk.icon()), listLeft + 2, y + 2);
			int nameColor = status == Perks.Status.OWNED ? AdventureScreen.TEXT_GREEN
					: status == Perks.Status.BUYABLE || status == Perks.Status.TOO_EXPENSIVE ? AdventureScreen.TEXT : AdventureScreen.TEXT_DIM;
			graphics.text(font, perk.name(), listLeft + 22, y + 6, nameColor);

			int right = listLeft + listWidth - 4;
			switch (status) {
				case OWNED -> graphics.blitSprite(RenderPipelines.GUI_TEXTURED, CHECKMARK, right - 9, y + 6, 9, 8);
				case LOCKED, NEEDS_PREREQUISITE -> graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LOCK, right - 10, y + 3, 10, 14);
				case BUYABLE, TOO_EXPENSIVE -> {
					Component cost = Component.translatable("screen.progressive_adventure.perk.points", perk.cost());
					graphics.text(font, cost, right - font.width(cost), y + 6,
							status == Perks.Status.BUYABLE ? AdventureScreen.TEXT_GOLD : AdventureScreen.TEXT_RED);
				}
			}

			if (hovered) {
				graphics.setComponentTooltipForNextFrame(font, tooltip(perk, status, progress, snapshot), mouseX, mouseY);
			}
		}
		graphics.disableScissor();

		if (maxScroll > 0) {
			int barX = listLeft + listWidth + 2;
			int thumbHeight = Math.max(12, listHeight * listHeight / (listHeight + maxScroll));
			int thumbY = listTop + (listHeight - thumbHeight) * scroll / maxScroll;
			graphics.fill(barX, listTop, barX + 3, listTop + listHeight, 0xFF1C1C26);
			graphics.fill(barX, thumbY, barX + 3, thumbY + thumbHeight, 0xFF8888A0);
		}
	}

	private int maxScroll(int perkCount) {
		return Math.max(0, perkCount * ROW_HEIGHT - 2 - listHeight);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
		if (!isInside(mouseX, mouseY, listLeft, listTop, listWidth + 5, listHeight)) {
			return false;
		}
		scroll = Math.clamp(scroll - (int) Math.round(scrollY * ROW_HEIGHT), 0, maxScroll(Perks.inCategory(selectedCategory).size()));
		return true;
	}

	private static List<Component> tooltip(Perk perk, Perks.Status status, PlayerProgress progress, ProgressionSnapshot snapshot) {
		List<Component> lines = new ArrayList<>();
		lines.add(perk.name().copy().withStyle(ChatFormatting.GOLD));
		lines.add(perk.description().copy().withStyle(ChatFormatting.GRAY));
		switch (status) {
			case OWNED -> lines.add(Component.translatable("screen.progressive_adventure.perk.owned").withStyle(ChatFormatting.GREEN));
			case BUYABLE -> lines.add(Component.translatable("screen.progressive_adventure.perk.click_to_buy", perk.cost()).withStyle(ChatFormatting.YELLOW));
			case TOO_EXPENSIVE -> lines.add(Component.translatable("screen.progressive_adventure.perk.too_expensive", perk.cost()).withStyle(ChatFormatting.RED));
			case LOCKED, NEEDS_PREREQUISITE -> {
				// List everything still missing, not just the first blocker
				if (perk.prerequisite() != null && !progress.perks().contains(perk.prerequisite())) {
					lines.add(Component.translatable("screen.progressive_adventure.perk.requires",
							Perks.byId(perk.prerequisite()).name()).withStyle(ChatFormatting.RED));
				}
				if (!perk.requirement().isMet(progress, snapshot)) {
					lines.add(Component.translatable("screen.progressive_adventure.perk.requires",
							perk.requirement().describe()).withStyle(ChatFormatting.RED));
				}
				lines.add(Component.translatable("screen.progressive_adventure.perk.cost", perk.cost()).withStyle(ChatFormatting.DARK_GRAY));
			}
		}
		return lines;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY) {
		PerkCategory[] categories = PerkCategory.values();
		for (int i = 0; i < categories.length; i++) {
			if (isInside(mouseX, mouseY, categoryLeft, listTop + i * ROW_HEIGHT, CATEGORY_WIDTH, ROW_HEIGHT - 2)) {
				selectedCategory = categories[i];
				scroll = 0;
				return true;
			}
		}

		if (!isInside(mouseX, mouseY, listLeft, listTop, listWidth, listHeight)) {
			return false;
		}
		Minecraft minecraft = Minecraft.getInstance();
		List<Perk> perks = Perks.inCategory(selectedCategory);
		for (int i = 0; i < perks.size(); i++) {
			Perk perk = perks.get(i);
			if (isInside(mouseX, mouseY, listLeft, listTop + i * ROW_HEIGHT - scroll, listWidth, ROW_HEIGHT - 2)) {
				Perks.Status status = Perks.status(perk, PamAttachments.progress(minecraft.player), PamAttachments.snapshot(minecraft.player));
				if (status == Perks.Status.BUYABLE) {
					ClientPlayNetworking.send(new BuyPerkPayload(perk.id()));
				}
				return true;
			}
		}
		return false;
	}

	static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
