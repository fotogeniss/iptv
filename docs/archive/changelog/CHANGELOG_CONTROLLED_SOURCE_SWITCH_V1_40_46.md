# Changelog — v1.40.46 Controlled Source Switch Coordinator

## Architecture

- Added `SourceGenerationGate` as the single owner of catalog and series freshness tokens.
- Added `SourceSwitchCoordinator` as the transaction boundary for explicit playlist changes.
- Added `SourceSwitchStatePolicy` for deterministic cleanup of source-bound UI state.
- Reduced `MainViewModel.selectPlaylist()` to one delegation call.
- Removed the mutable `loadGen` and `seriesLoadGen` counters from `MainViewModel`.
- Kept every existing public `MainViewModel` method and route unchanged.

## Source-switch contract

A valid switch now follows one tested order:

1. persist the target index,
2. invalidate catalog and series generations,
3. cancel old source jobs and EPG work,
4. release source-bound clients and working sets,
5. invalidate the requested provider/session cache,
6. publish one clean target-source state,
7. start automatic loading only after publication and only when a saved section choice exists.

An invalid index returns before any persistence, cancellation or state mutation.

## Bug fixed

Switching playlists while the previous source was still loading could leave source-bound UI fields attached to the target playlist, especially when the target had no remembered category choice and therefore did not immediately start a new load.

The stale state could include:

- `loading=true`,
- an old status message,
- the previous search query or selected group,
- category/refresh dialogs,
- an open series title, seasons or loading indicator.

The source-switch policy now clears these fields transactionally while preserving profile/settings state such as font scale, locked groups and sort mode.

## Behavior preserved

- A remembered source/section choice still triggers a forced fresh load.
- A source without a remembered choice still opens the content chooser.
- Favorites remain isolated by stable source identity.
- Source switching still clears visible channels, groups and EPG state before loading.
- Provider cancellation and stale-result rejection remain a two-layer defense.
- Active/inactive playlist edit and deletion behavior is unchanged.
- No player, route, database, EPG-provider or TV-focus behavior was refactored.

## Validation

- Source-switch focused runtime tests: 10 pass, 0 fail.
- MainViewModel callback-wiring semantic harness: passed.
- Compatibility contracts: 29 pass, 0 fail.
- Architecture contracts: 41 pass, 2 known size warnings, 0 fail.
- Deep validation contracts: 53 pass, 1 documented cleartext warning, 0 fail.
- Production critical-risk categories: 0 failures.
- Kotlin changed-file syntax scan: 0 parser findings.
- XML parse: 24 files, 0 errors.
- CI YAML and version contract: passed.
- Full Gradle gate was attempted but could not start because Gradle 8.9 was not cached and `services.gradle.org` was unreachable in the execution environment.
