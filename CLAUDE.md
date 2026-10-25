# CLAUDE.md

Working notes for Claude Code on this repo. Keep this file current: update it when the
stack, commands, conventions or key decisions change.

## Project

**PAM: Progressive Adventure Mod** (mod ID `progressive_adventure`), a Fabric mod for
Minecraft Java that makes the game more progressive: the player starts weaker (fewer
hearts, faster hunger) and grows stronger through milestones and by doing things.

- Feature ideas, design decisions, open questions, roadmap and the **playtest checklist**
  (what's built but unconfirmed, temporary test changes): [DESIGN.md](DESIGN.md)
- Status: **v0.1 tagged** (`v0.1.0`, playtested). **v0.2** (perks + Adventure screen) playtested,
  not tagged yet. Farming skill/perks/milestones, Defense perks, Deep Breath and Fleet Foot tiers,
  **v0.3a** mob unlocks, **v0.3b** mob gear + elites and **v0.3c** elite traits are implemented;
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

## Adding things

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
- `config/PamConfig`: all balance numbers, Gson → `config/progressive_adventure.json`
- `data/PlayerProgress`: immutable per-player record (kills, skill xp, foods, biomes, owned perks)
- `data/ProgressionSnapshot`: server-derived values synced for the UI (rank, hearts, stomach, perk points)
- `data/PamAttachments`: Fabric data attachments (progress + snapshot synced to owner; placed blocks per chunk)
- `milestone/Milestones`: milestone list + rewards; `Progression`: derives hearts/stomach/rank
  from advancement progress and applies them; `Trackers`: foods, biomes, first night
- `skill/`: `Skills` (levels, bonus curves, xp), tool skills (break speed), `Farming` (harvest/breeding
  xp, extra harvests, Replant/Harvester/Rancher), `PlacedBlocks` anti-cheese
- `bestiary/Bestiary`: familiarity tiers → damage multiplier
- `world/MobUnlocks`: which hostile mobs may spawn naturally at a rank (lowest rank of nearby players),
  unlock messages, "next unlock" for the snapshot; `world/Elites`: extra gear on normal mobs, elites
  (persistent `ELITE_RANK` attachment on the mob), elite XP/loot/milestone; `world/EliteTrait`: traits
  (persistent `ELITE_TRAIT`; Leaping goal re-added on every entity load since goals aren't saved)
- `perk/`: `Perks` (definitions, costs, requirements, status), `PerkEffects` (server behaviour:
  durability, prospector, timber, archaeologist, second wind, defense damage reduction); attribute perks are applied in `Progression`
- `network/`: C2S payloads `BuyPerkPayload`, `ResetPerksPayload`; server validates in `PamNetworking`
- `command/PamCommand`: `/pam`, `/pam bestiary`, `/pam elite <mob> [trait]` (ops, spawns a test elite)
- `mixin/`: FoodData (hunger multipliers), Player (break speed), LivingEntity (damage, Iron Stomach),
  PlayerAdvancements (advancement hook), BlockItem (placed blocks), ConsumeItemTrigger (food hook),
  ItemStack (Sturdy durability perks), FarmlandBlock (Light Step), BoneMealItem (Green Thumb,
  MixinExtras `@WrapMethod`), Animal (breeding hook),
  NaturalSpawner (mob unlocks after vanilla picks the type; elites/gear just before the mob is added), PatrolSpawner (patrol unlock)
- `access/`: duck interfaces for mixins (must live outside the mixin package)
- Client (`src/client/java/.../client/`): K keybind; `OreSense` (client-side particle trail); `screen/AdventureScreen` with
  `CharacterTab`, `PerksTab` (scrollable list), `BestiaryTab` (custom-drawn, simple Button tabs)
- Milestone advancements: `src/main/resources/data/progressive_adventure/advancement/`
- Decompiled MC sources: `./gradlew genSources`, then the `-sources.jar` files under
  `.gradle/loom-cache/minecraftMaven/net/minecraft/` (unzip to the scratchpad to grep).
  26.x GUI: `GuiGraphicsExtractor`, `extractRenderState`, `minecraft.gui.setScreen(...)`.
  The scratchpad is cleared between sessions, so re-unzip when needed.
- The Character tab and perk list are drawn by hand; when adding a skill or many perks, check
  the layout still fits (Character tab: 4 skill rows of 28px in a 250px panel).

## Conventions

- Prefer vanilla **attributes** (max_health, ...) with named modifiers over mixins
  wherever possible: multiplayer-safe, auto-synced, stack cleanly with other mods.
- Hearts/stomach/rank are **derived** from completed advancements, never stored, so
  rewards can't double-grant and balance changes apply to existing players.
- Attachments only sync when re-set → keep attachment values immutable, `setAttached` a new value.
- Anything the client computes (e.g. break speed) must read synced data.
- Code-driven milestones use a single `minecraft:impossible` criterion named `granted`.
- **Milestone rewards live in `Milestones.java` and are repeated in the advancement
  description (lang file).** Change both together.
- Perk costs/requirements live in `Perks.java` (client and server must agree); perk effect
  strengths live in config (server-only). Known gap: the client reads its own config for skill
  speed and familiarity tiers, so a server with changed values can show different numbers.
- Client reads attachments with `getAttachedOrElse` (never create attachments client-side).
- Register attachments with `AttachmentRegistry.create(id, builder -> ...)` (`builder()` is deprecated).
- Grown crops (age-based) give Farming xp even when player-planted; melons, pumpkins, cane, cactus
  and bamboo only when not player-placed.
- All balance numbers live in config, not hard-coded. Use tabs, match the template style.
- Git identity is set repo-locally (no global git config on this machine).
