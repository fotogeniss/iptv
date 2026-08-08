# v1.40.24 validation report

## Scope

Forward-port on top of v1.40.23:

- source-scoped/profile-scoped favorites persistence and migration;
- Android TV “Η λίστα μου” preview channel;
- secure launcher deep links and playback revalidation;
- independent Play Next / My List synchronization;
- launcher removal and TV Provider initialization handling.

## Automated validation completed

| Check | Result |
|---|---|
| Kotlin PSI parse — all `app/src` files | **188 files, 0 syntax errors** |
| Focused source-favorite / TV My List tests | **9 passed, 0 failed** |
| `PlaylistStore` + source-favorite semantic compile with Android/JSON contract stubs | **Passed** |
| TV Home publisher, entry store, policy and receiver semantic compile with Android API contract stubs | **Passed** |
| Android XML parse | **23 files, 0 errors** |
| GitHub Actions YAML parse | **Passed** |
| Production/test source count | **159 main / 29 test Kotlin files** |
| Version contract | **1.40.24 / 68** |

## Wiring audit

Verified:

- current UI/player writes use `sourceId` for add/remove/is-favorite operations;
- legacy no-argument favorite APIs are retained only inside migration code;
- source switching reloads only the selected provider's favorite keys;
- source deletion clears only that source's scoped records and schedules TV Home sync;
- catalog and episode refreshes reconcile favorite snapshots;
- profile change/deletion schedules launcher reconciliation;
- My List and Play Next enablement are independent;
- custom-channel rows are package-scoped, channel-scoped and use opaque tokens;
- preview-program removal broadcasts suppress and delete the matching row;
- deep-link launch checks profile, source, favorite membership and parental locks;
- no provider credential is written into channel/program intents or provider IDs.

## Full Gradle build limitation

Attempted:

```text
./gradlew --offline :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --stacktrace
```

The wrapper failed before Gradle project configuration because Gradle 8.9 was not
cached and this execution environment could not resolve `services.gradle.org`.
No Android compiler, lint or packaging task started. A real Android Studio or CI
build remains required before release, followed by device validation on at least
one Android TV / Google TV launcher.

## Required device smoke test

1. Upgrade an installation that already has favorites.
2. Open each provider once and confirm exact legacy favorites migrate only into
   the matching source.
3. Add the same item key in two providers and verify each opens through its own
   source.
4. Enable **Κανάλι Η λίστα μου**, approve channel visibility and confirm cards
   appear newest-first.
5. Remove a card from the launcher, trigger a sync and confirm it remains hidden.
6. Remove and re-add that favorite in-app and confirm the card returns.
7. Switch profile, delete a source and change parental locks; verify launcher
   rows reconcile accordingly.
