# CLAUDE.md

Working notes for Claude Code on this repo. Keep this file current: update it when the
stack, commands, conventions or key decisions change.

## Project

**PAM: Progressive Adventure Mod** (mod ID `progressive_adventure`), a Fabric mod for
Minecraft Java that makes the game more progressive: the player starts weaker (fewer
hearts, faster hunger) and grows stronger through milestones and by doing things.

- Feature ideas, design decisions and open questions: [DESIGN.md](DESIGN.md)
- Status: **v0.1 implemented**, not yet playtested in a client.
- Audience: the user plays this with their kids → fun first, no death penalty,
  progress must be visible and understandable.
- Must work on dedicated servers and LAN with players at different progress levels.
  The mod is required on both client and server.
- Core model: milestones (custom advancements) grant hearts/stomach upgrades;
  milestone count = "Adventure Rank", which will gate mob spawns and elites (v0.2+).

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
- Look up vanilla signatures with javap against
  `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-common-deobf/26.2/minecraft-common-deobf-26.2.jar`
  (client-only classes: `minecraft-clientonly-deobf`). Vanilla advancement/lang JSON can
  be unzipped from the same jar as format references.
- No `curl` on this machine; use `python3 -I` + urllib for HTTP.

## Code layout (`src/main/java/com/progressiveadventure/`)

- `ProgressiveAdventure`: entrypoint, registers all events
- `config/PamConfig`: all balance numbers, Gson → `config/progressive_adventure.json`
- `data/PlayerProgress`: immutable per-player record (kills, skill xp, foods, biomes)
- `data/PamAttachments`: Fabric data attachments (player progress synced to owner; placed blocks per chunk)
- `milestone/Milestones`: milestone list + rewards; `Progression`: derives hearts/stomach/rank
  from advancement progress and applies them; `Trackers`: foods, biomes, first night
- `skill/`: tool skills (break speed), `PlacedBlocks` anti-cheese
- `bestiary/Bestiary`: familiarity tiers → damage multiplier
- `command/PamCommand`: `/pam`, `/pam bestiary`
- `mixin/`: FoodData (hunger multipliers), Player (break speed), LivingEntity (damage),
  PlayerAdvancements (milestone hook), BlockItem (placed blocks), ConsumeItemTrigger (food hook)
- `access/`: duck interfaces for mixins (must live outside the mixin package)
- Milestone advancements: `src/main/resources/data/progressive_adventure/advancement/`

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
- All balance numbers live in config, not hard-coded. Use tabs, match the template style.
- Git identity is set repo-locally (no global git config on this machine).
