package com.progressiveadventure.client.screen;

import com.progressiveadventure.client.JournalClient;
import com.progressiveadventure.journal.JournalMarker;
import com.progressiveadventure.network.JournalMarkerEditPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.util.UUID;

/** Name and icon for a new marker on the journal map; goes back to the journal when done. */
public class JournalMarkerScreen extends Screen {
	private static final int PANEL_WIDTH = 220;
	private static final int PANEL_HEIGHT = 110;
	private static final int ICON_SIZE = 16;
	private static final int ICON_GAP = 6;

	private final JournalScreen parent;
	private final int x;
	private final int z;
	private EditBox nameBox;
	private Button doneButton;
	private String icon = JournalMarker.ICONS.get(1);

	public JournalMarkerScreen(JournalScreen parent, int x, int z) {
		super(Component.translatable("screen.progressive_adventure.journal.new_marker"));
		this.parent = parent;
		this.x = x;
		this.z = z;
	}

	private int left() {
		return (width - PANEL_WIDTH) / 2;
	}

	private int top() {
		return (height - PANEL_HEIGHT) / 2;
	}

	private int iconsLeft() {
		int count = JournalMarker.ICONS.size();
		return (width - (count * ICON_SIZE + (count - 1) * ICON_GAP)) / 2;
	}

	private int iconsTop() {
		return top() + 48;
	}

	@Override
	protected void init() {
		nameBox = new EditBox(font, left() + 10, top() + 22, PANEL_WIDTH - 20, 18, nameBox, Component.translatable("screen.progressive_adventure.journal.marker_name"));
		nameBox.setMaxLength(JournalMarker.MAX_NAME_LENGTH);
		nameBox.setHint(Component.translatable("screen.progressive_adventure.journal.marker_name_hint"));
		nameBox.setResponder(value -> updateDone());
		addRenderableWidget(nameBox);
		setInitialFocus(nameBox);

		int buttonWidth = (PANEL_WIDTH - 30) / 2;
		doneButton = addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> confirm())
				.bounds(left() + 10, top() + PANEL_HEIGHT - 28, buttonWidth, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
				.bounds(left() + 20 + buttonWidth, top() + PANEL_HEIGHT - 28, buttonWidth, 20).build());
		updateDone();
	}

	private void updateDone() {
		doneButton.active = !JournalMarker.cleanName(nameBox.getValue()).isEmpty();
	}

	private void confirm() {
		String name = JournalMarker.cleanName(nameBox.getValue());
		if (!name.isEmpty()) {
			ClientPlayNetworking.send(new JournalMarkerEditPayload(false, new UUID(0, 0), x, z, name, icon));
			onClose();
		}
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (super.keyPressed(event)) {
			return true;
		}
		if (event.isConfirmation()) {
			confirm();
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (int i = 0; i < JournalMarker.ICONS.size(); i++) {
			int iconX = iconsLeft() + i * (ICON_SIZE + ICON_GAP);
			if (event.x() >= iconX && event.x() < iconX + ICON_SIZE && event.y() >= iconsTop() && event.y() < iconsTop() + ICON_SIZE) {
				icon = JournalMarker.ICONS.get(i);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(left(), top(), left() + PANEL_WIDTH, top() + PANEL_HEIGHT, 0xE0101018);
		graphics.outline(left(), top(), PANEL_WIDTH, PANEL_HEIGHT, 0xFF6B6B7A);
		graphics.centeredText(font, title, width / 2, top() + 9, AdventureScreen.TEXT_AQUA);
		for (int i = 0; i < JournalMarker.ICONS.size(); i++) {
			String name = JournalMarker.ICONS.get(i);
			int iconX = iconsLeft() + i * (ICON_SIZE + ICON_GAP);
			int background = name.equals(icon) ? 0xFFC8B98C : 0xFF3A3A48;
			graphics.fill(iconX - 2, iconsTop() - 2, iconX + ICON_SIZE + 2, iconsTop() + ICON_SIZE + 2, background);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, JournalClient.sprite(name), iconX, iconsTop(), ICON_SIZE, ICON_SIZE);
		}
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
