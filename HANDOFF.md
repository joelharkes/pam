# Handoff

State of the project as of 2026-10-24 (last commit `b0263dc`), for the next agent picking this up.

**Read first:** [CLAUDE.md](CLAUDE.md) (stack, commands, code layout, conventions, how to add
milestones/perks) and [DESIGN.md](DESIGN.md) (full feature design, decisions log, roadmap).
This file only covers what those don't: where we are, what's pending, and how the user likes to work.

## Where we are

| Version | Content | Status |
|---|---|---|
| v0.1 (tag `v0.1.0`) | 5 starting hearts, milestones → hearts/stomach, metabolism, Mining/Woodcutting/Digging, bestiary damage modifier, `/pam` | Playtested, works well |
| v0.2 | Perk points from all advancements, 24 perks, Adventure screen (K: Character/Perks/Bestiary), free respec | Playtested: GUI, perks and reset work. **Not tagged yet** (tagging `v0.2.0` was offered, no answer) |
| v0.2.x | Farming skill + 10 farming perks + 3 Farmer milestones, scrollable perk list | Built, server smoke-tested, **not playtested** |
| v0.2.x | Ore Sense perk (client-side particle trail to nearest ore) | Playtested: works (took a moment to understand) |
| v0.2.x | Deep Breath split into tiers I/II/III (~30/45/60s air) | Built, **not playtested** |
| v0.3a | Mob unlocks by Adventure Rank (skeletons 5, creepers 12, ... all at 50), "Next" line on Character tab | Quick check OK (`/pam`), not tested in depth |
| v0.3b | Normal mobs wear more gear with Rank, rare elites ("Tough Zombie"), elite XP/loot, Elite Slayer milestone, `/pam elite <mob>` | Built, server smoke-tested, **not playtested** |
| v0.2.x | Defense category (Sturdy Armor, Blast/Arrow/Fire Guard, Tough Hide), Fleet Foot I/II/III | Built, server smoke-tested, **not playtested** |

The working tree is clean; everything is committed on `main`.

## Waiting on the user's playtest

The user said they'll report back; don't assume these work:
- **Farming:** XP pace (30 wheat = level 2; Replant normally at level 5 = 100 harvests),
  Replant (one seed replanted, rest dropped), Harvester (3×3 when breaking *and* right-click
  with Replant), Light Step, sweet berry XP, breeding XP + Rancher twins, Green Thumb.
- **Screen:** taller panel (250px) at the user's GUI scale; perk list scrolling.
- **Replant and Ore Sense: confirmed working.** Keep Replant unlocked until Harvester's right-click
  mode is tested (it needs Replant), then restore all three requirements.
- **Ore Sense:** does it feel right? Expected complaint: coal/copper are so common the trail
  mostly points at them. Options already offered: ignore coal/copper, prefer the most valuable
  ore in range, or add a tier II for rare ores. Interval (30 ticks), radius (12) and trail length
  are constants in `client/OreSense.java`.
- **Elites:** `/pam elite zombie` etc. Size, name on hover, gear by rank, drops only on player
  kill, XP, bonus loot, Elite Slayer milestone. Does normal-mob gear feel right?
- **Mob unlocks:** does night 1 feel right (only zombies, fewer mobs)? Too empty → raise
  `lockedMobReplacementChance`. Unlock chat message and the "Next: ..." line on the Character tab.
- **Defense perks:** do the reductions feel noticeable but not overpowered? Is 5/10 mob types
  the right gate? Fleet Foot: old `fleet_foot` owners lost it (point refunded).
- **Deep Breath tiers:** players who owned the old `deep_breath` perk lost it (point refunded).

## Temporary test changes to revert

Requirements removed so the user can test without levelling (TODO comments in `perk/Perks.java`):
- Ore Sense → `new SkillLevel(Skill.MINING, 40)`
- Replant → `new SkillLevel(Skill.FARMING, 5)`
- Harvester → `new SkillLevel(Skill.FARMING, 20)`

Restore them when the user confirms the perks work. Also update the DESIGN.md perk tables
("unlocked for testing") and the CLAUDE.md status lines.

## Open questions / next steps

1. **Elite traits (v0.3c)**: after the user playtests v0.3b elites. Design in DESIGN.md pillar 5.
   Later (parked user idea): mobs dropping things that help exploring (maps etc.).
2. **Balance** (needs playtesting): early milestones give hearts quickly; perk costs vs. point
   budget; XP curves.
3. **Ideas parked in DESIGN.md:** more skills (Combat, Archery, Fishing), diet variety / Well Fed,
   more milestones from the draft list, toasts instead of chat messages, exploration extras.
4. **Known gap:** the client uses its own config for skill bonus and familiarity tier display, so
   a server with a changed config shows different numbers on the client.

## How the user works

- Plays the mod **with their kids**: fun and clarity first, no death penalty, progress must
  be visible ("explicit over invisible"). Text in the game should be simple English.
- Wants **ideas and push-back**, not just execution: propose a design with a recommendation and
  2–3 concrete questions, let them decide, then build. They often answer briefly ("agree on all 3, go").
- Playtests in the client themselves and reports back. Be explicit about what you verified
  (compile, server start) versus what still needs their playtest.
- Asks to keep CLAUDE.md current and to record decisions in DESIGN.md; commit per change.
- For testing they may ask to drop perk requirements temporarily; track those as TODOs.

## Environment gotchas

- The decompiled Minecraft sources in the scratchpad get cleared between sessions; re-run
  `./gradlew genSources` if needed and unzip the `-sources.jar` files (paths in CLAUDE.md).
- After a server smoke test, `pkill -f KnotServer` can exit with code 144 (it signals its own
  shell). Check with `ps -eo pid,args | grep devlaunchinjector | grep -v grep` that the server stopped.
- The perk list and Character tab are drawn by hand; when adding a skill or many perks, check that
  the layout still fits (Character tab: 4 skill rows of 28px in a 250px panel).
