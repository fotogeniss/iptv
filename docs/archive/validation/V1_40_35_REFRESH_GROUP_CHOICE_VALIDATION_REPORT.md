# v1.40.35 Refresh/group-selection validation report

## Requested behavior

Every refresh now presents two explicit actions:

1. **Refresh current selection** — reload only the groups already saved for the active source and section.
2. **Refresh + choose new groups** — download the provider's current group list and open the group picker.

The behavior is section-scoped for `live`, `vod` and `series`, and is exposed from both Browse and Settings on Android TV and mobile.

## Implementation

### Existing-selection refresh

- Reads the existing selection from the established `source-id:content-type` key.
- Invalidates transient provider/session data.
- Reloads the selected group IDs without fetching or changing the selection.
- Keeps the visible catalog in place until the new catalog succeeds.
- If no previous selection exists, safely falls through to fresh group selection.

### Refresh and choose groups

- Invalidates the provider session and session-only catalog data.
- Downloads a fresh category/group list.
- Opens the picker directly instead of adding another “all or choose” dialog.
- Preselects remembered IDs that still exist.
- Shows newly discovered groups as available and unselected when the prior choice was a subset.
- Drops removed provider IDs from the preselection.
- Persists the confirmed selection using the existing per-source/per-section storage.
- Cancelling leaves the previously visible catalog unchanged.

### TV and mobile

- Browse menu refresh opens the new choice.
- TV Settings refresh opens the same choice.
- Mobile Settings refresh opens the same choice.
- Back/Cancel closes the refresh choice or refresh picker without replacing the current catalog.

## Validation performed

- `CatalogRefreshPolicy` semantic Kotlin compilation: **passed**.
- Focused policy assertions: **3 passed**.
- Actual `CatalogRefreshPolicyTest.kt` semantic compilation with JUnit API stubs: **passed**.
- `UiStateSlices.kt` and refresh policy semantic compilation with model stubs: **passed**.
- Source contract checks for both refresh paths, persistence, preselection and TV/mobile wiring: **passed**.
- Kotlin parser-risk scan: **0 syntax-like diagnostics**. The raw compiler invocation intentionally lacked Android/Compose dependencies and therefore is not an Android semantic compile.
- Manifest/XML parsing: **24 files, 0 errors**.
- Version contract: **1.40.35 / versionCode 79**.

## Gradle result

`:app:compileDebugKotlin` was attempted. The Gradle wrapper stopped before project compilation because Gradle 8.9 was not cached and this environment could not resolve `services.gradle.org` (`java.net.ConnectException`).

Therefore this report does **not** claim a complete Android/Kotlin Gradle build, lint run, APK build, or device test.
