package com.progressiveadventure.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.progressiveadventure.ProgressiveAdventure;
import com.progressiveadventure.client.JournalClient;
import com.progressiveadventure.journal.ExplorersJournalItem;
import com.progressiveadventure.journal.JournalContent;
import com.progressiveadventure.journal.JournalInk;
import com.progressiveadventure.network.JournalMapRequestPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;

/**
 * The open Explorer's Journal: the shared map around you (128×128 blocks, north up) on vanilla's map paper, with
 * you as a dot in the middle. Asks the server for updates every second while open; doesn't pause the game.
 */
public class JournalScreen extends Screen {
	private static final int MAP_BLOCKS = 128;
	private static final int REQUEST_INTERVAL_TICKS = 20;
	private static final Identifier MAP_TEXTURE = ProgressiveAdventure.id("journal_map");
	private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/map/map_background.png");

	private DynamicTexture texture;
	private int ticks;
	// What the texture shows now; redrawn when you move a block or new chunks arrive
	private int shownX = Integer.MIN_VALUE;
	private int shownZ = Integer.MIN_VALUE;
	private int shownChanges = -1;

	public JournalScreen() {
		super(Component.translatable("item.progressive_adventure.explorers_journal"));
	}

	@Override
	protected void init() {
		if (texture == null) {
			texture = new DynamicTexture(() -> "Explorer's Journal map", MAP_BLOCKS, MAP_BLOCKS, true);
			minecraft.getTextureManager().register(MAP_TEXTURE, texture);
		}
		requestUpdate();
	}

	@Override
	public void removed() {
		minecraft.getTextureManager().release(MAP_TEXTURE);
		texture = null;
	}

	@Override
	public void tick() {
		if (++ticks % REQUEST_INTERVAL_TICKS == 0) {
			requestUpdate();
		}
	}

	private void requestUpdate() {
		LocalPlayer player = minecraft.player;
		if (player != null && player.level().dimension() == Level.OVERWORLD) {
			ChunkPos chunk = player.chunkPosition();
			ClientPlayNetworking.send(new JournalMapRequestPayload(chunk.x(), chunk.z()));
		}
	}

	private void updateTexture(int centerX, int centerZ) {
		if (centerX == shownX && centerZ == shownZ && JournalClient.changes() == shownChanges) {
			return;
		}
		shownX = centerX;
		shownZ = centerZ;
		shownChanges = JournalClient.changes();
		NativeImage pixels = texture.getPixels();
		int minX = centerX - MAP_BLOCKS / 2;
		int minZ = centerZ - MAP_BLOCKS / 2;
		for (int z = 0; z < MAP_BLOCKS; z++) {
			int worldZ = minZ + z;
			for (int x = 0; x < MAP_BLOCKS; x++) {
				int worldX = minX + x;
				byte[] chunk = JournalClient.chunk(ChunkPos.pack(worldX >> 4, worldZ >> 4));
				// Undrawn = transparent, so the paper shows through
				pixels.setPixel(x, z, chunk == null ? 0 : MapColor.getColorFromPackedId(chunk[(worldZ & 15) * 16 + (worldX & 15)]));
			}
		}
		texture.upload();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		LocalPlayer player = minecraft.player;
		if (player == null || texture == null) {
			return;
		}
		// Whole multiples of the map size stay crisp
		int size = MAP_BLOCKS * Math.max(1, (height - 50) / MAP_BLOCKS);
		int left = (width - size) / 2;
		int top = (height - size) / 2;
		int border = size / 16;
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, left - border, top - border, 0, 0,
				size + 2 * border, size + 2 * border, 64, 64, 64, 64);
		graphics.text(font, title, (width - font.width(title)) / 2, top - border - 12, 0xFFFFFFFF);

		if (player.level().dimension() != Level.OVERWORLD) {
			Component text = Component.translatable("screen.progressive_adventure.journal.overworld_only");
			graphics.text(font, text, (width - font.width(text)) / 2, top + size / 2 - 4, 0xFF3F3F3F, false);
			return;
		}

		int centerX = Mth.floor(player.getX());
		int centerZ = Mth.floor(player.getZ());
		updateTexture(centerX, centerZ);
		graphics.blit(RenderPipelines.GUI_TEXTURED, MAP_TEXTURE, left, top, 0, 0, size, size, MAP_BLOCKS, MAP_BLOCKS, MAP_BLOCKS, MAP_BLOCKS);

		// You: a red dot with a dark rim in the middle
		int middleX = left + size / 2;
		int middleY = top + size / 2;
		graphics.fill(middleX - 2, middleY - 2, middleX + 3, middleY + 3, 0xFF000000);
		graphics.fill(middleX - 1, middleY - 1, middleX + 2, middleY + 2, 0xFFE03030);

		// Compass letters on the paper edge, north up
		int letterColor = 0xFF3F3F3F;
		drawLetter(graphics, "N", middleX, top - border / 2 - 4, letterColor);
		drawLetter(graphics, "S", middleX, top + size + border / 2 - 4, letterColor);
		drawLetter(graphics, "W", left - border / 2, middleY - 4, letterColor);
		drawLetter(graphics, "E", left + size + border / 2, middleY - 4, letterColor);

		JournalInk ink = carriedInk(player);
		if (ink != null) {
			Component text = Component.translatable("item.progressive_adventure.explorers_journal.ink", ink.ink(), ink.max());
			graphics.text(font, text, (width - font.width(text)) / 2, top + size + border + 4, ink.ink() > 0 ? 0xFFFFFFFF : 0xFFFF5555);
		}
	}

	private void drawLetter(GuiGraphicsExtractor graphics, String letter, int centerX, int y, int color) {
		graphics.text(font, letter, centerX - font.width(letter) / 2, y, color, false);
	}

	/** Ink of the journal that draws (the first one in the inventory, like the server). */
	private static JournalInk carriedInk(LocalPlayer player) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(JournalContent.EXPLORERS_JOURNAL)) {
				return ExplorersJournalItem.ink(stack);
			}
		}
		return null;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
