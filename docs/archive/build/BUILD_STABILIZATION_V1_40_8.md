# Build Stabilization — v1.40.8

## Purpose

This release begins Phase 13 in the required order: establish a repeatable real Android build before any further architecture refactor.

## Changes

- Replaced the placeholder `gradlew` script.
- Added a cross-platform `gradlew.bat`.
- Added a self-contained Java 17 Gradle bootstrap JAR at `gradle/wrapper/gradle-wrapper.jar`.
- Added the official SHA-256 for `gradle-8.9-bin.zip` to `gradle-wrapper.properties`.
- Added repeatable verification scripts:
  - `scripts/verify-debug.sh`
  - `scripts/verify-debug.bat`
- Version bumped to `1.40.8` / `versionCode 52`.

## Required environment

- JDK 17 or newer.
- Android SDK Platform 35.
- Android SDK Build Tools compatible with AGP 8.6.
- Internet access on the first wrapper/dependency run.

## Verification order

```text
1. gradlew --version
2. clean :app:compileDebugKotlin
3. :app:testDebugUnitTest
4. :app:lintDebug
5. :app:assembleDebug
```

Windows:

```bat
scripts\verify-debug.bat
```

macOS/Linux:

```sh
./scripts/verify-debug.sh
```

## Limitation of this environment

The wrapper launcher itself was compiled and locally smoke-tested. A real Android build still requires an Android SDK and access to Gradle/Maven repositories, which are not present in this execution environment.
