package com.progressiveadventure.client.screen;

import com.progressiveadventure.bestiary.Bestiary;
import com.progressiveadventure.client.BestiaryClient;
import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.config.PamConfig.FamiliarityTier;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.PlayerProgress;
import com.progressiveadventure.milestone.Progression;
import com.progressiveadventure.network.BestiaryInfoPayload;
import com.progressiveadventure.network.BestiaryInfoPayload.Drop;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Grid of every mob with a spawn egg (undiscovered ones show as "?"). Clicking a tile opens the mob's page: a 3D
 * model, where to find it, and more info as familiarity grows (stats and tips, drops, exact drop chances).
 */
class BestiaryTab implements AdventureTab {
	private static final int HEADER_HEIGHT = 22;
	private static final int CELL = 20;
	private static final int MODEL_WIDTH = 96;
	private static final int MODEL_HEIGHT = 104;
	private static final int COLUMN_GAP = 8;
	private static final int SECTION_GAP = 5;
	private static final int DROP_SLOT = 18;

	private static int page = 0;
	/** Index of the open mob page, or -1 for the grid. Remembered between openings, like the page (not between worlds). */
	private static int selected = -1;

	/** Back to the first page of the grid, e.g. when leaving a world (the next one has other discoveries). */
	static void forgetSelection() {
		page = 0;
		selected = -1;
	}

	private record Entry(EntityType<?> type, ItemStack icon) {
	}

	private final List<Entry> entries = buildEntries();
	private AdventureScreen screen;
	private int gridLeft;
	private int gridTop;
	private int columns;
	private int perPage;
	/** Mob shown on the open page, made once per page (never added to the world). */
	private LivingEntity model;
	private EntityType<?> modelType;

	/** Hostile mobs first, then the rest, each sorted by name. */
	private static List<Entry> buildEntries() {
		List<Entry> result = new ArrayList<>();
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			Optional<Holder<Item>> egg = SpawnEggItem.byId(type);
			egg.ifPresent(item -> result.add(new Entry(type, new ItemStack(item))));
		}
		result.sort(Comparator.<Entry>comparingInt(entry -> entry.type().getCategory() == MobCategory.MONSTER ? 0 : 1)
				.thenComparing(entry -> entry.type().getDescription().getString()));
		return result;
	}

	@Override
	public void init(AdventureScreen screen, int left, int top, int width, int height) {
		this.screen = screen;
		layout(left, top, width, height);
		if (selected >= entries.size()) {
			selected = -1;
		}
		if (selected >= 0) {
			initPage(left, top, width);
			return;
		}
		int pages = pageCount();
		page = Math.min(page, pages - 1);
		if (pages > 1) {
			Button previous = screen.addTabWidget(Button.builder(Component.literal("<"), b -> {
				page--;
				screen.refresh();
			}).bounds(left + width - 44, top, 20, 18).build());
			Button next = screen.addTabWidget(Button.builder(Component.literal(">"), b -> {
				page++;
				screen.refresh();
			}).bounds(left + width - 20, top, 20, 18).build());
			previous.active = page > 0;
			next.active = page < pages - 1;
		}
	}

	private void initPage(int left, int top, int width) {
		screen.addTabWidget(Button.builder(Component.translatable("screen.progressive_adventure.bestiary.back"), b -> {
			selected = -1;
			screen.refresh();
		}).bounds(left, top, 50, 18).build());
		screen.addTabWidget(Button.builder(Component.literal("<"), b -> select((selected - 1 + entries.size()) % entries.size()))
				.bounds(left + width - 44, top, 20, 18).build());
		screen.addTabWidget(Button.builder(Component.literal(">"), b -> select((selected + 1) % entries.size()))
				.bounds(left + width - 20, top, 20, 18).build());
	}

	private void select(int index) {
		selected = index;
		if (index >= 0) {
			page = index / perPage;
		}
		screen.refresh();
	}

	private void layout(int left, int top, int width, int height) {
		columns = Math.max(1, width / CELL);
		int rows = Math.max(1, (height - HEADER_HEIGHT) / CELL);
		perPage = columns * rows;
		gridLeft = left + (width - columns * CELL) / 2;
		gridTop = top + HEADER_HEIGHT;
	}

	private int pageCount() {
		return Math.max(1, (entries.size() + perPage - 1) / perPage);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY) {
		if (selected >= 0) {
			return false;
		}
		int index = cellAt(mouseX, mouseY);
		if (index < 0) {
			return false;
		}
		select(index);
		return true;
	}

	/** Entry index of the grid cell under the mouse, or -1. */
	private int cellAt(double mouseX, double mouseY) {
		if (mouseX < gridLeft || mouseY < gridTop) {
			return -1;
		}
		int column = (int) (mouseX - gridLeft) / CELL;
		int row = (int) (mouseY - gridTop) / CELL;
		if (column >= columns || (mouseX - gridLeft) % CELL >= CELL - 2 || (mouseY - gridTop) % CELL >= CELL - 2) {
			return -1;
		}
		int index = page * perPage + row * columns + column;
		return row * columns + column < perPage && index < entries.size() ? index : -1;
	}

	@Override
	public void render(GuiGraphicsExtractor graphics, int left, int top, int width, int height, int mouseX, int mouseY) {
		layout(left, top, width, height);
		if (selected >= 0) {
			renderPage(graphics, entries.get(selected), left, top, width, height, mouseX, mouseY);
		} else {
			renderGrid(graphics, left, top, width, mouseX, mouseY);
		}
	}

	private void renderGrid(GuiGraphicsExtractor graphics, int left, int top, int width, int mouseX, int mouseY) {
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		PlayerProgress progress = PamAttachments.progress(minecraft.player);

		long discovered = entries.stream().filter(entry -> progress.killsOf(Bestiary.key(entry.type())) > 0).count();
		long mastered = entries.stream().filter(entry -> Bestiary.reached(progress.killsOf(Bestiary.key(entry.type())), Bestiary.MASTER)).count();
		Component header = Component.translatable("screen.progressive_adventure.discovered", discovered, entries.size());
		graphics.text(font, header, left, top + 5, AdventureScreen.TEXT_GOLD);
		if (mastered > 0) {
			graphics.text(font, Component.translatable("screen.progressive_adventure.mastered", mastered),
					left + font.width(header) + 10, top + 5, AdventureScreen.TEXT_GOLD);
		}
		if (pageCount() > 1) {
			Component pageText = Component.translatable("screen.progressive_adventure.page", page + 1, pageCount());
			graphics.text(font, pageText, left + width - 50 - font.width(pageText), top + 5, AdventureScreen.TEXT_DIM);
		}

		int hovered = cellAt(mouseX, mouseY);
		int start = page * perPage;
		for (int i = start; i < Math.min(entries.size(), start + perPage); i++) {
			Entry entry = entries.get(i);
			int x = gridLeft + ((i - start) % columns) * CELL;
			int y = gridTop + ((i - start) / columns) * CELL;
			int kills = progress.killsOf(Bestiary.key(entry.type()));
			FamiliarityTier tier = Bestiary.tier(kills);

			graphics.fill(x, y, x + CELL - 2, y + CELL - 2, i == hovered ? 0xFF3A3A50 : kills > 0 ? 0xFF262636 : 0xFF141418);
			graphics.outline(x, y, CELL - 2, CELL - 2, kills > 0 ? tierColor(tier) : 0xFF2A2A2A);
			if (kills > 0) {
				graphics.item(entry.icon(), x + 1, y + 1);
			} else {
				graphics.centeredText(font, "?", x + (CELL - 2) / 2, y + 5, 0xFF555555);
			}
			if (i == hovered) {
				AdventureScreen.wrappedTooltip(graphics, font, tooltip(entry, kills, tier), mouseX, mouseY);
			}
		}
	}

	private static List<Component> tooltip(Entry entry, int kills, FamiliarityTier tier) {
		List<Component> lines = new ArrayList<>();
		if (kills == 0) {
			lines.add(Component.translatable("screen.progressive_adventure.bestiary.unknown_name").withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable("screen.progressive_adventure.bestiary.undiscovered").withStyle(ChatFormatting.DARK_GRAY));
		} else {
			lines.add(entry.type().getDescription().copy().withStyle(ChatFormatting.GOLD));
			lines.add(Component.translatable("screen.progressive_adventure.bestiary.kills", kills).withStyle(ChatFormatting.GRAY));
			lines.add(familiarityLine(tier).withStyle(ChatFormatting.GRAY));
		}
		lines.add(Component.translatable("screen.progressive_adventure.bestiary.click").withStyle(ChatFormatting.DARK_GRAY));
		return lines;
	}

	private void renderPage(GuiGraphicsExtractor graphics, Entry entry, int left, int top, int width, int height, int mouseX, int mouseY) {
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		PlayerProgress progress = PamAttachments.progress(minecraft.player);
		String key = Bestiary.key(entry.type());
		int kills = progress.killsOf(key);
		FamiliarityTier tier = Bestiary.tier(kills);
		BestiaryInfoPayload info = BestiaryClient.info(entry.type());

		Component name = kills > 0 ? entry.type().getDescription()
				: Component.translatable("screen.progressive_adventure.bestiary.unknown_name");
		graphics.centeredText(font, name, left + width / 2, top + 5, kills > 0 ? tierColor(tier) : AdventureScreen.TEXT_DIM);

		// Left column: model and familiarity
		int boxTop = top + 24;
		graphics.fill(left, boxTop, left + MODEL_WIDTH, boxTop + MODEL_HEIGHT, 0xFF141418);
		graphics.outline(left, boxTop, MODEL_WIDTH, MODEL_HEIGHT, kills > 0 ? tierColor(tier) : 0xFF2A2A2A);
		if (kills > 0) {
			renderModel(graphics, entry.type(), left + 1, boxTop + 1, left + MODEL_WIDTH - 1, boxTop + MODEL_HEIGHT - 1, mouseX, mouseY);
		} else {
			graphics.pose().pushMatrix();
			graphics.pose().translate(left + MODEL_WIDTH / 2f, boxTop + MODEL_HEIGHT / 2f - 12);
			graphics.pose().scale(3, 3);
			graphics.centeredText(font, "?", 0, 0, 0xFF555555);
			graphics.pose().popMatrix();
		}

		int y = boxTop + MODEL_HEIGHT + 4;
		if (kills == 0) {
			y = wrapped(graphics, font, Component.translatable("screen.progressive_adventure.bestiary.undiscovered"), left, y, MODEL_WIDTH,
					AdventureScreen.TEXT_DIM);
		} else {
			graphics.text(font, Component.translatable("screen.progressive_adventure.bestiary.kills", kills), left, y, AdventureScreen.TEXT);
			y += 10;
			y = wrapped(graphics, font, familiarityLine(tier), left, y, MODEL_WIDTH, tierColor(tier));
			FamiliarityTier next = nextTier(kills);
			if (next != null) {
				y = wrapped(graphics, font, Component.translatable("screen.progressive_adventure.bestiary.next_tier", next.kills() - kills,
						Bestiary.formatModifier(next.damageModifier())), left, y, MODEL_WIDTH, AdventureScreen.TEXT_DIM);
			}
			if (Bestiary.reached(kills, Bestiary.MASTER) && info != null) {
				wrapped(graphics, font, Component.translatable("screen.progressive_adventure.bestiary.master",
						Math.round(info.masterDoubleLootChance() * 100)), left, y, MODEL_WIDTH, AdventureScreen.TEXT_GOLD);
			}
		}

		// Right column: sections, unlocked by familiarity
		int x = left + MODEL_WIDTH + COLUMN_GAP;
		int columnWidth = width - MODEL_WIDTH - COLUMN_GAP;
		graphics.enableScissor(x, boxTop, left + width, top + height);
		y = boxTop;

		y = header(graphics, font, "where", x, y);
		Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entry.type());
		String whereKey = "bestiary.progressive_adventure.where." + id.getNamespace() + "." + id.getPath();
		y = wrapped(graphics, font, Component.translatable(Language.getInstance().has(whereKey) ? whereKey
				: "screen.progressive_adventure.bestiary.where_unknown"), x, y, columnWidth, AdventureScreen.TEXT);
		int rank = PamAttachments.snapshot(minecraft.player).rank();
		if (info != null && info.unlockRank() > rank) {
			y = wrapped(graphics, font, Component.translatable("screen.progressive_adventure.bestiary.locked_rank", info.unlockRank(), rank),
					x, y, columnWidth, AdventureScreen.TEXT_RED);
		}

		if (kills > 0) {
			y = header(graphics, font, "stats", x, y + SECTION_GAP);
			if (!Bestiary.reached(kills, Bestiary.KNOWN)) {
				y = locked(graphics, font, Bestiary.KNOWN, x, y, columnWidth);
			} else {
				y = stats(graphics, font, entry.type(), x, y, columnWidth);
				String tipKey = "bestiary.progressive_adventure.tip." + id.getNamespace() + "." + id.getPath();
				if (Language.getInstance().has(tipKey)) {
					y = header(graphics, font, "tips", x, y + SECTION_GAP);
					y = wrapped(graphics, font, Component.translatable(tipKey), x, y, columnWidth, AdventureScreen.TEXT);
				}
			}

			y = header(graphics, font, "drops", x, y + SECTION_GAP);
			if (!Bestiary.reached(kills, Bestiary.FAMILIAR)) {
				y = locked(graphics, font, Bestiary.FAMILIAR, x, y, columnWidth);
			} else {
				y = drops(graphics, font, info, kills, x, y, columnWidth, top + height, mouseX, mouseY);
			}

			if (info != null && info.canBeElite()) {
				y = header(graphics, font, "elites", x, y + SECTION_GAP);
				graphics.text(font, Component.translatable("screen.progressive_adventure.bestiary.elites_defeated", progress.eliteKillsOf(key)),
						x, y, AdventureScreen.TEXT);
				y += 10;
				int traitX = x;
				for (String trait : info.eliteTraits()) {
					boolean seen = progress.eliteTraits().contains(key + "/" + trait);
					Component traitName = seen ? Component.translatable("trait.progressive_adventure." + trait)
							: Component.translatable("screen.progressive_adventure.bestiary.unknown_name");
					if (traitX > x && traitX + font.width(traitName) > x + columnWidth) {
						traitX = x;
						y += 10;
					}
					graphics.text(font, traitName, traitX, y, seen ? AdventureScreen.TEXT_GREEN : 0xFF555555);
					traitX += font.width(traitName) + 8;
				}
			}
		}
		graphics.disableScissor();
	}

	/** Health, attack and armor of a fresh mob (no difficulty, gear or elite bonus), in hearts where that fits. */
	private int stats(GuiGraphicsExtractor graphics, Font font, EntityType<?> type, int x, int y, int width) {
		LivingEntity mob = model(type);
		if (mob == null) {
			return y;
		}
		List<Component> parts = new ArrayList<>();
		parts.add(Component.translatable("screen.progressive_adventure.bestiary.health", Progression.formatHearts(Math.round(mob.getMaxHealth()))));
		if (mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
			parts.add(Component.translatable("screen.progressive_adventure.bestiary.attack",
					Progression.formatHearts((int) Math.round(mob.getAttributeBaseValue(Attributes.ATTACK_DAMAGE)))));
		}
		if (mob.getArmorValue() > 0) {
			parts.add(Component.translatable("screen.progressive_adventure.bestiary.armor", mob.getArmorValue()));
		}
		int partX = x;
		for (Component part : parts) {
			if (partX > x && partX + font.width(part) > x + width) {
				partX = x;
				y += 10;
			}
			graphics.text(font, part, partX, y, AdventureScreen.TEXT);
			partX += font.width(part) + 10;
		}
		return y + 10;
	}

	/** Item icons, most likely first; hover shows how often. Exact chances from Expert. */
	private int drops(GuiGraphicsExtractor graphics, Font font, BestiaryInfoPayload info, int kills, int x, int y, int width, int bottom,
			int mouseX, int mouseY) {
		if (info == null) {
			graphics.text(font, Component.translatable("screen.progressive_adventure.bestiary.loading"), x, y, AdventureScreen.TEXT_DIM);
			return y + 10;
		}
		if (info.drops().isEmpty()) {
			graphics.text(font, Component.translatable("screen.progressive_adventure.bestiary.no_drops"), x, y, AdventureScreen.TEXT_DIM);
			return y + 10;
		}
		int perRow = Math.max(1, width / DROP_SLOT);
		for (int i = 0; i < info.drops().size(); i++) {
			Drop drop = info.drops().get(i);
			int slotX = x + (i % perRow) * DROP_SLOT;
			int slotY = y + (i / perRow) * DROP_SLOT;
			graphics.fill(slotX, slotY, slotX + DROP_SLOT - 1, slotY + DROP_SLOT - 1, 0xFF262636);
			graphics.item(drop.item(), slotX, slotY);
			if (mouseY < bottom && PerksTab.isInside(mouseX, mouseY, slotX, slotY, DROP_SLOT - 1, DROP_SLOT - 1)) {
				AdventureScreen.wrappedTooltip(graphics, font, List.of(drop.item().getHoverName(), dropChance(drop, kills)), mouseX, mouseY);
			}
		}
		return y + ((info.drops().size() + perRow - 1) / perRow) * DROP_SLOT;
	}

	private static Component dropChance(Drop drop, int kills) {
		if (Bestiary.reached(kills, Bestiary.EXPERT)) {
			int percent = Math.round(drop.chance() * 100);
			return (percent < 1 ? Component.translatable("screen.progressive_adventure.bestiary.drop_chance_tiny")
					: Component.translatable("screen.progressive_adventure.bestiary.drop_chance", percent)).withStyle(ChatFormatting.GRAY);
		}
		String word = drop.chance() >= 0.5f ? "often" : drop.chance() >= 0.1f ? "sometimes" : "rarely";
		return Component.translatable("screen.progressive_adventure.bestiary.drop_" + word).withStyle(ChatFormatting.GRAY);
	}

	private static int header(GuiGraphicsExtractor graphics, Font font, String section, int x, int y) {
		graphics.text(font, Component.translatable("screen.progressive_adventure.bestiary." + section), x, y, AdventureScreen.TEXT_GOLD);
		return y + 11;
	}

	private static int locked(GuiGraphicsExtractor graphics, Font font, String label, int x, int y, int width) {
		return wrapped(graphics, font, Component.translatable("screen.progressive_adventure.bestiary.locked",
				Component.translatable("familiarity.progressive_adventure." + label), Bestiary.killsFor(label)), x, y, width, 0xFF666666);
	}

	private static int wrapped(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int width, int color) {
		for (FormattedCharSequence line : font.split(text, width)) {
			graphics.text(font, line, x, y, color);
			y += 10;
		}
		return y;
	}

	private LivingEntity model(EntityType<?> type) {
		if (modelType != type) {
			modelType = type;
			model = type.create(Minecraft.getInstance().level, EntitySpawnReason.LOAD) instanceof LivingEntity living ? living : null;
			if (model != null) {
				// The renderer reads the ID (held item seed) and throws while it's unassigned; negative never clashes with real entities
				model.setId(-1);
			}
		}
		return model;
	}

	/** Like the inventory's player model: scaled to fit the box, turns to follow the mouse. */
	private void renderModel(GuiGraphicsExtractor graphics, EntityType<?> type, int x0, int y0, int x1, int y1, int mouseX, int mouseY) {
		LivingEntity mob = model(type);
		if (mob == null) {
			return;
		}
		float centerX = (x0 + x1) / 2f;
		float centerY = (y0 + y1) / 2f;
		float xAngle = (float) Math.atan((centerX - mouseX) / 40f);
		float yAngle = (float) Math.atan((centerY - mouseY) / 40f);
		Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
		Quaternionf xRotation = new Quaternionf().rotateX(yAngle * 20f * (float) (Math.PI / 180));
		rotation.mul(xRotation);

		EntityRenderer<? super LivingEntity, ?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(mob);
		EntityRenderState state = renderer.createRenderState(mob, 1f);
		state.shadowPieces.clear();
		state.outlineColor = 0;
		if (state instanceof LivingEntityRenderState living) {
			living.bodyRot = 180f + xAngle * 20f;
			living.yRot = xAngle * 20f;
			living.xRot = -yAngle * 20f;
			living.boundingBoxWidth = living.boundingBoxWidth / living.scale;
			living.boundingBoxHeight = living.boundingBoxHeight / living.scale;
			living.scale = 1f;
		}
		float size = Math.min((y1 - y0) * 0.8f / Math.max(state.boundingBoxHeight, 0.1f), (x1 - x0) * 0.7f / Math.max(state.boundingBoxWidth, 0.1f));
		Vector3f translation = new Vector3f(0, state.boundingBoxHeight / 2f, 0);
		graphics.entity(state, size, translation, rotation, xRotation, x0, y0, x1, y1);
	}

	/** "Familiar (+10% damage)" */
	private static MutableComponent familiarityLine(FamiliarityTier tier) {
		return Component.translatable("screen.progressive_adventure.bestiary.familiarity",
				Component.translatable("familiarity.progressive_adventure." + tier.label()), Bestiary.formatModifier(tier.damageModifier()));
	}

	private static int tierColor(FamiliarityTier tier) {
		return switch (tier.label()) {
			case "known" -> 0xFFDDDDDD;
			case "familiar" -> 0xFF55FF55;
			case "expert" -> 0xFF55FFFF;
			case "master" -> 0xFFFFAA00;
			default -> 0xFF777777;
		};
	}

	private static FamiliarityTier nextTier(int kills) {
		for (FamiliarityTier tier : PamConfig.get().familiarityTiers) {
			if (tier.kills() > kills) {
				return tier;
			}
		}
		return null;
	}
}
