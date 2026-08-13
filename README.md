# Prelude+ IPTV Player

Native IPTV media player for Android phones, tablets and Android TV, built with
Kotlin, Jetpack Compose, Media3/ExoPlayer and a libVLC fallback.

> Current app version: **1.64.0** (`versionCode 136`)

Prelude+ is a player only. It does not provide channels, subscriptions or media.
Users connect sources they are authorized to use.

## Current product status

The application is an advanced beta/release candidate. Core playback, catalog,
EPG and source-management flows are implemented. Google Play Billing is wired for
the non-consumable Premium product with device-side purchase verification, while
production signing, final store compliance material and broad physical-device
validation remain release gates. The product has no publisher backend or
account/cloud synchronization; profiles and playback state remain local.

Without a verified Play purchase the premium policy defaults to `FREE`. Purchases
reported as pending never unlock Premium. See
[the Play Billing setup guide](docs/PLAY_BILLING_SETUP.md) for the Play Console and
testing steps that cannot be completed from source code alone.

## Features

### Sources and catalogs

- M3U URLs and local playlists.
- Xtream Codes live TV, movies, series, seasons and episodes.
- Stalker/MAC portals with handshake and stream-link resolution.
- Multiple saved sources, source editing, deletion and controlled refresh.
- Guided mobile/TV source onboarding with plain-language choices, smart mobile
  credential detection, field-level validation and provider verification before
  a source can be saved.
- Large-catalog normalization, session cache and progressive loading.
- Search, favorites, watch history and continue watching.
- Five direct mobile/TV content destinations: Home, Live, movies, series and
  automatic Search; library, EPG and source management remain inside their
  owning Home, Live and Settings screens.
- User-controlled Home and category visibility/order on mobile.

### Live TV and EPG

- Category-first mobile and Android TV browsing.
- List/grid channel layouts with list as the mobile default.
- One-action Live playback on mobile and one-OK playback on Android TV.
- XMLTV/XMLTV.GZ, Xtream and Stalker/MAC EPG sources.
- Now/next information under channels and full EPG views.
- Catch-up URL support where the provider exposes it.
- Directional first-frame live-channel transitions adapted separately for mobile
  and Android TV without introducing another player or video surface.
- Android TV Home recommendations, Play Next and My List integration.

### Movies and series

- TMDB posters, backdrops, ratings, genres, cast and localized descriptions.
- TMDB episode stills, titles and per-episode summaries.
- Seasons, episode progress, resume playback and automatic next episode.
- Related movie/series rails and provider quality hints.
- Expandable descriptions and compact nested episode scrolling on mobile.

### Player

- Media3/ExoPlayer primary engine and libVLC compatibility fallback.
- HLS, transport streams and provider VOD playback.
- Audio/video/subtitle track selection.
- Embedded subtitles plus OpenSubtitles automatic and manual search.
- Global subtitle size, bold and background preferences.
- Aspect-ratio modes and detected playback-quality indicator.
- Picture-in-picture, mini player, sticky mobile player and multiview.
- Resume position, ±10-second seek, channel stepping and next-episode offer.
- Optional frame-rate matching on supported displays.

### Data and security

- Provider credentials stored with AES-GCM keys held by Android Keystore.
- Password-protected portable backups using AES-256-GCM.
- Source-scoped favorites/history to prevent cross-playlist collisions.
- No Android system backup of private application data.
- Cleartext HTTP remains enabled because many user-supplied IPTV servers do not
  support HTTPS; this is an explicit compatibility trade-off.
- No publisher backend, account sync, ads or analytics SDK is active. An
  opt-in-only Firebase Crashlytics integration is present for stability reports;
  it remains disconnected until the publisher supplies `app/google-services.json`.

### Privacy and store compliance

- The code-audited [privacy policy draft](docs/PRIVACY_POLICY.md),
  [terms draft](docs/TERMS_OF_USE.md) and
  [Play Data safety worksheet](docs/PLAY_DATA_SAFETY.md) are maintained with the
  application.
- A publisher legal name, privacy contact, public non-PDF privacy-policy URL and
  final Play Console declarations are still required before publication.
- Monetized use must confirm the applicable TMDB and OpenSubtitles commercial
  terms before release.

## Architecture

The app is a single Android application module with explicit packages:

```text
app/src/main/java/com/prelude/iptv/
├── billing/      Play Billing client, entitlement reduction and premium policy
├── category/     category layout models and rules
├── data/         storage, caches, EPG, TMDB, subtitles and policies
├── diagnostics/  opt-in crash privacy, redaction and Crashlytics boundary
├── net/          HTTP transport and cancellation boundaries
├── player/       playback engines and pure player policies
├── source/       M3U, Xtream and Stalker provider clients
├── tvhome/       Android TV Home channels and Play Next
└── ui/
    ├── coordinator/ asynchronous catalog/EPG/source coordinators
    ├── mobile/      phone/tablet compositions
    ├── player/      adaptive playback UI
    ├── route/       application route wiring
    └── tv/          D-pad/focus-oriented TV compositions
```

Mobile and TV share data and policy layers but use separate screen compositions
where touch and D-pad behavior differ. Large legacy files are being split
incrementally; see [the architecture plan](docs/ARCHITECTURE_REFACTOR_PLAN.md).

## Toolchain

- Android compile/target SDK 35, minimum SDK 26.
- Java 17 and Kotlin with Jetpack Compose.
- Media3 1.7.1, libVLC 3.6.0, OkHttp 4.12.0 and Coil 2.7.0.
- Google Play Billing Library 9.1.0.
- Firebase Crashlytics (opt-in, no Analytics; publisher configuration required).
- ABI-specific APKs for `arm64-v8a`, `armeabi-v7a` and `x86_64`, plus a universal
  APK.

## Local configuration

Open the project in Android Studio and ensure `local.properties` points to an
installed Android SDK. TMDB and OpenSubtitles credentials are entered in the app
settings; they must not be committed to source control.

## Verification

Common commands:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

For owner testing with every Premium capability unlocked, use the separate `qa`
variant (`./gradlew :app:assembleQa`). It installs as **Prelude+ QA** beside the
public application. See [Owner QA build](docs/OWNER_QA_BUILD.md). The public
`release` variant never enables this override.

Static project contracts:

```bash
python scripts/documentation_contracts.py
python scripts/compatibility_contracts.py
python scripts/architecture_audit.py
python scripts/deep_validation_audit.py
python scripts/risk_inventory.py
```

CI runs the static contracts, JVM tests, Android lint and debug/release builds.
Physical-device checks remain required for playback, TV focus, PiP, frame-rate
matching, Stalker portals and low-memory large-catalog behavior. Follow the
[device validation runbook](docs/DEVICE_VALIDATION_RUNBOOK.md) and the mandatory
[device QA matrix](docs/DEVICE_QA_MATRIX.md). Device runs save launch, logcat and
memory evidence under the ignored `validation/device-runs/` directory.

## Release signing

The checked-in release configuration currently uses the debug signing key so a
local release APK can be installed. Do not publish that artifact. A Play Store
release must use a private upload key/Play App Signing configuration supplied
outside the repository.

## Documentation and release discipline

The authoritative version is declared only in `app/build.gradle.kts`. The README
and [CHANGELOG](CHANGELOG.md) mirror it and are checked automatically by
`scripts/documentation_contracts.py` in CI.

For every user-visible change:

1. Add one concise entry under `Unreleased` in `CHANGELOG.md`.
2. Update this README only when capabilities, setup, architecture, limitations or
   release requirements changed.
3. When changing `versionName`/`versionCode`, create the matching release heading
   in `CHANGELOG.md` and update the version line near the top of this README.
4. Put generated reports in `validation/` or `docs/archive/validation/`, never in
   the repository root.
5. Let CI enforce the version/documentation contract.

Historical implementation notes and generated validation output are preserved in
[`docs/archive`](docs/archive); they are not current product documentation.
The exact update matrix is in the
[documentation maintenance checklist](docs/MAINTENANCE.md).

## Legal

Prelude+ does not host, sell or bundle television channels or video content. The
user is responsible for source authorization and compliance with applicable laws
and provider terms.
