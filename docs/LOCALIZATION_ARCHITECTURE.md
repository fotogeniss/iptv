# Prelude+ localization architecture

Status: design preview ready; Android implementation waits for owner approval.

## Product contract

Prelude+ initially supports three app-language choices:

- `SYSTEM`: follow the device language and region;
- `EL`: Greek;
- `EN`: English.

Language names are autonyms (`Ελληνικά`, `English`) and never use country flags.
The app-language choice affects interface copy, accessibility descriptions,
notifications and app-owned errors. It does not translate provider category
names, channel names, movie/series titles, EPG data or user-entered source names.
Audio and subtitle language preferences remain separate playback settings.

English is the unqualified resource set and ultimate fallback. Greek must be a
complete `values-el` translation before the language picker is exposed in a
public build. A missing translation must show English, never a resource key,
blank label or crash.

## Android runtime architecture

The implementation follows Android's public per-app language stack:

1. Add stable `androidx.appcompat:appcompat:1.7.1`.
2. Move all five Compose hosts from `ComponentActivity` to
   `AppCompatActivity`; use an AppCompat no-action-bar parent for both existing
   app themes.
3. Enable AGP `generateLocaleConfig` and declare `unqualifiedResLocale=en` in
   `app/src/main/res/resources.properties`.
4. Limit packaged locales to `en` and `el` so dependency translations are not
   accidentally advertised as supported app languages.
5. Enable AndroidX locale auto-storage for API 26–32. Android 13+ persists and
   synchronizes the same selection with the system App Languages screen.
6. Apply choices only through `AppCompatDelegate.setApplicationLocales` using
   an empty `LocaleListCompat` for `SYSTEM`, `el` for Greek and `en` for English.

This avoids a second custom locale store, custom `ContextWrapper` behavior and
different language state between activities. Locale changes may recreate the
current activity; Compose then resolves `stringResource` again from the new
configuration.

Primary references:

- <https://developer.android.com/guide/topics/resources/app-languages>
- <https://developer.android.com/guide/topics/resources/localization>
- <https://developer.android.com/develop/ui/compose/resources>
- <https://developer.android.com/jetpack/androidx/releases/appcompat>

## Resource ownership

Resources are split by feature responsibility, not collected in one giant file:

```text
app/src/main/res/
├── values/
│   ├── strings_core.xml
│   ├── strings_navigation.xml
│   ├── strings_home.xml
│   ├── strings_live.xml
│   ├── strings_catalog.xml
│   ├── strings_search.xml
│   ├── strings_details.xml
│   ├── strings_player.xml
│   ├── strings_sources.xml
│   ├── strings_settings.xml
│   ├── strings_errors.xml
│   └── strings_accessibility.xml
└── values-el/
    └── matching files with the same resource keys
```

Key names describe ownership and meaning, for example `nav_home`,
`settings_app_language`, `live_no_channels_in_category` and
`a11y_open_settings`. Generic numbered names or screen-position names are not
allowed.

## Kotlin boundaries

- Composables use `stringResource` and `pluralStringResource`.
- Counts use `<plurals>`; sentences are not assembled from translated fragments.
- Resource formatting uses positional placeholders where translators may need
  to reorder values.
- Android-free policies keep stable enum/route identifiers and never depend on
  `R`. The UI maps those identifiers to localized resources.
- Data/provider layers return typed states or stable error codes. Only the UI
  boundary turns those states into localized copy. Raw provider responses remain
  available for diagnostics but are not used as primary user-facing messages.
- App-owned dates, times, percentages and numbers use the active locale. Provider
  identifiers, URLs, credentials and protocol values use locale-independent
  formatting.
- User-visible `contentDescription`, TalkBack text and TV instructions are part
  of the same migration; decorative icons keep a null description.
- Speech-recognition intents receive the effective app language when supported.

## Migration order

The picker must not expose a half-translated application. Migration proceeds in
cohesive vertical slices:

1. Runtime foundation, resource generation and pure language-selection policy.
2. Primary navigation plus mobile/TV language settings UI.
3. Home, Live, movies, series, Search, details and episodes.
4. Player, audio/subtitle panels and playback errors.
5. Source onboarding/management, EPG and all settings flows.
6. Profiles, billing, legal, diagnostics, backup/export and system notifications.
7. Final hardcoded-string audit, translation parity gate and public picker
   activation.

The English base and Greek translation for each slice land together. No slice is
considered migrated when visible copy, errors or accessibility text still bypass
resources.

## Verification gates

- Every default translatable key exists in `values-el`; intentional brand and
  protocol constants are marked `translatable="false"`.
- Static audit rejects new user-facing string literals in migrated Compose files.
- Unit tests protect language-tag mapping, system fallback and resource-key
  parity.
- Instrumentation checks activity recreation and persistence on API 26–32 and
  system App Languages synchronization on API 33+.
- Phone QA covers rotation, process death, large text and both system languages.
- TV QA covers DPAD focus before/after activity recreation, Back restoration,
  overscan and long-label clipping.
- No build/runtime success is claimed until the owner completes the normal
  Android Studio build and device checks.
