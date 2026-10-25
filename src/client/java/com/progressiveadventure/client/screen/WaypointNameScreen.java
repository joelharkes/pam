package com.progressiveadventure.client.screen;

import com.progressiveadventure.network.NameWaypointPayload;
import com.progressiveadventure.waypoint.Waypoint;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Asks for a waypoint name: to turn a stone on (shows the shard cost) or to rename it. */
public class WaypointNameScreen extends Screen {
	private static final int PANEL_WIDTH = 220;
	private static final int PANEL_HEIGHT = 96;

	private final BlockPos pos;
	private final int shardCost;
	private final String currentName;
	private EditBox nameBox;
	private Button doneButton;

	public WaypointNameScreen(BlockPos pos, int shardCost, String currentName) {
		super(Component.translatable(currentName.isEmpty() ? "screen.progressive_adventure.waypoint.new" : "screen.progressive_adventure.waypoint.rename"));
		this.pos = pos;
		this.shardCost = shardCost;
		this.currentName = currentName;
	}

	@Override
	protected void init() {
		int left = (width - PANEL_WIDTH) / 2;
		int top = (height - PANEL_HEIGHT) / 2;
		nameBox = new EditBox(font, left + 10, top + 24, PANEL_WIDTH - 20, 18, nameBox, Component.translatable("screen.progressive_adventure.waypoint.name"));
		nameBox.setMaxLength(Waypoint.MAX_NAME_LENGTH);
		if (nameBox.getValue().isEmpty()) {
			nameBox.setValue(currentName);
		}
		nameBox.setHint(Component.translatable("screen.progressive_adventure.waypoint.name_hint"));
		nameBox.setResponder(value -> updateDone());
		addRenderableWidget(nameBox);
		setInitialFocus(nameBox);

		int buttonWidth = (PANEL_WIDTH - 30) / 2;
		doneButton = addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> confirm())
				.bounds(left + 10, top + PANEL_HEIGHT - 28, buttonWidth, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
				.bounds(left + 20 + buttonWidth, top + PANEL_HEIGHT - 28, buttonWidth, 20).build());
		updateDone();
	}

	private void updateDone() {
		doneButton.active = !Waypoint.cleanName(nameBox.getValue()).isEmpty();
	}

	private void confirm() {
		String name = Waypoint.cleanName(nameBox.getValue());
		if (!name.isEmpty()) {
			ClientPlayNetworking.send(new NameWaypointPayload(pos, name));
			onClose();
		}
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
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		int left = (width - PANEL_WIDTH) / 2;
		int top = (height - PANEL_HEIGHT) / 2;
		graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0101018);
		graphics.outline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF6B6B7A);
		graphics.centeredText(font, title, width / 2, top + 9, AdventureScreen.TEXT_AQUA);
		if (shardCost > 0) {
			graphics.centeredText(font, Component.translatable("screen.progressive_adventure.waypoint.shard_cost", shardCost),
					width / 2, top + 48, AdventureScreen.TEXT_PURPLE);
		}
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
