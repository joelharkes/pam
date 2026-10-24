# PAM: Progressive Adventure Mod — Design

> Living document: ideas, decisions and open questions.

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

## Core idea: one progression track

Everything hangs off **milestones**:

```
complete milestones ──► earn hearts / stomach upgrades
          │
          └──► milestone count = Adventure Rank ──► unlocks new mob types,
                                                    rare elite mobs, better loot
```

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

Draft list (★ = good early milestone for a first session):

| Category | Milestone                                     | Reward     |
|----------|-----------------------------------------------|------------|
| Survivor | ★ Survive your first night                    | ½ heart    |
| Survivor | ★ Sleep in a bed                              | ½ heart    |
| Survivor | Wear a full set of iron armor                 | ½ heart    |
| Survivor | Raise your shield to block an attack          | ½ heart    |
| Explorer | ★ Visit 5 different biomes                    | ½ heart    |
| Explorer | Visit 15 different biomes                     | 1 heart    |
| Explorer | Visit 30 different biomes                     | 1 heart    |
| Explorer | ★ Find a village                              | ½ heart    |
| Explorer | Find a desert or jungle temple                | ½ heart    |
| Explorer | Find a shipwreck or ocean ruin                | ½ heart    |
| Explorer | Go deep: reach Y=0 / find a lush or deep dark cave | ½ heart |
| Explorer | Climb a mountain peak                         | ½ heart    |
| Explorer | Use a map to find buried treasure             | 1 heart    |
| Explorer | Enter the Nether                              | 1 heart    |
| Explorer | Find a Nether fortress / bastion              | ½ heart    |
| Explorer | Enter the End                                 | 1 heart    |
| Explorer | Find an End city                              | 1 heart    |
| Gourmet  | ★ Eat 5 different foods                       | stomach    |
| Gourmet  | Eat 15 different foods                        | stomach    |
| Gourmet  | Eat 25 different foods                        | 1 heart    |
| Gourmet  | Bake a cake / cook a suspicious stew          | stomach    |
| Farmer   | ★ Harvest 3 different crops                   | stomach    |
| Farmer   | Breed 5 different kinds of animals            | ½ heart    |
| Farmer   | Tame a wolf, cat or horse                     | ½ heart    |
| Hunter   | ★ Defeat 5 different mob types                | ½ heart    |
| Hunter   | Defeat 15 different mob types                 | 1 heart    |
| Hunter   | Defeat 30 different mob types                 | 1 heart    |
| Hunter   | Defeat an elite mob (see pillar 5)            | ½ heart    |
| Hunter   | Defeat the Elder Guardian                     | 2 hearts   |
| Hunter   | Defeat the Wither                             | 2 hearts   |
| Hunter   | Defeat the Ender Dragon                       | 2 hearts   |
| Crafter  | ★ Craft iron tools                            | ½ heart    |
| Crafter  | Craft diamond tools                           | ½ heart    |
| Crafter  | Enchant an item                               | ½ heart    |
| Crafter  | Brew a potion                                 | ½ heart    |
| Social   | Trade with a villager                         | ½ heart    |
| Social   | Trade with 5 different villager professions   | ½ heart    |
| Angler   | Catch a fish / catch a treasure item          | ½ heart    |

Numbers still need balancing. Roughly: ~10 hearts to earn in total, so finishing most
milestones takes you from 5 to ~15+ hearts.

### 2. Metabolism: hunger annoying early, easy later

Keep the vanilla hunger bar size (20). Changing it is technically hard and breaks
regeneration rules and other mods. Instead, change **how hungry you get**:

- **Early game:** hunger drains faster (e.g. +50% exhaustion) and food gives less
  saturation. You have to think about food.
- **Stomach upgrades** (from Gourmet/Farmer milestones) step this down, ending
  *better* than vanilla: slower drain, more saturation, cheaper sprinting.
- **Diet variety bonus (idea):** eating a varied diet gives a temporary "Well Fed"
  buff (slower hunger drain, small regen). Eating the same food over and over gives
  less. This teaches kids to farm several crops and fits the Gourmet milestones.
- Feasibility: **medium.** Small mixin on exhaustion/food values. No HUD changes
  needed.
- Optional later: show the stomach level as a small icon next to the hunger bar.

### 3. Proficiency: get better by doing

Use-based skills that level up with use. Gains follow a **diminishing curve**: the
first levels come quickly and feel great, later levels are slow and small.

| Skill       | Trained by                    | Bonus                                      | Feasibility |
|-------------|-------------------------------|--------------------------------------------|-------------|
| Mining      | Pickaxe blocks                | Break speed (+ perk: chance of extra ore)  | Easy (attribute) |
| Woodcutting | Axe blocks                    | Break speed (+ perk: fell small trees)     | Easy        |
| Digging     | Shovel blocks                 | Break speed                                | Easy        |
| Combat      | Melee damage dealt            | Attack speed/damage                        | Easy        |
| Archery     | Bow/crossbow hits             | Arrow damage, draw speed                   | Medium      |
| Farming     | Harvesting grown crops        | Extra yield                                | Medium      |
| Fishing     | Catching fish                 | Faster bites, better loot                  | Medium      |

- No skill XP from blocks the player placed (stops place/break loops).
- **Perks** at certain levels are the exciting part. Percentages alone feel flat.

### 4. Bestiary: mob familiarity

Each mob type you defeat builds familiarity, which gives bonus damage against that
type. Gains are big early and flatten out:

| Kills | Damage bonus | Extra (ideas)                  |
|-------|--------------|--------------------------------|
| 1     | +5%          | Mob appears in your bestiary   |
| 5     | +10%         | See its health bar             |
| 15    | +20%         |                                |
| 30    | +30%         | See its drops in the bestiary  |
| 60    | +40%         |                                |
| 100   | +50%         | "Mastered" badge               |
| 250   | +55%         | (cap)                          |

- At 100 kills a zombie clearly goes down faster than in vanilla (goal achieved).
- Kills by AFK farms count for less or not at all.
- We keep our own kill counter (not vanilla `Stats.KILLED`) so old worlds don't get
  retroactive progress.

### 5. World progression: mob unlocks and elite mobs

#### Mob unlocks by Rank

- **Rank 0:** only zombies, skeletons, creepers (and passive mobs).
- Higher ranks unlock more: spiders → endermen/witches → phantoms, etc.
  (Exact order still to decide.)
- **Structure mobs always spawn** (blazes in fortresses, guardians in monuments,
  spawners, pillager outposts...). You chose to go there.
- Mobs from other mods are allowed by default.

#### Elite mobs

- Most mobs stay normal, so most of the time you feel strong.
- Rarely, a mob spawns as an **elite**: armor, an enchanted weapon, a bit more health.
- Elites are **easy to recognise** (name tag like "Armored Zombie", subtle particles)
  so it's exciting, not unfair.
- **Rewards:** much more XP and a high chance to drop their armor/weapon
  (vanilla is only ~8.5%).
- **Chance scales with Rank but stays rare and is capped** (e.g. 0% at Rank 0–1,
  growing to max ~5–8% of spawns).
- Later: elite tiers (bronze/silver/gold) with better loot at higher ranks.

#### Multiplayer

Spawns are decided by nearby players: when a mob spawns, use the rank of the
**lowest-ranked player nearby**. When a kid plays near a parent, the world stays
kid-friendly around them.

Feasibility: **medium.** Hook into natural spawning to cancel locked mobs or upgrade
mobs to elites.

---

## UI

Keep it simple and close to vanilla:

- **Milestones:** our own "Adventure" tab in the vanilla advancement screen.
  No custom UI needed.
- **Skills & Bestiary:** one custom screen with two tabs, opened by a keybind (and
  maybe a small button in the inventory). Styled like the advancement window
  (same frame/background), so it feels vanilla.
  - *Skills tab:* each skill with level, progress bar, current bonus, next perk.
  - *Bestiary tab:* grid of mob icons. Undiscovered mobs show as "?". Click for
    kills, damage bonus and (later) drops.
- **Toasts** for level-ups, new bestiary entries and Rank-ups.
- Show Adventure Rank and hearts progress on the Skills tab.

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
- 2026-10-24: Structure mobs always spawn.
- 2026-10-24: UI: milestones in the advancement screen; skills + bestiary in one
  tabbed screen.
- 2026-10-24: Target Minecraft 26.2 on Fabric.

## Open questions

- [ ] Final name: "PAM: Progressive Adventure Mod". Possible clash with the well-known
      "Pam's HarvestCraft" mods.
- [ ] Mob unlock order per Rank.
- [ ] Balance: hearts per milestone, Rank thresholds, elite chances.

## Roadmap (draft, still brainstorming)

- **v0.1 (MVP):** setup, 5 starting hearts, first ~10 milestones granting hearts,
  Mining skill, Bestiary damage bonus, config.
- **v0.2:** metabolism + stomach upgrades, Rank-based mob spawning.
- **v0.3:** elite mobs, skills/bestiary screen.
- **v0.4:** more skills + perks, diet variety, exploration extras.
