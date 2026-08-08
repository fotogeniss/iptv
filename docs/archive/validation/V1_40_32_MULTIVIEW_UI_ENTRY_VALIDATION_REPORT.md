# v1.40.32 Multiview TV UI Entry — Validation Report

## Result

The production Multiview flow is now reachable from the Android TV Live TV rail.

## User flow

1. Focus a Live TV channel card.
2. Long-press **OK / DPAD Center** to arm that channel as pane 1.
3. Press **OK** on a different channel to launch Multiview.
4. Press **Back** while pane 2 is being selected to cancel the selection without leaving Live TV.

A compact instruction banner is shown on the TV Live screen. The armed first channel receives a visible accent border and a `1` badge.

## Production changes

- `app/src/main/java/com/prelude/iptv/ui/PremiumLiveTvScreen.kt`
  - owns the two-step selection state
  - intercepts Back only while pane 2 is being selected
  - keeps the mobile Live TV path unchanged
- `app/src/main/java/com/prelude/iptv/ui/tv/live/TvLiveRail.kt`
  - adds explicit hardware-key short/long press handling for DPAD Center / Enter
  - suppresses the trailing short click after a long press
  - cancels pending long-press work when focus leaves the card
  - displays the pane-1 marker
- `app/src/main/java/com/prelude/iptv/ui/tv/live/TvPremiumLiveScreen.kt`
  - displays the Multiview instruction/selection state
- `app/src/main/java/com/prelude/iptv/ui/route/BrowseRoute.kt`
  - connects the Live TV screen to the existing secure `openMultiview(...)` launcher
- `app/src/main/java/com/prelude/iptv/ui/MultiviewSelectionPolicy.kt`
  - pure selection decision policy
- `app/src/test/java/com/prelude/iptv/ui/MultiviewSelectionPolicyTest.kt`
  - focused policy tests
- `app/build.gradle.kts`
  - `versionName = "1.40.32"`
  - `versionCode = 76`

## Security contract

The existing secure launch path is reused without modification. The Activity Intent carries only `MultiviewActivity.EXTRA_LAUNCH_TOKEN`. Stream URLs remain in the process-private, one-shot `MultiviewLaunchStore` and are not added to Intent extras.

## Checks executed

### Focused Kotlin semantic compile — PASS

The following real production files were compiled with `kotlinc` against focused Android/Compose API stubs and kotlinx-coroutines core:

- `MultiviewSelectionPolicy.kt`
- `PremiumLiveTvScreen.kt`
- `TvPremiumLiveScreen.kt`
- `TvLiveRail.kt`

The output JAR was emitted successfully.

Evidence:

- `focused_multiview_ui_semantic_compile_v1_40_32.txt`
- `focused_multiview_ui_semantic_result_v1_40_32.txt`

### Focused selection assertions — PASS

Assertions passed: 3

- no armed primary opens normal single-channel playback
- selecting the same channel keeps pane 1 armed
- selecting a different channel produces an ordered primary/secondary launch pair

Evidence: `focused_multiview_selection_policy_v1_40_32.txt`

### UI/route/security contract audit — PASS

Confirmed:

- Live TV route supplies `onMultiview`
- `openMultiview(...)` is called with primary and secondary channels
- explicit DPAD Center / Enter handling exists
- Back cancels selection state
- only the opaque launch token is written to the Multiview Intent

Evidence: `multiview_ui_contracts_v1_40_32.txt`

### XML parse — PASS

- XML files parsed: 23
- parse errors: 0

Evidence: `xml_validation_v1_40_32.txt`

### Version contract — PASS

- versionName: `1.40.32`
- versionCode: `76`

### Full Gradle compile — NOT COMPLETED IN THIS ENVIRONMENT

Attempted command:

```text
./gradlew :app:compileDebugKotlin --no-daemon --stacktrace
```

The Gradle wrapper stopped before Gradle startup because Gradle 8.9 is not cached and the environment cannot resolve/connect to `services.gradle.org`. No full Android/Kotlin Gradle compilation, lint, APK build, or Gradle JUnit result is claimed.

Evidence: `gradle_compile_attempt_v1_40_32.txt`

## Scope control

No provider, playback, mobile UI, or unrelated architecture refactor was performed. The change is limited to the TV Live rail entry flow, its small selection policy/test, route wiring, visible selection feedback, and version bump.
