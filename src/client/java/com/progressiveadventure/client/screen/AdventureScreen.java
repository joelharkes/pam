package com.progressiveadventure.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** The K screen: character overview, perks, bestiary and achievements in one tabbed window. */
public class AdventureScreen extends Screen {
	static final int TEXT = 0xFFFFFFFF;
	static final int TEXT_DIM = 0xFFAAAAAA;
	static final int TEXT_GREEN = 0xFF55FF55;
	static final int TEXT_GOLD = 0xFFFFAA00;
	static final int TEXT_RED = 0xFFFF5555;
	static final int TEXT_AQUA = 0xFF55FFFF;
	static final int TEXT_PURPLE = 0xFFFF55FF;

	private static final int PANEL_WIDTH = 330;
	private static final int PANEL_HEIGHT = 250;
	private static final int TAB_BAR_HEIGHT = 26;

	private static final List<Component> TAB_TITLES = List.of(
			Component.translatable("screen.progressive_adventure.tab.character"),
			Component.translatable("screen.progressive_adventure.tab.perks"),
			Component.translatable("screen.progressive_adventure.tab.bestiary"),
			Component.translatable("screen.progressive_adventure.tab.achievements"));

	/** Remembered between openings. */
	private static int selectedTab = 0;

	/** Tooltip lines wider than this wrap (vanilla's button tooltips wrap at 170). */
	private static final int TOOLTIP_WIDTH = 200;

	/** A tooltip whose long lines wrap, so descriptions never run off the screen. Keeps each line's colour. */
	static void wrappedTooltip(GuiGraphicsExtractor graphics, Font font, List<Component> lines, int mouseX, int mouseY) {
		List<FormattedCharSequence> wrapped = new ArrayList<>();
		for (Component line : lines) {
			wrapped.addAll(font.split(line, TOOLTIP_WIDTH));
		}
		graphics.setTooltipForNextFrame(font, wrapped, mouseX, mouseY);
	}

	/** Called when leaving a world or server: a mob page from the last world shouldn't stay open in the next. */
	public static void onDisconnect() {
		BestiaryTab.forgetSelection();
		AchievementsTab.forgetSelection();
	}

	private final List<AdventureTab> tabs = List.of(new CharacterTab(), new PerksTab(), new BestiaryTab(), new AchievementsTab());
	private int left;
	private int top;
	private int panelWidth;
	private int panelHeight;
	private Button selectedTabButton;

	public AdventureScreen() {
		super(Component.translatable("screen.progressive_adventure.title"));
	}

	@Override
	protected void init() {
		panelWidth = Math.min(PANEL_WIDTH, width - 8);
		panelHeight = Math.min(PANEL_HEIGHT, height - 8);
		left = (width - panelWidth) / 2;
		top = (height - panelHeight) / 2;

		int tabWidth = (panelWidth - 16 - (TAB_TITLES.size() - 1) * 4) / TAB_TITLES.size();
		for (int i = 0; i < TAB_TITLES.size(); i++) {
			int index = i;
			Button button = addRenderableWidget(Button.builder(TAB_TITLES.get(i), b -> selectTab(index))
					.bounds(left + 8 + i * (tabWidth + 4), top + 5, tabWidth, 18)
					.build());
			button.active = i != selectedTab;
			if (i == selectedTab) {
				selectedTabButton = button;
			}
		}
		tabs.get(selectedTab).init(this, contentLeft(), contentTop(), contentWidth(), contentHeight());
	}

	private void selectTab(int index) {
		selectedTab = index;
		tabs.get(index).reset();
		rebuildWidgets();
	}

	/** Lets tabs add their own widgets. */
	<T extends GuiEventListener & Renderable & NarratableEntry> T addTabWidget(T widget) {
		return addRenderableWidget(widget);
	}

	void refresh() {
		rebuildWidgets();
	}

	private int contentLeft() {
		return left + 8;
	}

	private int contentTop() {
		return top + TAB_BAR_HEIGHT + 4;
	}

	private int contentWidth() {
		return panelWidth - 16;
	}

	private int contentHeight() {
		return panelHeight - TAB_BAR_HEIGHT - 10;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xE0101018);
		graphics.outline(left, top, panelWidth, panelHeight, 0xFF6B6B7A);
		graphics.fill(left + 4, top + TAB_BAR_HEIGHT, left + panelWidth - 4, top + TAB_BAR_HEIGHT + 1, 0xFF3A3A48);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		tabs.get(selectedTab).render(graphics, contentLeft(), contentTop(), contentWidth(), contentHeight(), mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		// The current tab's button is greyed out to mark it, so it doesn't take the click itself
		if (event.button() == 0 && PerksTab.isInside(event.x(), event.y(), selectedTabButton.getX(), selectedTabButton.getY(),
				selectedTabButton.getWidth(), selectedTabButton.getHeight())) {
			selectedTabButton.playDownSound(minecraft.getSoundManager());
			selectTab(selectedTab);
			return true;
		}
		return event.button() == 0 && tabs.get(selectedTab).mouseClicked(event.x(), event.y());
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return tabs.get(selectedTab).mouseScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
