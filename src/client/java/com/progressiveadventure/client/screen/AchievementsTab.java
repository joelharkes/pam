package com.progressiveadventure.client.screen;

import com.progressiveadventure.client.AchievementsClient;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.PlayerProgress;
import com.progressiveadventure.network.AchievementListPayload;
import com.progressiveadventure.network.AchievementListPayload.Entry;
import com.progressiveadventure.network.AchievementListPayload.Group;
import com.progressiveadventure.waypoint.Waypoints;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Grid of every achievement, one block each, with a free unnamed tip of the day. Clicking a block opens its page: what it
 * asks, tips to buy with XP (free once it's done) and which parts are still missing.
 */
class AchievementsTab implements AdventureTab {
	private static final int CELL = 20;
	private static final int HEADER_HEIGHT = 22;
	private static final int HEADING_HEIGHT = 12;
	private static final int GROUP_GAP = 3;
	private static final int SECTION_GAP = 6;
	private static final int BUY_BUTTON_HEIGHT = 18;
	private static final int SCROLL_STEP = 20;

	private static boolean notDoneOnly = false;
	private static int gridScroll = 0;
	/** Id of the open page, or null for the grid. Remembered between openings (not between worlds). */
	private static String selected = null;

	static void forgetSelection() {
		notDoneOnly = false;
		gridScroll = 0;
		selected = null;
	}

	private record Cell(String id, int x, int y) {
	}

	private final List<Cell> cells = new ArrayList<>();
	private AdventureScreen screen;
	private boolean requested;
	private int pageScroll;
	private int gridMaxScroll;
	private int pageMaxScroll;
	private int viewTop;
	private int viewBottom;
	/** The buy button as drawn last frame; {@code buyTip} is 0 when there is none. */
	private int buyTip;
	private int buyX;
	private int buyY;
	private int buyWidth;
	private boolean buyAffordable;

	@Override
	public void init(AdventureScreen screen, int left, int top, int width, int height) {
		this.screen = screen;
		if (!requested) {
			requested = true;
			AchievementsClient.request();
		}
		if (selected != null) {
			screen.addTabWidget(Button.builder(Component.translatable("screen.progressive_adventure.bestiary.back"), b -> select(null))
					.bounds(left, top, 50, 18).build());
			screen.addTabWidget(Button.builder(Component.literal("<"), b -> step(-1)).bounds(left + width - 44, top, 20, 18).build());
			screen.addTabWidget(Button.builder(Component.literal(">"), b -> step(1)).bounds(left + width - 20, top, 20, 18).build());
			return;
		}
		screen.addTabWidget(Button.builder(Component.translatable(notDoneOnly ? "screen.progressive_adventure.achievements.show_not_done"
				: "screen.progressive_adventure.achievements.show_all"), b -> {
					notDoneOnly = !notDoneOnly;
					gridScroll = 0;
					screen.refresh();
				}).bounds(left + width - 90, top, 90, 18).build());
	}

	private void select(String id) {
		selected = id;
		pageScroll = 0;
		screen.refresh();
	}

	/** Previous or next block of the grid as it's filtered now. */
	private void step(int direction) {
		AchievementListPayload list = AchievementsClient.list();
		if (list == null) {
			return;
		}
		List<Entry> visible = new ArrayList<>();
		for (Group group : list.groups()) {
			visible.addAll(filtered(group));
		}
		if (visible.isEmpty()) {
			return;
		}
		int index = 0;
		for (int i = 0; i < visible.size(); i++) {
			if (visible.get(i).id().equals(selected)) {
				index = i;
			}
		}
		select(visible.get(Math.floorMod(index + direction, visible.size())).id());
	}

	private static List<Entry> filtered(Group group) {
		return group.entries().stream().filter(entry -> !notDoneOnly || !entry.done()).toList();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY) {
		if (mouseY < viewTop || mouseY >= viewBottom) {
			return false;
		}
		if (selected != null) {
			if (buyTip > 0 && buyAffordable && PerksTab.isInside(mouseX, mouseY, buyX, buyY, buyWidth, BUY_BUTTON_HEIGHT)) {
				Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
				AchievementsClient.buyTip(selected, buyTip);
				return true;
			}
			return false;
		}
		for (Cell cell : cells) {
			if (PerksTab.isInside(mouseX, mouseY, cell.x(), cell.y(), CELL - 2, CELL - 2)) {
				select(cell.id());
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
		if (mouseY < viewTop || mouseY >= viewBottom) {
			return false;
		}
		int step = (int) Math.round(scrollY * SCROLL_STEP);
		if (selected != null) {
			pageScroll = Math.clamp(pageScroll - step, 0, pageMaxScroll);
		} else {
			gridScroll = Math.clamp(gridScroll - step, 0, gridMaxScroll);
		}
		return true;
	}

	@Override
	public void render(GuiGraphicsExtractor graphics, int left, int top, int width, int height, int mouseX, int mouseY) {
		Font font = Minecraft.getInstance().font;
		AchievementListPayload list = AchievementsClient.list();
		cells.clear();
		buyTip = 0;
		if (list == null) {
			viewTop = viewBottom = top;
			graphics.text(font, Component.translatable("screen.progressive_adventure.bestiary.loading"), left, top + 5, AdventureScreen.TEXT_DIM);
			return;
		}
		Entry entry = selected == null ? null : find(list, selected);
		if (entry != null) {
			renderPage(graphics, font, list, entry, left, top, width, height, mouseX, mouseY);
		} else {
			renderGrid(graphics, font, list, left, top, width, height, mouseX, mouseY);
		}
	}

	private static Entry find(AchievementListPayload list, String id) {
		for (Group group : list.groups()) {
			for (Entry entry : group.entries()) {
				if (entry.id().equals(id)) {
					return entry;
				}
			}
		}
		return null;
	}

	private void renderGrid(GuiGraphicsExtractor graphics, Font font, AchievementListPayload list, int left, int top, int width, int height,
			int mouseX, int mouseY) {
		int done = 0;
		int total = 0;
		for (Group group : list.groups()) {
			done += (int) group.entries().stream().filter(Entry::done).count();
			total += group.entries().size();
		}
		graphics.text(font, Component.translatable("screen.progressive_adventure.achievements.done", done, total), left, top + 5, AdventureScreen.TEXT_GOLD);

		viewTop = renderTipOfTheDay(graphics, font, list, left, top + HEADER_HEIGHT, width) + 4;
		viewBottom = top + height;
		int columns = Math.max(1, (width - 5) / CELL);

		graphics.enableScissor(left, viewTop, left + width, viewBottom);
		int y = viewTop - gridScroll;
		boolean any = false;
		for (Group group : list.groups()) {
			List<Entry> entries = filtered(group);
			if (entries.isEmpty()) {
				continue;
			}
			any = true;
			long groupDone = group.entries().stream().filter(Entry::done).count();
			graphics.text(font, Component.translatable("screen.progressive_adventure.achievements.group", group.title(), groupDone, group.entries().size()),
					left, y + 2, AdventureScreen.TEXT_GOLD);
			y += HEADING_HEIGHT;
			for (int i = 0; i < entries.size(); i++) {
				cell(graphics, font, entries.get(i), left + (i % columns) * CELL, y + (i / columns) * CELL, mouseX, mouseY);
			}
			y += ((entries.size() + columns - 1) / columns) * CELL + GROUP_GAP;
		}
		if (!any) {
			graphics.text(font, Component.translatable("screen.progressive_adventure.achievements.all_done"), left, y + 2, AdventureScreen.TEXT_GREEN);
		}
		graphics.disableScissor();

		gridMaxScroll = Math.max(0, y + gridScroll - viewTop - (viewBottom - viewTop));
		gridScroll = Math.min(gridScroll, gridMaxScroll);
		scrollbar(graphics, left + width - 3, gridScroll, gridMaxScroll);
	}

	private void cell(GuiGraphicsExtractor graphics, Font font, Entry entry, int x, int y, int mouseX, int mouseY) {
		if (y + CELL <= viewTop || y >= viewBottom) {
			return;
		}
		cells.add(new Cell(entry.id(), x, y));
		boolean hovered = mouseY >= viewTop && mouseY < viewBottom && PerksTab.isInside(mouseX, mouseY, x, y, CELL - 2, CELL - 2);
		graphics.fill(x, y, x + CELL - 2, y + CELL - 2, hovered ? 0xFF3A3A50 : entry.done() ? 0xFF24402A : 0xFF141418);
		graphics.outline(x, y, CELL - 2, CELL - 2, entry.done() ? 0xFF55CC66 : 0xFF2A2A2A);
		if (entry.secret()) {
			graphics.centeredText(font, "?", x + (CELL - 2) / 2, y + 5, 0xFF555555);
		} else {
			graphics.item(entry.icon(), x + 1, y + 1);
		}
		if (hovered) {
			AdventureScreen.wrappedTooltip(graphics, font, tooltip(entry), mouseX, mouseY);
		}
	}

	private static List<Component> tooltip(Entry entry) {
		List<Component> lines = new ArrayList<>();
		if (entry.secret()) {
			lines.add(Component.translatable("screen.progressive_adventure.achievements.secret").withStyle(ChatFormatting.GRAY));
			return lines;
		}
		lines.add(entry.title().copy().withStyle(entry.done() ? ChatFormatting.GREEN : ChatFormatting.GOLD));
		lines.add(entry.description().copy().withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("screen.progressive_adventure.bestiary.click").withStyle(ChatFormatting.DARK_GRAY));
		return lines;
	}

	/** A free first tip of something the player could do next, without saying what it's for. Same all in-game day. Returns the bottom. */
	private static int renderTipOfTheDay(GuiGraphicsExtractor graphics, Font font, AchievementListPayload list, int left, int top, int width) {
		Minecraft minecraft = Minecraft.getInstance();
		PlayerProgress progress = PamAttachments.progress(minecraft.player);
		List<Entry> candidates = new ArrayList<>();
		for (Group group : list.groups()) {
			for (Entry entry : group.entries()) {
				if (!entry.done() && !entry.secret() && entry.parentDone() && progress.tipsBoughtOf(entry.id()) == 0 && tipCount(entry) > 0) {
					candidates.add(entry);
				}
			}
		}
		Component text;
		if (candidates.isEmpty()) {
			text = Component.translatable("screen.progressive_adventure.achievements.no_daily_tip");
		} else {
			long day = minecraft.level.getOverworldClockTime() / 24000L;
			int index = Math.floorMod(Long.hashCode(day * 31 + minecraft.player.getUUID().hashCode()), candidates.size());
			text = Component.translatable(tipKey(candidates.get(index), 1));
		}
		List<FormattedCharSequence> lines = font.split(text, width - 8);
		int bottom = top + 15 + lines.size() * 10;
		graphics.fill(left, top, left + width, bottom, 0xFF1C1C26);
		graphics.outline(left, top, width, bottom - top, 0xFF3A3A48);
		graphics.text(font, Component.translatable("screen.progressive_adventure.achievements.daily_tip"), left + 4, top + 3, AdventureScreen.TEXT_AQUA);
		int y = top + 13;
		for (FormattedCharSequence line : lines) {
			graphics.text(font, line, left + 4, y, AdventureScreen.TEXT);
			y += 10;
		}
		return bottom;
	}

	private void renderPage(GuiGraphicsExtractor graphics, Font font, AchievementListPayload list, Entry entry, int left, int top, int width, int height,
			int mouseX, int mouseY) {
		graphics.centeredText(font, entry.secret() ? Component.translatable("screen.progressive_adventure.bestiary.unknown_name") : entry.title(),
				left + width / 2, top + 5, entry.done() ? AdventureScreen.TEXT_GREEN : AdventureScreen.TEXT_GOLD);

		viewTop = top + HEADER_HEIGHT;
		viewBottom = top + height;
		int contentWidth = width - 6;
		graphics.enableScissor(left, viewTop, left + width, viewBottom);
		int y = viewTop - pageScroll;

		if (entry.secret()) {
			y = wrapped(graphics, font, Component.translatable("screen.progressive_adventure.achievements.secret"), left, y, contentWidth, AdventureScreen.TEXT_DIM);
		} else {
			graphics.fill(left, y, left + 18, y + 18, 0xFF262636);
			graphics.item(entry.icon(), left + 1, y + 1);
			int textBottom = wrapped(graphics, font, entry.description(), left + 22, y, contentWidth - 22, AdventureScreen.TEXT);
			y = Math.max(y + 20, textBottom) + 2;
			Component status = Component.translatable(entry.done() ? "screen.progressive_adventure.achievements.status_done"
					: "screen.progressive_adventure.achievements.status_not_done");
			graphics.text(font, status, left, y, entry.done() ? AdventureScreen.TEXT_GREEN : AdventureScreen.TEXT_DIM);
			if (entry.rank() > 0) {
				graphics.text(font, Component.translatable("screen.progressive_adventure.achievements.rank", entry.rank()),
						left + font.width(status) + 10, y, AdventureScreen.TEXT_AQUA);
			}
			y += 10;
			y = renderTips(graphics, font, list, entry, left, y + SECTION_GAP, contentWidth, mouseX, mouseY);
			y = renderProgress(graphics, font, entry, left, y + SECTION_GAP, contentWidth);
		}
		graphics.disableScissor();

		pageMaxScroll = Math.max(0, y + pageScroll - viewBottom);
		pageScroll = Math.min(pageScroll, pageMaxScroll);
		scrollbar(graphics, left + width - 3, pageScroll, pageMaxScroll);
	}

	/** Bought tips, then a button for the next one. Everything is free to read once the achievement is done. */
	private int renderTips(GuiGraphicsExtractor graphics, Font font, AchievementListPayload list, Entry entry, int left, int y, int width, int mouseX, int mouseY) {
		graphics.text(font, Component.translatable("screen.progressive_adventure.achievements.tips"), left, y, AdventureScreen.TEXT_GOLD);
		y += 11;
		int count = tipCount(entry);
		if (count == 0) {
			return wrapped(graphics, font, Component.translatable("screen.progressive_adventure.bestiary.where_unknown"), left, y, width, AdventureScreen.TEXT_DIM);
		}
		Minecraft minecraft = Minecraft.getInstance();
		int bought = PamAttachments.progress(minecraft.player).tipsBoughtOf(entry.id());
		int shown = entry.done() ? count : Math.min(bought, count);
		for (int tip = 1; tip <= shown; tip++) {
			y = wrapped(graphics, font, Component.translatable(tipKey(entry, tip)), left, y, width, AdventureScreen.TEXT) + 3;
		}
		if (shown == count) {
			return y;
		}
		int next = shown + 1;
		int cost = list.tipCost(next);
		int xp = Waypoints.totalXp(minecraft.player);
		buyTip = next;
		buyX = left;
		buyY = y;
		buyWidth = width;
		buyAffordable = xp >= cost;
		boolean hovered = mouseY >= viewTop && mouseY < viewBottom && PerksTab.isInside(mouseX, mouseY, buyX, buyY, buyWidth, BUY_BUTTON_HEIGHT);
		graphics.fill(buyX, buyY, buyX + buyWidth, buyY + BUY_BUTTON_HEIGHT, !buyAffordable ? 0xFF1C1C22 : hovered ? 0xFF3C5A3C : 0xFF2A3F2A);
		graphics.outline(buyX, buyY, buyWidth, BUY_BUTTON_HEIGHT, buyAffordable ? 0xFF55CC66 : 0xFF3A3A48);
		graphics.centeredText(font, Component.translatable("screen.progressive_adventure.achievements.buy_tip", next, count, cost),
				buyX + buyWidth / 2, buyY + 5, buyAffordable ? AdventureScreen.TEXT : AdventureScreen.TEXT_DIM);
		y += BUY_BUTTON_HEIGHT + 2;
		graphics.text(font, Component.translatable("screen.progressive_adventure.achievements.your_xp", xp), left, y, AdventureScreen.TEXT_DIM);
		return y + 10;
	}

	/** "31 / 40" for achievements with several parts, and the names of what's left when the server sent them. */
	private static int renderProgress(GuiGraphicsExtractor graphics, Font font, Entry entry, int left, int y, int width) {
		if (entry.total() <= 1 || entry.done()) {
			return y;
		}
		graphics.text(font, Component.translatable("screen.progressive_adventure.achievements.progress", entry.completed(), entry.total()), left, y, AdventureScreen.TEXT_GOLD);
		y += 11;
		if (entry.missing().isEmpty()) {
			return y;
		}
		List<String> names = entry.missing().stream().map(AchievementsTab::criterionName).toList();
		return wrapped(graphics, font, Component.translatable("screen.progressive_adventure.achievements.missing", String.join(", ", names)), left, y, width, AdventureScreen.TEXT);
	}

	/** Criterion names are usually an id of a biome, mob or item ("minecraft:plains", "apple"); vanilla's own name when there is one. */
	private static String criterionName(String criterion) {
		String name = criterion.replace("armor_trimmed_", "");
		Identifier id = Identifier.tryParse(name);
		if (id == null) {
			return pretty(name);
		}
		Language language = Language.getInstance();
		for (String kind : List.of("biome", "entity", "item", "block")) {
			String key = kind + "." + id.getNamespace() + "." + id.getPath();
			if (language.has(key)) {
				return language.getOrDefault(key);
			}
		}
		return pretty(id.getPath());
	}

	/** "black_cat" to "Black cat". */
	private static String pretty(String name) {
		String spaced = name.replace('_', ' ');
		return spaced.isEmpty() ? spaced : spaced.substring(0, 1).toUpperCase(Locale.ROOT) + spaced.substring(1);
	}

	/** Tips are whatever lang keys exist, numbered from 1, so an achievement can have any number and other mods' have none. */
	private static int tipCount(Entry entry) {
		int count = 0;
		while (Language.getInstance().has(tipKey(entry, count + 1))) {
			count++;
		}
		return count;
	}

	private static String tipKey(Entry entry, int tip) {
		return "achievement.progressive_adventure.tip." + entry.id().replace(':', '.').replace('/', '.') + "." + tip;
	}

	private void scrollbar(GuiGraphicsExtractor graphics, int x, int scroll, int maxScroll) {
		if (maxScroll <= 0) {
			return;
		}
		int view = viewBottom - viewTop;
		int thumb = Math.max(10, view * view / (view + maxScroll));
		int thumbTop = viewTop + (view - thumb) * scroll / maxScroll;
		graphics.fill(x, viewTop, x + 3, viewBottom, 0xFF1C1C26);
		graphics.fill(x, thumbTop, x + 3, thumbTop + thumb, 0xFF6B6B7A);
	}

	private static int wrapped(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int width, int color) {
		for (FormattedCharSequence line : font.split(text, width)) {
			graphics.text(font, line, x, y, color);
			y += 10;
		}
		return y;
	}
}
