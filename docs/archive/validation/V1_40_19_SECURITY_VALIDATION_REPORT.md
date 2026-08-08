# v1.40.19 Security Validation Report

## Scope

Forward-port of the v1.40.14 security work onto the user-provided v1.40.18
export-state build-fix codebase. The review covered persistence, migration,
backup/restore, parental PIN storage, PiP configuration, release shrinking,
CI wiring and preservation of the v1.40.18 state-slicing changes.

## Merge result

- Three-way merge base: original v1.40.13 codebase.
- Upgrade side: user-provided v1.40.18 codebase.
- Security side: previously validated security-hardening changes.
- Kotlin/XML merge markers remaining: **0**.
- The only direct textual conflict was the application version; it was resolved
  as `versionName 1.40.19` and `versionCode 63`.

## Validation completed

- Kotlin PSI parse of `app/src/main`: **142 files, 0 syntax errors**.
- Kotlin PSI parse of `app/src/test`: **21 files, 0 syntax errors**.
- Targeted semantic Kotlin compilation of `Backup`, `PlaylistStore`,
  `SecureStorage`, `PinHasher`, `PortableBackupCrypto`, `TmdbClient`, models and
  identity code with Android/JSON contract stubs: **passed**.
- XML parse of `AndroidManifest.xml`, backup rules and Android 12+ data extraction rules: **passed**.
- YAML parse of `.github/workflows/android-ci.yml`: **passed**.
- Crypto smoke tests:
  - parental PIN hash differs from plaintext: **passed**;
  - correct PIN verifies and incorrect PIN fails: **passed**;
  - encrypted backup payload round-trips: **passed**;
  - wrong backup password is rejected: **passed**;
  - passwords shorter than six characters are rejected: **passed**.
- Security and call-site audit:
  - Keystore playlist persistence: **passed**;
  - password-aware backup APIs and Compose callbacks: **passed**;
  - hashed source identity in `MainViewModel`: **passed**;
  - v1.40.18 `settingsState` preserved: **passed**;
  - v1.40.18 `exportState` preserved: **passed**;
  - PiP and backup exclusions present: **passed**.

## Full Gradle validation status

The local container could not bootstrap Gradle 8.9 because the distribution was
not pre-cached and outbound network/DNS access was unavailable. The exact failure
is saved in `gradle_compile_attempt_v40_19.txt`.

The included GitHub Actions workflow runs:

```text
:app:testDebugUnitTest
:app:lintDebug
:app:assembleDebug
:app:assembleRelease
```

The release APK should be treated as pending until that CI workflow completes,
particularly because R8 and resource shrinking are now enabled.

## Migration behavior

On first startup after updating from the supplied v1.40.18 build:

1. plaintext playlists and provider credentials move into Android Keystore-backed storage;
2. TMDB and OpenSubtitles credentials move into encrypted storage;
3. favorites and history snapshots containing transport URLs are re-encrypted;
4. an existing plaintext parental PIN is replaced by a salted PBKDF2 hash;
5. source-specific preference namespaces move from raw provider identities to hashes;
6. obsolete preference keys exposing URLs, usernames or MAC addresses are deleted.

Valid existing playlists should not require manual re-entry.
