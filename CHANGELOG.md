# Changelog

## 0.6.0: Achievements, tips and elite maps

### New

- **Achievements tab:** see every achievement in one grid, with a filter for the ones you haven't done yet. Click
  one to see what it needs.
- **Achievement tips:** stuck? Buy a tip with XP (10, 30 and 75 XP points, not levels). Every achievement has
  tips, and tips for achievements you've done are free. There's also a free tip of the day.
- **Elite maps:** about 1 in 10 elites drops a treasure or explorer map. Buried treasure comes first, then trial
  chambers, ocean monuments and, late in the game, woodland mansions.
- **Shockwave II and III** (Melee): your mace smash hurts the mobs around you for 65% or even 100% of a mace hit.
- **Quick Healing I/II** (Survival): with a full stomach you heal half a heart every 3 or 2 seconds, instead of
  every 4.
- **Deep Reserves** (Survival): you keep healing until your hunger bar drops below 4 food icons, instead of 9.
- **Steady Healing I/II** (Survival): healing your hearts makes you 25% or 50% less hungry.

### Changes

- **Skeletons and creepers come later:** skeletons now arrive at Rank 10 (was 5) and creepers at Rank 18 (was 12).
- **Bonus Hearts come earlier:** Bonus Heart I/II/III are now at Rank 5/10/20 (was 5/20/40).
- **Perks unlock earlier:** the first Sturdy perk for each tool and weapon needs no skill level now, and many small
  perks need a lower one. No perk got harder to get.
- **Shockwave I** is a bit stronger: it now counts the whole mace hit, not only the extra damage from the fall.
- **Adventure screen:** clicking a tab button takes you back to the start of that tab. Closing and reopening the
  screen keeps your place.

### For server admins

- The setting `shockwaveDamageRatio` is now the list `shockwaveDamageRatios`. An old override of it no longer works,
  and the mod warns about it at startup.
- If you overrode `mobUnlocks`, you keep your own ranks.

## 0.5.0: Melee, Fishing and a bigger Bestiary

### New

- **Melee skill:** hit mobs with a sword, axe, mace, spear or trident to level it up and attack faster. The
  Combat perk category is now called **Melee**, with the perks grouped by weapon:
  - Sword: **Careful Sweep** (your sweep no longer hurts your pets, villagers or golems), **Wide Sweep I/II** (your sweep hurts the other mobs nearby more), **Whirlwind** (sneak while you
    sweep to hit every hostile mob around you), **Disarm** (knock the weapon out of a mob's hand).
  - Axe: **Heavy Swing** (big knockback) and **Sunder** (cracks the mob's armor for a few seconds).
  - Mace: **Ground Pound** (sneak in the air to slam down) and **Shockwave** (your smash also hurts the mobs
    around you).
  - Spear: **Running Charge** (stronger charge on foot) and **Pinning Jab** (your jab slows mobs).
- **Shield perks** (Defense): **Shield Bash** (attack with your shield up to push mobs away), **Parry** (raise
  your shield just before a hit and the attacker gets hurt instead) and **Deflect** (arrows fly back at the
  shooter).
- **Hearty Kill I/II/III** (Survival): defeating a hostile mob heals ½, 1 or 1½ hearts.
- **Fishing skill:** catch fish to level it up and get faster bites. New perks: **Keen Eye** (a bell rings when
  a fish bites, more time to reel in), **Lucky** (more treasure), **Double Catch** and **Treasure Hunter**
  (treasure even in small ponds).
- **2 new milestones** in the Adventure tab: Sunken Treasure (fish up a treasure) and Fish Collector (catch all
  4 kinds of fish), ½ heart each.
- **Tridents** count for Archery when you throw them: they give Archery XP and charge faster as your Archery
  level goes up. New Archery perk **Loyal Trident**: thrown tridents always come back to you.
- **Bestiary pages:** click a mob in the Bestiary tab to see where to find it, its 3D model and, as you get to
  know it, its stats and a tip (Known), its drops (Familiar) and exact drop chances (Expert). Master gives a
  50% chance of double loot from that mob.
- **Bestiary collections:** 7 new advancements in the Adventure tab (Night Watch, Deep Sea Hunter, Raid
  Breaker, Nether Hunter, End Hunter, Master Hunter, Trait Collector). They give Adventure Rank, no hearts.
- **Elite records:** a mob's page shows how many Tough ones you defeated and which traits you've beaten.
- **Assist kills:** everyone who hurt a mob during the fight gets the bestiary kill, not only the last hit.
- **Character tab:** skills are shown in two columns to make room for new skills. Hover a skill to see how much
  XP you need for the next level and which perk it unlocks next.

### Changes

- **Familiarity is slower and gentler:** you become Expert at 100 kills and Master at 200 (was 60 and 100),
  and the damage bonus grows to +30% at Master and +35% at 500 kills (was +50% and +55%). Your kills are
  kept, so you may drop back a tier.
- **Swimming perks moved:** Deep Breath and Diver are now under Survival, Swimmer under Movement. **Second
  Wind** moved to Survival. Perks you own stay yours.
- **Sturdy Weapon** (was Sturdy Blade) now also works for spears, maces and tridents.
- **Double Jump** no longer gives a big forward boost when you sprint: the second jump only adds height.
- A new bestiary entry now shows a toast instead of a message above the hotbar.
- Long perk descriptions wrap instead of running off the screen.
- New settings: `masterDoubleLootChance`, `assistKills`, `fishingMaxBiteBonus`, `fishingFishXp`,
  `fishingJunkXp`, `fishingTreasureXp`, `keenEyeExtraTicks`, `luckyLuck`, `doubleCatchChance`.
  New op command: `/pam kills <mob> <count> [player]`.

### Fixes

- The Bestiary tab no longer stays on a mob's page when you switch to another world.

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
