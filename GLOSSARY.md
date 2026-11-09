# Glossary

The words we use for things, so the game text, the code and the docs say the same thing. Check here before naming
something new; add the term when you build it.

- **In game** is what players read (lang file). Simple English, for kids.
- **In code** is the class, field or id. It may differ from the in-game word (see the notes).
- Minecraft's own words win: when vanilla already has a name for something, use that one and don't invent a synonym.
- **Capitals in game text:** our terms are written lowercase in a sentence ("at a higher rank", "your adventure
  rank"), with only the first letter capital at the start of a sentence or label ("Adventure rank: 12", "Rank 10").
  Names of things you can hold or place stay capitalised (Waypoint Stone), like vanilla items.

## Minecraft terms (vanilla)

Words Minecraft already uses. We use them the same way and never give them another meaning.

| In game | In code | Meaning |
|---|---|---|
| Advancement | `AdvancementHolder`, `advancement/*.json` | Vanilla's goal system (the L screen). Ours are advancements too, in our own tab. Never "achievement" in game text. |
| Advancement tab | root advancement (`root.json`) | One tree in the L screen: Minecraft, Nether, The End, Adventure, Husbandry, and ours. |
| Normal / Goal / Challenge | `AdvancementType` TASK / GOAL / CHALLENGE | The three kinds of advancement (square, rounded, spiky frame). Worth 1 / 2 / 3 rank. Vanilla calls the first a "task" only in code and JSON; players never see that word, so game text says "normal". |
| Criterion | `criteria` | One condition of an advancement. Code-driven ones have one `minecraft:impossible` criterion named `granted`. |
| Toast | `Toast` | The pop-up in the top right (advancement done, new bestiary entry). |
| Hearts, health | `max_health` attribute | Red hearts. 1 heart = 2 health points. "½ heart" in text. |
| Hunger bar, food icons | `FoodData`, food level | The drumsticks. 10 icons = food level 20. Not the same as our stomach. |
| Saturation, exhaustion | `FoodData` | Hidden food values. Not shown to players by name; say "healing" or "hungry" instead. |
| Hunger (effect) | `MobEffects.HUNGER` | The green effect from rotten flesh. Capital H when it means the effect. |
| XP, experience levels | `experienceLevel`, `totalExperience` | The green bar. **Capital "XP" in game text always means this** (waypoint trips cost XP). |
| Mob | `Mob`, `EntityType` | Any creature. |
| Monster, hostile mob | `Monster`, `Targets` | Mobs that attack you. In game text "monster" is fine for kids. |
| Animal, pet | `Animal`, `TamableAnimal` | Passive mobs; a pet is a tamed one (wolf, cat, parrot). |
| Spawn | `NaturalSpawner` | A mob appearing in the world by itself. |
| Patrol, raid | `PatrolSpawner` | Vanilla pillager groups. |
| Attribute, modifier | `Attributes`, `AttributeModifier` | Vanilla stat system. We add named modifiers (hearts, speed, reach). |
| Effect | `MobEffect` | Regeneration, Resistance, Night Vision... |
| Durability | `ItemStack` damage | How worn a tool or armor piece is. |
| Biome, structure, chunk, dimension | same | As vanilla. A chunk is 16×16 blocks. |
| Map, compass, lodestone | `MapItem`, `Items.COMPASS` | Vanilla items. Elite maps are vanilla treasure/explorer maps. |
| Op | permission level | A server admin; our test commands are op-only. |

## Our terms (PAM)

### Progress

| In game | In code | Meaning |
|---|---|---|
| adventure rank, rank | `ProgressionSnapshot.rank`, `Progression` | Sum of all completed advancements: task 1, goal 2, challenge 3. Lowercase in a sentence (see Capitals above). Derived, never stored. Docs for us (DESIGN.md, CLAUDE.md) may write Rank. |
| Perk points | `Perks.pointsAvailable` | Rank minus the cost of the perks you own. "N perk points", not "skill points". |
| Milestone | `Milestones` entry with a reward | An advancement that gives hearts or a stomach upgrade. Ours (our tab) or vanilla ones in `Milestones.VANILLA`. Its description ends in "Reward: ...". |
| Reward | lang "Reward: ..." line | What a milestone gives: "+½ heart", "+1 heart", "stomach upgrade". |
| Stomach, stomach level, stomach upgrade | `ProgressionSnapshot.stomach` | Our hunger progression: a higher stomach level means you get hungry more slowly ("hunger drain %"). Not the hunger bar. |
| Bonus heart | Bonus Heart perk | A heart from a perk, on top of milestone hearts (ignores the heart cap). |
| Heart cap | config | The most hearts milestones can give. |
| Adventure screen | `AdventureScreen` | The K screen, with the tabs Character, Perks, Bestiary, Achievements. |
| Advancements tab | `AchievementsTab`, `Achievements` | Our list of every advancement with progress and tips. The code (and older docs) still say "achievements"; game text says advancements. Our own tab's heading there is "Progressive Adventure". |
| Tip, free tip | `BuyTipPayload`, lang `...tip.<id>.<n>` | A hint for an advancement, bought with XP (free once it's done). The Free tip button shows a tip of the day. |

### Skills

| In game | In code | Meaning |
|---|---|---|
| Skill | `Skill` enum | Mining, Woodcutting, Digging, Farming, Melee, Archery, Fishing. Gets better by doing. |
| Level | skill level | A skill's level, e.g. "Mining level 12". |
| xp | skill xp | Skill experience. **Lowercase "xp" in game text** to tell it apart from vanilla XP. |
| Bonus | skill bonus curve | What a skill level gives (break speed, draw speed, bite speed), shown as "+35%". |

### Perks

| In game | In code | Meaning |
|---|---|---|
| Perk | `Perk`, `Perks` | A choosable upgrade bought with perk points. |
| Tier (I, II, III) | `_1`, `_2`, `_3` id suffix | Stronger versions of one perk. Roman numerals in game, a higher tier replaces the lower one. |
| Category | `PerkCategory` | A tile in the Perks tab: one per skill, plus Defense, Health, Survival, Movement, Exploration. |
| Group | perk `group` | A heading inside a category (Melee's weapons, Defense's Shield). |
| Requirement | `Requirement` | What you need first: a Rank, a skill level or a number of mob types. |
| Prerequisite | `prerequisite` | The perk you must own first (usually the tier below). |
| Owned, buyable, too expensive, locked | `Perks.Status` | The states of a perk row. |
| Cooldown | `Cooldown` | Time before a perk works again (Second Wind, Shield Bash). |
| Sense (Ore / Treasure / Relic Sense) | `BlockSense` | Perks that show a sparkle trail to nearby blocks. |

### Bestiary and mobs

| In game | In code | Meaning |
|---|---|---|
| Bestiary | `Bestiary`, `BestiaryTab` | The book of mobs you defeated. |
| Discovered | first kill | A mob you've defeated at least once; it gets a page and a toast. |
| Familiarity | `FamiliarityTier` | How well you know a mob, from kills: Unknown, Unfamiliar, Known, Familiar, Expert, Master. Gives a damage bonus against it. |
| Kills | `PlayerProgress` kills | Mobs you defeated, per type. In descriptions say "defeat". "kill" is fine in short labels. |
| Assist kill | `assistKills` | A kill that counts for everyone who hit the mob, not only the last hit. Not shown by name in game. |
| Mob types | kill count per `EntityType` | How many different mobs you defeated: "Defeat 5 different mob types". Not "kinds of mobs". |
| Collection | `Bestiary.Collection` | A set of mobs (Night Watch, Ocean Deep, Raiders...); defeating all gives an advancement. |
| Mob unlock, "appears from Rank N" | `MobUnlocks` | A hostile mob only spawns naturally once the lowest Rank nearby is high enough. Chat: "New danger". |
| Elite | `Elites`, `ELITE_RANK` | A rare stronger mob with better gear, XP and loot, named "Elite Zombie". Not "Tough". |
| Trait | `EliteTrait` | An extra power on an elite (shown in its name instead of "Elite"). |
| Gear | `Elites` | Extra armor and weapons. Normal mobs can get some too; elites always have it. |
| Elite map | `EliteMap` | A treasure or explorer map that elites can drop. |

### Waypoints

| In game | In code | Meaning |
|---|---|---|
| Waypoint Stone | `WaypointStoneBlock` | Block you turn on and travel between. Always both words, capitalised. |
| Waypoint Shard | `WaypointContent` | Dropped by elites, used to turn on a stone. |
| Turn on (a stone) | ACTIVE state | Not "activate" in game text. |
| Owner | `Waypoint.owner` | The player who turned the stone on. |
| Trip | `WaypointTeleportPayload` | Travelling from one stone to another. Not "teleport" in game text. |
| Charge-up | waypoint charge | The 2 seconds before a trip; getting hurt cancels it. |

### Planned: Atlas (not built)

| In game | In code | Meaning |
|---|---|---|
| Atlas | - | The map item that draws itself as you walk. |
| Ink | - | What drawing costs; refilled with coal, ink sacs or glow ink sacs. |
| Drawn chunk | - | A chunk the Atlas has drawn; only new chunks cost ink. |
| Marker | - | A named icon on the Atlas, placed by you or automatically (bed, Waypoint Stones). |

## Open questions

Places where we don't use one word consistently yet. Decide, fix, then remove the line. None right now.
