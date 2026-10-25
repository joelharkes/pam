# Contributing to PAM

Thanks for wanting to help! PAM is a small hobby project, made to play with family.
The most useful thing you can do is **play it and tell us how it felt**.

## Feedback, bugs and ideas

Use [GitHub issues](https://github.com/joelharkes/pam/issues/new/choose). There's a form for each kind:

- **Bug report:** something is broken, crashes or behaves strangely.
- **Playtest feedback:** you played a session; what felt too easy, too hard, too slow, confusing or fun?
  This is the most valuable kind right now, because balance is still being tuned.
- **Perk or feature idea:** a new perk, milestone, skill or other feature.

Before opening an issue, search the existing ones. If someone already reported it, add a 👍 or a comment.

### What makes a good idea

PAM follows a few principles (more in [DESIGN.md](DESIGN.md)). Ideas that fit them are the easiest to say yes to:

- **Fun first, also for kids.** No death penalty, no harsh punishments.
- **The world never outgrows the player.** You should almost always feel stronger than what you face.
- **Explicit over invisible.** Upgrades come from named milestones and perks you can see, not silent stat creep.
- **Diminishing returns.** Big gains early, small gains later, so grinding isn't the best strategy.
- **Works in multiplayer**, with players at different progress levels.

## Code

Pull requests are welcome. For anything bigger than a small fix, **open an issue first** so we can
agree on the design before you spend time on it.

### Setup

You need **Java 25**. The Gradle wrapper downloads everything else.

```sh
./gradlew build                     # compile and build the jar in build/libs/
./gradlew runClient                 # start a dev client
./gradlew runServer --args="--nogui"  # start a dev server
```

Minecraft 26.x is unobfuscated, so the code uses Mojang's names directly (no Yarn mappings).

### Where things are

- [DESIGN.md](DESIGN.md): the design, decisions log, roadmap and what still needs a playtest.
- [CLAUDE.md](CLAUDE.md): code layout, conventions and how to add a milestone or perk.
  (It started as notes for an AI coding assistant, but it's the best map of the code.)

### Conventions

- Tabs for indentation; match the style of the code around your change.
- Balance numbers go in the config (`PamConfig`), not hard-coded.
- All player-facing text goes through `lang/en_us.json`, in **simple English** (kids read it).
- Prefer vanilla attributes over mixins where possible.
- The mod runs on both client and server: test on a dedicated server if your change touches gameplay.

By contributing you agree that your contribution is licensed under the [MIT license](LICENSE).
