# Screen 07 — Settings / Sources validation report

Version: **1.38.0 (41)**

## Scope

Validated the new Settings / Sources UI integration for Mobile and Android TV.
The data, repository, source loading and credential persistence layers were not
replaced.

## Source validation

- New settings source files compile against Compose-compatible contract stubs
  with no Kotlin source errors.
- `MainActivity.kt` passes Kotlin parser/syntax diagnostics with no `expecting`,
  redeclaration or conflicting-overload errors.
- All new Kotlin files are under 300 lines.
- `AdaptiveSettingsScreen` has one production call site.
- Braces, brackets and parentheses are balanced in every changed Kotlin file.

## Integration checks

- M3U add/edit uses the existing `AddPlaylistScreen` M3U tab.
- Xtream add/edit uses the existing Xtream tab.
- Stalker add/edit uses the existing portal/MAC tab.
- Opening a source uses `MainViewModel.selectPlaylist` and the existing browse
  flow.
- Deletion uses `MainViewModel.deletePlaylist` after an explicit confirmation.
- Player, EPG, TMDB, OpenSubtitles, profiles, parental PIN, backup/restore,
  sharing and font-scale actions remain connected to their existing handlers.

## Privacy checks

- The UI receives a reduced `SettingsSourceUi` model rather than raw
  credentials.
- Endpoint display is host-only.
- Runtime policy tests confirm that URL user info and passwords are not exposed.
- Stalker MAC addresses and Xtream usernames/passwords are never included in
  the presentation model.

## UI architecture

- Shared primitives: `ui/components/settings/`
- Mobile touch-first UI: `ui/mobile/settings/`
- Android TV DPAD-first UI: `ui/tv/settings/`
- Platform dispatcher: `ui/AdaptiveSettingsScreen.kt`
- Android TV hides the old bottom navigation only while Settings is open,
  avoiding duplicate navigation chrome.

## Environment limitation

A full Android Gradle build could not be executed in this environment because
the project contains a placeholder `gradlew`, no `gradle-wrapper.jar`, and no
Android SDK. The report therefore does not claim a completed Android Gradle
build.
