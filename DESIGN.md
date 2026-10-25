# PAM: Progressive Adventure Mod — Design

> Living document: ideas, decisions and open questions.

**Built so far (2026-10-25):** pillars 1–4 and 6 (hearts, metabolism, Mining/Woodcutting/
Digging/Farming skills, bestiary, 50 perks in 7 categories, Adventure screen) and pillar 5
mob unlocks, elites and elite traits. Not built: Combat/Archery/Fishing skills, Well Fed.
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

Implemented: 20 PAM milestones + 18 vanilla advancements with a reward (half hearts unless noted):

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
| Hunter   | Defeat 5 / 15 / 30 different mob types     | ½ / ½ / 1 heart|
| Hunter   | Defeat the Elder Guardian / the Wither     | 2 hearts each  |
| Crafter  | Get a diamond pickaxe                      | ½ heart        |

| Vanilla advancement                         | Reward   |
|---------------------------------------------|----------|
| Sweet Dreams, What a Deal!, Isn't It Iron Pick, Diamonds!, Not Today Thank You, Enchanter, Best Friends Forever, Fishy Business, Local Brewery, A Terrible Fortress, Those Were the Days | ½ heart each |
| We Need to Go Deeper, The End?, Zombie Doctor, Hero of the Village, The City at the End of the Game | 1 heart each |
| Free the End                                | 2 hearts |
| A Balanced Diet                             | stomach  |

Totals: 24½ hearts to earn (start 5, cap 20: 15 to earn, so there are many routes to
the cap) and 6 stomach levels. 20 is our config cap, not a vanilla limit (vanilla
max_health goes to 1024); 20 hearts = two full rows on the HUD.

Milestone ideas not built yet: raise a shield, desert/jungle temple, shipwreck or ocean
ruin, reach Y=0 / lush or deep dark cave, mountain peak, buried treasure, cake or
suspicious stew, defeat an elite, 5 villager professions. More advancements may be
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
| Archery     | Bow/crossbow hits             | Arrow damage, draw speed                   | Idea |
| Farming     | Harvesting grown crops, breeding | Extra harvest chance (see 3b)           | Built (3b) |
| Fishing     | Catching fish                 | Faster bites, better loot                  | Idea |

- No skill XP from blocks the player placed (stops place/break loops).
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
| Replant | Right-click a grown crop to harvest and replant it | Farming 5 (unlocked for testing) | 2 |
| Sturdy Hoe I/II/III | 10/20/30% chance hoes use no durability | Farming 5/15/30 | 1/2/3 |
| Bountiful Harvest I/II | 10%/20% chance of double crop drops | Farming 10/25 | 2/3 |
| Harvester | Breaking or Replanting a grown crop with a hoe also does the grown crops in a 3×3 area | Farming 20 (unlocked for testing) | 3 |
| Rancher | 15% chance of twins when breeding | Farming 10 | 2 |
| Green Thumb | 25% chance bone meal isn't used up | Farming 15 | 2 |

**Farmer milestones:** harvest 3 different crops (stomach), harvest 10 different crops
(½ heart), breed 5 different kinds of animals (½ heart).

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
| Mining      | Ore Sense           | Holding a pickaxe: particle trail points at the nearest ore within 12 blocks, coloured by ore type, bigger when closer | Mining 40 (unlocked for testing) | 4 |
| Woodcutting | Sturdy Axe I/II/III | Same as pickaxe, for axes                           | Woodcutting 5/15/30 | 1/2/3|
| Woodcutting | Timber              | Chopping a log fells the whole tree (sneak = off)  | Woodcutting 15      | 3    |
| Digging     | Sturdy Shovel I/II/III | Same, for shovels                                | Digging 5/15/30     | 1/2/3|
| Digging     | Archaeologist       | 3% chance of a bonus find (flint, clay, bone, ...) from dirt, sand, gravel | Digging 10 | 2 |
| Combat      | Sturdy Weapon I/II  | 10%/20% chance swords use no durability             | 5 / 15 mob types defeated | 1/2 |
| Combat      | Quick Study         | Kills count double towards familiarity              | 5 mob types defeated | 2   |
| Combat      | Second Wind         | Dropping below 3 hearts gives Regeneration II for 5s (2 min cooldown) | — | 2 |
| Defense     | Sturdy Armor I/II/III | 10/20/30% chance armor uses no durability       | 5 / 10 / 15 mob types defeated | 1/2/3 |
| Defense     | Blast Guard I/II    | 15% / 30% less damage from explosions               | 5 / 10 mob types    | 2/3  |
| Defense     | Arrow Guard I/II    | 15% / 30% less damage from projectiles              | 5 / 10 mob types    | 2/3  |
| Defense     | Fire Guard I/II     | 15% / 30% less damage from fire, lava, burning      | 5 / 10 mob types    | 2/3  |
| Defense     | Tough Hide I/II     | 10% / 20% less damage from mob hits (melee)         | 5 / 10 mob types    | 2/3  |
| Survival    | Bonus Heart I/II/III| +1 heart each, on top of milestone hearts (ignores cap) | Rank 5 / 20 / 40 | 3/4/5|
| Survival    | Iron Stomach        | Immune to the Hunger effect (rotten flesh, husks)   | —                   | 1    |
| Survival    | Deep Breath I/II/III | ~30s / ~45s / ~60s of air underwater (vanilla 15s) | — / Rank 10 / Rank 25 | 1/2/3 |
| Survival    | Soft Landing        | Fall 3 more blocks before taking damage             | —                   | 2    |
| Survival    | Fleet Foot I/II/III | +5% / +10% / +15% movement speed                    | Rank 10 / 25 / 40   | 2/3/4|

Implementation notes:
- Durability: one hook where items lose durability; chance by tool type.
- Prospector: re-drops the block's loot (respects Fortune). Skipped when the drop is
  the ore block itself (Silk Touch).
- Timber: breaks connected logs (max 64), each log uses durability and gives skill xp.
  Player-placed logs are never felled, so houses are safe.
- Defense guards: multiply incoming damage before armor (stacks with Protection
  enchantments). Only the first matching type counts: explosion, projectile, fire, then
  hits whose direct source is a mob. Uses vanilla damage type tags, so modded damage works.
- Deep Breath / Soft Landing / Fleet Foot / Bonus Hearts: vanilla attributes.
- Later ideas: Well Fed (diet variety), extra crop yield (Farming skill), faster bow
  draw (Archery), auto-smelt, night vision underground.
- Parked: Ore Sense II (also points at rare ores, or prefers the most valuable ore in range;
  nearby coal/copper currently hide rarer ores further away).

---

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
- **Mobs drop exploration helpers** (user idea, parked): e.g. elites sometimes drop a
  treasure map, explorer map or compass-like hint that leads somewhere new.
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

## Playtest checklist

Built but not confirmed by the user's playtest yet; don't assume these work or feel right:

- **Farming:** XP pace (30 wheat = level 2; Replant normally at level 5 = 100 harvests),
  Replant (one seed replanted, rest dropped), Harvester (3×3 when breaking *and* on
  right-click with Replant), Light Step, sweet berry XP, breeding XP + Rancher twins, Green Thumb.
- **Screen:** taller panel (250px) at the user's GUI scale; perk list scrolling.
- **Ore Sense** (playtested, works, took a moment to understand): does it feel right? Expected
  complaint: coal/copper are so common the trail mostly points at them. Options: ignore
  coal/copper, prefer the most valuable ore in range, or Ore Sense II (parked). Interval (30
  ticks), radius (12) and trail length are constants in `client/OreSense.java`.
- **Deep Breath tiers / Fleet Foot tiers:** owners of the old single `deep_breath` /
  `fleet_foot` perk lost it (point refunded).
- **Defense perks:** reductions noticeable but not overpowered? Is 5/10 mob types the right gate?
- **Mob unlocks (v0.3a, quick `/pam` check OK):** does night 1 feel right (only zombies, fewer
  mobs)? Too empty → raise `lockedMobReplacementChance`. Unlock chat message and the "Next: ..."
  line on the Character tab.
- **Elites (v0.3b):** `/pam elite zombie` etc. Size, name on hover, gear by rank, drops only on
  player kill, XP, bonus loot, Elite Slayer milestone. Does normal-mob gear feel right?
- **Traits (v0.3c):** `/pam elite zombie frost` etc. Does each trait feel fair for kids?

**Temporary test changes** (TODO comments in `perk/Perks.java`): requirements removed so the
user can test without levelling. Replant and Ore Sense are confirmed working; keep Replant
unlocked until Harvester's right-click mode is tested (it needs Replant), then restore all three
and update the perk tables above and the CLAUDE.md status:
- Ore Sense → `new SkillLevel(Skill.MINING, 40)`
- Replant → `new SkillLevel(Skill.FARMING, 5)`
- Harvester → `new SkillLevel(Skill.FARMING, 20)`

## Open questions

- [ ] Perk balance: costs and point budget (playtest).
- [ ] Balance: hearts per milestone (early milestones give hearts quickly), Rank thresholds,
  elite chances, skill XP curves.
- [ ] Tag `v0.2.0`? Offered, no answer yet.

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
- **v0.4:** more skills (Combat, Archery, Fishing) + their perks, diet
  variety, more milestones, exploration extras.
