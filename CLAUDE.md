# CLAUDE.md

Working notes for Claude Code on this repo. Keep this file current: update it when the
stack, commands, conventions or key decisions change.

## Project

**PAM: Progressive Adventure Mod** (mod ID `progressive_adventure`), a Fabric mod for
Minecraft Java that makes the game more progressive: the player starts weaker (fewer
hearts, faster hunger) and grows stronger through milestones and by doing things.

- Feature ideas, design decisions and open questions: [DESIGN.md](DESIGN.md)
- Status: **v0.1 tagged** (`v0.1.0`, playtested). **v0.2** (perks + Adventure screen) implemented and playtested.
- Audience: the user plays this with their kids → fun first, no death penalty,
  progress must be visible and understandable.
- Must work on dedicated servers and LAN with players at different progress levels.
  The mod is required on both client and server.
- Core model, two tracks: (1) PAM milestones (custom advancements) grant hearts/stomach;
  milestone count = "Adventure Rank", which will gate mob spawns and elites (v0.3).
  (2) every advancement (vanilla + PAM) gives perk points (task/goal/challenge = 1/2/3)
  to spend on choosable perks; skills/rank/mob types gate which perks can be bought.

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
- `data/PlayerProgress`: immutable per-player record (kills, skill xp, foods, biomes, owned perks)
- `data/ProgressionSnapshot`: server-derived values synced for the UI (rank, hearts, stomach, perk points)
- `data/PamAttachments`: Fabric data attachments (progress + snapshot synced to owner; placed blocks per chunk)
- `milestone/Milestones`: milestone list + rewards; `Progression`: derives hearts/stomach/rank
  from advancement progress and applies them; `Trackers`: foods, biomes, first night
- `skill/`: tool skills (break speed), `PlacedBlocks` anti-cheese
- `bestiary/Bestiary`: familiarity tiers → damage multiplier
- `perk/`: `Perks` (definitions, costs, requirements, status), `PerkEffects` (server behaviour:
  durability, prospector, timber, archaeologist, second wind); attribute perks are applied in `Progression`
- `network/`: C2S payloads `BuyPerkPayload`, `ResetPerksPayload`; server validates in `PamNetworking`
- `command/PamCommand`: `/pam`, `/pam bestiary`
- `mixin/`: FoodData (hunger multipliers), Player (break speed), LivingEntity (damage, Iron Stomach),
  PlayerAdvancements (advancement hook), BlockItem (placed blocks), ConsumeItemTrigger (food hook),
  ItemStack (Sturdy durability perks)
- `access/`: duck interfaces for mixins (must live outside the mixin package)
- Client (`src/client/java/.../client/`): K keybind; `screen/AdventureScreen` with
  `CharacterTab`, `PerksTab`, `BestiaryTab` (custom-drawn, simple Button tabs)
- Milestone advancements: `src/main/resources/data/progressive_adventure/advancement/`
- Decompiled MC sources: `./gradlew genSources`, then the `-sources.jar` files under
  `.gradle/loom-cache/minecraftMaven/net/minecraft/` (unzip to the scratchpad to grep).
  26.x GUI: `GuiGraphicsExtractor`, `extractRenderState`, `minecraft.gui.setScreen(...)`.

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
- All balance numbers live in config, not hard-coded. Use tabs, match the template style.
- Git identity is set repo-locally (no global git config on this machine).
