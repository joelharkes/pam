package com.progressiveadventure.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** The foods you ate as an icon grid in the stomach tooltip (registered in the client entrypoint). */
public record FoodsTooltip(Component title, List<ItemStack> foods) implements TooltipComponent {
	private static final int COLUMNS = 9;
	private static final int CELL = 18;
	private static final int TITLE_HEIGHT = 12;

	public static ClientTooltipComponent client(FoodsTooltip tooltip) {
		return new ClientTooltipComponent() {
			private int rows() {
				return (tooltip.foods().size() + COLUMNS - 1) / COLUMNS;
			}

			@Override
			public int getHeight(Font font) {
				return TITLE_HEIGHT + rows() * CELL + 2;
			}

			@Override
			public int getWidth(Font font) {
				return Math.max(font.width(tooltip.title()), Math.min(tooltip.foods().size(), COLUMNS) * CELL);
			}

			@Override
			public void extractText(GuiGraphicsExtractor graphics, Font font, int x, int y) {
				graphics.text(font, tooltip.title(), x, y, 0xFFAAAAAA);
			}

			@Override
			public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
				List<ItemStack> foods = tooltip.foods();
				for (int i = 0; i < foods.size(); i++) {
					int cellX = x + i % COLUMNS * CELL;
					int cellY = y + TITLE_HEIGHT + i / COLUMNS * CELL;
					graphics.fill(cellX, cellY, cellX + CELL - 1, cellY + CELL - 1, 0x40FFFFFF);
					graphics.item(foods.get(i), cellX, cellY);
				}
			}
		};
	}
}
