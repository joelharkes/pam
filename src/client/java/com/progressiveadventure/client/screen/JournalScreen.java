package com.progressiveadventure.client.screen;

import com.progressiveadventure.ProgressiveAdventure;
import com.progressiveadventure.client.JournalClient;
import com.progressiveadventure.journal.ExplorersJournalItem;
import com.progressiveadventure.journal.JournalInk;
import com.progressiveadventure.journal.Journals;
import com.progressiveadventure.network.JournalMarkerEditPayload;
import com.progressiveadventure.network.JournalMarkersPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The open Explorer's Journal: the shared map on vanilla's map paper, north up. Scroll to zoom, drag to move,
 * right-click to add a marker (or remove your own). You are an arrow with a compass in your inventory, otherwise a
 * dot. Asks the server for the visible area while open; doesn't pause the game.
 */
public class JournalScreen extends Screen {
	/** Screen pixels per block; texels are blocks, or every second block at the widest zoom. */
	private static final double[] ZOOMS = {0.5, 1, 2, 4};
	private static final Identifier MAP_TEXTURE = ProgressiveAdventure.id("journal_map");
	private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/map/map_background.png");
	private static final int BORDER = 8;
	private static final int MARKER_SIZE = 8;
	private static final int HOVER_RADIUS = 6;
	private static final String[] DIRECTIONS = {"south", "south_west", "west", "north_west", "north", "north_east", "east", "south_east"};

	// Kept while the game runs, so the journal opens the way you left it
	private static int zoom = 2;

	private boolean following = true;
	private double centerX;
	private double centerZ;
	private boolean dragging;
	private Button centerButton;
	private Component notice;
	private long noticeUntil;

	private int viewLeft;
	private int viewTop;
	private int viewRight;
	private int viewBottom;

	private DynamicTexture texture;
	private int textureWidth;
	private int textureHeight;
	private String paintedKey = "";

	public JournalScreen() {
		super(Component.translatable("item.progressive_adventure.explorers_journal"));
	}

	@Override
	protected void init() {
		viewLeft = 20;
		viewTop = 30;
		viewRight = width - 20;
		viewBottom = height - 46;
		centerButton = addRenderableWidget(Button.builder(Component.translatable("screen.progressive_adventure.journal.center"), b -> following = true)
				.bounds(viewLeft - BORDER, height - 26, 90, 20).build());
	}

	@Override
	public void removed() {
		if (texture != null) {
			minecraft.getTextureManager().release(MAP_TEXTURE);
			texture = null;
		}
	}

	private double pixelsPerBlock() {
		return ZOOMS[zoom];
	}

	/** World x of the view's left edge. */
	private double leftBlock() {
		return centerX - (viewRight - viewLeft) / (2 * pixelsPerBlock());
	}

	private double topBlock() {
		return centerZ - (viewBottom - viewTop) / (2 * pixelsPerBlock());
	}

	private double screenX(double worldX) {
		return viewLeft + (worldX - leftBlock()) * pixelsPerBlock();
	}

	private double screenY(double worldZ) {
		return viewTop + (worldZ - topBlock()) * pixelsPerBlock();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		LocalPlayer player = minecraft.player;
		if (player == null) {
			return;
		}
		centerButton.active = !following;
		graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, viewLeft - BORDER, viewTop - BORDER, 0, 0,
				viewRight - viewLeft + 2 * BORDER, viewBottom - viewTop + 2 * BORDER, 64, 64, 64, 64);
		graphics.text(font, title, (width - font.width(title)) / 2, viewTop - BORDER - 12, 0xFFFFFFFF);

		if (player.level().dimension() != Level.OVERWORLD) {
			Component text = Component.translatable("screen.progressive_adventure.journal.overworld_only");
			graphics.text(font, text, (width - font.width(text)) / 2, (viewTop + viewBottom) / 2 - 4, 0xFF3F3F3F, false);
			return;
		}
		if (following) {
			centerX = player.getX();
			centerZ = player.getZ();
		}
		requestVisibleArea();

		graphics.enableScissor(viewLeft, viewTop, viewRight, viewBottom);
		drawLand(graphics);
		JournalMarkersPayload.Marker hovered = drawMarkers(graphics, player, mouseX, mouseY);
		drawPlayer(graphics, player);
		graphics.disableScissor();

		drawEdgeLetters(graphics);
		drawFooter(graphics, player);
		if (hovered != null) {
			AdventureScreen.wrappedTooltip(graphics, font, markerTooltip(hovered, player), mouseX, mouseY);
		}
	}

	private void requestVisibleArea() {
		int minChunkX = Mth.floor(leftBlock()) >> 4;
		int minChunkZ = Mth.floor(topBlock()) >> 4;
		int maxChunkX = Mth.floor(leftBlock() + (viewRight - viewLeft) / pixelsPerBlock()) >> 4;
		int maxChunkZ = Mth.floor(topBlock() + (viewBottom - viewTop) / pixelsPerBlock()) >> 4;
		// A very big screen at the widest zoom sees more than the server sends at once: ask for the middle part
		int limit = Journals.MAX_REQUEST_CHUNKS - 1;
		if (maxChunkX - minChunkX > limit) {
			minChunkX = (minChunkX + maxChunkX - limit) / 2;
			maxChunkX = minChunkX + limit;
		}
		if (maxChunkZ - minChunkZ > limit) {
			minChunkZ = (minChunkZ + maxChunkZ - limit) / 2;
			maxChunkZ = minChunkZ + limit;
		}
		JournalClient.request(minChunkX, minChunkZ, maxChunkX, maxChunkZ);
	}

	private void drawLand(GuiGraphicsExtractor graphics) {
		double pixelsPerBlock = pixelsPerBlock();
		int step = pixelsPerBlock >= 1 ? 1 : (int) Math.round(1 / pixelsPerBlock);
		int texelSize = (int) Math.round(pixelsPerBlock * step);
		int width = (viewRight - viewLeft) / texelSize + 2;
		int height = (viewBottom - viewTop) / texelSize + 2;
		if (texture == null || width != textureWidth || height != textureHeight) {
			if (texture != null) {
				minecraft.getTextureManager().release(MAP_TEXTURE);
			}
			texture = new DynamicTexture(() -> "Explorer's Journal map", width, height, true);
			minecraft.getTextureManager().register(MAP_TEXTURE, texture);
			textureWidth = width;
			textureHeight = height;
			paintedKey = "";
		}
		int minX = Math.floorDiv(Mth.floor(leftBlock()), step) * step;
		int minZ = Math.floorDiv(Mth.floor(topBlock()), step) * step;
		String key = minX + "," + minZ + "," + step + "," + JournalClient.changes();
		if (!key.equals(paintedKey)) {
			paintedKey = key;
			JournalClient.paint(texture.getPixels(), width, height, minX, minZ, step);
			texture.upload();
		}
		int x = (int) Math.round(screenX(minX));
		int y = (int) Math.round(screenY(minZ));
		graphics.blit(RenderPipelines.GUI_TEXTURED, MAP_TEXTURE, x, y, 0, 0, width * texelSize, height * texelSize,
				width, height, width, height);
	}

	/** Draws all markers in view; returns the one under the mouse, if any. */
	private JournalMarkersPayload.Marker drawMarkers(GuiGraphicsExtractor graphics, LocalPlayer player, int mouseX, int mouseY) {
		List<JournalMarkersPayload.Marker> markers = new ArrayList<>(JournalClient.markers());
		GlobalPos death = player.getLastDeathLocation().orElse(null);
		if (death != null && death.dimension() == Level.OVERWORLD) {
			markers.add(new JournalMarkersPayload.Marker(new UUID(0, 1), -1, "target_point", "", death.pos().getX(), death.pos().getZ(), "", false));
		}
		JournalMarkersPayload.Marker hovered = null;
		double hoveredDistance = HOVER_RADIUS;
		boolean showNames = pixelsPerBlock() >= 1;
		for (JournalMarkersPayload.Marker marker : markers) {
			int x = (int) Math.round(screenX(marker.x() + 0.5));
			int y = (int) Math.round(screenY(marker.z() + 0.5));
			if (x < viewLeft - MARKER_SIZE || x > viewRight + MARKER_SIZE || y < viewTop - MARKER_SIZE || y > viewBottom + MARKER_SIZE) {
				continue;
			}
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, JournalClient.sprite(JournalClient.spriteOf(marker)),
					x - MARKER_SIZE / 2, y - MARKER_SIZE / 2, MARKER_SIZE, MARKER_SIZE);
			if (showNames && !marker.name().isEmpty()
					&& (marker.kind() == JournalMarkersPayload.KIND_MARKER || marker.kind() == JournalMarkersPayload.KIND_WAYPOINT)) {
				graphics.text(font, marker.name(), x - font.width(marker.name()) / 2, y + MARKER_SIZE / 2 + 1, 0xFFFFFFFF);
			}
			double distance = Math.hypot(mouseX - x, mouseY - y);
			if (distance < hoveredDistance && isInView(mouseX, mouseY)) {
				hovered = marker;
				hoveredDistance = distance;
			}
		}
		return hovered;
	}

	private void drawPlayer(GuiGraphicsExtractor graphics, LocalPlayer player) {
		float x = (float) screenX(player.getX());
		float y = (float) screenY(player.getZ());
		boolean compass = JournalClient.hasCompass(player);
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		if (compass) {
			// The sprite points up (north); facing south is yaw 0
			graphics.pose().rotate((float) Math.toRadians(player.getYRot() + 180));
		}
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, JournalClient.sprite(compass ? "player" : "player_off_map"),
				-MARKER_SIZE / 2, -MARKER_SIZE / 2, MARKER_SIZE, MARKER_SIZE);
		graphics.pose().popMatrix();
	}

	private void drawEdgeLetters(GuiGraphicsExtractor graphics) {
		int middleX = (viewLeft + viewRight) / 2;
		int middleY = (viewTop + viewBottom) / 2;
		int color = 0xFF3F3F3F;
		drawLetter(graphics, "N", middleX, viewTop - BORDER + 0, color);
		drawLetter(graphics, "S", middleX, viewBottom, color);
		drawLetter(graphics, "W", viewLeft - BORDER / 2, middleY - 4, color);
		drawLetter(graphics, "E", viewRight + BORDER / 2, middleY - 4, color);
	}

	private void drawLetter(GuiGraphicsExtractor graphics, String letter, int centerX, int y, int color) {
		graphics.text(font, letter, centerX - font.width(letter) / 2, y, color, false);
	}

	private void drawFooter(GuiGraphicsExtractor graphics, LocalPlayer player) {
		int y = viewBottom + BORDER + 4;
		ItemStack journal = JournalClient.firstJournal(player);
		if (!journal.isEmpty()) {
			JournalInk ink = ExplorersJournalItem.ink(journal);
			Component text = Component.translatable("item.progressive_adventure.explorers_journal.ink", ink.ink(), ink.max());
			graphics.text(font, text, viewRight + BORDER - font.width(text), height - 20, ink.ink() > 0 ? 0xFFFFFFFF : 0xFFFF5555);
		}
		Component hint = notice != null && System.currentTimeMillis() < noticeUntil
				? notice
				: Component.translatable("screen.progressive_adventure.journal.hint");
		graphics.text(font, hint, (width - font.width(hint)) / 2, y, 0xFFC0C0C0);
	}

	private List<Component> markerTooltip(JournalMarkersPayload.Marker marker, LocalPlayer player) {
		List<Component> lines = new ArrayList<>();
		Component title = switch (marker.kind()) {
			case JournalMarkersPayload.KIND_BED -> Component.translatable("screen.progressive_adventure.journal.marker.bed");
			case JournalMarkersPayload.KIND_TREASURE -> Component.literal(marker.name());
			case -1 -> Component.translatable("screen.progressive_adventure.journal.marker.death");
			case JournalMarkersPayload.KIND_WAYPOINT -> Component.translatable("screen.progressive_adventure.journal.marker.waypoint", marker.name());
			default -> Component.literal(marker.name());
		};
		lines.add(title.copy().withStyle(ChatFormatting.GOLD));
		if (!marker.owner().isEmpty()) {
			lines.add(Component.translatable("screen.progressive_adventure.journal.marker.owner", marker.owner()).withStyle(ChatFormatting.GRAY));
		}
		double dx = marker.x() + 0.5 - player.getX();
		double dz = marker.z() + 0.5 - player.getZ();
		int distance = (int) Math.round(Math.hypot(dx, dz));
		if (distance < 8) {
			lines.add(Component.translatable("screen.progressive_adventure.journal.marker.here").withStyle(ChatFormatting.GRAY));
		} else {
			// Angle from south, counter-clockwise like Minecraft's yaw: 0 south, 90 west, 180 north, 270 east
			double angle = Math.toDegrees(Math.atan2(-dx, dz));
			int index = Math.floorMod((int) Math.round(angle / 45), 8);
			lines.add(Component.translatable("screen.progressive_adventure.journal.marker.distance", distance,
					Component.translatable("direction.progressive_adventure." + DIRECTIONS[index])).withStyle(ChatFormatting.GRAY));
		}
		if (marker.mine() && marker.kind() == JournalMarkersPayload.KIND_MARKER) {
			lines.add(Component.translatable("screen.progressive_adventure.journal.marker.remove").withStyle(ChatFormatting.YELLOW));
		}
		return lines;
	}

	private boolean isInView(double mouseX, double mouseY) {
		return mouseX >= viewLeft && mouseX < viewRight && mouseY >= viewTop && mouseY < viewBottom;
	}

	private void showNotice(Component text) {
		notice = text;
		noticeUntil = System.currentTimeMillis() + 3000;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		if (!isInView(event.x(), event.y()) || minecraft.player == null || minecraft.player.level().dimension() != Level.OVERWORLD) {
			return false;
		}
		if (event.button() == 0) {
			dragging = true;
			return true;
		}
		if (event.button() == 1) {
			onRightClick(event.x(), event.y());
			return true;
		}
		return false;
	}

	private void onRightClick(double mouseX, double mouseY) {
		// Your own marker under the mouse: remove it
		for (JournalMarkersPayload.Marker marker : JournalClient.markers()) {
			if (marker.kind() == JournalMarkersPayload.KIND_MARKER && marker.mine()
					&& Math.hypot(mouseX - screenX(marker.x() + 0.5), mouseY - screenY(marker.z() + 0.5)) < HOVER_RADIUS) {
				ClientPlayNetworking.send(new JournalMarkerEditPayload(true, marker.id(), 0, 0, "", ""));
				return;
			}
		}
		int x = Mth.floor(leftBlock() + (mouseX - viewLeft) / pixelsPerBlock());
		int z = Mth.floor(topBlock() + (mouseY - viewTop) / pixelsPerBlock());
		if (JournalClient.chunk(ChunkPos.pack(x >> 4, z >> 4)) == null) {
			showNotice(Component.translatable("screen.progressive_adventure.journal.not_drawn").withStyle(ChatFormatting.YELLOW));
			return;
		}
		minecraft.gui.setScreen(new JournalMarkerScreen(this, x, z));
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging) {
			following = false;
			centerX -= dx / pixelsPerBlock();
			centerZ -= dy / pixelsPerBlock();
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (!isInView(mouseX, mouseY) || scrollY == 0) {
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}
		int newZoom = Mth.clamp(zoom + (scrollY > 0 ? 1 : -1), 0, ZOOMS.length - 1);
		if (newZoom != zoom && !following) {
			// Keep the spot under the mouse where it is
			double worldX = leftBlock() + (mouseX - viewLeft) / pixelsPerBlock();
			double worldZ = topBlock() + (mouseY - viewTop) / pixelsPerBlock();
			double factor = ZOOMS[zoom] / ZOOMS[newZoom];
			centerX = worldX + (centerX - worldX) * factor;
			centerZ = worldZ + (centerZ - worldZ) * factor;
		}
		zoom = newZoom;
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
