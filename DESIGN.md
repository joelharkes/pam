# PAM: Progressive Adventure Mod — Design

> Living document: ideas, decisions and open questions.

**Built so far (2026-10-25):** pillars 1–4 and 6 (hearts, metabolism, Mining/Woodcutting/
Digging/Farming/Archery skills, bestiary, 87 perks in 10 categories, Adventure screen) and pillar 5
mob unlocks, elites and elite traits, pillar 7 waypoints (shards, stones, travel). Not built: Combat/Fishing skills, Well Fed.
Much of it still needs a playtest: see the [playtest checklist](#playtest-checklist) and the roadmap.

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

---

## Core idea: one source, two rewards

```
ANY advancement (vanilla + PAM)
  ├──► +1/2/3 Adventure Rank (task/goal/challenge) ──► mob unlocks, elites (v0.3)
  │         └── every rank is also a perk point ──► spend on perks
  └──► some of them (milestones) also give hearts or a stomach upgrade
skill levels / mob types / rank ──► unlock which perks you may buy
```

- **Adventure Rank = perk points earned** (about 217 in total). Spending or resetting
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
- Rewards (hearts etc.) are granted when the advancement completes.
- Some criteria need custom triggers (e.g. "visit 10 biomes", "walk 1 km").

---

## Feature pillars

### 1. Vitality: hearts from milestones

- Start with **5 hearts** (configurable). Cap around **15–20 hearts** in the late game.
- Each milestone grants **half a heart, a full heart or a stomach upgrade**. Big ones
  (bosses) grant more.
- Feasibility: **easy.** A permanent `max_health` attribute modifier.

#### Milestone list

What makes a good milestone:
- ✅ Makes you **go somewhere new** (biome, structure, dimension).
- ✅ Makes you **try a mechanic** you might skip (brewing, trading, taming, fishing).
- ✅ Clear and checkable: a kid knows when it's done.
- ❌ Pure grind ("break 10,000 stone", "walk 100 km").
- ❌ Luck-only or tedious (rare drops, full collections of everything).

Implemented: 24 PAM milestones + 18 vanilla advancements with a reward (half hearts unless noted):

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

| Vanilla advancement                         | Reward   |
|---------------------------------------------|----------|
| Sweet Dreams, What a Deal!, Isn't It Iron Pick, Diamonds!, Not Today Thank You, Enchanter, Best Friends Forever, Fishy Business, Local Brewery, A Terrible Fortress, Those Were the Days | ½ heart each |
| We Need to Go Deeper, The End?, Zombie Doctor, Hero of the Village, The City at the End of the Game | 1 heart each |
| Free the End                                | 2 hearts |
| A Balanced Diet                             | stomach  |

Totals: 26½ hearts to earn (start 5, cap 20: 15 to earn, so there are many routes to
the cap) and 7 stomach levels. 20 is our config cap, not a vanilla limit (vanilla
max_health goes to 1024); 20 hearts = two full rows on the HUD.

Milestone ideas not built yet: raise a shield, desert/jungle temple, shipwreck or ocean
ruin, reach Y=0 / lush or deep dark cave, mountain peak, buried treasure, cake or
suspicious stew, 5 villager professions. More advancements may be
added later to give more Rank/points.

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
| 6             | ×0.7         | ×1.3            | A Balanced Diet          |
- Stomach levels can come in any order; the table shows a typical one.
- **Diet variety bonus (idea, not built):** eating a varied diet gives a temporary "Well Fed"
  buff (slower hunger drain, small regen). Eating the same food over and over gives
  less. This teaches kids to farm several crops and fits the Gourmet milestones.
- Feasibility: **medium.** Small mixin on exhaustion/food values. No HUD changes
  needed.
- Optional later: show the stomach level as a small icon next to the hunger bar.

### 3. Proficiency: get better by doing

Use-based skills that level up with use. Gains follow a **diminishing curve**: the
first levels come quickly and feel great, later levels are slow and small.
Like the bestiary, skills **start below vanilla (−20% speed)**, reach vanilla around
level 4 (~64 blocks) and cap near +60%.

| Skill       | Trained by                    | Bonus                                      | Status      |
|-------------|-------------------------------|--------------------------------------------|-------------|
| Mining      | Pickaxe blocks                | Break speed (+ perk: chance of extra ore)  | Built |
| Woodcutting | Axe blocks                    | Break speed (+ perk: fell small trees)     | Built |
| Digging     | Shovel blocks                 | Break speed                                | Built |
| Combat      | Melee damage dealt            | Attack speed/damage                        | Idea |
| Archery     | Arrow hits on mobs, kills     | Bow/crossbow draw speed (see 3c)           | Built (3c) |
| Farming     | Harvesting grown crops, breeding | Extra harvest chance (see 3b)           | Built (3b) |

- Farm blocks (crops, melons, pumpkins, cocoa, cane, cactus, bamboo) only train Farming, even
  though vanilla lets an axe mine some of them.
| Fishing     | Catching fish                 | Faster bites, better loot                  | Idea |

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
| Sturdy Hoe I/II/III | 10/20/30% chance hoes use no durability | Farming 5/15/30 | 1/2/3 |
| Bountiful Harvest I/II | 10%/20% chance of double crop drops | Farming 10/25 | 2/3 |
| Harvester | Breaking or Replanting a grown crop with a hoe also does the grown crops in a 3×3 area | Farming 20 | 3 |
| Rancher | 15% chance of twins when breeding | Farming 10 | 2 |
| Green Thumb | 25% chance bone meal isn't used up | Farming 15 | 2 |

**Farmer milestones:** harvest 3 different crops (stomach), harvest 10 different crops
(½ heart), breed 5 different kinds of animals (½ heart).

### 3c. Archery skill

- **XP:** 3 per arrow a player shot that hits a mob, 5 for a kill with it (raised from 1/3: hits
  are much rarer than mined blocks; Archery 20 ≈ 320 kills, 40 ≈ 1300). Any mob counts (animals
  too), but not players, armor stands or blocked hits. Tridents don't count.
- **Passive bonus:** bow and crossbow draw speed, 0% → +50% with the usual curve. No start
  penalty: unknown mobs already take less damage early. The use timer itself runs faster, so the
  pull animation, zoom and crossbow charge match the real power.
- **No damage perks:** the bestiary is the damage progression (and already counts arrows).
  Damage perks would undercut it and need matching sword perks.

| Perk | Effect | Requires | Cost |
|---|---|---|---|
| Thrifty Quiver I/II/III | 10/20/30% chance a shot uses no arrow (like Infinity: that arrow can't be picked up) | Archery 5/15/30 | 1/2/3 |
| Fire Arrows | 15% chance the arrow is on fire | Archery 10 | 2 |
| Frost Arrows | 15% chance the arrow slows the mob it hits (Slowness II, 3s), snowflake trail | Archery 10 | 2 |
| Piercing Shot | Arrows fly through 1 extra mob (stacks with the Piercing enchantment) | Archery 15 | 2 |
| Steady Aim | Arrows turn up to 2°/tick toward the hostile mob nearest their path (15° cone, 20 blocks, in sight). Neutral mobs only when they target you; never animals, villagers, pets | Archery 20 | 3 |
| Ricochet | After hitting (or killing) a mob, a new arrow flies to the nearest other hostile mob within 8 blocks, with the same damage, fire and frost. Bounces once; can't be picked up | Archery 40 | 5 |

Poison arrows were dropped: zombies and skeletons are immune, so it would look broken to kids.

### 4. Bestiary: mob familiarity

Each mob type you defeat builds familiarity, which changes how much damage you deal
to that type. **Unknown mobs are a bit tougher than in vanilla**, you break even
after a few kills, and then gains are big early and flatten out:

| Kills | Damage modifier | Label      | Extra (ideas)                  |
|-------|-----------------|------------|--------------------------------|
| 0     | −15%            | Unknown    | Shows as "?" in the bestiary   |
| 1     | −10%            | Unfamiliar | Mob appears in your bestiary   |
| 3     | −5%             | Unfamiliar |                                |
| 5     | ±0% (vanilla)   | Known      | See its health bar             |
| 15    | +10%            | Familiar   |                                |
| 30    | +20%            | Familiar   | See its drops in the bestiary  |
| 60    | +35%            | Expert     |                                |
| 100   | +50%            | Master     | "Mastered" badge               |
| 250   | +55%            | Master     | (cap)                          |

- Example: a zombie (20 HP) vs. a wooden sword (4 damage) takes 5 hits in vanilla
  and 6 hits at −15%. Noticeable, not brutal.
- The label is visible (bestiary, maybe on hover/health bar) so a penalty never
  feels like a bug: "I don't know this mob yet."
- The −15% stacks with the other early-game penalties (5 hearts, faster hunger).
  This is OK because early ranks only spawn zombies. Watch in playtests.
- At 100 kills a zombie clearly goes down faster than in vanilla (goal achieved).
- Kills by AFK farms count for less or not at all.
- We keep our own kill counter (not vanilla `Stats.KILLED`) so old worlds don't get
  retroactive progress.

### 5. World progression: mob unlocks and elite mobs

#### Mob unlocks by Rank (v0.3a, built)

Overworld hostile mobs unlock one group at a time as Adventure Rank (points earned,
~217 total, ~10 after a first session) goes up. Rank 20 is expected while still in the
Overworld (a few in-game days); everything is unlocked by Rank 50, so the world
reaches vanilla danger fairly quickly.

| Rank | New mobs                                         | Share of vanilla hostile spawns (roughly) |
|------|--------------------------------------------------|------------------------|
| 0    | Zombies (incl. husks, zombie villagers)          | ~⅙                     |
| 5    | Skeletons (incl. strays, bogged, parched)        | ~⅓                     |
| 12   | Creepers                                         | ~½                     |
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
  appear!"), "Next: Creepers at Rank 12" on the Character tab (sent by the server, so
  it's correct even with a changed server config), and in `/pam`.
- **Untouched:** structure spawn lists (witch huts, outposts, monuments, fortresses),
  spawners, trial chambers, raids, spawn eggs, the Nether and End, phantoms, and mobs
  from other mods unless added to the config list.
- Groups, ranks and mob ids are all in the config (`mobUnlocks`), plus an on/off switch.
- **Creepers and health:** a point-blank creeper can kill even a vanilla 10-heart
  player without armor. By Rank 12 players usually have a few extra hearts and some
  armor, and Blast Guard exists. Watch in playtests.

#### Mobs get stronger: gear and elites (v0.3b, built)

Every natural Overworld hostile spawn uses the lowest Adventure Rank of nearby players.

**Normal mobs:** zombies and skeletons (and their variants) get a growing chance of 1–2
extra armor pieces, on top of vanilla's own: 0% at Rank 10 → 25% at Rank 100. Material
leather → chainmail (Rank 30) → iron (Rank 60). No stat changes; vanilla drop chances.

**Elites ("Tough Zombie"):**
- Mob types: zombies, husks, zombie villagers, drowned, skeletons, strays, bogged,
  parched, spiders, cave spiders, endermen, slimes. No creepers, witches or pillagers.
- Start 5 ranks after the type unlocks (zombies at Rank 5, skeletons 10, creepers never,
  spiders 25, ...): 2% of that type's spawns, +0.1% per rank, max 10%.
- 20% bigger (vanilla `scale` attribute), +50% health, +25% damage, named (name shows
  when you look at it). They despawn like normal mobs.
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
- Milestone "Elite Slayer": defeat an elite, ½ heart.
- Test command for ops: `/pam elite <mob>` spawns an elite at your rank.

**Traits (v0.3c, built):** from Rank 100, an elite has a 25% → 75% (Rank 160) chance of one
random trait, shown in its name instead of "Tough":

| Trait    | Effect |
|----------|--------|
| Swift    | +30% movement speed |
| Frost    | its hits and arrows give Slowness II for 3s |
| Blazing  | its hits and arrows set you on fire for 4s |
| Leaping  | leaps at you like a spider (not skeletons/slimes) |
| Summoner | the first time a player hits it, calls 2 normal helpers of its kind |

Trait elites have ×1.5 bonus loot chances. Test: `/pam elite <mob> [trait]`.

#### Multiplayer

Spawns are decided by nearby players: when a mob spawns, use the rank of the
**lowest-ranked player nearby**. When a kid plays near a parent, the world stays
kid-friendly around them.

Feasibility: **medium.** Hook into natural spawning to cancel locked mobs or upgrade
mobs to elites.

### 6. Perks: choosable upgrades

#### Perk points

- **Every advancement** with a visible entry gives perk points, vanilla and PAM:
  **task = 1, goal = 2, challenge = 3** (vanilla's own frame types, so hard
  advancements are worth more).
- Not counted: tab roots ("Minecraft", "Nether", ...) and recipe unlocks (they have
  no display).
- Points are **derived**, never stored: `available = earned − cost of owned perks`.
  Old worlds get points for advancements they already have, which is fine.
- Rough budget: vanilla has ~125 advancements (~190 points), PAM adds ~40. Buying
  every perk costs ~75 points, so early on you choose, late game you can have most.

#### Rules

- A perk has a **cost**, an optional **requirement** (skill level, mob types
  defeated or Adventure Rank) and an optional **prerequisite perk** (tier II needs I).
- Requirements are shown on locked perks so kids know what to do ("Reach Mining 15").
- **Free respec:** a "Reset perks" button refunds everything (no confirmation, it's free). No
  penalty, fits the family rule.
- Owned perks stay owned even if a config change lowers your points.
- Server validates every purchase (no cheating from a modified client).

#### Perk list

| Category    | Perk                | Effect                                              | Requires            | Cost |
|-------------|---------------------|-----------------------------------------------------|---------------------|------|
| Mining      | Sturdy Pickaxe I    | 10% chance pickaxes use no durability               | Mining 5            | 1    |
| Mining      | Sturdy Pickaxe II   | 20% chance (replaces I)                             | Mining 15, perk I   | 2    |
| Mining      | Sturdy Pickaxe III  | 30% chance                                          | Mining 30, perk II  | 3    |
| Mining      | Prospector I        | 10% chance of double ore drops                      | Mining 10           | 2    |
| Mining      | Prospector II       | 20% chance                                          | Mining 25, perk I   | 3    |
| Mining      | Ore Sense           | Holding a pickaxe: particle trail points at the nearest ore within 12 blocks, coloured by ore type, bigger when closer | Mining 40 | 4 |
| Mining      | Treasure Sense      | Holding a pickaxe: white sparkle trail (every 2.5s) to the nearest diamond ore / ancient debris within 24 blocks; quiet amethyst chime once per block within 6 | Mining 50, Ore Sense | 5 |
| Mining      | Vein Miner          | Mining an ore also mines up to 16 connected ores of the same kind (sneak = off) | Mining 30 | 4 |
| Mining      | Cat's Eyes I/II     | 25% / 50% of Night Vision's brightening, always on  | Mining 15 / 35      | 2/3  |
| Woodcutting | Sturdy Axe I/II/III | Same as pickaxe, for axes                           | Woodcutting 5/15/30 | 1/2/3|
| Woodcutting | Lumberjack          | Chopping a log also chops up to 6 connected logs (sneak = off) | Woodcutting 10 | 2 |
| Woodcutting | Timber              | Chopping a log fells the whole tree (sneak = off)  | Woodcutting 25, Lumberjack | 4 |
| Woodcutting | Forester            | Lumberjack/Timber plant the matching sapling where the bottom log stood | Woodcutting 15, Lumberjack | 2 |
| Woodcutting | Clear Cut           | After Timber, the tree's leaves break at once (same drops as decay, Orchard applies) | Woodcutting 30, Timber | 2 |
| Woodcutting | Orchard             | Natural leaves you break: 5% apple, 0.04% golden apple (any tree) | Woodcutting 5 | 2 |
| Woodcutting | Bird's Nest         | 2% per natural log chopped with an axe: 1–2 of feather/egg/string/stick/seeds, 10% of nests also an emerald | Woodcutting 15 | 2 |
| Digging     | Sturdy Shovel I/II/III | Same, for shovels                                | Digging 5/15/30     | 1/2/3|
| Digging     | Archaeologist       | 3% chance of a bonus find (flint, clay, bone, ...) from dirt, sand, gravel | Digging 10 | 2 |
| Digging     | Flint Finder        | 30% chance of an extra flint from natural gravel    | Digging 5           | 1    |
| Digging     | Path Maker          | Making a path with a shovel flattens a 3×3 square (sneak = single) | Digging 5 | 1 |
| Digging     | Relic Sense         | Holding a brush or shovel: trail to the nearest suspicious sand/gravel within 16 blocks | Digging 15 | 2 |
| Digging     | Excavator           | Shovels dig a 3×3 square facing you (sneak = off)   | Digging 20          | 4    |
| Combat      | Sturdy Weapon I/II/III | 10/20/30% chance swords use no durability        | 5 / 15 / 25 mob types defeated | 1/2/3 |
| Combat      | Quick Study         | Kills count double towards familiarity              | 5 mob types defeated | 2   |
| Combat      | Second Wind         | Dropping below 3 hearts gives Regeneration II for 5s (2 min cooldown) | — | 2 |
| Defense     | Sturdy Armor I/II/III | 10/20/30% chance armor uses no durability       | 5 / 10 / 15 mob types defeated | 1/2/3 |
| Defense     | Blast Guard I/II    | 15% / 30% less damage from explosions               | 5 / 10 mob types    | 2/3  |
| Defense     | Arrow Guard I/II    | 15% / 30% less damage from projectiles              | 5 / 10 mob types    | 2/3  |
| Defense     | Fire Guard I/II     | 15% / 30% less damage from fire, lava, burning      | 5 / 10 mob types    | 2/3  |
| Defense     | Tough Hide I/II     | 10% / 20% less damage from mob hits (melee)         | 5 / 10 mob types    | 2/3  |
| Defense     | Steady Stance       | 30% knockback resistance                            | 10 mob types        | 2    |
| Survival    | Bonus Heart I/II/III| +1 heart each, on top of milestone hearts (ignores cap) | Rank 5 / 20 / 40 | 3/4/5|
| Survival    | Iron Stomach        | Immune to the Hunger effect (rotten flesh, husks)   | —                   | 1    |
| Survival    | Long Arms           | +1.5 blocks block reach                             | Rank 10             | 2    |
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
| Swimming    | Deep Breath I/II/III | ~30s / ~45s / ~60s of air underwater (vanilla 15s) | — / Rank 10 / Rank 25 | 1/2/3 |
| Swimming    | Swimmer I/II        | Water movement efficiency 0.33 / 0.66 (Depth Strider) | Rank 5 / 20       | 2/3  |
| Swimming    | Diver               | Mine underwater at full speed (Aqua Affinity)       | Rank 10             | 2    |

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
- Clear Cut: 10 ticks after felling (leaf distance spreads one block per tick), breaks the
  non-persistent leaves at decay distance 7 in the felled logs' box + 7. Only leaves that would
  decay anyway, so a neighbouring tree is never touched. Breaks 32 leaves per tick (no lag spike
  or particle burst from a huge canopy).
- Forester: only when the broken log is the bottom one (no log below) and the sapling can survive
  there. Single saplings for 2×2 trees (dark oak, big spruce/jungle) won't grow into the big tree.
- Cat's Eyes: client mixin on the lightmap, raises the night-vision intensity it already uses.
  Player-placed logs are never felled, so houses are safe.
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
  (`jumpFromGround`, so Spring Step adds to it). Mantle: falling or near the top of a jump, pushing
  forward into a wall whose top is 0.6–1.9 blocks above the feet, with room to stand on it; never
  on fences/walls (collision taller than 1 block).
- Later ideas: Night Owl (light/night vision underground), a trail home (bed/spawn), Wall
  Slide, Lucky (fishing loot, with the Fishing skill), Fireproof Skin (overlaps Fire Guard). Animal
  Friend can't be an attribute (tempt range belongs to the animal). Well Fed (diet variety), extra crop yield (Farming skill), faster bow
  draw (Archery), auto-smelt, night vision underground.
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
  Only from stone to stone (teleport from anywhere = maybe a later Pathfinder tier).
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
- Adventure Rank, hearts (current max / cap, as heart icons), stomach level. Milestone
  count and hunger drain % are in hover tooltips.
- One row per skill: tool icon, name, level, xp progress bar to next level, current
  speed bonus.

#### Perks tab
```
┌──────────────────────────────────────────────────────┐
│ Perk points: 7                       [Reset perks]   │
│ ┌──────────┐ ┌────────────────────────────────────┐  │
│ │ Mining   │ │ ⛏ Sturdy Pickaxe I               ✔ │  │
│ │ Woodcut. │ │ ⛏ Sturdy Pickaxe II          2 pts │  │
│ │ Digging  │ │ ⛏ Sturdy Pickaxe III            🔒 │  │
│ │ Farming  │ │ 💎 Prospector I              2 pts │  │
│ │ Combat   │ │ 💎 Prospector II                🔒 │  │
│ │ Defense  │ │                                    │  │
│ │ Survival │ │                                    │  │
│ └──────────┘ └────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
```
- Categories on the left, perks of the selected category on the right (scrolls when a
  category has more perks than fit). Hover a perk for its full description.
- States: owned (✔), buyable (cost in gold, click the row), too expensive (cost in red),
  locked (🔒). What blocks a locked perk is only in the row tooltip: one hover per row,
  no separate hover on the lock icon.
- Reset perks has no confirmation (it's free); the button tooltip says so.

#### Bestiary tab
- Grid of spawn-egg icons for every mob with a spawn egg, hostile mobs first, with
  pages. "Discovered 12 / 80" at the top.
- Undiscovered mobs show a dark "?" tile ("Defeat one to discover").
- Hover: name, kills, familiarity label, damage modifier, kills needed for next tier.
- Tile border colour by tier (grey → white → green → gold for Master).

- **Toasts** for level-ups, new bestiary entries and Rank-ups (later; chat/action
  bar messages for now).
- `/pam` and `/pam bestiary` stay as text fallbacks.

---

## Future ideas: more reasons to explore

To brainstorm later:

- **Heart containers in loot:** rare items in structure chests (dungeons, temples,
  ancient cities) that grant a bonus heart. Rewards exploring structures.
- **Biome bonuses:** first visit to a biome grants a small one-time XP bonus to a
  related skill.
- **Mob discovery:** first kill of a new mob type gives a big XP burst and a toast.
- **Mobs drop exploration helpers** (user idea): became Waypoint Shards (see 7). Treasure
  or explorer maps from elites are still an option.
- **Shrines/landmarks:** small custom structures with a challenge and a reward.

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
  only with a pickaxe in hand. Requirement Mining 40, temporarily removed for testing.
- 2026-10-24: Deep Breath split into 3 tiers. Replant/Harvester requirements temporarily
  removed for testing.
- 2026-10-24: Defense perks are specific (explosion, projectile, fire, mob hits), no general
  damage reduction: a general one would make the others pointless, and Bonus Hearts already
  act as general protection. Gated on mob types defeated (5 / 10), kept easy.
- 2026-10-24: Fleet Foot split into 3 tiers (+5% each, Rank 5/10/15).
- 2026-10-24: Mob unlock order: zombies → skeletons → creepers → spiders →
  endermen → slimes → witches. Elites per mob type, starting early (zombies at Rank 2).
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

## Playtest checklist

Built but not confirmed by the user's playtest yet; don't assume these work or feel right:

- **Farming:** XP pace (30 wheat = level 2; Replant at level 5 = 100 harvests), Light Step,
  sweet berry XP, breeding XP + Rancher twins, Green Thumb. Axes on pumpkins/melons no longer
  give Woodcutting XP (fixed, check it).
- **Archery (3c):** test the perks with `/pam skill archery 40` (ops). XP pace, draw speed feel (does the pull animation match?), each perk.
  Steady Aim: helpful but not silly? Does the curving arrow look smooth on the client? Ricochet:
  does the bounce look and sound clear, and does the new arrow ever hit the first mob again? Character
  tab: 5 skill rows only just fit with two rows of hearts.
- **Screen:** taller panel (250px) at the user's GUI scale; perk list scrolling.
- **Second perk round:** Treasure Sense trail + chime (not annoying?), Vein Miner, Cat's Eyes
  brightness (I/II noticeable, caves not fully bright?), Lumberjack/Timber, Orchard and Bird's Nest
  rates, Excavator plane direction, Path Maker, Relic Sense near a desert pyramid or trail ruins,
  Sturdy Blade III, Forester (sapling appears?), Clear Cut (leaves vanish ~0.5s after felling, drops OK,
  neighbouring trees untouched), Flint Finder. Ore Sense was moved into `BlockSense` (recheck it still works). Players who
  owned Timber keep it without Lumberjack.
- **Movement / Swimming perks:** 10 category rows fit at the user's GUI scale? Each attribute
  perk noticeable? Double Jump: feels instant, no rubber-band
  on a server, no fall damage after it, off with elytra. Note it can be used to cancel any fall
  (jump just before landing): fun or too strong? Mantle: triggers on 2-block walls, not on fences,
  not on every hop up a block, smooth on a server? Shadow Step I: sneak past mobs closer than
  normal. II: mob gives up at 8+ blocks, message readable, hitting it re-aggros.
- **Deep Breath tiers / Fleet Foot tiers:** owners of the old single `deep_breath` /
  `fleet_foot` perk lost it (point refunded).
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
- **Clear Cut 32 leaves/tick:** big jungle/dark oak canopy disappears smoothly over a few ticks,
  Orchard apples still drop.

Confirmed: Ore Sense, Replant, Harvester (requirements restored 2026-10-25).

## Open questions

- [ ] Perk balance: costs and point budget (playtest).
- [ ] Balance: hearts per milestone (early milestones give hearts quickly), Rank thresholds,
  elite chances, skill XP curves.
- [ ] Tag `v0.2.0`? Offered, no answer yet.
- [ ] Waypoint XP cap (50, +20 across dimensions): maybe higher? Revisit after the playtest.

## Roadmap

- **v0.1 (tag `v0.1.0`, first test version):** 5 starting hearts, 25 milestones
  granting hearts/stomach, metabolism, Mining/Woodcutting/Digging skills, bestiary
  damage modifier, config, `/pam` commands. Playtested: works well.
- **v0.2:** perk points + perk list above, Adventure screen (character, perks,
  bestiary tabs), keybind. Playtested: works (not tagged yet).
- **v0.2.x:** Farming skill, farming perks, Farmer milestones, Ore Sense, Deep Breath and Fleet
  Foot tiers, Defense perks.
  *(Implemented, needs playtesting.)*
- **v0.3a:** Rank-based mob unlocking. *(Implemented, needs playtesting.)*
- **v0.3b:** mob gear + elite mobs + elite loot. *(Implemented, needs playtesting.)*
- **v0.3c:** elite traits for high ranks. *(Implemented, needs playtesting.)* Then tag v0.3.
- **v0.3.x:** Archery skill + perks. *(Implemented, needs playtesting.)*
- **v0.3.x:** Movement and Swimming perk categories (Double Jump, Mantle, Shadow Step, ...).
  *(Implemented, needs playtesting.)*
- **v0.4a:** Waypoint Shards + Waypoint Stones, travel for XP, Pathfinder perk.
  *(Implemented, needs playtesting.)*
- **v0.4:** more skills (Combat, Fishing) + their perks, diet
  variety, more milestones, exploration extras.
