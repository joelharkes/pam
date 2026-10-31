package com.progressiveadventure.network;

import com.progressiveadventure.ProgressiveAdventure;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Server's answer to {@link AchievementListRequestPayload}.
 *
 * @param tipCosts XP points per tip, from the server's config so the prices shown are the ones charged
 */
public record AchievementListPayload(List<Group> groups, List<Integer> tipCosts) implements CustomPacketPayload {
	public static final Type<AchievementListPayload> TYPE = new Type<>(ProgressiveAdventure.id("achievement_list"));

	/** One advancement tab. */
	public record Group(Component title, List<Entry> entries) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Group> CODEC = StreamCodec.composite(
				ComponentSerialization.STREAM_CODEC, Group::title,
				Entry.CODEC.apply(ByteBufCodecs.list()), Group::entries,
				Group::new);
	}

	/**
	 * @param secret      hidden by its advancement and not done yet, so the client shows "???"
	 * @param parentDone  the advancement before it is done (or it has none), so it's a sensible next goal
	 * @param rank        Adventure Rank it gives
	 * @param completed   parts done, of {@code total}
	 * @param missing     criterion names of the parts not done, empty when the names would spoil something
	 */
	public record Entry(String id, Component title, Component description, ItemStack icon, boolean secret, boolean done, boolean parentDone,
			int rank, int completed, int total, List<String> missing) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Entry> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Entry::id,
				ComponentSerialization.STREAM_CODEC, Entry::title,
				ComponentSerialization.STREAM_CODEC, Entry::description,
				ItemStack.STREAM_CODEC, Entry::icon,
				ByteBufCodecs.BOOL, Entry::secret,
				ByteBufCodecs.BOOL, Entry::done,
				ByteBufCodecs.BOOL, Entry::parentDone,
				ByteBufCodecs.VAR_INT, Entry::rank,
				ByteBufCodecs.VAR_INT, Entry::completed,
				ByteBufCodecs.VAR_INT, Entry::total,
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), Entry::missing,
				Entry::new);
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, AchievementListPayload> CODEC = StreamCodec.composite(
			Group.CODEC.apply(ByteBufCodecs.list()), AchievementListPayload::groups,
			ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), AchievementListPayload::tipCosts,
			AchievementListPayload::new);

	/** XP points for a tip (from 1); tips past the end of the list cost the last price. */
	public int tipCost(int tip) {
		return tipCosts.isEmpty() ? 0 : tipCosts.get(Math.clamp(tip - 1, 0, tipCosts.size() - 1));
	}

	@Override
	public Type<AchievementListPayload> type() {
		return TYPE;
	}
}
