# v1.40.19 — Security hardening forward-port

This release applies the security and release hardening work to the upgraded
v1.40.18 codebase without reverting the UI-state slicing, direct-player launch,
TV detail-flow or export-state build fixes introduced in v1.40.14–v1.40.18.

## Security

- Added Android Keystore-backed AES-GCM storage for provider playlists and credentials.
- Moved TMDB and OpenSubtitles credentials into encrypted storage.
- Moved favorites and history snapshots into encrypted storage because their stream URLs may contain provider credentials.
- Replaced plaintext parental PIN storage with salted PBKDF2-HMAC-SHA256 verification.
- Added automatic migration from the previous plaintext SharedPreferences format.
- Replaced raw provider identities in preference keys with stable SHA-256-derived IDs.
- Added cleanup of obsolete keys that exposed URLs, usernames or MAC addresses.
- Disabled Android system backup and explicitly excluded every app-data domain from cloud backup and device transfer.

## Portable backup

- Upgraded portable backups from plaintext v1 JSON to a password-protected v2 envelope.
- Encrypted the complete payload with AES-256-GCM authenticated encryption.
- Derived the backup key using PBKDF2-HMAC-SHA256, a random salt and 210,000 iterations.
- Kept the backup password entirely user-managed; the application never stores it.
- Rejected wrong passwords and modified/corrupted encrypted payloads.
- Preserved import support for legacy plaintext v1 backups, followed by immediate local migration.
- Adapted the password flow to the v1.40.18 `SettingsUiState` route boundary.

## Playback and release

- Enabled Picture-in-Picture for `PlayerActivity` in the manifest.
- Added `singleTop`, resizable activity support and the complete player configuration-change set.
- Enabled R8 code shrinking and resource shrinking for release builds.
- Added conservative keep rules for libVLC and dynamically selected Media3 components.

## Verification and CI

- Added unit coverage for salted PIN hashes and portable backup encryption.
- Added GitHub Actions CI for unit tests, lint, debug APK and release APK.
- Preserved the v1.40.18 sliced state streams, including `settingsState` and `exportState`.
- Bumped the application to `1.40.19` (`versionCode 63`).
