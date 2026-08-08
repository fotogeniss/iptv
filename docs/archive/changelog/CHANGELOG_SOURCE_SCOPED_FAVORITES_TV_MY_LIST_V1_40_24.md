# v1.40.24 — Source-scoped favorites and Android TV “My List”

## Data integrity

- Favorites are now persisted as `(profile, sourceId, itemKey)` records instead
  of unscoped item keys.
- Identical stream IDs or URLs from different providers can coexist without
  opening through the wrong provider account.
- Stored favorites include an encrypted metadata snapshot so Library and TV Home
  can reconstruct cards after process death or source switching.
- Fresh provider catalogs reconcile title, artwork and playback metadata without
  changing the favorite's source ownership or original add time.
- Source deletion removes only that source's favorites.

## Migration

- Existing unscoped favorites remain readable for compatibility.
- Migration is exact-match only and requires provider playback transport; series
  containers and identifier-only guesses are deliberately not auto-migrated.
- Migration is incremental as each source catalog is loaded and is idempotent.
- Ambiguous legacy rows remain in the legacy store rather than being assigned by
  name or loose metadata matching.

## Android TV Home channel

- Adds an opt-in **“Κανάλι Η λίστα μου”** setting on Android TV.
- Publishes up to 20 playable source-scoped favorites as a dedicated preview
  channel, newest first.
- Filters deleted sources, parental-lock groups and non-playable series
  containers.
- Uses opaque random deep-link tokens. Provider URLs, credentials and Stalker
  commands remain in Android Keystore-backed encrypted storage.
- Launcher playback revalidates active profile, source existence, current
  favorite membership and parental locks before resolving a fresh playback URL.
- The launcher-owned browsable state is respected. User-removed cards are
  suppressed until the favorite is explicitly added again.
- Handles Android TV program initialization and both Watch Next and preview
  program removal broadcasts.

## Sync behavior

The existing unique WorkManager reconciliation job now updates Play Next and
“My List” independently after favorite changes, source deletion, profile changes,
parental changes, playback and TV Provider initialization.

## Version

- `versionName`: `1.40.24`
- `versionCode`: `68`
