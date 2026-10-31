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

## Melee skill review (2026-10-26)

A read-through of the Melee skill, shield perks and trident changes after building them. Not playtested.

- [x] **M1. Ground Pound trusted the client to start a dive at any height** (`skill/MeleeMoves`). A modified client
  could send the dive message just before every landing and never take fall damage while holding a mace.
  FIXED: the server also checks there are at least 2 blocks of air below the player (same check as the client).
- [x] **M2. A dive that never "landed" could cancel the fall damage of a later fall.** Ladders, cobwebs and powder
  snow stop a fall without vanilla's landing call, so the dive stayed remembered up to 10 s.
  FIXED: the dive is forgotten on the ground, on a ladder/vine, in water, while gliding, or once the player isn't
  moving down any more (after the first 3 ticks).
- [x] **M3. Disarm played the item-break sound:** a kid could think their own sword broke. FIXED: a short clang.
- [x] **M4. The Deflect arrow marker was saved to disk** although it only lives for one tick. FIXED: not persistent.
- [x] **M5. Three copies of the same cooldown code** (Second Wind, Hearty Kill, Shield Bash), never cleaned on logout.
  FIXED: `perk/Cooldown`; all cleared when the server stops, finished ones dropped on logout (running ones stay, so
  relogging doesn't reset Second Wind).
- [x] **M6. The "hostile mob" filter lived in Archery** and was used by melee and shield code; the "friendly" filter
  was in Melee. FIXED: `skill/Targets` (`isHostile`, `isFriendly`).
- [x] **M7. Bestiary tab stayed on the last mob page after switching worlds** (static UI state). FIXED: the grid
  goes back to page 1 when you leave a world or server.
- [x] **M8. Sturdy Weapon only covered swords.** FIXED: swords, spears, maces and tridents (vanilla's tags); axes
  keep Sturdy Axe. Renamed from "Sturdy Blade" to Sturdy Weapon (its id), listed under "Any weapon".
- [ ] **M9. Disarm also works on mobs hit by the sweep** (15% each), so a big Whirlwind can disarm several. Fine for
  now, documented in DESIGN 3e.
- [ ] **M10. Parry also works against other players** when PvP is on. Fine for now.
- [ ] **M11. Shield Bash swings your arm on every click,** even while the server ignores it (1 s cooldown). Visual only.

## Review of 2026-10-26's code (after 0.5.0)

- [ ] **R1. Loyal Trident is lost when the trident is reloaded** (`mixin/ThrownTridentMixin`). The perk sets
  loyalty only in the throw constructor; vanilla's `ThrownTrident.readAdditionalSaveData` recomputes it from the
  item's enchantments. A trident lying in the ocean when the player logs out (or its chunk unloads, or the server
  restarts) never comes back.
  Fix: mark the trident with a persistent attachment and set loyalty again at the end of `readAdditionalSaveData`.
- `MeleeMoves.onLanding`: `levelEvent(2013)` replaced by `LevelEvent.PARTICLES_SMASH_ATTACK`.
- Master double loot's "not a baby" rule added to the update checklist (copy of `shouldDropLoot`).

## Minecraft update checklist

The mod hooks into vanilla code. Every mixin has `defaultRequire: 1`, so a hook whose target is gone or renamed
**stops the game at startup** (safe: you notice). The dangerous ones are hooks that still apply but could hit the
wrong spot, and code that copies a vanilla rule. Go through this list on every Minecraft update: start the dev
server (`runServer`) and client, then re-read the vanilla code marked "re-read".

**Silent risk (re-read the vanilla method):**

| Hook | Picks | Check that it's still |
|---|---|---|
| `KineticWeaponMixin` (Running Charge) | 1st `Vec3.dot` in `KineticWeapon.damageEntities` | the attacker's speed (`attackerLookVector.dot(getMotion(livingEntity))`) |
| `FishingHookMixin` (bite speed, Keen Eye) | 1st and 3rd `Mth.nextInt` in `FishingHook.catchingFish` | the wait before a fish swims up, and the reel-in window |
| `ProjectileWeaponItemMixin` (Thrifty Quiver) | every `EnchantmentHelper.processAmmoUse` in `useAmmo` (one in 26.2); the 2nd `ItemStack` argument of `useAmmo` | a single ammo-count call; the projectile stack |
| `FoodDataMixin` (Steady Healing) | every `addExhaustion` in `FoodData.tick` (two in 26.2) | only the natural healing costs |
| `FoodDataMixin` (Quick Healing, Deep Reserves) | 1st constant `80` and the constant `18` in `FoodData.tick` | the healing timer (the 2nd `80` is starving) and the food level for healing without saturation |
| `PlayerMixin` Whirlwind | `getEntitiesOfClass` and `isAlliedTo` in `Player.doSweepAttack` | the sweep's target search and team filter; the 3-block distance check (Whirlwind's radius) |

**Copies of vanilla rules (update by hand if vanilla changes):**

| Where | Copies |
|---|---|
| `MeleeMoves.smashBonus` | `MaceItem.getAttackDamageBonus` fall formula (Shockwave damage) |
| `MeleeMoves.SMASH_RADIUS` (3.5), `Melee.WHIRLWIND_RADIUS` (3) | mace smash knockback reach; sweep distance check |
| `MeleeMoves.onLanding` | relies on `Block.fallOn` calling `causeFallDamage` on every landing |
| `ShieldPerks.inParryWindow` | shield blocks only after `BlocksAttacks.blockDelayTicks` |
| Client `ShieldBash` | `Minecraft.handleKeybinds` drops attack clicks while using an item (Fabric's start-of-tick runs first) |
| `Melee.disarm` | skeletons switch to melee when their main hand changes (`AbstractSkeleton.setItemSlot`) |
| `PlayerMixin.pam$rememberAttackStrength` | vanilla resets the attack strength in `onAttack()` before the hit lands |
| `LivingEntityMixin.pam$drawSpeed` | bow/crossbow/trident power comes from `useItemRemaining` |
| `MobUnlocks` (128 blocks) | vanilla's spawn/despawn distance |
| `Milestones.VANILLA` + lang overrides | vanilla advancement ids and their description keys |
| `Targets.isFriendly` | shulkers extend `AbstractGolem` |
| `Bestiary.onKill` (Master double loot) | `LivingEntity.shouldDropLoot` = not a baby (and the `mobDrops` game rule) |

**Also check:** vanilla mob list vs. the bestiary hint/tip lang keys and `mobUnlocks`; item tags used
(`SWORDS`, `SPEARS`, `MACE_ENCHANTABLE`, `TRIDENT_ENCHANTABLE`, ore/log tags); `fishing_treasure` tag contents;
GUI classes for the Adventure screen (`GuiGraphicsExtractor`, `extractRenderState`).

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

## Checked and consistent (2026-10-25 audit; counts are from then, there are more perks and milestones now)

- 42 milestones: rewards in `Milestones.java` match the "Reward: ..." lang text; 26½ hearts, 24 PAM + 18 vanilla.
- 87 perks: all have name + description; costs/requirements match the DESIGN table; numbers in descriptions match
  the config.
- Waypoints: refunds equal what was paid, costs and range re-checked on the server, `/setblock` removal handled.
- Network handlers re-check everything server-side; mixins match CLAUDE.md.
