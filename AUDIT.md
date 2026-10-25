# Code and design audit

From a full code read on 2026-10-25 (all Java, DESIGN.md, CLAUDE.md, README, CHANGELOG, build files), cross-checked
with scripts and the decompiled 26.2 sources. Not playtested. Checked items are fixed; the note says how.

## Bugs

- [x] **1. Relogging heals players with more than 10 hearts** (`milestone/Progression.apply`)
  The hearts modifier is transient, so it isn't saved. On login, health loads while max health is vanilla's 20 HP
  and gets capped there; `apply()` then sees max health "grow" and heals the difference. 15 hearts, log out at 6,
  log back in at 11.
  FIXED: the hearts modifier is permanent now (saved, in place before health loads). Vanilla drops it on death,
  so the respawn heal still works, and keeps it with health when leaving the End. Trade-off: removing PAM from a
  world leaves players' current max health in their saved data (README Known limitations has the `/attribute`
  command to undo it). Players get one last free heal on the first login
  after updating.
- [x] **2. The 7th stomach upgrade did nothing** (`PamConfig.stomach*Multipliers`)
  7 milestones give a stomach upgrade (First Trip was the 7th), but the tables only had levels 0–6. Level 7 reused
  level 6, so the last upgrade a player earned was wasted while the Character tab said "level 7".
  FIXED: 8th entry (×0.6 drain, ×1.4 saturation). Existing config files need it added by hand.
- [x] **3. Character tab draws a third row of hearts over the stomach line** (`client/screen/CharacterTab`)
  Room is kept for 2 rows (20 hearts); 20 milestone hearts + Bonus Hearts can reach 23.
  FIXED: more than two rows overlap like the HUD, so they stay in two rows' space (the skill rows can't move down).
- [x] **4. In-memory state carries over between singleplayer worlds** (`PerkEffects`, `Trackers`, ...)
  Static maps are never cleared when a world closes, and the singleplayer UUID is the same in every world:
  - Second Wind cooldown stores a world time: after a newer world it can be blocked for hours.
  - First Night: dark in world A, open world B by day → milestone granted within a second.
  - Clear Cut queues keep a reference to the closed world.

  FIXED: everything is cleared on `SERVER_STOPPED`. First Night is also forgotten on disconnect (logging out at
  night and back in by day no longer counts as surviving it).

## Smaller issues (to discuss)

- [x] **5.** Revoking an advancement (`/advancement revoke`) doesn't recompute hearts/Rank until the player relogs
  (only `award` is hooked).
  FIXED: `revoke` is hooked too and recomputes right away (recipe unlocks skipped).
- [x] **6.** Replant / Harvester-replant change blocks directly: no break event, no spawn-protection check, so claim
  mods may not see them.
  FIXED: `PerkEffects.mayChangeBlock` does vanilla's break checks (spawn protection, Adventure mode) for every
  extra block of Replant, Harvester, Path Maker, Timber/Lumberjack, Vein Miner and Excavator. No break event is fired
  for Replant (other mods could treat it as a real break), so claim mods only stop it via their right-click check.
- [x] **7.** Path Maker's 8 extra blocks use no durability (Excavator and Vein Miner do).
  FIXED: each extra block uses 1 durability (Sturdy Shovel applies); stops when the shovel breaks.
- [x] **8.** `mobUnlockPlayerRadius` below 128 lets mobs spawning further away skip the lock (no player "near").
  At least say so in the config comment.
  FIXED: no longer a setting; fixed at 128 (vanilla's spawn distance) and measured from the same point as vanilla.
- [x] **11.** (found while discussing 8) The config file had every setting written back on each start, so the
  defaults of the first launch were frozen and rebalances never reached existing installs (singleplayer and LAN too).
  FIXED: the file only holds overrides; `progressive_adventure.defaults.json` is a reference rewritten every start;
  unknown keys give a warning. Existing (pre-release) config files deleted, so no migration.
- [x] **9.** `/pam bestiary` throws if the familiarity tiers in the config don't start at 0 kills.
  FIXED: `Bestiary.tier` never returns null; below the first configured tier it uses "Unknown, no damage change".
  This also fixed the same crash in the Bestiary tab tooltip.
- [x] **10.** Spiders can roll the Leaping trait, which they already have (a wasted trait).
  FIXED: spiders and cave spiders can't get Leaping.

## Known limitations

Things that stay true are listed for players in README.md / docs/modrinth-description.md ("Known limitations").

## Docs out of date

All FIXED (also: README/Modrinth text now invite milestone ideas too, decisions log updated).

- [x] DESIGN perk budget said "every perk costs ~75 points"; it's 206 of ~217 Rank.
- [x] DESIGN said "7 stomach levels" but the metabolism table stopped at level 6 (see bug 2).
- [x] DESIGN bestiary: "Kills by AFK farms count for less or not at all" isn't built.
- [x] DESIGN skills table broken by a stray Fishing row.
- [x] DESIGN playtest checklist: "Sturdy Blade III" is called Sturdy Weapon III.
- [x] DESIGN open question "Tag v0.2.0?" is moot (0.4.0 is the first release).
- [x] CLAUDE.md: Character tab is 5 skill rows of 26px (not 4 of 28px) and full.
- [x] CLAUDE.md: a missing CHANGELOG section fails `releaseNotes`/`modrinth`, not `./gradlew build`.

## Checked and consistent

- 42 milestones: rewards in `Milestones.java` match the "Reward: ..." lang text; 26½ hearts, 24 PAM + 18 vanilla.
- 87 perks: all have name + description; costs/requirements match the DESIGN table; numbers in descriptions match
  the config.
- Waypoints: refunds equal what was paid, costs and range re-checked on the server, `/setblock` removal handled.
- Network handlers re-check everything server-side; mixins match CLAUDE.md.
