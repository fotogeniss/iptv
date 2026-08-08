# Changelog — v1.40.43 Deep Validation & TV Focus Gate

## Reliability and routing

- Invalid playlist indexes are rejected before persistence or navigation.
- Editing or deleting an inactive playlist no longer cancels, blanks or reloads the active source.
- Active-source deletion invalidates running generations before state cleanup and selects a bounded replacement.
- Duplicate playlist entries that point to the same stable source no longer erase shared favorites, history, group choices or local source data until the last reference is removed.
- Provider EPG returned after a playlist switch is discarded.

## Android TV focus and shortcuts

- Added a reusable dialog text action with a visible TV focus ring.
- Applied explicit focus actions to main/profile/settings/browse dialogs.
- Added deterministic initial focus and test tags to refresh/load-mode dialogs.
- Category selection starts on a visible action and supports a DPAD-right shortcut to Load.
- Added five Compose instrumentation scenarios for initial focus and DPAD transitions.
- Expanded pure remote-input tests for channel, media, menu/info, captions, seek and reveal behavior.

## Security

- TV Home exported playback accepts only exact `upl://play-next/<canonical-uuid>` and `upl://my-list/<canonical-uuid>` routes.
- TV Provider rows are suppressed/deleted only after the row contains an application-owned internal ID and that token resolves in the matching app store.
- Foreign or malformed program IDs cannot authorize a delete through the receiver.
- Remaining production Kotlin non-null assertions were removed.
- SAF backup/import handles document providers returning a null stream without crashing.
- Stalker session user-agent races now fail as controlled provider errors rather than NPEs.

## Structured cancellation

- Added a shared, tested provider-cancellation policy.
- TMDB, subtitle, M3U probe and Xtream fallback paths no longer convert cancellation into empty data or ordinary connection failure.
- Stalker connection-test and create-link fallback paths preserve cancellation.
- Player/metadata/EPG jobs retain source and generation guards.

## Lifecycle and performance gates

- Player teardown clears both named and anonymous delayed Handler work.
- Added JVM stress fixtures for 50,000 live rows and 20,000 series episodes.
- Added a device validation runbook and TV focus/shortcut matrix.
- Corrected CI action majors and placed static compatibility/security gates before Gradle jobs.

## Validation status

- 179 broad JVM assertions pass.
- Compatibility: 11 pass, 0 fail.
- Architecture: 29 pass, 2 large-file warnings, 0 fail.
- Deep validation: all blocking contracts pass; one documented cleartext compatibility warning remains.
- Instrumentation source exists but was not executable without an Android device/SDK.
- Full Gradle unit/lint/debug/release gate was attempted but blocked before startup because Gradle 8.9 could not be downloaded in this environment.
