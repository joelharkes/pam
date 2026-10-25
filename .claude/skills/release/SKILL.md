---
name: release
description: Prepare and publish a new PAM version - pick the version number, write the CHANGELOG entry, bump gradle.properties, commit, tag and push the tag so GitHub Actions uploads it to Modrinth. Use when the user asks to release, publish or ship a version.
---

# Release a PAM version

Pushing a `vX.Y.Z` tag starts `.github/workflows/release.yml`. That workflow builds the jar,
uploads it to Modrinth (Minotaur, project `pam-progressive-adventure`), syncs the Modrinth
description from `docs/modrinth-description.md`, and creates a GitHub release. This skill does
everything up to the push. Never upload to Modrinth from this machine.

## 1. Check the state

- On `main`, up to date with `origin/main`, no staged or modified tracked files. Untracked files
  (e.g. work in progress) are fine but mention them; never commit them as part of the release.
- `./gradlew build` passes.
- Last release: `git describe --tags --abbrev=0`. Commits since: `git log <tag>..HEAD --oneline`.
  Nothing since the last tag → stop and say so.
- Look at the DESIGN.md playtest checklist: tell the user what in this release hasn't been
  playtested yet. They decide whether to release anyway.
- Look for temporary test changes (TODOs that drop perk requirements, etc.) and list them.
  They must not ship.

## 2. Propose the version (ask the user)

Current version is `version=` in `gradle.properties`, channel is `release_channel=` (alpha, beta, release).

- New features, perks, milestones or balance changes → bump the minor (0.4.0 → 0.5.0).
- Only fixes → bump the patch (0.4.0 → 0.4.1).
- If the current version has never been tagged (first release), keep it as is.
- Channel stays the same unless the user says otherwise.

Propose one version + channel with a one-line reason and let the user confirm.

## 3. Write the CHANGELOG entry

Add a section at the top of `CHANGELOG.md` (below `# Changelog`), header `## X.Y.Z` plus an
optional short title. The build reads exactly this section for Modrinth and the GitHub release,
and fails if it's missing.

- Written for **players**, in simple English (kids read it). Not commit messages.
- Group as: **New**, **Changes** (balance, behaviour), **Fixes**. Leave out empty groups,
  refactors, docs-only and dev-only changes.
- Mention anything that changes existing worlds or configs (e.g. a perk refunded, a config
  default changed: existing config files keep the old value).

Show the entry to the user and wait for their OK or edits.

## 4. Update the rest

- `gradle.properties`: `version` (and `release_channel` if it changed).
- If features were added or counts changed (perks, milestones, skills), update `README.md`
  **and** `docs/modrinth-description.md` together (the latter uses absolute image links).
- New Minecraft version? That's a different job: update the Stack section of CLAUDE.md, and
  `gameVersions` comes from `minecraft_version` automatically.

## 5. Verify

```sh
./gradlew build releaseNotes --no-configuration-cache
MODRINTH_TOKEN=dummy ./gradlew modrinth -PmodrinthDebug --no-configuration-cache
```

The second command prints what would be uploaded without uploading. Check the version number,
channel, game version and changelog in its output.

## 6. Commit, tag, push (confirm first)

```sh
git add CHANGELOG.md gradle.properties README.md docs/modrinth-description.md
git commit -m "Release X.Y.Z"
git tag -a vX.Y.Z -m "PAM X.Y.Z"
```

Pushing publishes to Modrinth for everyone, so **ask the user before pushing**, then:

```sh
git push origin main
git push origin vX.Y.Z
```

## 7. After the push

- The workflow runs at https://github.com/joelharkes/pam/actions (no `gh` on this machine;
  the public API works with `python3 -I` + urllib:
  `https://api.github.com/repos/joelharkes/pam/actions/runs?event=push&per_page=1`).
- If it failed before the Modrinth upload: fix, delete the tag (`git tag -d vX.Y.Z`,
  `git push origin :refs/tags/vX.Y.Z`), and tag again. If Modrinth already has the version,
  don't re-tag the same number: release the next patch.
- Update the roadmap/decisions in DESIGN.md if the release was a milestone (e.g. "v0.5 released").
