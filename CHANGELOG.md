# Changelog

## Unreleased

- **Bestiary pages:** click a mob in the Bestiary tab to see where to find it, its 3D model and, as you
  get to know it, its stats and a tip (Known), its drops (Familiar) and exact drop chances (Expert).
  Master gives a 50% chance of double loot from that mob.
- **Bestiary collections:** 7 new advancements in the Adventure tab (Night Watch, Deep Sea Hunter, Raid
  Breaker, Nether Hunter, End Hunter, Master Hunter, Trait Collector). They give Adventure Rank, no hearts.
- **Elite records:** a mob's page shows how many Tough ones you defeated and which traits you've beaten.
- **Assist kills:** everyone who hurt a mob during the fight gets the bestiary kill, not only the last hit.
- **Familiarity is slower and gentler:** you become Expert at 100 kills and Master at 200 (was 60 and 100),
  and the damage bonus grows to +30% at Master and +35% at 500 kills (was +50% and +55%). Your kills are
  kept, so you may drop back a tier.
- A new bestiary entry now shows a toast instead of a message above the hotbar.
- **Fishing skill:** catch fish to level it up and get faster bites. New fishing perks: Keen Eye (a bell rings
  when a fish bites, more time to reel in), Lucky (more treasure), Double Catch and Treasure Hunter (treasure
  even in small ponds).
- **2 new milestones** in the Adventure tab: Sunken Treasure (fish up a treasure) and Fish Collector (catch all
  4 kinds of fish), ½ heart each.
- **Melee skill:** hit mobs with a sword, axe, mace, spear or trident to level it up and attack faster. The Combat
  perk category is now called Melee, with the perks grouped by weapon. New: **Wide Sweep I/II** (your sword sweep
  hurts the other mobs nearby more), **Whirlwind** (sneak while you sweep to hit every hostile mob around you),
  **Disarm** (knock the weapon out of a mob's hand), and for axes **Heavy Swing** (big knockback) and **Sunder**
  (cracks the mob's armor for a few seconds).
- **Shield perks** (Defense): **Shield Bash** (attack with your shield up to push mobs away), **Parry** (raise your
  shield just before a hit and the attacker gets hurt instead) and **Deflect** (arrows fly back at the shooter).
- **Careful Sweep** (Melee): your sword sweep no longer hurts your pets, villagers or golems.
- **Hearty Kill I/II/III** (Survival): defeating a hostile mob heals ½, 1 or 1½ hearts. **Second Wind** moved
  to Survival (you keep it if you have it).
- **Tridents** now count for Archery when you throw them: they give Archery XP and charge faster as your Archery
  level goes up. New Archery perk **Loyal Trident**: thrown tridents always come back to you.
- **Character tab:** skills are shown in two columns to make room for new skills. Hover a skill to see how much
  xp you need for the next level and which perk it unlocks next.
- **Swimming perks moved:** Deep Breath and Diver are now under Survival, Swimmer under Movement. Perks you
  own stay yours.
- New settings: `masterDoubleLootChance`, `assistKills`, `fishingMaxBiteBonus`, `fishingFishXp`, `fishingJunkXp`,
  `fishingTreasureXp`, `keenEyeExtraTicks`, `luckyLuck`, `doubleCatchChance`. Ops: `/pam kills <mob> <count> [player]`.

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
