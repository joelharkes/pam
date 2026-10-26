# Changelog

## Unreleased

- **Bestiary pages:** click a mob in the Bestiary tab to see where to find it, its 3D model and, as you
  get to know it, its stats and a tip (Known), its drops (Familiar) and exact drop chances (Expert).
  Master gives a 50% chance of double loot from that mob.
- **Bestiary collections:** 7 new advancements in the Adventure tab (Night Watch, Deep Sea Hunter, Raid
  Breaker, Nether Hunter, End Hunter, Master Hunter, Trait Collector). They give Adventure Rank, no hearts.
- **Elite records:** a mob's page shows how many Tough ones you defeated and which traits you've beaten.
- **Assist kills:** everyone who hurt a mob during the fight gets the bestiary kill, not only the last hit.
- A new bestiary entry now shows a toast instead of a message above the hotbar.
- New settings: `masterDoubleLootChance`, `assistKills`. Ops: `/pam kills <mob> <count> [player]`.

## 0.4.0 (Alpha): first public release

First version on Modrinth. Everything below is new.

- **Hearts from milestones:** start with 5 hearts, earn up to 20 from 42 milestones
  (24 in PAM's own Adventure tab, 18 vanilla advancements with a reward).
- **Stomach:** hunger drains faster early on; eating new foods and growing crops levels up your stomach.
- **Skills:** Mining, Woodcutting, Digging, Farming and Archery level up as you use them.
- **Adventure Rank and perks:** every advancement gives Rank, every Rank is a perk point.
  87 perks in 10 categories (Mining, Woodcutting, Digging, Farming, Archery, Combat, Defense,
  Survival, Movement, Swimming). Perks can be reset at any time.
- **Adventure screen** (press K): Character, Perks and Bestiary tabs.
- **Bestiary:** deal more damage to mob types you've beaten often.
- **Mob unlocks:** new hostile mob types appear as your Rank goes up (on servers: the lowest Rank
  of nearby players).
- **Mob gear and elites:** mobs get better gear with Rank; rare elite mobs with traits and better loot.
- **Waypoints:** elites drop Waypoint Shards; craft and name Waypoint Stones and travel between them for XP.
- Settings can be changed in `config/progressive_adventure.json`; it only holds what you change, so updates
  that rebalance PAM reach you automatically (see the README).
