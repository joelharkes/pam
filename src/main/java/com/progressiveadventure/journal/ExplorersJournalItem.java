package com.progressiveadventure.journal;

import com.progressiveadventure.config.PamConfig;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Draws the shared map while it's in your inventory (see {@link Journals}); right-click to look at it. Click ink sacs,
 * glow ink sacs or coal onto it in your inventory to refill it, like putting things in a bundle.
 */
public class ExplorersJournalItem extends Item {
	/** Set by the client: opens the journal screen. The common code can't reference client classes. */
	public static Runnable openScreen = () -> { };

	public ExplorersJournalItem(Properties properties) {
		super(properties);
	}

	public static JournalInk ink(ItemStack stack) {
		JournalInk ink = stack.get(JournalContent.INK);
		return ink != null ? ink : new JournalInk(0, PamConfig.get().journalMaxInk);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			openScreen.run();
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess carriedItem) {
		Integer perItem = PamConfig.get().journalInkRefills.get(BuiltInRegistries.ITEM.getKey(other.getItem()).toString());
		if (perItem == null || perItem <= 0 || !slot.allowModification(player)) {
			return false;
		}
		JournalInk ink = ink(self);
		// Never waste ink: only as many items as fit completely (right-click: one at a time)
		int fits = (ink.max() - ink.ink()) / perItem;
		int used = Math.min(action == ClickAction.SECONDARY ? 1 : other.getCount(), fits);
		if (used <= 0) {
			player.playSound(SoundEvents.BUNDLE_INSERT_FAIL, 1.0f, 1.0f);
			return true;
		}
		other.shrink(used);
		self.set(JournalContent.INK, new JournalInk(ink.ink() + used * perItem, ink.max()));
		player.playSound(SoundEvents.INK_SAC_USE, 0.8f, 1.0f);
		return true;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		JournalInk ink = ink(stack);
		return ink.max() <= 0 ? 0 : Mth.clamp(Math.round(13.0f * ink.ink() / ink.max()), 0, 13);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return ink(stack).ink() > 0 ? 0x3F76E4 : 0xFF5555;
	}
}
