# CLAUDE.md

Working notes for Claude Code on this repo. Keep this file current: update it when the
stack, commands, conventions or key decisions change.

## Project

**PAM: Progressive Adventure Mod** (mod ID `progressive_adventure`), a Fabric mod for
Minecraft Java that makes the game more progressive: the player starts weaker (fewer
hearts, faster hunger) and grows stronger through milestones and by doing things.

- Design of what's built, non-goals (check before building an idea that touches them), decisions log, open questions, roadmap and the **playtest checklist**
  (what's built but unconfirmed, temporary test changes): [DESIGN.md](DESIGN.md). **All not-built ideas
  live in its single "Ideas" section**: add new ones there (not in the pillars), and move an idea's text
  into its pillar when it gets built.
- Performance review (fixed and open items): [PERFORMANCE.md](PERFORMANCE.md)
- Code/design audit (bugs, smaller issues, doc drift; fixed and open items): [AUDIT.md](AUDIT.md). Its **Minecraft
  update checklist** lists the hooks that can break silently: go through it on every Minecraft update, and add to it
  when a new mixin targets a call by position (`ordinal`) or code copies a vanilla rule.
- Status: **0.6.0 released** (tag `v0.6.0`, Modrinth Alpha; 0.4.0 was the first public release). **v0.1 tagged** (`v0.1.0`, playtested). **v0.2** (perks + Adventure screen) playtested,
  not tagged yet. Farming skill/perks/milestones, Fishing skill/perks/milestones, Melee skill (all 4 slices, shield perks in Defense), bestiary pages and collections, Defense perks, Deep Breath and Fleet Foot tiers,
  **v0.3a** mob unlocks, **v0.3b** mob gear + elites, **v0.3c** elite traits and **v0.4a** waypoints are implemented;
  what still needs a playtest is in the DESIGN.md playtest checklist.
- Audience: the user plays this with their kids → fun first, no death penalty,
  progress must be visible and understandable.
- Must work on dedicated servers and LAN with players at different progress levels.
  The mod is required on both client and server.
- Core model: every advancement (vanilla + PAM) gives task/goal/challenge = 1/2/3
  **Adventure Rank**; Rank = perk points earned (available = rank − cost of owned perks).
  Rank will gate mob spawns and elites (v0.3). **Milestones** = advancements with a
  heart/stomach reward: PAM ones (Adventure tab) plus a list of vanilla ones in
  `Milestones.VANILLA`. Never add a PAM milestone that duplicates a vanilla advancement
  (double toast); put the reward on the vanilla one instead.

## Stack

- Minecraft **26.2**, Fabric Loader 0.20.1 (mod accepts >=0.19.5), Fabric API 0.161.0+26.2
- Loom 1.19.3 (`net.fabricmc.fabric-loom` plugin), Gradle 9.8 wrapper, Java 25
- Minecraft is **unobfuscated since 26.1**: Mojang names, no Yarn/mappings.
  Note the 1.21.11+ renames: `Identifier` (not ResourceLocation), `ResourceKey.identifier()`.
- Base template: FabricMC/fabric-example-mod (split client/main source sets)

## Commands

- `./gradlew build`: compile + jar in `build/libs/`
- `./gradlew runClient`: dev client · `./gradlew runServer --args="--nogui"`: dev server
  (run dir `run/`, `run/eula.txt` already accepted)
- The dev client logs in with a fixed name (`--username Player502` in `build.gradle`). Without it
  Loom picks a random `PlayerNNN` each launch = new UUID = a fresh player with no progress.
- Look up vanilla signatures with javap against
  `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-common-deobf/26.2/minecraft-common-deobf-26.2.jar`
  (client-only classes: `minecraft-clientonly-deobf`). Vanilla advancement/lang JSON can
  be unzipped from the same jar as format references.
- No `curl` on this machine; use `python3 -I` + urllib for HTTP.
- Server smoke test after mixin changes: start `runServer` in the background, wait for
  `Done (` in the log, check for ERROR/Mixin lines, then stop it. Mixin targets that
  don't match crash at startup (`defaultRequire: 1`). `pkill -f KnotServer` can exit with
  code 144 (it signals its own shell); confirm with
  `ps -eo pid,args | grep devlaunchinjector | grep -v grep` that the server stopped.

## Workflow

- The user playtests in the client (`./gradlew runClient`); Claude can't see the game window,
  so report what was verified (compile, server start) vs. what still needs a playtest.
- The user wants **ideas and push-back**, not just execution: propose a design with a
  recommendation and 2–3 concrete questions, let them decide, then build. They often answer
  briefly ("agree on all 3, go").
- Record every decision in the DESIGN.md decisions log; keep its roadmap and playtest
  checklist current (add what you build, remove what the user confirms).
- In-game text: simple English (kids read it).
- For testing the user may ask to drop perk requirements temporarily; mark them with TODOs
  and list them in the playtest checklist.
- Commit per feature/change. Tag playtested versions (`v0.1.0` = first test version).

## Release (Modrinth)

- Use the `/release` skill (`.claude/skills/release/`). Pushing a `vX.Y.Z` tag runs
  `.github/workflows/release.yml`: Modrinth upload (Minotaur, project `hCWGq4lt` = `pam-progressive-adventure`),
  description sync, GitHub release. Needs the `MODRINTH_TOKEN` repo secret. Never upload from here.
- `version` and `release_channel` (alpha/beta/release) in `gradle.properties`; the tag must match `version`.
  `0.4.0` = first public release (tagged and uploaded 2026-10-25). Players now have saves/configs: removed or
  renamed perks, milestones or settings need a migration or a CHANGELOG note.
- `CHANGELOG.md`: one `## X.Y.Z` section per release, written for players; `releaseNotes` and `modrinth` fail (plain `build` doesn't) if the
  current version has none (`./gradlew releaseNotes`). Dry run:
  `MODRINTH_TOKEN=dummy ./gradlew modrinth -PmodrinthDebug --no-configuration-cache` (Minotaur breaks the config cache).
- One Minecraft version at a time (decided); no multi-version builds.
- `README.md` is for players (GitHub). `docs/modrinth-description.md` is the same text with absolute
  `raw.githubusercontent.com` image links for the Modrinth page: update both together.
- Screenshots in `docs/screenshots/` as JPG (quality 88) to keep the repo small.
- Issue forms in `.github/ISSUE_TEMPLATE/` (bug, playtest feedback, perk/feature idea); `CONTRIBUTING.md`.

## Adding things

- **Achievement tip:** lang key `achievement.progressive_adventure.tip.<namespace>.<path with dots>.<n>`, numbered from 1, cheap nudge first and near-answer last.
  The tab counts the keys, no code needed. Check the conditions in the vanilla JSON, not from memory. Simple English.
- **Bestiary text:** where-to-find hints and tips are lang keys `bestiary.progressive_adventure.where|tip.<namespace>.<mob>`;
  the page hides a missing tip and shows "Nobody knows yet..." for a missing hint.
- **Milestone:** add an entry in `Milestones.java` (reward), an advancement JSON under
  `data/progressive_adventure/advancement/<category>/`, and title + description (ending in
  "Reward: ...") in `lang/en_us.json`. Code-driven ones use the `granted` impossible criterion.
  **Vanilla reward:** add to `Milestones.VANILLA` and override the vanilla description key
  (`advancements.<tab>.<name>.description`) in our lang file with the vanilla text + "\nReward: ...".
- **Perk:** register in `Perks.java`, add name + description to the lang file, implement the
  effect in `PerkEffects` (behaviour) or `Progression.apply` (attribute modifier).
- **Text:** all player-facing text goes through `lang/en_us.json` (edit by hand).

## Code layout (`src/main/java/com/progressiveadventure/`)

- `ProgressiveAdventure`: entrypoint, registers all events
- `config/PamConfig`: all balance numbers (defaults in code). `config/progressive_adventure.json` holds only
  overrides; `config/progressive_adventure.defaults.json` is a reference rewritten every start, never read
- `data/PlayerProgress`: immutable per-player record (kills, skill xp, foods, biomes, owned perks, elite kills and traits per mob, tips bought)
- `data/ProgressionSnapshot`: server-derived values synced for the UI (rank, hearts, stomach, perk points)
- `data/PamAttachments`: Fabric data attachments (progress + snapshot synced to owner; placed blocks per chunk)
- `milestone/Milestones`: milestone list + rewards; `Progression`: derives hearts/stomach/rank
  from advancement progress and applies them; `Trackers`: foods, biomes, first night
- `skill/`: `Skills` (levels, bonus curves, xp), tool skills (break speed), `Farming` (harvest/breeding
  xp, extra harvests, Replant/Harvester/Rancher), `Melee` (hand-to-hand hit xp, attack speed, sword/axe perks), `MeleeMoves` (Ground Pound, Shockwave,
  spear perks), `Targets` (hostile/friendly filters shared by the combat perks),
  `Archery` (arrow/thrown-trident hit xp, draw speed incl. tridents, arrow perks), `Fishing`
  (catch xp, bite speed, fishing perks; treasure = item tag `fishing_treasure` or enchanted),
  `PlacedBlocks` anti-cheese
- `bestiary/Bestiary`: familiarity tiers → damage multiplier, what each tier unlocks (labels known/familiar/expert/master),
  assist kills (CombatTracker entries via `CombatTrackerAccessor`), Master double loot, collections; `BestiaryInfo`:
  answers the mob page's request (drop chances sampled from the loot table, cached; unlock rank; elite traits)
- `world/MobUnlocks`: which hostile mobs may spawn naturally at a rank (lowest rank of nearby players),
  unlock messages, "next unlock" for the snapshot; `world/Elites`: extra gear on normal mobs, elites
  (persistent `ELITE_RANK` attachment on the mob), elite XP/loot/milestone; `world/EliteTrait`: traits
  (persistent `ELITE_TRAIT`; Leaping goal re-added on every entity load since goals aren't saved); `world/EliteMap`: treasure and explorer maps elites drop
- `waypoint/`: `WaypointContent` (the mod's own items/blocks: shard, stone), `WaypointStoneBlock`
  (ACTIVE state = look only), `Waypoint` record, `Waypoints` (shared list as an Overworld attachment;
  turn on/rename/break, XP cost, charge-up, safe landing, pets, text-display name labels)
- `perk/`: `Perks` (definitions, costs, requirements, status), `PerkEffects` (server behaviour:
  durability, prospector, timber/lumberjack, forester, clear cut (delayed via a tick queue), flint finder, vein miner, excavator, archaeologist, orchard, bird's nest,
  second wind, defense damage reduction); attribute perks are applied in `Progression`; `MovementPerks`
  (Shadow Step visibility/escape, server side of Double Jump and Mantle), `ShieldPerks` (Parry, Deflect, Shield Bash),
  `Cooldown` (per-player perk cooldowns, cleared on server stop), `DeathPerks` (Keepsake, Way Back trail, Faithful Friend)
- `achievement/Achievements`: full advancement list for the Achievements tab (the client only gets ones near what the player did), buying tips
- `network/`: C2S payloads `AchievementListRequestPayload`, `BuyTipPayload`, `BuyPerkPayload`, `ResetPerksPayload`, `DoubleJumpPayload`, `MantlePayload`,
  `ShieldBashPayload`, `GroundPoundPayload`, `NameWaypointPayload`, `WaypointTeleportPayload`, `BestiaryInfoRequestPayload`; S2C `OpenWaypointNamePayload`,
  `OpenWaypointListPayload` open client screens, `BestiaryInfoPayload`, `AchievementListPayload`, `BestiaryDiscoveredPayload` (toast); server validates in `PamNetworking`
- `command/PamCommand`: `/pam`, `/pam bestiary`, `/pam elite <mob> [trait]` (ops, spawns a test elite), `/pam elitemap <type>` (ops, gives an elite's map, shows search time),
  `/pam skill <skill> <level> [player]` (ops, sets a skill level for testing perks), `/pam kills <mob> <count> [player]` (ops, bestiary tiers), `/pam waypoint list|tp|remove` (ops)
- `mixin/`: FoodData (hunger multipliers, Steady Healing, Quick Healing, Deep Reserves), Player (break speed, attack strength, Careful Sweep, Whirlwind, Ground Pound landing), LivingEntity (damage, Iron Stomach, Shadow Step visibility, shield blocks for Parry/Deflect),
  PlayerAdvancements (advancement hook), PlayerList (flush pending progress on save/respawn), ServerPlayer (Keepsake restore), BlockItem (placed blocks), ConsumeItemTrigger (food hook),
  ItemStack (Sturdy durability perks), ShovelItem (Path Maker), FarmlandBlock (Light Step), BoneMealItem (Green Thumb,
  MixinExtras `@WrapMethod`), Animal (breeding hook), FishingHook (bite speed, Keen Eye, catch xp/Double Catch, Treasure Hunter),
  ProjectileWeaponItem (Thrifty Quiver, arrow perks on shoot), AbstractArrow (pierce, in-flight perks, Deflect), ThrownTrident (Loyal Trident), KineticWeapon/PiercingWeapon (spear charge and jab perks),
  LivingEntity also speeds up bow/crossbow/trident use ticks (draw speed), NaturalSpawner (mob unlocks after vanilla picks the type; elites/gear just before the mob is added), PatrolSpawner (patrol unlock)
- `access/`: duck interfaces for mixins (must live outside the mixin package)
- Client (`src/client/java/.../client/`): K keybind; `BlockSense` (Ore/Treasure/Relic Sense particle trails); `AirMoves`
  (Double Jump, Mantle and Ground Pound: client moves itself, then tells the server); `ShieldBash` (attack click while
  blocking, read at the start of the client tick); client mixin on the lightmap
  (Cat's Eyes); `screen/AdventureScreen` with
  `CharacterTab`, `PerksTab` (scrollable list), `BestiaryTab` (grid + mob page, custom-drawn, simple Button tabs), `AchievementsTab` (grid + page, list from `AchievementsClient`);
  `BestiaryClient` (info cache per connection, discovery toast `BestiaryToast`);
  `WaypointNameScreen`, `WaypointListScreen`
- Milestone advancements: `src/main/resources/data/progressive_adventure/advancement/`
- Decompiled MC sources: `./gradlew genSources`, then the `-sources.jar` files under
  `.gradle/loom-cache/minecraftMaven/net/minecraft/` (unzip to the scratchpad to grep).
  26.x GUI: `GuiGraphicsExtractor`, `extractRenderState`, `minecraft.gui.setScreen(...)`.
  The scratchpad is cleared between sessions, so re-unzip when needed.
- The Character tab and perk list are drawn by hand; when adding a skill or many perks, check
  the layout still fits (Character tab: two columns of 4 skill rows of 26px, so 8 skills fit; Perks tab: category
  column holds 10 rows of 19px, so an 11th category needs a new layout; one category per skill plus
  Defense, Survival and Movement; a long category uses perk groups (`register(..., group, ...)` in `Perks`,
  lang `perk_group.progressive_adventure.<group>`) for headings, like Melee's weapons). A skill's enum name must
  match its `PerkCategory` (the Character tab takes the icon from it).

## Conventions

- Textures: 16×16 PNGs generated with Python/PIL (installed) from vanilla textures unzipped from the
  client jar, e.g. recolors.

- Prefer vanilla **attributes** (max_health, ...) with named modifiers over mixins
  wherever possible: multiplayer-safe, auto-synced, stack cleanly with other mods.
- Hearts/stomach/rank are **derived** from completed advancements, never stored, so
  rewards can't double-grant and balance changes apply to existing players.
- Attachments only sync when re-set → keep attachment values immutable, `setAttached` a new value.
- Player progress: always use `PamAttachments.progress()` / `setProgress()`, never the attachment directly.
  On the server, changes are held per player and stored (= synced) once at the end of the tick, or earlier
  when the player is saved or respawns (`PlayerListMixin`).
- Anything the client computes (e.g. break speed) must read synced data.
- Code-driven milestones use a single `minecraft:impossible` criterion named `granted`.
- **Milestone rewards live in `Milestones.java` and are repeated in the advancement
  description (lang file).** Change both together.
- Perk costs/requirements live in `Perks.java` (client and server must agree); perk effect
  strengths live in config (server-only). Known gap (also in README's Known limitations): the client reads its own
  config for skill speed and familiarity tiers, so a server with changed values can show different numbers.
- Client reads attachments with `getAttachedOrElse` (never create attachments client-side).
- Register attachments with `AttachmentRegistry.create(id, builder -> ...)` (`builder()` is deprecated).
- `PlacedBlocks` only remembers logs, ores and melon/pumpkin/cane/cactus/bamboo (mutable set per chunk,
  max 10k, not synced). Grown crops (age-based) give Farming xp even when player-planted; melons, pumpkins, cane, cactus
  and bamboo only when not player-placed.
- The config file only holds overrides, so changing a default in `PamConfig` reaches everyone who didn't override
  that setting (no config files to update). Renaming or removing a setting makes old overrides of it unknown: the
  mod warns about unknown keys at startup, so mention renames in the CHANGELOG.
- All balance numbers live in config, not hard-coded. Exception: values that must match vanilla to be correct
  (e.g. the 128-block mob unlock radius = vanilla's spawn/despawn distance) are constants with a comment.
  Use tabs, match the template style.
- **Known limitations** (things players or server admins should know that stay true, like claim mods and Replant)
  go in the "Known limitations" section of README.md and docs/modrinth-description.md. AUDIT.md is the log of
  what an audit found and fixed.
- Git identity is set repo-locally (no global git config on this machine).
