package com.progressiveadventure.perk;

import com.progressiveadventure.data.PlayerProgress;
import com.progressiveadventure.data.ProgressionSnapshot;
import com.progressiveadventure.skill.Skill;
import com.progressiveadventure.skill.Skills;

import net.minecraft.network.chat.Component;

/** What a player must have reached before a perk can be bought. Only uses synced data, so the client can check it too. */
public sealed interface Requirement {
	Requirement NONE = new None();

	boolean isMet(PlayerProgress progress, ProgressionSnapshot snapshot);

	/** E.g. "Mining 15", shown on locked perks. */
	Component describe();

	record None() implements Requirement {
		@Override
		public boolean isMet(PlayerProgress progress, ProgressionSnapshot snapshot) {
			return true;
		}

		@Override
		public Component describe() {
			return Component.empty();
		}
	}

	record SkillLevel(Skill skill, int level) implements Requirement {
		@Override
		public boolean isMet(PlayerProgress progress, ProgressionSnapshot snapshot) {
			return Skills.level(progress.xpOf(skill.id)) >= level;
		}

		@Override
		public Component describe() {
			return Component.translatable("requirement.progressive_adventure.skill",
					Component.translatable("skill.progressive_adventure." + skill.id), level);
		}
	}

	record MobTypes(int count) implements Requirement {
		@Override
		public boolean isMet(PlayerProgress progress, ProgressionSnapshot snapshot) {
			return progress.kills().size() >= count;
		}

		@Override
		public Component describe() {
			return Component.translatable("requirement.progressive_adventure.mob_types", count);
		}
	}

	record Rank(int rank) implements Requirement {
		@Override
		public boolean isMet(PlayerProgress progress, ProgressionSnapshot snapshot) {
			return snapshot.rank() >= rank;
		}

		@Override
		public Component describe() {
			return Component.translatable("requirement.progressive_adventure.rank", rank);
		}
	}
}
