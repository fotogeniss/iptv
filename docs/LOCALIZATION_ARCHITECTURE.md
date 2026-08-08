# Prelude+ localization architecture

Status: owner-approved design; runtime foundation, navigation/settings, Home,
Live TV, movie/series browsing and global Search are implemented behind the
QA/parity rollout gate.

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

English is the final unqualified resource set and ultimate fallback. During the
partial migration, however, the public `main` source set deliberately preserves
the existing Greek baseline. Matching English resources live only in the shared
`localizationQa` source set consumed by debug and QA builds. This prevents an
English-device release from showing an English navigation shell over a still
Greek application. At final parity, English moves to unqualified `values`, Greek
moves to `values-el`, and `unqualifiedResLocale` changes from `el` to `en`.

## Android runtime architecture

The implementation follows Android's public per-app language stack:

1. Add stable `androidx.appcompat:appcompat:1.7.1`.
2. Move all five Compose hosts from `ComponentActivity` to
   `AppCompatActivity`; use an AppCompat no-action-bar parent for both existing
   app themes.
3. Declare the current public baseline with `unqualifiedResLocale=el`. Change it
   to `en` and enable AGP `generateLocaleConfig` only at the final parity gate;
   enabling production language routing earlier would expose the incomplete
   translation.
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
└── values/                 # Greek public baseline during migration
    ├── strings_core.xml
    ├── strings_navigation.xml
    ├── strings_home.xml
    ├── strings_browse.xml
    ├── strings_live.xml
    ├── strings_catalog.xml
    ├── strings_search.xml
    ├── strings_details.xml
    ├── strings_player.xml
    ├── strings_sources.xml
    ├── strings_settings.xml
    ├── strings_errors.xml
    └── strings_accessibility.xml
app/src/localizationQa/res/
└── values-en/              # merged only into debug and QA
    └── matching English files
```

At the final parity gate, the English files become `main/res/values` and the
Greek files move together to `main/res/values-el`.

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

1. Runtime foundation, staged fallback and pure language-selection policy.
   **Implemented.**
2. Primary navigation plus mobile/TV language settings UI. **Implemented for
   QA builds; public visibility remains gated.**
3. Home. **Implemented for mobile and TV, including shared empty/recovery
   states; public visibility remains gated.**
4. Live TV. **Implemented for mobile and TV, including inline programme copy,
   Multiview failures and category/PIN actions; the full EPG screens remain a
   later slice.**
5. Movie and series browsing. **Implemented for mobile and TV; provider titles,
   categories and metadata remain data.**
6. Global Search. **Implemented for mobile and TV, including typed headings,
   categories and locale-aware TV keyboard selection.**
7. Movie/series details, seasons and episodes.
8. Player, audio/subtitle panels and playback errors.
9. Source onboarding/management, EPG and all settings flows.
10. Profiles, billing, legal, diagnostics, backup/export and system notifications.
11. Final hardcoded-string audit, translation parity gate and public picker
   activation.

The Greek baseline and QA English translation for each slice land together. No
slice is considered migrated when visible copy, errors or accessibility text
still bypass resources.

## Verification gates

- Every staged Greek key has a matching QA English key; after final inversion,
  every English default key must have a matching `values-el` key. Intentional
  brand and protocol constants are marked `translatable="false"`.
- `python scripts/localization_contracts.py` enforces current resource parity,
  the release-safe Greek baseline, host coverage, Android-free display-copy
  boundaries, migrated Home/Live/catalog/Search mappings and hardcoded-copy
  audits, and the closed public rollout gate.
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
