package com.progressiveadventure.journal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Item component on a journal. The max travels with the item, so the client's ink bar matches the server's config. */
public record JournalInk(int ink, int max) {
	public static final Codec<JournalInk> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("ink").forGetter(JournalInk::ink),
			Codec.INT.fieldOf("max").forGetter(JournalInk::max)
	).apply(instance, JournalInk::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, JournalInk> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, JournalInk::ink,
			ByteBufCodecs.VAR_INT, JournalInk::max,
			JournalInk::new);
}
