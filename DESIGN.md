# PAM: Progressive Adventure Mod — Design

> Living document: ideas, decisions and open questions.

**Built so far (2026-10-24):** pillars 1–4 and 6 (hearts, metabolism, Mining/Woodcutting/
Digging/Farming skills, bestiary, 50 perks in 7 categories, Adventure screen). Not built:
pillar 5 (mob unlocks, elites), Combat/Archery/Fishing skills, Well Fed. See the roadmap.

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

## Core idea: two progression tracks

```
GUARANTEED TRACK (survival, automatic)
  PAM milestones ──► hearts + stomach upgrades
        │
        └──► milestone count = Adventure Rank ──► mob unlocks, elites, loot

CHOOSABLE TRACK (play style, player picks)
  ANY advancement (vanilla + PAM) ──► perk points ──► spend on perks
  skill levels / bestiary ─────────► unlock which perks you may buy
```

- **Guaranteed track:** survival stats (hearts, hunger) always grow, so the world
  (driven by Rank) can never outgrow the player. Works without opening any menu.
- **Choosable track:** extras on top. Two players in the same world can grow
  differently. See pillar 6.

- Easy to understand: "I need 3 more milestones for Rank 2."
- Kids can see exactly what to do next.
- One number (Rank) drives the world's difficulty, so it's simple to balance.
- Because danger grows **only** by completing milestones, and each milestone also
  makes you stronger (hearts), the player always gets a power boost alongside the
  harder world.

### Milestones are vanilla advancements

Milestones are **custom advancements** in their own "Adventure" tab:

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

Implemented (28 milestones, "Adventure" advancement tab). Rewards are in half hearts:

| Category | Milestone                                  | Reward         |
|----------|--------------------------------------------|----------------|
| Survivor | Survive your first night                   | ½ heart        |
| Survivor | Sleep in a bed                             | ½ heart        |
| Survivor | Wear a full set of iron armor              | ½ heart        |
| Gourmet  | Eat your first food / 3 / 8 / 15 foods     | stomach each   |
| Gourmet  | Eat 25 different foods                     | 1 heart        |
| Farmer   | Harvest 3 different crops                  | stomach        |
| Farmer   | Harvest 10 different crops                 | ½ heart        |
| Farmer   | Breed 5 different kinds of animals         | ½ heart        |
| Explorer | Visit 5 / 15 / 30 biomes                   | ½ / ½ / 1 heart|
| Explorer | Find a village                             | ½ heart        |
| Explorer | Enter the Nether / the End                 | 1 heart each   |
| Hunter   | Defeat 5 / 15 / 30 different mob types     | ½ / ½ / 1 heart|
| Hunter   | Defeat the Elder Guardian / Wither / Ender Dragon | 2 hearts each |
| Crafter  | Craft iron tools / diamond tools           | ½ heart each   |
| Crafter  | Enchant an item / brew a potion            | ½ heart each   |
| Social   | Trade with a villager                      | ½ heart        |

Totals: 18½ hearts to earn (start 5, cap 20, so the cap is reached before everything
is done) and 5 stomach levels. Boss hearts are 6 of those 18½.

Milestone ideas not built yet: raise a shield, desert/jungle temple, shipwreck or ocean
ruin, reach Y=0 / lush or deep dark cave, mountain peak, buried treasure, fortress /
bastion, End city, cake or suspicious stew, tame a wolf/cat/horse, defeat an elite,
5 villager professions, catch a fish / treasure.

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
| 6             | ×0.7         | ×1.3            | (no milestone yet)       |
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

#### Mob unlocks by Rank

Overworld hostile mobs unlock one at a time, and each type gets elites a couple of
ranks after it first appears. Hearts assume ~0.6 heart per milestone on average.

| Rank | ~Hearts | New mobs                                  | Elites start            |
|------|---------|-------------------------------------------|-------------------------|
| 0    | 5       | Zombies (incl. husks, zombie villagers)   |                         |
| 2    | 6       | Skeletons (incl. strays)                  | Elite zombies           |
| 5    | 8       | Creepers                                  | Elite skeletons         |
| 7    | 9       | Spiders, drowned                          |                         |
| 9    | 10      | Endermen                                  | Elite spiders           |
| 11   | 11      | Slimes                                    | Elite drowned           |
| 13   | 12      | Witches, pillager patrols                 |                         |
| 15   | 14      | (phantoms already follow vanilla sleep rules) | Elite endermen?     |

- Biome variants follow their parent (husk = zombie, stray = skeleton, bogged =
  skeleton).
- **Creepers and health:** a point-blank creeper can kill even a vanilla 10-heart
  player without armor. Rank 5 (~8 hearts) plus a bit of armor should survive one.
  Options if playtests show otherwise: require the "iron armor" milestone before
  creepers unlock, or soften creeper damage for low-rank players.
- **Structure mobs always spawn** (blazes in fortresses, guardians in monuments,
  spawners, pillager outposts, cave spiders, silverfish...). You chose to go there.
- **Nether and End** (proposal): vanilla spawns. Entering them is your own choice and
  already a milestone.
- Mobs from other mods are allowed by default.

#### Elite mobs

- Most mobs stay normal, so most of the time you feel strong.
- Occasionally a mob spawns as an **elite**: armor, an enchanted weapon, a bit more
  health.
- Elites are **easy to recognise** (name tag like "Armored Zombie", subtle particles)
  so it's exciting, not unfair.
- **Rewards:** much more XP and a high chance to drop their armor/weapon
  (vanilla is only ~8.5%).
- **Per mob type, the chance builds up with Rank:** starts at ~1% when that type's
  elites unlock, +0.5% per rank, capped at ~8%.
- **Elites get better too:** early elite zombies have leather/chain armor and a
  plain sword; at higher ranks they get iron, then diamond and enchantments. Better
  elites = better drops.
- Creepers have no elite version (an "elite creeper" just means a bigger surprise
  explosion, which isn't fun).

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
- **Free respec:** a "Reset perks" button refunds everything (with a confirm). No
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
| Survival    | Bonus Heart I/II/III| +1 heart each, on top of milestone hearts (ignores cap) | Rank 3 / 8 / 15 | 3/4/5|
| Survival    | Iron Stomach        | Immune to the Hunger effect (rotten flesh, husks)   | —                   | 1    |
| Survival    | Deep Breath I/II/III | ~30s / ~45s / ~60s of air underwater (vanilla 15s) | — / Rank 5 / Rank 10 | 1/2/3 |
| Survival    | Soft Landing        | Fall 3 more blocks before taking damage             | —                   | 2    |
| Survival    | Fleet Foot I/II/III | +5% / +10% / +15% movement speed                    | Rank 5 / 10 / 15    | 2/3/4|

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
- **Treasure maps from elites:** elites sometimes drop a map to buried loot.
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
- 2026-10-24: Adventure Rank (milestone count) gates mob unlocks and elite chance.
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
- 2026-10-24: UI declutter: perk rows show only an icon (✔ / 🔒) or the cost, details
  and blockers live in the single row tooltip. Next-perk line removed from skills, rank
  and stomach details moved to tooltips, perk reset without confirmation.

## Open questions

- [ ] Perk balance: costs and point budget (playtest).
- [ ] Balance: hearts per milestone, Rank thresholds, elite chances.
- [ ] Double advancements (parked by user): some PAM milestones match a vanilla advancement
  (same trigger), so two toasts pop at once. Fix later (e.g. drop the overlap, hide one toast,
  or make the PAM milestone require something different).

## Roadmap

- **v0.1 (tag `v0.1.0`, first test version):** 5 starting hearts, 25 milestones
  granting hearts/stomach, metabolism, Mining/Woodcutting/Digging skills, bestiary
  damage modifier, config, `/pam` commands. Playtested: works well.
- **v0.2:** perk points + perk list above, Adventure screen (character, perks,
  bestiary tabs), keybind. Playtested: works (not tagged yet).
- **v0.2.x:** Farming skill, farming perks, Farmer milestones, Defense perks, Fleet Foot tiers.
  *(Implemented, needs playtesting.)*
- **v0.3:** Rank-based mob unlocking, elite mobs (postponed by user for now).
- **v0.4:** more skills (Combat, Archery, Fishing) + their perks, diet
  variety, more milestones, exploration extras.
