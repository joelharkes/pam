package com.progressiveadventure.client.screen;

import com.progressiveadventure.data.PamAttachments;
import com.progressiveadventure.data.PlayerProgress;
import com.progressiveadventure.data.ProgressionSnapshot;
import com.progressiveadventure.milestone.Progression;
import com.progressiveadventure.perk.Perk;
import com.progressiveadventure.perk.PerkCategory;
import com.progressiveadventure.perk.Perks;
import com.progressiveadventure.perk.Requirement;
import com.progressiveadventure.skill.Skill;
import com.progressiveadventure.skill.Skills;
import com.progressiveadventure.world.MobUnlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Rank, hearts, stomach, perk points and the skills in two columns (details in a hover tooltip). */
class CharacterTab implements AdventureTab {
	private static final Identifier HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");
	private static final Identifier HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
	private static final Identifier HEART_HALF = Identifier.withDefaultNamespace("hud/heart/half");
	// Two columns of 26px rows: 4 rows (8 skills) fit below the progress section
	private static final int SKILL_ROW_HEIGHT = 26;
	private static final int SKILL_COLUMN_GAP = 8;
	private static final int SKILL_BAR_WIDTH = 100;

	@Override
	public void render(GuiGraphicsExtractor graphics, int left, int top, int width, int height, int mouseX, int mouseY) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		Font font = minecraft.font;
		PlayerProgress progress = PamAttachments.progress(player);
		ProgressionSnapshot snapshot = PamAttachments.snapshot(player);

		int y = top + 2;
		graphics.text(font, Component.translatable("screen.progressive_adventure.progress"), left, y, AdventureScreen.TEXT_GOLD);

		y += 12;
		Component rank = Component.translatable("screen.progressive_adventure.rank", snapshot.rank());
		graphics.text(font, rank, left, y, AdventureScreen.TEXT_AQUA);
		if (PerksTab.isInside(mouseX, mouseY, left, y - 1, font.width(rank), 10)) {
			graphics.setComponentTooltipForNextFrame(font, List.of(
					Component.translatable("screen.progressive_adventure.rank.tooltip.1"),
					Component.translatable("screen.progressive_adventure.rank.tooltip.2").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
		}
		if (!snapshot.nextMobGroup().isEmpty()) {
			Component nextMobs = Component.translatable("screen.progressive_adventure.next_mobs",
					MobUnlocks.groupName(snapshot.nextMobGroup()), snapshot.nextMobRank());
			int nextX = left + width - font.width(nextMobs);
			graphics.text(font, nextMobs, nextX, y, AdventureScreen.TEXT_RED);
			if (PerksTab.isInside(mouseX, mouseY, nextX, y - 1, font.width(nextMobs), 10)) {
				graphics.setComponentTooltipForNextFrame(font, List.of(
						Component.translatable("screen.progressive_adventure.next_mobs.tooltip.1"),
						Component.translatable("screen.progressive_adventure.next_mobs.tooltip.2").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
			}
		}

		y += 12;
		Component points = Component.translatable("screen.progressive_adventure.perk_points", Perks.pointsAvailable(progress, snapshot));
		graphics.text(font, points, left, y, AdventureScreen.TEXT_PURPLE);
		if (PerksTab.isInside(mouseX, mouseY, left, y - 1, font.width(points), 10)) {
			graphics.setComponentTooltipForNextFrame(font, List.of(
					Component.translatable("screen.progressive_adventure.perk_points.tooltip.1"),
					Component.translatable("screen.progressive_adventure.perk_points.tooltip.2").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
		}

		y += 14;
		graphics.text(font, Component.translatable("screen.progressive_adventure.hearts",
				Progression.formatHearts(snapshot.maxHalfHearts()), snapshot.heartCap()), left, y, AdventureScreen.TEXT);
		drawHearts(graphics, left + 120, y - 1, snapshot.maxHalfHearts());

		y += snapshot.maxHalfHearts() > 20 ? 24 : 14;
		graphics.item(new ItemStack(Items.BREAD), left, y - 4);
		Component stomach = Component.translatable("screen.progressive_adventure.stomach", snapshot.stomach());
		graphics.text(font, stomach, left + 20, y, AdventureScreen.TEXT);
		if (PerksTab.isInside(mouseX, mouseY, left, y - 4, 20 + font.width(stomach), 16)) {
			graphics.setComponentTooltipForNextFrame(font, List.of(
					Component.translatable("screen.progressive_adventure.stomach.tooltip", Math.round(snapshot.hungerDrain() * 100))),
					mouseX, mouseY);
		}

		y += 18;
		graphics.text(font, Component.translatable("screen.progressive_adventure.skills"), left, y, AdventureScreen.TEXT_GOLD);
		y += 12;
		int columnWidth = (width - SKILL_COLUMN_GAP) / 2;
		Skill[] skills = Skill.values();
		for (int i = 0; i < skills.length; i++) {
			int x = left + (i % 2) * (columnWidth + SKILL_COLUMN_GAP);
			int rowY = y + (i / 2) * SKILL_ROW_HEIGHT;
			drawSkill(graphics, font, progress, skills[i], x, rowY, columnWidth, mouseX, mouseY);
		}
	}

	/** Ten hearts per row, like the HUD. More than two rows overlap (also like the HUD) so they fit in two rows' space. */
	private static void drawHearts(GuiGraphicsExtractor graphics, int x, int y, int halfHearts) {
		int hearts = (halfHearts + 1) / 2;
		int rows = (hearts + 9) / 10;
		int rowStep = rows > 2 ? 10 / (rows - 1) : 10;
		for (int i = 0; i < hearts; i++) {
			int hx = x + (i % 10) * 8;
			int hy = y + (i / 10) * rowStep;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER, hx, hy, 9, 9);
			boolean half = i * 2 + 1 == halfHearts;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, half ? HEART_HALF : HEART_FULL, hx, hy, 9, 9);
		}
	}

	private static void drawSkill(GuiGraphicsExtractor graphics, Font font, PlayerProgress progress, Skill skill, int x, int y, int width,
			int mouseX, int mouseY) {
		int xp = progress.xpOf(skill.id);
		int level = Skills.level(xp);
		int levelStart = Skills.xpForLevel(level);
		int levelEnd = Skills.xpForLevel(level + 1);
		PerkCategory category = PerkCategory.valueOf(skill.name());
		Component name = Component.translatable("skill.progressive_adventure." + skill.id);
		Component title = Component.translatable("screen.progressive_adventure.skill_level", name, level);
		int bonusColor = Skills.bonus(skill, level) < 0 ? AdventureScreen.TEXT_RED : AdventureScreen.TEXT_GREEN;

		graphics.item(new ItemStack(category.icon), x, y);
		graphics.text(font, title, x + 20, y, AdventureScreen.TEXT);

		int barX = x + 20;
		int barY = y + 11;
		float fraction = levelEnd > levelStart ? (float) (xp - levelStart) / (levelEnd - levelStart) : 0f;
		graphics.fill(barX, barY, barX + SKILL_BAR_WIDTH, barY + 4, 0xFF2A2A2A);
		graphics.fill(barX, barY, barX + Math.round(SKILL_BAR_WIDTH * fraction), barY + 4, 0xFF55CC55);
		graphics.text(font, Skills.formatBonus(skill, level), barX + SKILL_BAR_WIDTH + 6, barY - 2, bonusColor);

		if (PerksTab.isInside(mouseX, mouseY, x, y - 1, width, SKILL_ROW_HEIGHT - 4)) {
			Perk nextPerk = Perks.nextForSkill(skill, level);
			Component perkLine = nextPerk == null
					? Component.translatable("screen.progressive_adventure.skill_no_perks_left")
					: Component.translatable("screen.progressive_adventure.skill_next_perk", nextPerk.name(),
							((Requirement.SkillLevel) nextPerk.requirement()).level());
			graphics.setComponentTooltipForNextFrame(font, List.of(
					title,
					Skills.describeBonus(skill, level).copy().withColor(bonusColor),
					Component.translatable("screen.progressive_adventure.skill_xp", levelEnd - xp, level + 1).withStyle(ChatFormatting.GRAY),
					perkLine.copy().withStyle(ChatFormatting.GOLD)), mouseX, mouseY);
		}
	}
}
