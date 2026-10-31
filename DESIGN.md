# PAM: Progressive Adventure Mod — Design

> Living document: how the mod works and why, decisions, ideas and open questions.
> The pillars describe what is **built**; everything not built yet lives in [Ideas](#ideas).

**Status (2026-10-26):** 0.5.0 released (Modrinth Alpha). All 7 pillars are built: hearts, metabolism,
Mining/Woodcutting/Digging/Farming/Melee/Archery/Fishing skills, bestiary, mob unlocks, elites + traits, 111 perks in 10
categories, the Adventure screen and waypoints. Much of it still needs a playtest: see the
[playtest checklist](#playtest-checklist).

## Vision

Vanilla Minecraft front-loads your power: you spawn with full health and hunger, and
the whole mob roster is out to get you on night one. The only progression is gear.

This mod makes the **player character and the world** progress together:

1. **Start weak, grow strong.** Fewer hearts and a hungrier body at the start. You earn
   upgrades through explicit, fun milestones.
2. **Get better by doing.** Repeating an action makes you better at it, with fast
   early gains that taper off.
3. **The world opens up as you grow.** Early nights only have a few basic mobs. New
   mob types, and rare stronger variants with better loot, appear as you progress.
4. **Exploring makes you stronger.** The best upgrades come from going somewhere new
   or trying something different, not from repeating the same thing.

### Principles (decided)

- **Fun first, also for kids.** No death penalty. Being strong at the end is the
  reward: a mob you've killed 100 times *should* feel easier than in vanilla.
- **The world never outgrows the player.** Difficulty follows your progress, but
  you should almost always feel stronger than what you face.
- **Explicit over invisible.** Upgrades come from named milestones with a toast and a
  checklist, not from silent stat creep.
- **Diminishing returns.** Big gains early, small gains later, so grinding isn't the
  best strategy.
- **Works on servers and LAN**, with players at different progress levels.
- **Configurable.** All numbers live in config.

### Non-goals (decided)

Things we deliberately leave out. Check here before building an idea that touches them.

- **Fighting farms or kill chambers.** No nerfs, no detection, no penalties. Progress needs the player
  to do the thing (land the hit, harvest the crop), so fully automatic farms give no progress, but
  anything where the player is involved counts fully. The goal is for a stronger player to need farms
  less, not for farms to stop working.
- **Mobs that scale with the player.** The world opens up with Rank (new mob types, rare elites), but
  normal mobs never get tougher to match you (see [Risks](#risks--things-to-watch), level-scaling treadmill).
- **A death penalty.** See Principles.
- **Several Minecraft versions at once.** One version at a time, no multi-version builds.
- **Server-only or client-only installs.** The mod is needed on both sides (synced progress, the
  Adventure screen, client-side perks).

---

## Core idea: one source, two rewards

```
ANY advancement (vanilla + PAM)
  ├──► +1/2/3 Adventure Rank (task/goal/challenge) ──► mob unlocks, elites
  │         └── every rank is also a perk point ──► spend on perks
  └──► some of them (milestones) also give hearts or a stomach upgrade
skill levels / mob types / rank ──► unlock which perks you may buy
```

- **Adventure Rank = perk points earned** (about 220 in total). Spending or resetting
  perks never changes it, so buying a perk never makes the world harder and a reset
  can't make it easier.
- **Milestones** are advancements with a heart/stomach reward: our own (Adventure tab)
  for things vanilla doesn't have, plus a list of vanilla advancements. No achievement
  exists twice, so only one toast pops.
- Hearts/stomach always grow automatically; perks are the choosable extras on top, so
  two players in the same world can grow differently. See pillar 6.
- One number (Rank) drives the world's difficulty, and every rank also gives a perk
  point, so the player gets stronger with every step the world takes.

### Milestones are vanilla advancements

Milestones are advancements: our own in the "Adventure" tab, or vanilla ones with a
reward added (their description is overridden in our lang file to show "Reward: ..."):

- The advancement screen, toasts and chat announcements come for free.
- Mostly data-driven (JSON), so adding a milestone is cheap.
- Rewards are never stored: hearts and stomach are derived from completed advancements, so they can't
  double-grant and balance changes reach existing players.
- Counts the vanilla triggers can't express (foods, crops, biomes, mob types, waypoints) are granted from
  code through a single `granted` impossible criterion.

---

## Feature pillars

### 1. Vitality: hearts from milestones

- Start with **5 hearts** (configurable). Cap around **15–20 hearts** in the late game.
- Each milestone grants **half a heart, a full heart or a stomach upgrade**. Big ones
  (bosses) grant more.
- A `max_health` attribute modifier (Bonus Heart perks add on top and ignore the cap).

#### Milestone list

What makes a good milestone:
- ✅ Makes you **go somewhere new** (biome, structure, dimension).
- ✅ Makes you **try a mechanic** you might skip (brewing, trading, taming, fishing).
- ✅ Clear and checkable: a kid knows when it's done.
- ❌ Pure grind ("break 10,000 stone", "walk 100 km").
- ❌ Luck-only or tedious (rare drops, full collections of everything).

Implemented: 26 PAM milestones + 18 vanilla advancements with a reward (half hearts unless noted):

| Category | Milestone (PAM tab)                        | Reward         |
|----------|--------------------------------------------|----------------|
| Survivor | Survive your first night                   | ½ heart        |
| Survivor | Wear a full set of iron armor              | ½ heart        |
| Gourmet  | Eat your first food / 3 / 8 / 15 foods     | stomach each   |
| Gourmet  | Eat 25 different foods                     | 1 heart        |
| Farmer   | Harvest 3 different crops                  | stomach        |
| Farmer   | Harvest 10 different crops                 | ½ heart        |
| Farmer   | Breed 5 different kinds of animals         | ½ heart        |
| Explorer | Visit 5 / 15 / 30 biomes                   | ½ / ½ / 1 heart|
| Explorer | Find a village                             | ½ heart        |
| Explorer | Turn on a Waypoint Stone                   | ½ heart        |
| Explorer | Travel between two Waypoint Stones         | stomach        |
| Explorer | Have 5 of your own Waypoint Stones on      | 1 heart        |
| Hunter   | Defeat 5 / 15 / 30 different mob types     | ½ / ½ / 1 heart|
| Hunter   | Defeat the Elder Guardian / the Wither     | 2 hearts each  |
| Hunter   | Defeat an elite (Elite Slayer)             | ½ heart        |
| Crafter  | Get a diamond pickaxe                      | ½ heart        |
| Angler   | Fish up a treasure (Sunken Treasure)       | ½ heart        |
| Angler   | Catch all 4 kinds of fish (Fish Collector) | ½ heart        |

| Vanilla advancement                         | Reward   |
|---------------------------------------------|----------|
| Sweet Dreams, What a Deal!, Isn't It Iron Pick, Diamonds!, Not Today Thank You, Enchanter, Best Friends Forever, Fishy Business, Local Brewery, A Terrible Fortress, Those Were the Days | ½ heart each |
| We Need to Go Deeper, The End?, Zombie Doctor, Hero of the Village, The City at the End of the Game | 1 heart each |
| Free the End                                | 2 hearts |
| A Balanced Diet                             | stomach  |

Totals: 27½ hearts to earn (start 5, cap 20: 15 to earn, so there are many routes to
the cap) and 7 stomach upgrades (levels 0–7). 20 is our config cap, not a vanilla limit (vanilla
max_health goes to 1024); 20 hearts = two full rows on the HUD.

### 2. Metabolism: hunger annoying early, easy later

Keep the vanilla hunger bar size (20). Changing it is technically hard and breaks
regeneration rules and other mods. Instead, change **how hungry you get**:

- **Early game:** hunger drains faster (e.g. +50% exhaustion) and food gives less
  saturation. You have to think about food.
- **Stomach upgrades** (from Gourmet/Farmer milestones) step this down, ending
  *better* than vanilla: slower drain, more saturation, cheaper sprinting.
  The first two come fast (first food, 3 different foods) so progress is felt in
  the first session:

| Stomach level | Hunger drain | Food saturation | Reached by (typical)     |
|---------------|--------------|-----------------|--------------------------|
| 0             | ×1.5         | ×0.6            | start                    |
| 1             | ×1.35        | ×0.7            | first food               |
| 2             | ×1.2         | ×0.8            | 3 different foods        |
| 3             | ×1.0         | ×1.0 (vanilla)  | 8 different foods        |
| 4             | ×0.9         | ×1.1            | 15 different foods       |
| 5             | ×0.8         | ×1.2            | 3 different crops        |
| 6             | ×0.7         | ×1.3            | First Trip (waypoints)   |
| 7             | ×0.6         | ×1.4            | A Balanced Diet          |

- Stomach levels can come in any order; the table shows a typical one. The config lists need one entry per
  level (8 now); a missing entry reuses the last one, so a new stomach milestone needs a new entry too.
- A mixin on `FoodData` scales exhaustion and saturation. No HUD changes.

### 3. Proficiency: get better by doing

Use-based skills that level up with use. Gains follow a **diminishing curve**: the
first levels come quickly and feel great, later levels are slow and small.
Like the bestiary, skills **start below vanilla (−20% speed)**, reach vanilla around
level 4 (~64 blocks) and cap near +60%.

| Skill       | Trained by                       | Passive bonus                    |
|-------------|----------------------------------|----------------------------------|
| Mining      | Pickaxe blocks (ores give 3 XP)  | Break speed                      |
| Woodcutting | Axe blocks                       | Break speed                      |
| Digging     | Shovel blocks                    | Break speed                      |
| Farming     | Harvesting grown crops, breeding | Extra harvest chance (see 3b)    |
| Melee       | Hand-to-hand hits on mobs, kills | Attack speed (see 3e)            |
| Archery     | Arrow/trident hits on mobs, kills | Bow/crossbow/trident draw speed (see 3c) |
| Fishing     | Catches (fish, junk, treasure)   | Bite speed (see 3d)              |

- Level = floor(√(xp / 4)); bonus curve and numbers are in the config.
- Farm blocks (crops, melons, pumpkins, cocoa, cane, cactus, bamboo) only train Farming, even
  though vanilla lets an axe mine some of them.
- The Character tab shows skills in two columns of 4 rows: 8 fit (7 used).
- No skill XP from blocks the player placed (stops place/break loops). Only logs, ores and
  melon/pumpkin/cane/cactus/bamboo are remembered (max 10,000 per chunk, oldest forgotten); placed
  dirt, stone, sand, gravel, leaves and planks count like natural blocks.
- **Perks** at certain levels are the exciting part. Percentages alone feel flat.

### 3b. Farming skill

**XP sources:**

| Action | XP |
|---|---|
| Harvesting a fully grown crop (wheat, carrots, potatoes, beetroot, torchflower, nether wart, cocoa) | 1 |
| Harvesting melons and pumpkins (grown from stems, never player-placed) | 2 |
| Sugar cane, cactus, bamboo (only grown parts; the planted block is marked as placed) | 1 |
| Picking sweet berries (right-click) | 1 |
| Breeding animals (both players get XP if two players fed the parents) | 5 |

- No XP for planting or tilling (spammable), unripe crops, or automatic farms
  (only blocks a player breaks count). Bone meal grinding is accepted: it costs bones.
- **Passive bonus:** chance of +1 extra crop, 0% → ~50% with the usual diminishing curve.
- **No start penalty** (unlike Mining/bestiary): food is already harder early, a second
  penalty on food would be harsh for kids.

**Farming perks:**

| Perk | Effect | Requires | Cost |
|---|---|---|---|
| Light Step | You no longer trample farmland | — | 1 |
| Replant | Right-click a grown crop to harvest and replant it | Farming 5 | 2 |
| Sturdy Hoe I/II/III | 10/20/30% chance hoes use no durability | — / Farming 8 / 18 | 1/2/3 |
| Bountiful Harvest I/II | 10%/20% chance of double crop drops | Farming 10/25 | 2/3 |
| Harvester | Breaking or Replanting a grown crop with a hoe also does the grown crops in a 3×3 area | Farming 20 | 3 |
| Rancher | 15% chance of twins when breeding | Farming 3 | 2 |
| Green Thumb | 25% chance bone meal isn't used up | Farming 13 | 2 |

**Farmer milestones:** harvest 3 different crops (stomach), harvest 10 different crops
(½ heart), breed 5 different kinds of animals (½ heart).

### 3c. Archery skill

- **XP:** 3 per arrow a player shot that hits a mob, 5 for a kill with it (raised from 1/3: hits
  are much rarer than mined blocks; Archery 20 ≈ 320 kills, 40 ≈ 1300). Any mob counts (animals
  too), but not players, armor stands or blocked hits. Thrown tridents count like arrows (stabbing with one
  trains Melee).
- **Passive bonus:** bow, crossbow and trident draw speed, 0% → +50% with the usual curve. No start
  penalty: unknown mobs already take less damage early. The use timer itself runs faster, so the
  pull animation, zoom and crossbow charge match the real power.
- **No damage perks:** the bestiary is the damage progression (and already counts arrows).
  Damage perks would undercut it and need matching sword perks.

| Perk | Effect | Requires | Cost |
|---|---|---|---|
| Thrifty Quiver I/II/III | 10/20/30% chance a shot uses no arrow (like Infinity: that arrow can't be picked up) | Archery 3/8/18 | 1/2/3 |
| Fire Arrows | 15% chance the arrow is on fire | Archery 10 | 2 |
| Frost Arrows | 15% chance the arrow slows the mob it hits (Slowness II, 3s), snowflake trail | Archery 6 | 2 |
| Piercing Shot | Arrows fly through 1 extra mob (stacks with the Piercing enchantment) | Archery 15 | 2 |
| Steady Aim | Arrows turn up to 2°/tick toward the hostile mob nearest their path (15° cone, 20 blocks, in sight). Neutral mobs only when they target you; never animals, villagers, pets | Archery 20 | 3 |
| Ricochet | After hitting (or killing) a mob, a new arrow flies to the nearest other hostile mob within 8 blocks, with the same damage, fire and frost. Bounces once; can't be picked up | Archery 40 | 5 |
| Loyal Trident | Thrown tridents come back like Loyalty I (tridents that already have Loyalty keep theirs) | Archery 13 | 2 |

The arrow perks (Thrifty Quiver, Fire/Frost, Piercing Shot, Steady Aim, Ricochet) stay bows/crossbows only.

Poison arrows were dropped: zombies and skeletons are immune, so it would look broken to kids.

### 3d. Fishing skill

- **XP per catch:** fish 4, junk 2, treasure 10 (catches are slow: Fishing 10 ≈ 100 catches, 25 ≈ 600).
  Treasure = vanilla's treasure loot: name tag, saddle, nautilus shell, enchanted book (item tag
  `progressive_adventure:fishing_treasure`) or an enchanted bow/rod. Hooking mobs or items gives nothing.
- **Passive bonus:** bite speed, **+0% → +60%** with the usual curve. Starts at vanilla (no −20%): waiting is
  already the slow part. The wait before a fish swims up is divided by (1 + bonus), on top of Lure.
- AFK fish farms count (the player holds the rod), see Non-goals.

| Perk | Effect | Requires | Cost |
|---|---|---|---|
| Keen Eye | A bell rings when a fish bites; +1s (20 ticks) to reel in (vanilla 1–2s) | Fishing 3 | 1 |
| Lucky | +2 luck (vanilla attribute, like Luck of the Sea II): more treasure, less junk | Fishing 8 | 2 |
| Double Catch | 25% chance a fish catch gives the fish twice | Fishing 15 | 3 |
| Treasure Hunter | Treasure can be caught without open water (small ponds) | Fishing 25 | 4 |

Implementation: `FishingHookMixin` changes the wait and the reel window (`Mth.nextInt` calls in
`catchingFish`), the caught items (`retrieve`, before the advancement trigger, so Double Catch fish count)
and `isOpenWaterFishing` (Treasure Hunter). Campfire Catch (cooked fish) stays an idea, with auto-smelt for Mining.

### 3e. Melee skill

One skill for every hand-to-hand weapon (sword, axe, mace, spear, trident stab), with perks grouped by weapon in
the Melee perk category. Built in four slices on 2026-10-26: a (skill, Wide Sweep, Hearty Kill), b (Whirlwind, Disarm, axe perks), c (shield
perks, in Defense), d (mace and spear).

- **XP:** 1 per hit on a mob that deals at least 2 damage (1 heart), 4 for a kill. The minimum stops spam-clicked
  weak hits (cooldown not filled) from counting. Sweeps, mace smashes and spear attacks count; thorns and
  explosions don't. Like Archery: any mob, not players, armor stands or blocked hits.
- **Passive bonus:** attack speed (how fast the attack cooldown fills), 0% → +30% with the usual curve, no start
  penalty. An `attack_speed` modifier that multiplies the total, so every weapon gets the same share faster
  (multiplying the base would add the same +1.2 attacks/s to a slow axe and a fast sword).
- **No damage bonus on your main hit:** the bestiary is the damage progression. Wide Sweep only changes the
  damage the *other* mobs take.
- **Sweeps hit like vanilla** (everything near the target that isn't on your team, also pets and villagers) unless
  you own **Careful Sweep**. The user wanted vanilla kept and the protection as a choice.

| Group | Perk | Effect | Requires | Cost |
|---|---|---|---|---|
| Any weapon | Quick Study | Kills count double towards familiarity | 5 mob types defeated | 2 |
| Any weapon | Sturdy Weapon I/II/III | 10/20/30% chance swords, spears, maces and tridents use no durability (axes: Sturdy Axe) | 5 / 15 / 25 mob types | 1/2/3 |
| Sword | Careful Sweep | Your sweep no longer hurts tamed pets, villagers or golems (not shulkers) | Melee 3 | 1 |
| Sword | Wide Sweep I/II | Sweep damage to the other mobs +25% / +50% of the hit (`sweeping_damage_ratio`, stacks with Sweeping Edge up to 1.0) | Melee 10 / 25 | 2/3 |
| Sword | Whirlwind | A sweep while sneaking hits every hostile mob within 3 blocks around you (not only next to the target), ring of sweep particles + wind sound. Same sweep damage, so it needs Wide Sweep I | Melee 18, Wide Sweep I | 3 |
| Sword | Disarm | 15% per sword hit: the mob's main-hand item drops next to it (skeletons then walk up and punch; zombies may pick it up again) | Melee 13 | 2 |
| Axe | Heavy Swing | Fully charged axe hit: extra knockback (1.0) and Slowness II for 2 s | Melee 8 | 2 |
| Axe | Sunder | Fully charged axe hit: the mob's armor −50% for 5 s (shield-break sound, crit sparks). The answer to elites in diamond gear | Melee 20 | 3 |
| Mace | Ground Pound | Holding a mace in the air (at least 2 blocks up), press sneak: dive straight down (1.5 blocks/tick). No fall damage on landing; hostile mobs within 3.5 blocks are pushed away like by vanilla's smash | Melee 15 | 3 |
| Mace | Shockwave | A mace smash that hits a mob, and a Ground Pound landing, also hurt the hostile mobs within 3.5 blocks for 30% of vanilla's fall bonus (e.g. 5 blocks: 16 → ~5 damage) | Melee 30 | 4 |
| Spear | Running Charge | On foot, the spear charge counts your speed ×1.75 (sprint ≈ 5.6 → 9.8 blocks/s): more charge damage, and riders get knocked off (iron spear: 11) | Melee 6 | 2 |
| Spear | Pinning Jab | Every hostile mob a jab hits gets Slowness II for 1.5 s | Melee 12 | 2 |

- "Fully charged" = cooldown over 90%, like vanilla's crit/sweep check. Vanilla resets the strength before the hit
  lands, so `PlayerMixin` stores it at the start of `Player.attack` (`PlayerAttackAccess`).
- Whirlwind wraps vanilla's sweep (`doSweepAttack`): another search box around the player and a hostile-only filter
  (Steady Aim's: hostile, or neutral and targeting you). Vanilla's 3-block distance check stays, so 3 is the radius.
- Sunder: a transient `armor` modifier (multiply total −0.5) that `Melee.tick` removes after 5 s. It lands with the
  hit, so it helps the *next* hits, not the one that cracked the armor. Disarm and the axe perks run on hits that
  give Melee XP (≥ 1 heart), also the ones from a sweep (so Whirlwind can disarm several mobs).
- Ground Pound is a client move (`AirMoves`, sneak pressed in the air, `GroundPoundPayload`); the server remembers
  the dive and handles the landing in `Player.causeFallDamage` instead of fall damage. The server also checks the
  2 blocks of air (else a modified client could cancel any fall). The dive is forgotten on the ground, on a ladder,
  in water, while gliding, once you stop moving down, or after 10 s. The dive keeps fall distance, so hitting a mob
  mid-dive is a big vanilla smash.
- Vanilla already does what the first ideas for these were: the mace smash pushes everything within 3.5 blocks
  (so Shockwave deals damage instead of pushing) and the spear jab already hits every mob along its reach (so
  Skewer became Pinning Jab). A sprint already passes the spear's damage and knockback speeds, so Running Charge
  mostly adds damage and the dismount.
- Running Charge scales the attacker's speed in `KineticWeapon.damageEntities` (first `dot`); Pinning Jab wraps
  `stabAttack` in `PiercingWeapon.attack`. Shockwave hits use player-attack damage (they count as Melee hits).

Hearty Kill (heal on a kill) is in Survival, since it works with any weapon.

**Shield perks** (Defense category, "Shield" heading; blocking is defense):

| Perk | Effect | Requires | Cost |
|---|---|---|---|
| Shield Bash | Attack click while blocking: pushes hostile mobs within 3 blocks in front of you away (once per second) | 5 mob types | 2 |
| Parry | A melee hit blocked within 0.5 s after the shield starts blocking goes back to the attacker (thorns damage credited to you, max 4 hearts) with knockback; "Parry!" + clang; 3 Melee XP | 10 mob types | 3 |
| Deflect | An arrow blocked in the same window flies straight back at its shooter (now your arrow) | 15 mob types, Parry | 2 |

- Window: vanilla's shield only blocks 5 ticks after raising; Parry counts the next 10 ticks (generous for ping).
  Holding the shield up doesn't parry: you raise it at the right moment.
- Explosions (direct entity = the creeper), piercing arrows (vanilla doesn't block them) and the Warden's sonic
  boom (bypasses shields) can't be parried. Parry uses thorns damage so it doesn't count as a Melee hit.
- Deflect marks the arrow when it's blocked; after vanilla bounces it (`AbstractArrow.onHitEntity` tail) it's aimed
  at the shooter, a little high for gravity, owned by the player.
- Shield Bash: vanilla drops attack clicks while an item is used, so the client checks at the start of its tick
  (before vanilla's key handling) and sends `ShieldBashPayload`; the server checks perk, blocking and cooldown.

### 4. Bestiary: mob familiarity

Each mob type you defeat builds familiarity, which changes how much damage you deal
to that type. **Unknown mobs are a bit tougher than in vanilla**, you break even
after a few kills, and then gains are big early and flatten out:

| Kills | Damage modifier | Label      |
|-------|-----------------|------------|
| 0     | −15%            | Unknown ("?" in the bestiary) |
| 1     | −10%            | Unfamiliar (mob appears in the bestiary) |
| 3     | −5%             | Unfamiliar |
| 5     | ±0% (vanilla)   | Known      |
| 15    | +5%             | Familiar   |
| 30    | +10%            | Familiar   |
| 60    | +15%            | Familiar   |
| 100   | +20%            | Expert     |
| 200   | +30%            | Master     |
| 500   | +35%            | Master (cap) |

- Example: a zombie (20 HP) vs. a wooden sword (4 damage) takes 5 hits in vanilla
  and 6 hits at −15%. Noticeable, not brutal.
- The label is visible (Bestiary tab, `/pam bestiary`, a toast on a new entry and an action-bar message
  on every new tier) so a penalty never feels like a bug: "I don't know this mob yet."
- An elite kill counts as 3 kills; the Quick Study perk doubles kills.
- **Assist kills:** everyone who hurt the mob during the fight gets the bestiary kill, not only the player
  who landed the last hit (vanilla's combat tracker already lists them, so no extra cost). Only the bestiary
  kill: loot, XP, Archery XP and the elite milestone stay with the killer. `assistKills` in config.
- The −15% stacks with the other early-game penalties (5 hearts, faster hunger).
  This is OK because early ranks only spawn zombies. Watch in playtests.
- At Master (200 kills) a zombie clearly goes down faster than in vanilla, but it takes several play sessions:
  zombies are almost the only early mob, so faster tiers (Master at 100, +50%) made the early game easy too soon.
- Only kills credited to a player count (the player or their arrow dealt the killing blow), so fully
  automatic farms give nothing. AFK farms where the player still lands the hit (kill chambers)
  count fully, on purpose (see [Non-goals](#non-goals-decided)).
- We keep our own kill counter (not vanilla `Stats.KILLED`) so old worlds don't get
  retroactive progress.

**What familiarity shows (mob page in the Bestiary tab):**

| Tier | Shows / gives |
|------|---------------|
| Unknown | "?" tile, where to find it, "Appears from Adventure Rank X" while it's still locked |
| Unfamiliar (1) | Name, 3D model (follows the mouse), kills, familiarity |
| Known (5) | Stats (health, attack in hearts, armor of a fresh mob) and a tip |
| Familiar (15) | Drops as item icons, hover says Often / Sometimes / Rarely |
| Expert (100) | Exact drop chances ("35% of kills") |
| Master (200) | Gold border, "Mastered" count, **50% chance of double loot** (`masterDoubleLootChance`) |

- Expert and Master are a lot of grinding, so their rewards stay small on purpose.
- Drops come from the server: it rolls the mob's loot table 1000 times as a player kill without Looting and
  sends the share of kills per item (cached per mob type until /reload). Sampling follows every loot condition
  the way the game does, so other mods' mobs work too. Equipment drops (elite gear) are not in it.
- Double loot = the normal loot table rolled once more, only for the killer, only with mob drops on and not
  for babies.
- Where-to-find hints and tips are hand-written lang text (`bestiary.progressive_adventure.where/tip.<ns>.<mob>`);
  mobs without one show "Nobody knows yet..." / no tips section (e.g. other mods' mobs).
- **Elite records:** for mobs that can be elites, the page shows elites defeated and which traits you've beaten
  (others "???"). Stored in `PlayerProgress` (`elite_kills`, `elite_traits`); counted for assists too.
- **Collections** (advancements, Rank only, no hearts: the user felt hearts are already plentiful):
  Night Watch (zombie, skeleton, spider, creeper; task), Deep Sea Hunter (drowned, guardian, zombie nautilus),
  Raid Breaker (pillager, vindicator, evoker, ravager, witch), Nether Hunter (zombified piglin, ghast, magma cube,
  blaze, wither skeleton, hoglin, piglin), End Hunter (enderman, endermite, shulker) as goals; Master Hunter
  (first Master) and Trait Collector (an elite with every trait) as challenges. Also checked on join, so old
  kills count.

### 5. World progression: mob unlocks and elite mobs

#### Mob unlocks by Rank (v0.3a)

Overworld hostile mobs unlock one group at a time as Adventure Rank (points earned,
~220 total, ~10 after about half an hour of focused play) goes up. Rank 20 is expected while still in the
Overworld (a few in-game days); everything is unlocked by Rank 50, so the world
reaches vanilla danger fairly quickly.

| Rank | New mobs                                         | Share of vanilla hostile spawns (roughly) |
|------|--------------------------------------------------|------------------------|
| 0    | Zombies (incl. husks, zombie villagers)          | ~⅙                     |
| 10   | Skeletons (incl. strays, bogged, parched)        | ~⅓                     |
| 18   | Creepers                                         | ~½                     |
| 20   | Spiders (incl. cave spiders), drowned            | ~⅔                     |
| 30   | Endermen                                         |                        |
| 40   | Slimes (incl. sulfur cubes)                      |                        |
| 50   | Witches, pillager patrols                        | vanilla                |

- **Fewer mobs, not other mobs:** when vanilla picks a locked mob for a natural spawn,
  that spawn group is skipped. Night 1 has about vanilla's number of zombies and
  nothing else. Config `lockedMobReplacementChance` (default 0) can instead replace a
  locked mob with an unlocked one from the same biome, if nights feel too empty.
- **Only hostile mobs:** animals spawn in their own category with their own cap and are
  never touched.
- **Multiplayer:** the lowest rank among non-spectator players within 128 blocks of the
  spawn counts, so the world stays kid-friendly around a kid.
- **Visible:** chat message when a group unlocks ("New danger: Creepers can now
  appear!"), "Next: Creepers at Rank 18" on the Character tab (sent by the server, so
  it's correct even with a changed server config), and in `/pam`.
- **Untouched:** structure spawn lists (witch huts, outposts, monuments, fortresses),
  spawners, trial chambers, raids, spawn eggs, the Nether and End, phantoms, and mobs
  from other mods unless added to the config list.
- Groups, ranks and mob ids are all in the config (`mobUnlocks`), plus an on/off switch.
- **Creepers and health:** a point-blank creeper can kill even a vanilla 10-heart
  player without armor. By Rank 18 players usually have a few extra hearts and some
  armor, and Blast Guard exists. Watch in playtests.

#### Mobs get stronger: gear and elites (v0.3b)

Every natural Overworld hostile spawn uses the lowest Adventure Rank of nearby players (same rule as
mob unlocks), so the world stays kid-friendly around a kid playing near a parent.

**Normal mobs:** zombies and skeletons (and their variants) get a growing chance of 1–2
extra armor pieces, on top of vanilla's own: 0% at Rank 10 → 25% at Rank 100. Material
leather → chainmail (Rank 30) → iron (Rank 60). No stat changes; vanilla drop chances.

**Elites ("Tough Zombie"):**
- Mob types: zombies, husks, zombie villagers, drowned, skeletons, strays, bogged,
  parched, spiders, cave spiders, endermen, slimes. No creepers, witches or pillagers.
- Start 5 ranks after the type unlocks (zombies at Rank 5, skeletons 15, creepers never,
  spiders 25, ...): 2% of that type's spawns, +0.1% per rank, max 10%.
- 20% bigger (vanilla `scale` attribute), +50% health, +25% damage, named (name shows
  when you look at it). They despawn like normal mobs.
- Elite zombie types get a sword (stone / iron / diamond by rolled material).
- Zombie/skeleton types get gear that builds up gradually and randomly: every armor slot
  is rolled on its own (35% → 95% at Rank 160, at least 1 piece), and every piece and the
  weapon roll their own material. Odds per piece (blended smoothly between rows):

| Rank | Leather | Chain | Iron | Diamond |
|------|---------|-------|------|---------|
| 0    | 90%     | 10%   |      |         |
| 15   | 70%     | 30%   |      |         |
| 30   | 36%     | 55%   | 9%   |         |
| 60   | 18%     | 55%   | 27%  |         |
| 90   |         | 36%   | 55%  | 9%      |
| 120  |         | 9%    | 55%  | 36%     |
| 160+ |         |       | 10%  | 90%     |

  Result (simulated): Rank 18 avg 1.8 pieces, full set 3%; Rank 60 full set 11%; Rank 100
  full set 28%; Rank 160 full set 81%, full diamond 54%, often still 1–2 iron pieces.
  Enchant chance per item 10% (Rank 30) → 80% (Rank 150), level 5 → 30. Normal mobs roll
  materials like an elite at half their rank.

**Elite rewards** (scale with the rank the elite spawned at):
- Gear always drops, but only for player kills and with random wear (vanilla rule for
  drop chance 1.0).
- XP ×3 → ×6 at Rank 100 (vanilla XP rules: player kills only).
- Bestiary: an elite kill counts as 3 kills.
- Bonus loot, player kills only: emerald 10% (1) → 50% (1–3) at Rank 100; enchanted book
  from Rank 40, 5% (level 5) → 25% (level 30); golden apple from Rank 80, 3% → 8%.
  Elites are only 2–10% of spawns, so this stays hard to farm, especially early.
- Waypoint Shards (see pillar 7).
- Milestone "Elite Slayer": defeat an elite, ½ heart.
- Test command for ops: `/pam elite <mob>` spawns an elite at your rank.

**Traits (v0.3c):** from Rank 100, an elite has a 25% → 75% (Rank 160) chance of one
random trait, shown in its name instead of "Tough":

| Trait    | Effect |
|----------|--------|
| Swift    | +30% movement speed |
| Frost    | its hits and arrows give Slowness II for 3s |
| Blazing  | its hits and arrows set you on fire for 4s |
| Leaping  | leaps at you like a spider (not skeletons, slimes or spiders, which already leap) |
| Summoner | the first time a player hits it, calls 2 normal helpers of its kind |

Trait elites have ×1.5 bonus loot (and shard) chances. Test: `/pam elite <mob> [trait]`.

### 6. Perks: choosable upgrades

#### Perk points

- **Every advancement** with a visible entry gives perk points, vanilla and PAM:
  **task = 1, goal = 2, challenge = 3** (vanilla's own frame types, so hard
  advancements are worth more).
- Not counted: tab roots ("Minecraft", "Nether", ...) and recipe unlocks (they have
  no display).
- Points are **derived**, never stored: `available = earned − cost of owned perks`.
  Old worlds get points for advancements they already have, which is fine.
- Budget: vanilla advancements give ~175 points, PAM's 26 milestones add 45, so ~220 Rank in total. Buying all
  111 perks costs **266**, so early on you choose and owning everything is out of reach until more milestones
  come (accepted 2026-10-26). Players can suggest perks and milestones through the GitHub
  issue forms.

#### Rules

- A perk has a **cost**, an optional **requirement** (skill level, mob types
  defeated or Adventure Rank) and an optional **prerequisite perk** (tier II needs I).
- Requirements are shown on locked perks so kids know what to do ("Reach Mining 15").
- **Free respec:** a "Reset perks" button refunds everything (no confirmation, it's free). No
  penalty, fits the family rule.
- Owned perks stay owned even if a config change lowers your points.
- Server validates every purchase (no cheating from a modified client).

#### Perk list

Skill perks built later have their tables in their own section: Farming in 3b, Archery in 3c, Fishing in 3d,
Melee (by weapon) and the Shield perks (Defense) in 3e.

| Category    | Perk                | Effect                                              | Requires            | Cost |
|-------------|---------------------|-----------------------------------------------------|---------------------|------|
| Mining      | Sturdy Pickaxe I    | 10% chance pickaxes use no durability               | —                   | 1    |
| Mining      | Sturdy Pickaxe II   | 20% chance (replaces I)                             | Mining 8, perk I    | 2    |
| Mining      | Sturdy Pickaxe III  | 30% chance                                          | Mining 18, perk II  | 3    |
| Mining      | Prospector I        | 10% chance of double ore drops                      | Mining 10           | 2    |
| Mining      | Prospector II       | 20% chance                                          | Mining 25, perk I   | 3    |
| Mining      | Ore Sense           | Holding a pickaxe: particle trail points at the nearest ore within 12 blocks, coloured by ore type, bigger when closer | Mining 40 | 4 |
| Mining      | Treasure Sense      | Holding a pickaxe: white sparkle trail (every 2.5s) to the nearest diamond ore / ancient debris within 24 blocks; quiet amethyst chime once per block within 6 | Mining 50, Ore Sense | 5 |
| Mining      | Vein Miner          | Mining an ore also mines up to 16 connected ores of the same kind (sneak = off) | Mining 30 | 4 |
| Mining      | Cat's Eyes I/II     | 25% / 50% of Night Vision's brightening, always on  | Mining 5 / 35       | 2/3  |
| Woodcutting | Sturdy Axe I/II/III | Same as pickaxe, for axes                           | — / Woodcutting 8 / 18 | 1/2/3|
| Woodcutting | Lumberjack          | Chopping a log also chops up to 6 connected logs (sneak = off) | Woodcutting 10 | 2 |
| Woodcutting | Timber              | Chopping a log fells the whole tree (sneak = off)  | Woodcutting 25, Lumberjack | 4 |
| Woodcutting | Forester            | Lumberjack/Timber plant the matching sapling where the bottom log stood | Woodcutting 13, Lumberjack | 2 |
| Woodcutting | Clear Cut           | After Timber, the tree's leaves break at once (same drops as decay, Orchard applies) | Woodcutting 30, Timber | 2 |
| Woodcutting | Orchard             | Natural leaves you break: 5% apple, 0.04% golden apple (any tree) | Woodcutting 3 | 2 |
| Woodcutting | Bird's Nest         | 2% per natural log chopped with an axe: 1–2 of feather/egg/string/stick/seeds, 10% of nests also an emerald | Woodcutting 6 | 2 |
| Digging     | Sturdy Shovel I/II/III | Same, for shovels                                | — / Digging 8 / 18  | 1/2/3|
| Digging     | Archaeologist       | 3% chance of a bonus find (flint, clay, bone, ...) from dirt, sand, gravel | Digging 10 | 2 |
| Digging     | Flint Finder        | 30% chance of an extra flint from natural gravel    | Digging 6           | 1    |
| Digging     | Path Maker          | Making a path with a shovel flattens a 3×3 square (sneak = single); each block uses durability | Digging 3 | 1 |
| Digging     | Relic Sense         | Holding a brush or shovel: trail to the nearest suspicious sand/gravel within 16 blocks | Digging 15 | 2 |
| Digging     | Excavator           | Shovels dig a 3×3 square facing you (sneak = off)   | Digging 20          | 4    |
| Melee       | Sturdy Weapon I/II/III | 10/20/30% chance swords, spears, maces, tridents use no durability | — / 10 / 20 mob types defeated | 1/2/3 |
| Melee       | Quick Study         | Kills count double towards familiarity              | 5 mob types defeated | 2   |
| Defense     | Sturdy Armor I/II/III | 10/20/30% chance armor uses no durability       | 5 / 10 / 15 mob types defeated | 1/2/3 |
| Defense     | Blast Guard I/II    | 15% / 30% less damage from explosions               | 5 / 10 mob types    | 2/3  |
| Defense     | Arrow Guard I/II    | 15% / 30% less damage from projectiles              | 5 / 10 mob types    | 2/3  |
| Defense     | Fire Guard I/II     | 15% / 30% less damage from fire, lava, burning      | 5 / 10 mob types    | 2/3  |
| Defense     | Tough Hide I/II     | 10% / 20% less damage from mob hits (melee)         | 5 / 10 mob types    | 2/3  |
| Defense     | Steady Stance       | 30% knockback resistance                            | 10 mob types        | 2    |
| Survival    | Bonus Heart I/II/III| +1 heart each, on top of milestone hearts (ignores cap) | Rank 5 / 10 / 20 | 3/4/5|
| Survival    | Iron Stomach        | Immune to the Hunger effect (rotten flesh, husks)   | —                   | 1    |
| Survival    | Long Arms           | +1.5 blocks block reach                             | Rank 10             | 2    |
| Survival    | Deep Breath I/II/III | ~30s / ~45s / ~60s of air underwater (vanilla 15s) | — / Rank 10 / Rank 25 | 1/2/3 |
| Survival    | Diver               | Mine underwater at full speed (Aqua Affinity)       | Rank 10             | 2    |
| Survival    | Second Wind         | Dropping below 3 hearts gives Regeneration II for 5s (2 min cooldown) | — | 2 |
| Survival    | Steady Healing I/II | Natural healing costs 25% / 50% less hunger (stacks with the stomach) | — / Rank 10 | 2/3 |
| Survival    | Hearty Kill I/II/III | Defeating a hostile mob (any weapon) heals ½ / 1 / 1½ hearts, at most once per second | 10 / 20 / 30 mob types | 2/3/4 |
| Movement    | Fleet Foot I/II/III | +5% / +10% / +15% movement speed                    | Rank 10 / 25 / 40   | 2/3/4|
| Movement    | Soft Landing        | Fall 3 more blocks before taking damage             | —                   | 2    |
| Movement    | Spring Step         | Jump strength +0.08 (~1.7 blocks, over fences)      | Rank 5              | 2    |
| Movement    | Step Up             | Step height 1.0: walk up full blocks                | Rank 5              | 2    |
| Movement    | Sure Feet           | Soul sand and honey don't slow you down             | —                   | 1    |
| Movement    | Sneaky Feet         | Sneaking speed 0.3 → 0.5                            | —                   | 1    |
| Movement    | Shadow Step I       | Sneaking: mobs notice you at half their normal distance | Rank 10         | 2    |
| Movement    | Shadow Step II      | Sneak > 8 blocks from a chasing mob: it gives up and can't see you (while sneaking) for 10s, unless you hit it | Rank 25, perk I | 3 |
| Movement    | Mantle              | Jump against a wall up to 2 blocks high while walking forward: pull yourself up | Rank 15 | 3 |
| Movement    | Double Jump         | One extra jump in the air (not while wearing an elytra) | Rank 30         | 4    |
| Movement    | Pathfinder I/II     | Waypoint trips cost 25% / 50% less XP               | Rank 15 / 35        | 2/3  |
| Movement    | Swimmer I/II        | Water movement efficiency 0.33 / 0.66 (Depth Strider) | Rank 5 / 20       | 2/3  |

Implementation notes:
- Durability: one hook where items lose durability; chance by tool type.
- Prospector: re-drops the block's loot (respects Fortune). Skipped when the drop is
  the ore block itself (Silk Touch).
- Timber / Lumberjack: breaks connected logs (max 64 / 6), each log uses durability and gives skill xp.
- Vein Miner / Excavator work like Timber: extra blocks go through `destroyBlock`, so each one uses
  durability, gives skill xp and can trigger Prospector/Archaeologist; player-placed ores and logs
  are skipped (Excavator digs placed dirt/sand too, since those aren't remembered). Vein Miner counts deepslate and normal variants of an ore as one vein (conventional ore
  tags). Excavator: the 3×3 plane follows the look direction (flat when looking more than 45° up
  or down), only shovel blocks without a block entity (so never suspicious sand/gravel).
- Sense perks (`BlockSense`, client): one scanner that skips chunk sections whose palette can't
  contain the target, so a 24-block radius for rare blocks stays cheap.
- Perks that change extra blocks (Timber, Lumberjack, Vein Miner, Excavator, Harvester, Replant, Path Maker)
  skip blocks the player couldn't change by hand: spawn protection and Adventure mode, the same checks as a
  normal break. Replant doesn't fire a break event, so claim mods only stop it if they block right-clicks.
- Clear Cut: 10 ticks after felling (leaf distance spreads one block per tick), breaks the
  non-persistent leaves at decay distance 7 in the felled logs' box + 7. Only leaves that would
  decay anyway, so a neighbouring tree is never touched. Breaks 32 leaves per tick (no lag spike
  or particle burst from a huge canopy).
- Forester: only when the broken log is the bottom one (no log below) and the sapling can survive
  there. Single saplings for 2×2 trees (dark oak, big spruce/jungle) won't grow into the big tree.
- Cat's Eyes: client mixin on the lightmap, raises the night-vision intensity it already uses.
- Player-placed logs are never felled, so houses are safe.
- Defense guards: multiply incoming damage before armor (stacks with Protection
  enchantments). Only the first matching type counts: explosion, projectile, fire, then
  hits whose direct source is a mob. Uses vanilla damage type tags, so modded damage works.
- Deep Breath / Soft Landing / Fleet Foot / Bonus Hearts and the other attribute perks (Spring
  Step, Step Up, Sure Feet, Sneaky Feet, Swimmer, Diver, Long Arms, Steady Stance): vanilla attributes.
- Shadow Step: mixin on `LivingEntity.getVisibilityPercent` (what mobs use to notice targets,
  vanilla sneaking already gives 0.8). II clears the target of mobs chasing a sneaking player from
  further than 8 blocks (checked every 10 ticks, action-bar message) and makes the player invisible
  to that mob (visibility 0 = noticed only within 2 blocks) for 10s while sneaking. Hitting it
  re-aggros (revenge targeting ignores visibility).
- Double Jump / Mantle: movement is client-driven, so the client (`AirMoves`) moves the player and
  sends a payload; the server checks the perk, allows one of each per time in the air, resets fall
  distance and plays a sound (and cloud particles for Double Jump). Double jump = a normal jump
  (`jumpFromGround`, so Spring Step adds to it) but without the sprint push (decided 2026-10-26: it sent players
  flying forward). Mantle: falling or near the top of a jump, pushing
  forward into a wall whose top is 0.6–1.9 blocks above the feet, with room to stand on it; never
  on fences/walls (collision taller than 1 block).
- No 3×3 mining for pickaxes (trivialises caving and building); Vein Miner instead.

---

### 7. Waypoints: travel between places you found (v0.4a)

Elites drop **Waypoint Shards**; shards turn on **Waypoint Stones**; you travel between stones
for XP. Rewards fighting elites and exploring far away, and leaves a visible network of
places you've been.

- **Shards:** only from elites, player kills only. Chance 25% → 50% at Rank 100 (×1.5 for
  trait elites). The **first elite a player kills always drops one** (checked via the Elite
  Slayer milestone, no extra data).
- **Waypoint Stone:** crafted from 8 stone bricks around a compass. Right-click with enough
  shards in your inventory → name screen → it turns on (glows, soft particles, floating name).
  Your Nth stone costs N shards (`waypointShardCostBase` + `Step` × stones you own). Breaking
  it gives the shards back, so moving a stone is never a punishment. Owner sneak + right-click
  (empty hand) renames it.
- **Shared:** every player sees and can use every stone; each pays their own XP.
- **Tough:** blast-proof, pistons can't move it, only the owner or an op can break a
  turned-on stone.
- **Travel:** right-click a stone → list of all other stones with cost, owner and distance.
  Only from stone to stone.
  2-second charge-up with particles; getting hurt or walking away cancels it (nothing paid).
- **Cost in XP points, not levels**, so it doesn't feel harsh at level 30 vs level 3
  (30 points ≈ 2 levels at level 3, ¼ level at level 30): 15 + 10 per 1000 blocks, max 50.
  Distance is in Overworld blocks (Nether ×8), so a trip costs the same in every dimension.
  Changing dimension adds +20 on top of the cap; to/from the End always costs 50 + 20.
- **Arrival:** next to the stone, facing it, on solid ground, not in lava or with your head
  underwater; on top of it as a last resort. No room → refused, nothing paid. A stone that
  disappeared is removed from the list.
- **Pets** (tamed, following: not sitting, not on a lead) within 16 blocks come along. You get
  off mounts and boats; they stay behind.
- **Milestones:** Waypoint (first stone, ½ heart), First Trip (first teleport, stomach),
  Network (5 of your own stones turned on, 1 heart).
- **Perk:** Pathfinder I/II (Movement, Rank 15/35, 2/3 points): trips cost 25% / 50% less.
- **Ops:** `/pam waypoint list`, `/pam waypoint tp <name>` (free, no charge-up),
  `/pam waypoint remove <name>` (turns the stone off, drops its shards there). Creative players
  pay no shards or XP.
- Storage: one shared list on the Overworld (Fabric world attachment). The floating name is a
  text display entity; orphaned ones are removed when they load.

## UI

Keep it simple and close to vanilla.

- **Milestones:** our own "Adventure" tab in the vanilla advancement screen.
- **Adventure screen:** one window opened with **K** (rebindable in Controls), using
  vanilla's tab bar so it looks like the world-creation screen. Three tabs:

#### Character tab
- **Progress:** Adventure Rank, the next mob unlock ("Next: Creepers at Rank 18"), available perk
  points, hearts (as heart icons) and stomach level. Details (what Rank is, hunger drain %) are in
  hover tooltips.
- **Skills:** two columns of 4 rows (room for 8). Each skill: tool icon, name and level, xp bar to
  the next level and the bonus as a number ("+35%", red below vanilla). Hover for the details: the
  bonus in words, how much xp to the next level and the next perk the skill unlocks.

#### Perks tab
```
┌──────────────────────────────────────────────────────┐
│ Perk points: 7                       [Reset perks]   │
│ ┌──────────┐ ┌────────────────────────────────────┐  │
│ │ Mining   │ │ ⛏ Sturdy Pickaxe I               ✔ │  │
│ │ Woodcut. │ │ ⛏ Sturdy Pickaxe II          2 pts │  │
│ │ Digging  │ │ ⛏ Sturdy Pickaxe III            🔒 │  │
│ │ Farming  │ │ 💎 Prospector I              2 pts │  │
│ │ Melee    │ │ 💎 Prospector II                🔒 │  │
│ │ Archery  │ │                                    │  │
│ │ Fishing  │ │                                    │  │
│ │ Defense  │ │                                    │  │
│ │ Survival │ │                                    │  │
│ │ Movement │ │                                    │  │
│ └──────────┘ └────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
```
- Tooltips wrap at 200px (`AdventureScreen.wrappedTooltip`): long perk descriptions ran off the screen.
- The category column holds 10 rows of 19px: an 11th category needs a new layout (16px rows would fit 12).
- Perks can have a **group** (Melee: one per weapon); the list then shows a small gold heading above each group,
  so a long category reads as a few short lists.
- Categories on the left, perks of the selected category on the right (scrolls when a
  category has more perks than fit). Hover a perk for its full description.
- States: owned (✔), buyable (cost in gold, click the row), too expensive (cost in red),
  locked (🔒). What blocks a locked perk is only in the row tooltip: one hover per row,
  no separate hover on the lock icon.
- Reset perks has no confirmation (it's free); the button tooltip says so.

#### Bestiary tab
- Grid of spawn-egg icons for every mob with a spawn egg, hostile mobs first, with
  pages. "Discovered 12 / 84" (and "Mastered N") at the top.
- Undiscovered mobs show a dark "?" tile ("Defeat one to discover").
- Hover: name, kills, familiarity label, damage modifier. Click opens the mob's page (also for undiscovered
  mobs, to see where to find them).
- Mob page: Back and </> buttons; left a model box (big "?" when undiscovered: GUI entity rendering forces full
  brightness, so no black silhouette) with kills, familiarity, next tier and the Master bonus; right the
  sections Where to find, Stats, Tips, Drops, Tough ones. Locked sections say "Locked: reach Known (5 kills)".
- Tile border colour by tier (dark grey for Unknown/Unfamiliar → light grey Known → green Familiar →
  aqua Expert → gold Master).

#### Messages
- Chat: hearts gained, stomach upgrade, perk points / Rank gained, mob unlocks, waypoint actions.
- Action bar: skill level-up, new familiarity tier, Shadow Step II escape, Bird's Nest.
- Toast: new bestiary entry (advancement-style, with the spawn egg).
- `/pam` and `/pam bestiary` are text fallbacks for the screen.

---

## Ideas

**The one place for everything not built yet.** When an idea comes up, add it here (with a date when
it's new); when it gets built, move its description into the pillars above and log the decision.
Players can suggest more through the GitHub issue forms.

### New skills
- **Campfire Catch / Auto-smelt (2026-10-26):** fish come out cooked; a matching Mining perk that smelts ores.
  Left out of Fishing for now.

### Milestones and Rank
- More milestones: raise a shield, desert/jungle temple, shipwreck or ocean ruin, reach Y=0 / lush or deep dark
  cave, mountain peak, buried treasure, cake or suspicious stew, 5 villager professions. Check first that vanilla
  has no advancement for it (if it does, put the reward on the vanilla one).
- More advancements in general = more Rank and perk points (perks cost 216 of ~220 Rank today).

### Food and hunger
- **Well Fed (diet variety):** eating a varied diet gives a temporary buff (slower hunger drain, small regen);
  eating the same food over and over gives less. Teaches kids to farm several crops, fits the Gourmet milestones.
- Show the stomach level as a small icon next to the hunger bar.

### Perks
- A trail home (to bed/spawn), like the Sense perks.
- Wall Slide.
- Auto-smelt.
- Fireproof Skin (overlaps Fire Guard, so maybe not).
- Animal Friend: can't be an attribute (tempt range belongs to the animal), needs a mixin.

### Bestiary
- **Look-at info (2026-10-26, on hold):** a small "Zombie ❤ 14/20" line under the crosshair for Known mobs,
  instead of floating health bars (rejected: changes vanilla too much). The user wants the HUD vanilla for now.
- **More bestiary content (2026-10-26):** the user feels there's still a gap after the mob pages; more ideas
  welcome (e.g. per-mob behaviour notes, biome/dimension filters, a "favourite"/tracked mob).
- First-discovery XP burst: dropped for now (collections already add Rank); the toast is built.

### Mobs and elites
- **More elite weapons (2026-10-25):** elite zombies always get a sword; they could also roll an axe (and maybe
  other weapons) by material. A mob hitting with an axe disables your shield for a few seconds (vanilla), which
  can surprise kids, so axes maybe only from a higher rank. Maces are probably too strong.
- **Recycle elite gear (2026-10-25):** elites drop a lot of armor and weapons. Vanilla smelts iron, chainmail and
  gold gear into a single nugget each, which feels poor for a whole piece; diamond and leather gear can't be
  recycled at all. Idea: a grindstone/recycling station that returns part of the ingots or diamonds (more for less
  worn gear), or salvaging into something useful.
- Elites drop treasure or explorer maps (the other half of the "mobs drop exploration helpers" idea; the first
  half became Waypoint Shards).
- Mob unlocks / elites in the Nether and End (vanilla spawns there for now).

### Waypoints
- Teleport from anywhere (not only stone to stone), e.g. as a Pathfinder III tier.

### Exploration
- **Heart containers in loot:** rare items in structure chests (dungeons, temples, ancient cities) that grant a
  bonus heart.
- **Biome bonuses:** first visit to a biome grants a small one-time XP bonus to a related skill.
- **Shrines/landmarks:** small custom structures with a challenge and a reward.

### UI
- Toasts instead of chat/action-bar messages for level-ups and Rank-ups (new bestiary entries have one).

---

## Risks & things to watch

1. **Level-scaling treadmill.** If mobs get stronger exactly as fast as the player,
   progress feels pointless (Oblivion's famous problem). Mitigated by elites being
   rare and every Rank-up also granting hearts.
2. **Early frustration.** 5 hearts + faster hunger + no death penalty should be
   fine, but needs playtesting with the kids.
3. **Mod compatibility.** Spawn restrictions may clash with mods that add mobs.
4. **Scope.** Many small systems. Build in thin vertical slices.

---

## Decisions log

- 2026-10-24: Target servers + LAN, not just singleplayer.
- 2026-10-24: No death penalty (family-friendly).
- 2026-10-24: Bonuses use diminishing returns. Late-game player is stronger than vanilla.
- 2026-10-24: Hearts come from explicit milestones (custom advancements).
- 2026-10-24: Keep the hunger bar size. Progress via metabolism (drain/saturation).
- 2026-10-24: Adventure Rank gates mob unlocks and elite chance (Rank redefined later the same day, see below).
- 2026-10-24: Elites are rare, recognisable and drop their gear.
- 2026-10-24: Structure mobs always spawn. Nether/End keep vanilla spawns for now
  (changing them is a later idea).
- 2026-10-24: Mod ID `progressive_adventure`.
- 2026-10-24: UI: milestones in the advancement screen; skills + bestiary in one
  tabbed screen.
- 2026-10-24: Target Minecraft 26.2 on Fabric.
- 2026-10-24: Name: "PAM: Progressive Adventure Mod" (Pam's HarvestCraft clash
  accepted; it hasn't been updated in years).
- 2026-10-24: Unknown mobs take less damage (−15%) until you've killed a few.
- 2026-10-24: Tool skills start at −20% break speed (after first playtest: levelling felt rewarding).
- 2026-10-24: Two tracks: hearts/stomach stay automatic (milestones); every
  advancement gives perk points for choosable perks. Skills gate perks. Free respec.
- 2026-10-24: v0.2 = perks + Adventure screen (character, perks, bestiary); mob
  unlocking moves to v0.3.
- 2026-10-24: v0.2 playtested: GUI, perks and perk reset work. Character tab got a
  separate Progress section + tooltips because rank looked skill-based.
- 2026-10-24: Farming skill (v0.2.x): XP from grown crops + breeding (no separate
  Ranching skill), extra-harvest bonus, no start penalty, Replant is a perk.
- 2026-10-24: Ore Sense perk: client-side particle trail toward nearest ore (12 blocks),
  only with a pickaxe in hand. Requirement Mining 40 (was briefly removed for testing, restored 2026-10-25).
- 2026-10-24: Deep Breath split into 3 tiers. (Replant/Harvester requirements were briefly removed for
  testing, restored 2026-10-25.)
- 2026-10-24: Defense perks are specific (explosion, projectile, fire, mob hits), no general
  damage reduction: a general one would make the others pointless, and Bonus Hearts already
  act as general protection. Gated on mob types defeated (5 / 10), kept easy.
- 2026-10-24: Fleet Foot split into 3 tiers (+5% each; ranks later rescaled to 10/25/40, see below).
- 2026-10-24: Mob unlock order: zombies → skeletons → creepers → spiders →
  endermen → slimes → witches. Elites per mob type (later set to 5 ranks after the type unlocks).
- 2026-10-24: **One progression model.** Adventure Rank = perk points earned from all
  advancements (not perks owned: buying shouldn't make the world harder, reset shouldn't
  make it easier). Rewards can sit on vanilla advancements; the 8 PAM milestones that
  duplicated vanilla (bed, trade, iron pickaxe, enchant, brew, Nether, End, dragon) were
  removed, so no double toasts. 10 more vanilla advancements got rewards. Rank-gated perks
  rescaled (Bonus Heart 5/20/40, Deep Breath 10/25, Fleet Foot 10/25/40). The name stays
  "Rank" (not "level": clashes with XP and skill levels).
- 2026-10-24: Mob unlocks (v0.3a): Rank 5 skeletons, 12 creepers, 20 spiders + drowned,
  30 endermen, 40 slimes, 50 witches + patrols. Locked mobs are skipped (fewer hostile
  mobs early, animals untouched), replacement chance configurable. Elites after a playtest.
- 2026-10-24: Elites (v0.3b): normal mobs slowly wear more gear; rare elites (2→10%) are
  bigger/tougher/named with gear that improves with Rank. Gear, XP (×3→×6) and bonus loot
  (emeralds, books from 40, golden apples from 80) scale with Rank, player kills only, so
  it's hard to farm early. Traits only at very high Rank, later (v0.3c).
- 2026-10-24: Elite traits (v0.3c): Swift, Frost, Blazing, Leaping, Summoner, from Rank 100
  (25% → 75%), ×1.5 bonus loot. Exploration-helper drops stay parked.
- 2026-10-24: Elite gear is rolled per piece (count, material, enchant) with odds that shift
  slowly with Rank, instead of fixed full sets per tier (full chainmail at Rank 18 felt too much).
- 2026-10-24: UI declutter: perk rows show only an icon (✔ / 🔒) or the cost, details
  and blockers live in the single row tooltip. Next-perk line removed from skills, rank
  and stomach details moved to tooltips, perk reset without confirmation.
- 2026-10-25: Farm blocks never train tool skills (an axe on a pumpkin gave Woodcutting XP).
  Ore Sense, Replant and Harvester playtested; their requirements are restored.
- 2026-10-25: Archery skill with draw speed as its passive bonus (not a perk). Perks: Thrifty
  Quiver, Fire/Frost Arrows, Piercing Shot, Steady Aim, Ricochet (Archery 40, 5 points). Poison
  dropped for now, no damage perks (bestiary covers damage).
- 2026-10-25: New perk categories **Movement** (Fleet Foot and Soft Landing moved there) and
  **Swimming** (Deep Breath moved there) so Survival stays short; perk ids unchanged, so owned perks
  stay owned. New: Spring Step, Step Up, Sure Feet, Sneaky Feet, Shadow Step I/II, Mantle, Double
  Jump, Swimmer I/II, Diver, Long Arms (Survival), Steady Stance (Defense). No double jump with an
  elytra equipped (jump in the air starts gliding). Category rows are 19px so 10 fit.
- 2026-10-25: Spring Step clearing fences (and animal pens) is fine.
- 2026-10-25: Orchard golden apple chance 0.2% → 0.04% per leaf: with Timber + Clear Cut a tree
  breaks ~60-100 leaves at once, so 0.2% gave a golden apple every 5-8 trees (now ~1 in 30).
- 2026-10-25: Second perk round: Sturdy Weapon III; Treasure Sense (Ore Sense II: diamonds +
  ancient debris, 24 blocks, chime once per block so it never nags), Vein Miner (no 3×3 for
  pickaxes), Cat's Eyes I/II (partial night vision; light around you would need lighting hacks or
  light blocks that stop mob spawns). Woodcutting: Lumberjack (6 logs) becomes Timber's prerequisite,
  Timber moves to Woodcutting 25 / 4 points; Orchard (leaves) and Bird's Nest (logs) trigger on
  different blocks so they don't compete. Digging: Excavator, Path Maker, Relic Sense (vanilla
  suspicious sand/gravel). Then also Clear Cut, Forester and Flint Finder.
- 2026-10-25: **Waypoints (v0.4a):** shards only from elites (first elite always drops one);
  stones are shared; the Nth stone you own costs N shards, refunded on break; travel only
  stone to stone, cost in XP *points* (fair at any level) by distance, capped (cap to revisit
  after playtest); Nether included, distance in Overworld blocks, +20 for a dimension jump;
  pets come along, mounts don't; particles + floating name; milestones Waypoint / First Trip /
  Network; Pathfinder perk lowers the cost; op commands list/tp/remove.
- 2026-10-25: **Placed blocks (performance):** only remember blocks where place/break farming
  matters (logs, ores, melon/pumpkin/cane/cactus/bamboo); placing dirt/gravel/leaves again is more
  hassle than it's worth (vanilla gravel flint isn't protected either). Max 10,000 per chunk, then
  the oldest is forgotten. One mutable set per chunk instead of copying it on every place/break.
- 2026-10-25: **Progress sync once per tick (performance):** every progress change used to send the
  whole record to the client. Now changes are collected and sent once at the end of the tick (also
  before a save or respawn, so nothing is lost on logout). The server still decides all XP, so this
  changes nothing for cheating.
- 2026-10-25: **Clear Cut spread over ticks (performance):** 32 leaves per tick. Timber and Vein
  Miner stay instant: spreading them would feel slower and needs handling for tool switches,
  walking away and logging out, and their caps (64/16) keep them cheap enough.
- 2026-10-25: **Mob unlock rank table (performance):** required ranks per mob type are looked up
  in a table built at server start instead of comparing id strings on every spawn attempt. Small
  gain (estimated <1% of a tick before), done because it's cheap.
- 2026-10-25: **License: MIT** (was All-Rights-Reserved), ahead of a first Modrinth release, so
  people can fork and send PRs.
- 2026-10-25: **First public release = 0.4.0, Alpha channel** on Modrinth. Icon: the Waypoint Shard
  on a night sky (more memorable than a heart). Feedback goes through GitHub issue forms (bug,
  playtest feedback, perk/feature idea).
- 2026-10-25: **Releases are automated:** a `/release` Claude skill prepares version + changelog
  and pushes a tag; a GitHub Action uploads to Modrinth and makes a GitHub release. Only the
  latest Minecraft version is supported (no multi-version builds for now).
- 2026-10-25: **Audit** ([AUDIT.md](AUDIT.md)): First Trip was a 7th stomach upgrade but the tables stopped at
  level 6, so an 8th level was added (×0.6 drain, ×1.4 saturation). Perk budget kept: all perks cost 206 of ~217
  Rank, so owning everything is an endgame goal; more advancements may come later, and players can suggest perks
  and milestones as GitHub issues. The `v0.2.0` tag question is dropped (0.4.0 is the first release).
- 2026-10-25: **Config holds only overrides.** The mod used to write every setting back to the config file, which
  froze the defaults of the first launch, so rebalances never reached existing players (also singleplayer/LAN).
  Now the file only has what someone changed; `progressive_adventure.defaults.json` lists all settings as a
  reference. Done before the first release, so no migration (old files deleted). The mob unlock radius is no
  longer a setting: it's fixed at 128 = vanilla's spawn distance (smaller let far spawns skip the lock).
  Known limitations for players/admins live in README.
- 2026-10-25: **0.4.0 released** (tag `v0.4.0`, Alpha) on Modrinth and as a GitHub release, with much of the
  playtest checklist still open. From now on players have real saves and configs: removing or renaming a perk,
  milestone or setting needs a migration or a CHANGELOG note.
- 2026-10-25: **DESIGN.md cleanup:** all not-built ideas moved into one [Ideas](#ideas) section; the pillars only
  describe what's built. Checked against the code; stale bits fixed (Perks tab mockup with 10 categories, bestiary
  tier colours, which messages use the action bar, perk "later ideas" that were already built).
- 2026-10-26: **Bestiary upgrade.** Mob pages (grid + click → page with 3D model) instead of only a tooltip.
  Tiers unlock info: Known = stats + tip, Familiar = drops, Expert = exact drop chances, Master = 50% double loot.
  Rewards for Expert/Master stay small because reaching 60/100 kills is a grind. Where-to-find hints for every
  vanilla mob, shown even before discovery. Collections are advancements with Rank only (no hearts), plus Master
  Hunter and Trait Collector. Elite records per mob. Assist kills (cheap: vanilla's combat tracker). New entry =
  toast; no XP burst (collections already give Rank). **No health bar / HUD change** for now: the user wants the
  HUD vanilla (look-at info line stays an idea). Hunting requests belong to a future quests feature.
  Drop chances are sampled on the server (1000 loot rolls) rather than read from the loot table structure.
- 2026-10-26: **Slower, smaller familiarity bonuses.** 100 zombie kills came fast (zombies are almost the only
  early mob) and +50% made you strong too soon. Now +5/+10/+15% at 15/30/60 (Familiar), +20% Expert at 100,
  +30% Master at 200, +35% at 500 (was +10/+20 Familiar, +35 Expert 60, +50 Master 100, +55 at 250). Elites
  stay at 3 kills (they're ≤10% of spawns, so barely matter) and Quick Study stays at double. Master's 50%
  double loot stays. No playtest item: feedback from players later.
- 2026-10-26: **Non-goals section** added under Vision. No protection against farms or kill chambers: the
  mod should make them less needed by making the player stronger, not stop them working. Fully automatic
  farms still give no progress (the player has to do the thing). The idea "kill chambers count for less" is
  dropped. Also listed there: mobs scaling with the player, a death penalty, multi-version builds,
  server-only or client-only installs (all decided earlier).
- 2026-10-26: **Swimming perk category removed** to make room for Fishing: Deep Breath and Diver moved to
  Survival, Swimmer to Movement. Rule: one category per skill plus Defense, Survival and Movement (10 with
  Fishing). Perk ids unchanged, so owned perks stay owned.
- 2026-10-26: **Character tab in two columns** (room for 8 skills; 7 planned with Fishing and Combat). Rows show
  level, bar and bonus number only; "x / total" xp moved to a hover tooltip with the bonus in words, xp to the
  next level and the next perk the skill unlocks.
- 2026-10-26: **Fishing skill** built (see 3d): trained by catches, passive bonus = faster bites starting at vanilla
  (+0%, not −20% like the other skills; waiting is already the slow part). Perks: Lucky, Keen Eye, Double
  Catch, Treasure Hunter. Campfire Catch (cooked fish) left out for now, with auto-smelt for Mining as a
  matching idea. XP raised to fish 4 / junk 2 / treasure 10 since catches are slow (1 xp each would need
  ~400 catches for level 10). Treasure Hunter at Fishing 25. Milestones Sunken Treasure (code-driven) and Fish
  Collector (vanilla `fishing_rod_hooked` criteria, all four fish), ½ heart each, new Angler category.
- 2026-10-26: **Melee skill** (slice a built, see 3e): one skill for all hand-to-hand weapons, not one per weapon
  (one Character tab row, XP isn't split when you switch weapons; same as Archery covering bows and crossbows).
  Passive bonus attack speed, +0% → +30%. Perks change how you fight, never the main hit (bestiary = damage).
  The Combat perk category becomes **Melee** (category ids aren't saved, perk ids unchanged) with weapon group
  headings in the list instead of an 11th category; shield perks will go to Defense; Second Wind moved to Survival
  with the new **Hearty Kill I/II/III** (½ / 1 / 1½ hearts per hostile kill, 1 s cooldown so a sweep through a crowd
  heals once). Sword: **Wide Sweep I/II** (more sweep damage, the vanilla way) *and* Whirlwind later (more moves are
  fun). Sweeps never hit pets, villagers or golems (always on). **Tridents:** thrown ones train Archery and get the
  draw speed bonus; Loyal Trident perk in Archery. Owning every perk is out of reach for now (97 perks cost 232 of
  ~220 Rank); more milestones later. Sweep protection first shipped always on; the user wanted vanilla kept, so it
  became the **Careful Sweep** perk (Melee 5, 1 point) the same day. Slices: a = skill + Wide Sweep + Hearty Kill + headings, b = Whirlwind, Disarm,
  axe perks, c = Parry/Deflect/Shield Bash, d = mace and spear moves.
- 2026-10-26: **Melee slice b:** Whirlwind needs Wide Sweep I (without it the sweep does 1 damage and Whirlwind
  would feel broken) and wraps vanilla's sweep instead of being its own attack. Sunder is a 5 s armor crack
  (−50%) instead of "ignore armor on this hit": simpler, visible, and it rewards following up. Disarm drops the
  item (no special loot rule), so it can also free a drowned's trident.
- 2026-10-26: **Shield perks (Melee slice c)** in Defense under a "Shield" heading, gated on mob types like the
  other Defense perks. Parry window 10 ticks after the shield starts blocking; reflected damage capped at 4 hearts
  and dealt as thorns (credited to the player, no Melee hit XP; a parry gives its own 3 XP). Shield Bash is a
  client-detected attack click while blocking (vanilla ignores those), 1 s cooldown, pushes only (no damage).
- 2026-10-26: **Melee slice d (mace, spear):** vanilla already pushes mobs on a mace smash and the spear jab already
  hits every mob in its line, so Shockwave deals damage (30% of the fall bonus, hostile mobs only) and Skewer was
  replaced by Pinning Jab (slow). Ground Pound is a client air move like Double Jump, landing handled on the server
  instead of fall damage. Running Charge ×1.75 on-foot speed. Melee now has 15 perks in 5 groups; all perks cost 261.
- 2026-10-26: **Combat review** ([AUDIT.md](AUDIT.md), Melee skill review): Ground Pound checked on the server too,
  dives forgotten when the fall ends without a landing; shared `Targets` and `Cooldown` helpers. **Sturdy Weapon**
  (was "Sturdy Blade", swords only) now covers swords, spears, maces and tridents; axes keep Sturdy Axe so nothing
  gets two Sturdy perks. Bestiary goes back to the grid when you leave a world. AUDIT.md now has a **Minecraft
  update checklist** of hooks that could silently break on an update.
- 2026-10-26: Perk group headings stay as they are (only Melee and the Shield perks use them; they don't add much
  to the other categories). Tooltips in the Adventure screen wrap, since long perk descriptions went off screen.
  Avoiding all fall damage (Double Jump, Ground Pound) and the double jump's sprint push are open questions.
- 2026-10-26: **Double Jump has no sprint push.** It called vanilla's jump, which adds +0.2 blocks/tick forward when
  sprinting; with little drag in the air that second push was a big dash. Now it only adds height (Spring Step and
  Jump Boost still count).
- 2026-10-26: **0.5.0 released** (tag `v0.5.0`, Alpha): Melee (slices a–d + shield perks), Fishing, trident Archery,
  bestiary pages/collections, two-column Character tab. Released before most of it was playtested (user's call).
- 2026-10-31: **Skill perks spread over the levels.** At Rank 10 with no skill levels almost nothing reflected how you
  play. Sturdy I (tools and weapon) has no requirement, Sturdy II/III and Thrifty Quiver II/III are level 8/18, Sturdy
  Weapon II/III 10/20 mob types. Small perks moved to levels 3/5/6/8/12/13, strong ones stayed put, none got harder.
  Aim for 10–12 useful choices at Rank 10.
- 2026-10-31: **Steady Healing I/II** (Survival): natural healing costs 25% / 50% less hunger, on top of the stomach
  multiplier. Free / Rank 10, cost 2/3.
- 2026-10-31: **Later skeletons and creepers, earlier Bonus Hearts.** Dev saves reached Rank 5 in about 9 minutes and
  Rank 10 in about 30, so skeletons came before the first night and before iron for a shield; the user met them before
  finding a first mine. Skeletons Rank 5 → 10, creepers 12 → 18 (skeleton elites follow, Rank 15). Rank points stay
  as they are: quick early rewards are fun. Bonus Heart I/II/III now Rank 5/10/20 (was 5/20/40): most valuable early.

## Playtest checklist

Built but not confirmed by the user's playtest yet; don't assume these work or feel right:
- **Skill perk spread (2026-10-31):** a new world gets a perk from each skill often enough, with no long wait.
- **Steady Healing (2026-10-31):** hunger clearly drops slower while healing with the perk (Survival list, no missing text).
- **Rank pacing (2026-10-31):** skeletons (Rank 10) and creepers (Rank 18) no longer arrive before you have iron and a
  shield; the first nights don't feel empty. Bonus Hearts at Rank 5/10/20 don't make the early game too easy.
- **Melee skill, slice a (2026-10-26):** XP pace (Melee 10 ≈ 50 zombies?), no XP from spam-clicked weak hits.
  Attack speed noticeable at higher levels (`/pam skill melee 30`): cooldown bar fills faster, also for axes.
  Wide Sweep: other mobs in a sweep clearly take more. Without Careful Sweep a sweep hits your dog (vanilla); with it,
  never your dog, a villager or an iron golem.
  Hearty Kill heals on a hostile kill, only once when a sweep kills several. Perks tab: Melee category with
  "Any weapon" / "Sword" headings, scrolling and clicking still hit the right row; Second Wind and Hearty Kill
  under Survival. Character tab shows 7 skills. **Slice b:** Whirlwind (sneak + sweep) hits mobs behind you but not
  your dog or a cow; is the ring visible? Disarm: skeleton drops its bow and walks up; message readable. Heavy Swing
  knockback feels heavy, not silly. Sunder on an armored elite (`/pam elite zombie`): sparks/sound, next hits clearly
  stronger, armor back after 5 s. **Slice c** (Parry and Shield Bash work in singleplayer): Parry timing on a server
  with ping; Deflect: skeleton arrow flies back and hits it (aim OK at 10-20 blocks?). **Slice d:** Ground Pound (mace, jump or fall, press sneak):
  dive feels good, no fall damage, mobs pushed, no rubber-band on a server; not too low (2 blocks); water landing
  doesn't leave it "stuck" (next normal fall still hurts). Shockwave hurts mobs around a smash and a pound landing.
  Running Charge: sprinting spear charge clearly stronger, can knock a skeleton jockey off. Pinning Jab slows.
  Perk list: 5 Melee headings (Any weapon, Sword, Axe, Mace, Spear) scroll fine. **Review fixes:** Sturdy Weapon
  saves durability on a spear, mace and trident; Disarm clang. Open a mob page in the Bestiary, leave the world,
  open another: the grid shows. A Ground Pound onto a ladder, then a normal fall: fall damage again. Tridents: throw charge faster with Archery, thrown hits give
  Archery XP, Loyal Trident brings a plain trident back (a mixin failure would crash on the first throw).

- **Bestiary pages (2026-10-26):** click a tile → page; does the 3D model fit its box for big and small mobs
  (ghast, ender dragon, wither, slime, bat)? Do the sections fit the panel (long tips + 2 rows of drops + elite
  traits)? Drops load (server request) and look right (zombie: rotten flesh often, iron/carrot/potato rarely)?
  Exact chances at Expert, double loot at Master (test with `/pam kills zombie 100`). New-entry
  toast. Assist kills: two players hit a mob, both get the kill. Collections and Trait Collector granted, also
  on join for old saves.

- **Farming:** XP pace (30 wheat = level 2; Replant at level 5 = 100 harvests), Light Step,
  sweet berry XP, breeding XP + Rancher twins, Green Thumb. Axes on pumpkins/melons no longer
  give Woodcutting XP (fixed, check it).
- **Archery (3c):** test the perks with `/pam skill archery 40` (ops). XP pace, each perk.
  Steady Aim: helpful but not silly? Does the curving arrow look smooth on the client? Ricochet:
  does the bounce look and sound clear, and does the new arrow ever hit the first mob again?
- **Screen:** taller panel (250px) at the user's GUI scale; perk list scrolling.
- **Second perk round:** Treasure Sense trail + chime (not annoying?), Vein Miner, Cat's Eyes
  brightness (I/II noticeable, caves not fully bright?), Lumberjack/Timber, Orchard and Bird's Nest
  rates, Excavator plane direction, Path Maker, Relic Sense near a desert pyramid or trail ruins,
  Sturdy Weapon III, Forester (sapling appears?), Clear Cut (drops OK incl. Orchard apples, neighbouring trees
  untouched), Flint Finder. Ore Sense was moved into `BlockSense` (recheck it still works).
- **Fishing skill (2026-10-26):** cast a rod (a mixin failure would crash there). XP pace (Fishing 5 after ~25
  catches?), bites noticeably faster at higher levels (test with `/pam skill fishing 30`). Keen Eye bell audible and
  not annoying, Lucky more treasure, Double Catch, Treasure Hunter in a 1-block pond. Sunken Treasure and Fish
  Collector granted. Character tab shows Fishing with the next-perk tooltip.
- **Character tab, two columns (2026-10-26):** skills fit at the user's GUI scale, also with two rows of
  hearts? Hover tooltip readable (bonus, xp to next level, next perk)? Swimming perks now show under Survival
  (Deep Breath, Diver) and Movement (Swimmer).
- **Movement perks:** 10 category rows fit at the user's GUI scale? Each attribute
  perk noticeable? Double Jump: no forward dash when sprinting (push removed 2026-10-26), feels instant, no rubber-band
  on a server, no fall damage after it, off with elytra. Note it can be used to cancel any fall
  (jump just before landing): fun or too strong? Mantle: triggers on 2-block walls, not on fences,
  not on every hop up a block, smooth on a server? Shadow Step I: sneak past mobs closer than
  normal. II: mob gives up at 8+ blocks, message readable, hitting it re-aggros.
- **Defense perks:** reductions noticeable but not overpowered? Is 5/10 mob types the right gate?
- **Mob unlocks (v0.3a, quick `/pam` check OK):** does night 1 feel right (only zombies, fewer
  mobs)? Too empty → raise `lockedMobReplacementChance`. Unlock chat message and the "Next: ..."
  line on the Character tab.
- **Elites (v0.3b):** `/pam elite zombie` etc. Size, name on hover, gear by rank, drops only on
  player kill, XP, bonus loot, Elite Slayer milestone. Does normal-mob gear feel right?
- **Traits (v0.3c):** `/pam elite zombie frost` etc. Does each trait feel fair for kids?
- **Waypoints (v0.4a):** `/pam elite zombie` → shard drop (first kill always). Craft a stone,
  turn it on (name screen, shard cost), particles + floating name, texture looks OK? Second stone
  costs 2. Travel: list, costs, greyed out without XP, charge-up, cancel by getting hurt/walking
  away, landing spot, pets come along (sitting ones don't), Nether trip cost. Rename (sneak +
  empty hand), another player can't break your stone, breaking gives shards back. First Trip and
  Network milestones, Pathfinder discount. `/pam waypoint list/tp/remove`. Works on a server/LAN
  with two players?

- **Placed blocks:** placing and breaking a log/ore still gives no XP; dirt/stone now do. Log
  cabin next to a tree survives Timber. Old worlds load fine (dev server start checked).
- **Progress sync per tick:** XP bar/levels on the Character tab still update right away when mining
  and with Timber; skill XP kept after logging out right after mining, and after dying.
- **Stomach level 7:** with all 7 stomach milestones the Character tab tooltip shows 60% hunger drain.
- **Audit fixes:** with more than 10 hearts, get hurt, relog: health stays the same (log in twice after updating,
  the first one still heals). Die and respawn: full health. More than 20 hearts (e.g. `/pam` + Bonus Hearts):
  hearts on the Character tab overlap neatly, nothing drawn over the stomach line. Second Wind works in a
  second world opened after using it in another.
- **Audit fixes 2:** `/advancement revoke` takes hearts/Rank away right away. Replant, Harvester, Timber and
  Path Maker don't change blocks inside spawn protection (test on the dev server as a non-op). Path Maker 3×3 uses
  9 durability (less with Sturdy Shovel). `/pam elite spider leaping` is refused.

Confirmed 2026-10-26: the combat perks work well in a first playtest; Double Jump + Ground Pound with a mace is fun
and its damage felt fine (exact perks tested not recorded, so the Melee items above stay open).
Confirmed: Ore Sense, Replant, Harvester (requirements restored 2026-10-25). Clear Cut: leaves come down
smoothly. Archery draw speed: very fast at level 80, but fair for the grind it takes. General testing over time:
most things work well and feel good; balance needs more play time (see Open questions).

Dropped (never released, so no old saves to migrate): owners of the old single Deep Breath / Fleet Foot perk,
owners of Timber without Lumberjack. Config overrides were checked on the dev server (empty file + defaults file,
override applied, typo warned).

## Open questions

- [ ] Perk balance: costs and point budget (playtest).
- [ ] Balance: hearts per milestone (early milestones give hearts quickly), Rank thresholds,
  elite chances, skill XP curves.
- [ ] Waypoint XP cap (50, +20 across dimensions): maybe higher? Revisit after the playtest.
- [ ] **Fall damage can be avoided completely (2026-10-26, design question, not fixing yet).** Double Jump resets
  fall distance (a jump just before landing cancels any fall), and Ground Pound with a mace cancels the landing
  damage of a dive started 2+ blocks up, even at the end of a huge fall. Together, a player with either perk never
  needs to fear heights. Fun, but it takes away a danger. Options: Double Jump only resets part of the fall (or
  only below some height); Ground Pound only protects the dive itself (fall distance from where it started);
  leave it as the reward for late perks (Rank 30, Melee 15).
- [ ] Ground Pound may get too strong later (2026-10-26): Double Jump + dive is fun and fine now, but watch it with
  Shockwave and big falls (its damage grows with the fall). Knobs: `shockwaveDamageRatio`, a fall cap for the bonus.

## Roadmap

- **v0.1 (tag `v0.1.0`, first test version):** 5 starting hearts, 25 milestones
  granting hearts/stomach, metabolism, Mining/Woodcutting/Digging skills, bestiary
  damage modifier, config, `/pam` commands. Playtested: works well.
- **v0.2:** perk points + perk list above, Adventure screen (character, perks,
  bestiary tabs), keybind. Playtested: works (not tagged yet).
- **v0.2.x:** Farming skill, farming perks, Farmer milestones, Ore Sense, Deep Breath and Fleet
  Foot tiers, Defense perks.
- **v0.3a:** Rank-based mob unlocking. **v0.3b:** mob gear + elite mobs + elite loot.
  **v0.3c:** elite traits for high ranks.
- **v0.3.x:** Archery skill + perks; Movement and Swimming perk categories (Double Jump, Mantle,
  Shadow Step, ...).
- **v0.4a:** Waypoint Shards + Waypoint Stones, travel for XP, Pathfinder perk.
- **0.4.0 (tag `v0.4.0`, 2026-10-25):** first public release on Modrinth (Alpha), everything above.
  Not tagged separately: v0.2 and v0.3. What still needs a playtest is in the playtest checklist.
- **0.5.0 (tag `v0.5.0`, 2026-10-26):** Melee skill (all 4 slices, shield perks in Defense), Fishing skill,
  trident Archery, bestiary pages and collections, two-column Character tab. Released with much of it still on
  the playtest checklist (user's call).
- **Next:** work through the playtest checklist and Open questions (balance), then pick from
  [Ideas](#ideas) (likely candidates: Well Fed, more milestones).
