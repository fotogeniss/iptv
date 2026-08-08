# v1.40.25 Multiview validation report

## Implemented scope

- Secure one-shot launch hand-off using an opaque internal token.
- No provider URL or credential in `MultiviewActivity` Intent extras.
- Real audio isolation by disabling Media3's audio track type on the inactive player.
- Per-source serialized provider URL resolution, covering session-sensitive Stalker/Xtream flows.
- Independent player failure handling: secondary playback failure leaves primary playback untouched.
- `onStop`/`onDestroy` release both players; Home/background exits Multiview.
- DPAD pane selection, center/Enter audio transfer, Back/Escape exit and launch-in-flight suppression.
- Version bump to `1.40.25` / `69`.

## Automated checks completed

| Check | Result |
|---|---|
| Pure Kotlin semantic compile (`MultiviewLaunchStore`, `MultiviewPolicy`) | Passed |
| Focused executable assertions | 8 passed |
| Android XML parse | Passed, 0 errors |
| Manifest contract | Passed: activity is non-exported |
| Secure launch audit | Passed: token is the only Multiview extra |
| Audio isolation contract | Passed |
| Lifecycle/failure contract audit | Passed |
| Version contract | Passed: 1.40.25 / 69 |
| ZIP integrity | Passed |

## Gradle/Android compiler limitation

`./gradlew --offline :app:testDebugUnitTest :app:compileDebugKotlin --stacktrace` was attempted. The wrapper could not start Gradle because Gradle 8.9 was not cached and the environment could not resolve `services.gradle.org`. Therefore no full Android compile, JUnit Gradle task, lint, APK build or device test is claimed.

## Required release-gate checks outside this environment

Run `testDebugUnitTest`, `lintDebug`, `assembleDebug` (and release build as applicable), then validate two simultaneous provider streams on Android TV hardware, including Home/background, secondary failure, DPAD focus/audio transfer and rapid repeated launch input.
