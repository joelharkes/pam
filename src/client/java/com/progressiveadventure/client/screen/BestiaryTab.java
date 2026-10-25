package com.progressiveadventure.client.screen;

import com.progressiveadventure.bestiary.Bestiary;
import com.progressiveadventure.config.PamConfig;
import com.progressiveadventure.config.PamConfig.FamiliarityTier;
import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.PlayerProgress;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Grid of every mob with a spawn egg. Undiscovered mobs show as "?". */
class BestiaryTab implements AdventureTab {
	private static final int HEADER_HEIGHT = 22;
	private static final int CELL = 20;

	private static int page = 0;

	private record Entry(EntityType<?> type, ItemStack icon) {
	}

	private final List<Entry> entries = buildEntries();
	private int gridLeft;
	private int gridTop;
	private int columns;
	private int perPage;

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
		layout(left, top, width, height);
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
	public void render(GuiGraphicsExtractor graphics, int left, int top, int width, int height, int mouseX, int mouseY) {
		Minecraft minecraft = Minecraft.getInstance();
		Font font = minecraft.font;
		PlayerProgress progress = PamAttachments.progress(minecraft.player);
		layout(left, top, width, height);

		long discovered = entries.stream().filter(entry -> progress.killsOf(Bestiary.key(entry.type())) > 0).count();
		graphics.text(font, Component.translatable("screen.progressive_adventure.discovered", discovered, entries.size()),
				left, top + 5, AdventureScreen.TEXT_GOLD);
		if (pageCount() > 1) {
			Component pageText = Component.translatable("screen.progressive_adventure.page", page + 1, pageCount());
			graphics.text(font, pageText, left + width - 50 - font.width(pageText), top + 5, AdventureScreen.TEXT_DIM);
		}

		int start = page * perPage;
		for (int i = start; i < Math.min(entries.size(), start + perPage); i++) {
			Entry entry = entries.get(i);
			int x = gridLeft + ((i - start) % columns) * CELL;
			int y = gridTop + ((i - start) / columns) * CELL;
			int kills = progress.killsOf(Bestiary.key(entry.type()));
			FamiliarityTier tier = Bestiary.tier(kills);

			graphics.fill(x, y, x + CELL - 2, y + CELL - 2, kills > 0 ? 0xFF262636 : 0xFF141418);
			graphics.outline(x, y, CELL - 2, CELL - 2, kills > 0 ? tierColor(tier) : 0xFF2A2A2A);
			if (kills > 0) {
				graphics.item(entry.icon(), x + 1, y + 1);
			} else {
				graphics.centeredText(font, "?", x + (CELL - 2) / 2, y + 5, 0xFF555555);
			}

			if (PerksTab.isInside(mouseX, mouseY, x, y, CELL - 2, CELL - 2)) {
				graphics.setComponentTooltipForNextFrame(font, tooltip(entry, kills, tier), mouseX, mouseY);
			}
		}
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

	private static List<Component> tooltip(Entry entry, int kills, FamiliarityTier tier) {
		List<Component> lines = new ArrayList<>();
		if (kills == 0) {
			lines.add(Component.literal("???").withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable("screen.progressive_adventure.bestiary.undiscovered").withStyle(ChatFormatting.DARK_GRAY));
			return lines;
		}
		lines.add(entry.type().getDescription().copy().withStyle(ChatFormatting.GOLD));
		lines.add(Component.translatable("screen.progressive_adventure.bestiary.kills", kills).withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("screen.progressive_adventure.bestiary.familiarity",
				Component.translatable("familiarity.progressive_adventure." + tier.label()),
				Bestiary.formatModifier(tier.damageModifier())).withStyle(ChatFormatting.GRAY));
		FamiliarityTier next = nextTier(kills);
		if (next != null) {
			lines.add(Component.translatable("screen.progressive_adventure.bestiary.next_tier", next.kills() - kills,
					Bestiary.formatModifier(next.damageModifier())).withStyle(ChatFormatting.DARK_GRAY));
		}
		return lines;
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
