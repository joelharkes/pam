package com.progressiveadventure.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.function.Consumer;

/**
 * Item component on a journal. The max travels with the item, so the client's ink bar matches the server's config.
 * Also writes the journal's tooltip (registered in {@link JournalContent}).
 */
public record JournalInk(int ink, int max) implements TooltipProvider {
	public static final Codec<JournalInk> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("ink").forGetter(JournalInk::ink),
			Codec.INT.fieldOf("max").forGetter(JournalInk::max)
	).apply(instance, JournalInk::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, JournalInk> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, JournalInk::ink,
			ByteBufCodecs.VAR_INT, JournalInk::max,
			JournalInk::new);

	@Override
	public void addToTooltip(Item.TooltipContext context, Consumer<Component> builder, TooltipFlag flag, DataComponentGetter components) {
		builder.accept(Component.translatable("item.progressive_adventure.explorers_journal.ink", ink, max)
				.withStyle(ink > 0 ? ChatFormatting.BLUE : ChatFormatting.RED));
		builder.accept(Component.translatable("item.progressive_adventure.explorers_journal.refill").withStyle(ChatFormatting.GRAY));
	}
}
