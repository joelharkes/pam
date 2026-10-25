package com.progressiveadventure.client.screen;

import com.progressiveadventure.network.OpenWaypointListPayload;
import com.progressiveadventure.network.OpenWaypointListPayload.Entry;
import com.progressiveadventure.network.WaypointTeleportPayload;
import com.progressiveadventure.waypoint.Waypoints;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Opened by using a waypoint stone: pick where to go. Costs come from the server; too expensive trips are greyed out. */
public class WaypointListScreen extends Screen {
	private static final int PANEL_WIDTH = 260;
	private static final int ROW_HEIGHT = 22;
	private static final int ROWS_PER_PAGE = 7;
	private static final int HEADER_HEIGHT = 36;
	private static final int FOOTER_HEIGHT = 30;

	private final OpenWaypointListPayload list;
	private int page;

	public WaypointListScreen(OpenWaypointListPayload list) {
		super(Component.literal(list.fromName()));
		this.list = list;
	}

	private int panelHeight() {
		return HEADER_HEIGHT + Math.max(1, Math.min(ROWS_PER_PAGE, list.entries().size())) * ROW_HEIGHT + FOOTER_HEIGHT;
	}

	private int pages() {
		return Math.max(1, (list.entries().size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
	}

	@Override
	protected void init() {
		int left = (width - PANEL_WIDTH) / 2;
		int top = (height - panelHeight()) / 2;
		boolean free = minecraft.player.hasInfiniteMaterials();
		int xp = Waypoints.totalXp(minecraft.player);

		List<Entry> entries = list.entries();
		int first = page * ROWS_PER_PAGE;
		for (int i = first; i < Math.min(entries.size(), first + ROWS_PER_PAGE); i++) {
			Entry entry = entries.get(i);
			Button button = addRenderableWidget(Button.builder(rowText(entry, free), b -> travel(entry))
					.bounds(left + 10, top + HEADER_HEIGHT + (i - first) * ROW_HEIGHT, PANEL_WIDTH - 20, 20)
					.tooltip(Tooltip.create(tooltip(entry, free, xp)))
					.build());
			button.active = free || xp >= entry.cost();
		}

		int bottom = top + panelHeight() - 26;
		if (pages() > 1) {
			addRenderableWidget(Button.builder(Component.literal("<"), b -> turnPage(-1)).bounds(left + 10, bottom, 20, 20).build()).active = page > 0;
			addRenderableWidget(Button.builder(Component.literal(">"), b -> turnPage(1)).bounds(left + 34, bottom, 20, 20).build()).active = page < pages() - 1;
		}
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(left + PANEL_WIDTH - 90, bottom, 80, 20).build());
	}

	private void turnPage(int delta) {
		page = Math.clamp(page + delta, 0, pages() - 1);
		rebuildWidgets();
	}

	private Component rowText(Entry entry, boolean free) {
		MutableComponent text = Component.literal(entry.name());
		if (!free) {
			text.append(Component.translatable("screen.progressive_adventure.waypoint.row_cost", entry.cost()));
		}
		return text;
	}

	private Component tooltip(Entry entry, boolean free, int xp) {
		MutableComponent text = Component.translatable("screen.progressive_adventure.waypoint.owner", entry.owner());
		text.append("\n");
		if (entry.distance() >= 0) {
			text.append(Component.translatable("screen.progressive_adventure.waypoint.distance", entry.distance()));
		} else {
			Identifier dimension = Identifier.parse(entry.dimension());
			text.append(Component.translatable("screen.progressive_adventure.waypoint.other_dimension",
					Component.translatableWithFallback("dimension." + dimension.getNamespace() + "." + dimension.getPath(), dimension.getPath())));
		}
		if (!free && xp < entry.cost()) {
			text.append("\n").append(Component.translatable("screen.progressive_adventure.waypoint.needs_xp", entry.cost(), xp));
		}
		return text;
	}

	private void travel(Entry entry) {
		ClientPlayNetworking.send(new WaypointTeleportPayload(list.from(), entry.id()));
		onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int left = (width - PANEL_WIDTH) / 2;
		int top = (height - panelHeight()) / 2;
		graphics.fill(left, top, left + PANEL_WIDTH, top + panelHeight(), 0xE0101018);
		graphics.outline(left, top, PANEL_WIDTH, panelHeight(), 0xFF6B6B7A);
		graphics.centeredText(font, title, width / 2, top + 8, AdventureScreen.TEXT_AQUA);
		Component subtitle = minecraft.player.hasInfiniteMaterials()
				? Component.translatable("screen.progressive_adventure.waypoint.pick")
				: Component.translatable("screen.progressive_adventure.waypoint.your_xp", Waypoints.totalXp(minecraft.player));
		graphics.centeredText(font, subtitle, width / 2, top + 21, AdventureScreen.TEXT_GREEN);
		if (list.entries().isEmpty()) {
			graphics.centeredText(font, Component.translatable("screen.progressive_adventure.waypoint.none"),
					width / 2, top + HEADER_HEIGHT + 6, AdventureScreen.TEXT_DIM);
		}
		if (pages() > 1) {
			graphics.text(font, Component.literal((page + 1) + "/" + pages()), left + 60, top + panelHeight() - 20, AdventureScreen.TEXT_DIM);
		}
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
