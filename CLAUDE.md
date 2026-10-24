# CLAUDE.md

Working notes for Claude Code on this repo. Keep this file current: update it when the
stack, commands, conventions or key decisions change.

## Project

A Minecraft Java Edition mod built on **Fabric** that makes the game more progressive:
the player starts weaker (fewer hearts, smaller hunger capacity) and grows stronger
through play, and gets better at things the more they do them (mining, fighting
specific mobs, etc.).

- Feature ideas, design decisions and open questions: [DESIGN.md](DESIGN.md)
- Status: **planning** — no code scaffolded yet.
- Audience: the user plays this with their kids → fun first, no death penalty,
  progress must be visible and understandable.
- Must work on dedicated servers and LAN with players at different progress levels.
- Core model: milestones (custom advancements) grant hearts/stomach upgrades;
  milestone count = "Adventure Rank", which gates mob spawns and mob strength.

## Stack (to be confirmed when scaffolding)

- Fabric Loader + Fabric API, Fabric Loom (Gradle)
- Java: JDK 25 is installed locally; use whatever the target MC version requires
- Minecraft version: **26.2** (user has it installed locally with Fabric Loader)
- Mappings: TBD (Yarn or Mojang official)

## Environment notes

- Git identity is set repo-locally (no global git config on this machine).

## Conventions

- Prefer vanilla **attributes** (max_health, block_break_speed, attack_damage, ...)
  with named modifiers over mixins wherever possible: they are multiplayer-safe,
  sync automatically and stack cleanly with other mods.
- Use mixins only where no attribute/event exists (e.g. hunger cap, HUD rendering).
- Store per-player progression with the Fabric Data Attachment API (persistent + synced).
- All balance numbers live in config, not hard-coded.
- Design must work on dedicated servers, not just singleplayer.
