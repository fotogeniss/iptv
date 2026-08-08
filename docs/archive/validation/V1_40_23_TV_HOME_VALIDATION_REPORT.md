# v1.40.23 Android TV Home / Play Next — Validation Report

Date: 2026-07-21  
Baseline: v1.40.22 (`versionCode 66`)  
Candidate: v1.40.23 (`versionCode 67`)

## Scope

This release adds an opt-in, privacy-preserving Android TV Play Next integration
while retaining the v1.40.22 Auto Frame Rate implementation, the v1.40.21 player
seek architecture and the v1.40.19 security hardening.

The release deliberately targets the system Continue Watching row only. It does
not create a duplicate app preview channel from resume data.

## Implementation audit

### Eligibility

- Only `vod`, `movie` and `series_ep` items are accepted.
- Movies start at the earlier of 3% or 2 minutes watched.
- Episodes start at 2 minutes watched.
- Items are excluded at 95% completion or with 3 minutes or less remaining.
- Live channels, series containers, blank source/item identities and invalid
  durations are rejected.
- Locked groups are normalized and excluded case-insensitively.
- Selection is newest-first, deduplicated, capped at five and limited to one
  episode per source/series.

### Privacy and security

- TV Provider rows receive a random opaque token, stable content digest,
  metadata, progress and an explicit app deep link.
- No provider URL, portal command, username, password, token or MAC address is
  inserted into the launcher URI.
- Full channel/source playback data is serialized only inside `SecureStorage`.
- Token playback verifies that TV Home is enabled, the profile is still active,
  the group is not currently locked and the source still exists.
- A playable URL is resolved only after these checks and is passed directly to
  the non-exported `PlayerActivity`.

### TV Provider contract

- `WRITE_EPG_DATA` is declared.
- Program type, Watch Next type, content ID, title, poster URI, poster aspect
  ratio, duration, position, engagement time and intent URI are supplied.
- Missing/unsafe artwork receives an `android.resource` fallback.
- `COLUMN_BROWSABLE` is queried but never written by the app.
- The system removal broadcast is handled and the row is deleted.
- Suppression is stored until a strictly newer engagement timestamp is observed.

### Synchronization

- A unique expedited `CoroutineWorker` reconciles desired and existing rows.
- `ExistingWorkPolicy.REPLACE` collapses bursts of state changes into the latest
  requested sync.
- Sync hooks cover TV app startup, player background/exit, source deletion,
  history/progress deletion, profile deletion/switch and lock changes.
- Disabling the setting clears encrypted token state and removes owned Play Next
  rows.

### Migration and future architecture

- Existing resume rows gain a one-time deterministic engagement timestamp and do
  not require the user to replay everything after update.
- New position writes store an actual engagement timestamp.
- Publisher access is behind `TvContinueWatchingPublisher`, allowing a later
  Engage SDK adapter without changing eligibility, encrypted payloads or deep
  link validation.

## Automated and static validation

| Check | Result |
|---|---|
| Kotlin PSI syntax parse | 180 files, 0 syntax errors |
| Production Kotlin count | 153 files |
| Test Kotlin count | 27 files |
| Focused TV Home policy runner | 10 passed, 0 failed |
| TV Home semantic compilation with Android/WorkManager stubs | Passed |
| TV Home wiring/security audit | 24 passed, 0 failed |
| XML parse | 22 files, 0 errors |
| GitHub Actions YAML parse | 1 file, 0 errors |
| Version audit | `1.40.23` / `67` |

Focused cases cover live rejection, movie and episode thresholds, completion,
locked groups, newest-first ordering, five-item limit, one episode per series,
deduplication and removal suppression/new-engagement behavior.

## Full Gradle build status

The wrapper attempted to run:

```text
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

It could not bootstrap because Gradle 8.9 was not cached and this execution
environment could not resolve/download `services.gradle.org`. The failure
occurred before project configuration or Kotlin/Android compilation. See
`gradle_build_attempt_v1_40_23.txt`.

A normal Android Studio or connected GitHub Actions run remains mandatory before
publishing an APK/AAB.

## Recommended real-device regression matrix

1. Enable/disable Android TV Home and confirm row addition/removal.
2. Resume an eligible movie and episode from Play Next with ExoPlayer and VLC.
3. Verify no live item, locked group or nearly completed item is published.
4. Switch profiles and confirm old-profile rows disappear.
5. Remove a card from the launcher and confirm it stays absent after app restart.
6. Play the removed item again and confirm it becomes eligible again.
7. Delete a source while it has a published card and confirm stale cleanup.
8. Test missing artwork and HTTP artwork on at least two TV launchers.
9. Test Android TV 8/9 and a modern Google TV device; launcher rendering and
   provider behavior vary by OEM.
10. Verify Play Next launch after process death and device reboot.

## Files added

- `app/src/main/java/com/prelude/iptv/tvhome/TvHomeEligibilityPolicy.kt`
- `app/src/main/java/com/prelude/iptv/tvhome/TvHomeCatalogRepository.kt`
- `app/src/main/java/com/prelude/iptv/tvhome/TvHomeEntryStore.kt`
- `app/src/main/java/com/prelude/iptv/tvhome/LegacyWatchNextPublisher.kt`
- `app/src/main/java/com/prelude/iptv/tvhome/TvHomePlaybackActivity.kt`
- `app/src/main/java/com/prelude/iptv/tvhome/TvHomeBrowsableDisabledReceiver.kt`
- `app/src/main/java/com/prelude/iptv/tvhome/TvHomeDevice.kt`
- `app/src/main/java/com/prelude/iptv/tvhome/TvHomeSyncWorker.kt`
- `app/src/test/java/com/prelude/iptv/tvhome/TvHomeEligibilityPolicyTest.kt`
- `CHANGELOG_TV_HOME_PLAY_NEXT_V1_40_23.md`
- This validation report and machine-readable validation logs.
