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

/** Rank, hearts, stomach, perk points and one row per skill. */
class CharacterTab implements AdventureTab {
	private static final Identifier HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");
	private static final Identifier HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
	private static final Identifier HEART_HALF = Identifier.withDefaultNamespace("hud/heart/half");

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
		Component rank = Component.translatable("screen.progressive_adventure.rank", snapshot.rank(), snapshot.rank(), snapshot.milestones());
		graphics.text(font, rank, left, y, AdventureScreen.TEXT_AQUA);
		if (PerksTab.isInside(mouseX, mouseY, left, y - 1, font.width(rank), 10)) {
			graphics.setComponentTooltipForNextFrame(font, List.of(
					Component.translatable("screen.progressive_adventure.rank.tooltip.1"),
					Component.translatable("screen.progressive_adventure.rank.tooltip.2").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
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
		graphics.text(font, Component.translatable("screen.progressive_adventure.stomach", snapshot.stomach(),
				Math.round(snapshot.hungerDrain() * 100)), left + 20, y, AdventureScreen.TEXT);

		y += 18;
		graphics.text(font, Component.translatable("screen.progressive_adventure.skills"), left, y, AdventureScreen.TEXT_GOLD);
		y += 12;
		for (Skill skill : Skill.values()) {
			drawSkill(graphics, font, progress, skill, left, y, width);
			y += 28;
		}
	}

	/** Ten hearts per row, like the HUD. */
	private static void drawHearts(GuiGraphicsExtractor graphics, int x, int y, int halfHearts) {
		int hearts = (halfHearts + 1) / 2;
		for (int i = 0; i < hearts; i++) {
			int hx = x + (i % 10) * 8;
			int hy = y + (i / 10) * 10;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER, hx, hy, 9, 9);
			boolean half = i * 2 + 1 == halfHearts;
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, half ? HEART_HALF : HEART_FULL, hx, hy, 9, 9);
		}
	}

	private static void drawSkill(GuiGraphicsExtractor graphics, Font font, PlayerProgress progress, Skill skill, int x, int y, int width) {
		int xp = progress.xpOf(skill.id);
		int level = Skills.level(xp);
		int levelStart = Skills.xpForLevel(level);
		int levelEnd = Skills.xpForLevel(level + 1);
		PerkCategory category = PerkCategory.valueOf(skill.name());

		graphics.item(new ItemStack(category.icon), x, y);
		Component name = Component.translatable("skill.progressive_adventure." + skill.id);
		graphics.text(font, Component.translatable("screen.progressive_adventure.skill_level", name, level), x + 20, y, AdventureScreen.TEXT);
		graphics.text(font, Component.translatable("screen.progressive_adventure.skill_speed", Skills.formatBonus(level)),
				x + 150, y, Skills.speedBonus(level) < 0 ? AdventureScreen.TEXT_RED : AdventureScreen.TEXT_GREEN);

		int barX = x + 20;
		int barY = y + 11;
		int barWidth = 120;
		float fraction = levelEnd > levelStart ? (float) (xp - levelStart) / (levelEnd - levelStart) : 0f;
		graphics.fill(barX, barY, barX + barWidth, barY + 4, 0xFF2A2A2A);
		graphics.fill(barX, barY, barX + Math.round(barWidth * fraction), barY + 4, 0xFF55CC55);
		graphics.text(font, (xp - levelStart) + " / " + (levelEnd - levelStart), barX + barWidth + 6, barY - 2, AdventureScreen.TEXT_DIM);

		Perk next = nextPerk(skill, level);
		if (next != null) {
			graphics.text(font, Component.translatable("screen.progressive_adventure.next_perk", next.name(),
					((Requirement.SkillLevel) next.requirement()).level()), x + 20, y + 18, AdventureScreen.TEXT_DIM);
		}
	}

	/** The perk with the lowest skill requirement above the current level. */
	private static Perk nextPerk(Skill skill, int level) {
		Perk best = null;
		for (Perk perk : Perks.inCategory(PerkCategory.valueOf(skill.name()))) {
			if (perk.requirement() instanceof Requirement.SkillLevel req && req.skill() == skill && req.level() > level
					&& (best == null || req.level() < ((Requirement.SkillLevel) best.requirement()).level())) {
				best = perk;
			}
		}
		return best;
	}
}
