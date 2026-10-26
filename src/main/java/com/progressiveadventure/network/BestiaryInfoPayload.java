package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Server's answer to {@link BestiaryInfoRequestPayload}. The client decides what to show by the player's familiarity.
 *
 * @param unlockRank             Adventure Rank before it spawns naturally, 0 if never locked
 * @param drops                  normal loot from a player kill, most likely first
 * @param eliteTraits            traits an elite of this type can have, empty when it can't be elite
 * @param canBeElite             whether this type can spawn as an elite
 * @param masterDoubleLootChance chance of double loot at Master
 */
public record BestiaryInfoPayload(String entityType, int unlockRank, List<Drop> drops, List<String> eliteTraits, boolean canBeElite,
		float masterDoubleLootChance) implements CustomPacketPayload {
	public static final Type<BestiaryInfoPayload> TYPE = new Type<>(ProgressiveAdventure.id("bestiary_info"));

	/** @param chance share of kills that drop this item (sampled) */
	public record Drop(ItemStack item, float chance) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Drop> CODEC = StreamCodec.composite(
				ItemStack.STREAM_CODEC, Drop::item,
				ByteBufCodecs.FLOAT, Drop::chance,
				Drop::new);
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, BestiaryInfoPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, BestiaryInfoPayload::entityType,
			ByteBufCodecs.VAR_INT, BestiaryInfoPayload::unlockRank,
			Drop.CODEC.apply(ByteBufCodecs.list()), BestiaryInfoPayload::drops,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), BestiaryInfoPayload::eliteTraits,
			ByteBufCodecs.BOOL, BestiaryInfoPayload::canBeElite,
			ByteBufCodecs.FLOAT, BestiaryInfoPayload::masterDoubleLootChance,
			BestiaryInfoPayload::new);

	@Override
	public Type<BestiaryInfoPayload> type() {
		return TYPE;
	}
}
