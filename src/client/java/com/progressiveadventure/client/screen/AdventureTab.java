package com.progressiveadventure.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** One tab of the Adventure screen. Coordinates are absolute screen coordinates. */
interface AdventureTab {
	/** Called whenever the screen is (re)initialised; add any widgets here. */
	default void init(AdventureScreen screen, int left, int top, int width, int height) {
	}

	/** Clicked in the tab bar: back to how the tab first looks. Closing and reopening the screen keeps everything instead. */
	default void reset() {
	}

	void render(GuiGraphicsExtractor graphics, int left, int top, int width, int height, int mouseX, int mouseY);

	default boolean mouseClicked(double mouseX, double mouseY) {
		return false;
	}

	default boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
		return false;
	}
}
