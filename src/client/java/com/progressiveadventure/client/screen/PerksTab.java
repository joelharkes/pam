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
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Categories on the left, the selected category's perks on the right. Click a perk to buy it. */
class PerksTab implements AdventureTab {
	private static final int HEADER_HEIGHT = 22;
	private static final int CATEGORY_WIDTH = 96;
	private static final int ROW_HEIGHT = 22;

	private static PerkCategory selectedCategory = PerkCategory.MINING;

	// Layout of the last render, used for click handling
	private int listLeft;
	private int listTop;
	private int listWidth;
	private int categoryLeft;

	@Override
	public void init(AdventureScreen screen, int left, int top, int width, int height) {
		screen.addTabWidget(Button.builder(Component.translatable("screen.progressive_adventure.reset_perks"), b -> confirmReset(screen))
				.bounds(left + width - 90, top, 90, 18)
				.build());
	}

	private static void confirmReset(AdventureScreen screen) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
			if (confirmed) {
				ClientPlayNetworking.send(new ResetPerksPayload());
			}
			minecraft.gui.setScreen(screen);
		}, Component.translatable("screen.progressive_adventure.reset_perks.title"),
				Component.translatable("screen.progressive_adventure.reset_perks.message")));
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

		List<Perk> perks = Perks.inCategory(selectedCategory);
		for (int i = 0; i < perks.size(); i++) {
			Perk perk = perks.get(i);
			int y = listTop + i * ROW_HEIGHT;
			Perks.Status status = Perks.status(perk, progress, snapshot);
			boolean hovered = isInside(mouseX, mouseY, listLeft, y, listWidth, ROW_HEIGHT - 2);
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

			Component statusText = statusText(perk, status);
			int statusColor = switch (status) {
				case OWNED -> AdventureScreen.TEXT_GREEN;
				case BUYABLE -> AdventureScreen.TEXT_GOLD;
				case TOO_EXPENSIVE -> AdventureScreen.TEXT_RED;
				default -> AdventureScreen.TEXT_DIM;
			};
			graphics.text(font, statusText, listLeft + listWidth - 4 - font.width(statusText), y + 6, statusColor);

			if (hovered) {
				graphics.setComponentTooltipForNextFrame(font, tooltip(perk, status), mouseX, mouseY);
			}
		}
	}

	private static Component statusText(Perk perk, Perks.Status status) {
		return switch (status) {
			case OWNED -> Component.translatable("screen.progressive_adventure.perk.owned");
			case BUYABLE -> Component.translatable("screen.progressive_adventure.perk.buy", perk.cost());
			case TOO_EXPENSIVE -> Component.translatable("screen.progressive_adventure.perk.cost", perk.cost());
			case LOCKED -> perk.requirement().describe();
			case NEEDS_PREREQUISITE -> Component.translatable("screen.progressive_adventure.perk.needs", Perks.byId(perk.prerequisite()).name());
		};
	}

	private static List<Component> tooltip(Perk perk, Perks.Status status) {
		List<Component> lines = new ArrayList<>();
		lines.add(perk.name().copy().withStyle(ChatFormatting.GOLD));
		lines.add(perk.description().copy().withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("screen.progressive_adventure.perk.cost", perk.cost()).withStyle(ChatFormatting.LIGHT_PURPLE));
		if (status == Perks.Status.LOCKED) {
			lines.add(Component.translatable("screen.progressive_adventure.perk.requires", perk.requirement().describe()).withStyle(ChatFormatting.RED));
		} else if (status == Perks.Status.BUYABLE) {
			lines.add(Component.translatable("screen.progressive_adventure.perk.click_to_buy").withStyle(ChatFormatting.YELLOW));
		}
		return lines;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY) {
		PerkCategory[] categories = PerkCategory.values();
		for (int i = 0; i < categories.length; i++) {
			if (isInside(mouseX, mouseY, categoryLeft, listTop + i * ROW_HEIGHT, CATEGORY_WIDTH, ROW_HEIGHT - 2)) {
				selectedCategory = categories[i];
				return true;
			}
		}

		Minecraft minecraft = Minecraft.getInstance();
		List<Perk> perks = Perks.inCategory(selectedCategory);
		for (int i = 0; i < perks.size(); i++) {
			Perk perk = perks.get(i);
			if (isInside(mouseX, mouseY, listLeft, listTop + i * ROW_HEIGHT, listWidth, ROW_HEIGHT - 2)) {
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
