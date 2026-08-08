# v1.40.6 validation report

## Scope

Validated the shared mobile bottom navigation, Android system-navigation
insets, Settings refresh action, fresh provider loading and source-scoped
progress reporting.

## Checks performed

### Kotlin syntax

- Parsed all **134 production Kotlin files** with the Kotlin PSI syntax checker.
- Result: **0 syntax errors**.

### Provider/progress contracts

Compiled the actual production sources below against controlled Android/JSON/
HTTP stubs to check Kotlin cross-file signatures and unresolved references:

- `Models.kt`
- `SourceLoadProgress.kt`
- `M3uParser.kt`
- `XtreamClient.kt`
- `StalkerClient.kt`
- `Repository.kt`

Result: **PASS**.

This check caught and allowed correction of an invalid Stalker page-progress
loop variable during development.

### HTTP progress implementation

Compiled the actual `Http.kt`, including `getWithProgress`, against controlled
OkHttp/Okio stubs.

Result: **PASS**.

### M3U runtime smoke test

Executed the actual M3U parser with a mixed Live/VOD/Series sample and a
progress callback.

Result:

```text
M3U progress runtime PASS: (7, 7) / [live, vod, series]
```

### Navigation and policy audit

- No `import androidx.compose.foundation.layout.weight` remains.
- No `RowColumnParentData` reference remains.
- The shared navigation uses `WindowInsets.navigationBars` plus a fixed 10 dp
  safety gap.
- Content padding is applied to Home, generic catalogs, Live, Search, Library,
  Settings, Details and Mobile EPG.
- The source refresh UI is present on Mobile and TV Settings.
- HTTP catalog requests retain `Cache-Control: no-cache, no-store, max-age=0`
  and `Pragma: no-cache`.
- ZIP integrity is checked after packaging.

## Progress accuracy

A determinate byte percentage is possible only when the provider returns a
valid `Content-Length`. For chunked or otherwise unknown-length responses, the
UI deliberately shows an indeterminate transfer indicator. Once measurable
parsing/page-processing begins, the UI reports the corresponding real stage or
percentage.

## Build limitation

A full Android `clean assembleDebug`, lint, instrumentation test and device
run were **not** executed in this environment because the project does not
contain a functional `gradle-wrapper.jar` and the environment has no Android
SDK/dependency installation. The checks above do not replace a real Android
build on the user's build machine.
