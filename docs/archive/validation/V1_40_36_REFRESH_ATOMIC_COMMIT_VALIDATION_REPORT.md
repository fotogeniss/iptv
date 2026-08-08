# v1.40.36 Refresh atomic-commit validation report

## Scope

Release hardening of the v1.40.35 two-mode refresh flow. No new feature surface was added.

## Fixed behavior

### Transactional group-selection persistence

In v1.40.35, confirming a new set of groups wrote the new selection to memory/preferences before the provider fetch completed. If the fetch then failed or was cancelled, the old catalog remained visible but the saved selection had already changed.

v1.40.36 changes the refresh-picker path to:

1. Keep the old saved selection while loading.
2. Fetch, normalize and cache the refreshed catalog.
3. Commit the new group selection only after those steps succeed.
4. Leave the previous selection untouched on failure or cancellation.

Normal first-time category loading retains its previous persistence behavior.

### Visible group continuity

Both refresh modes now keep the active visible group filter when that group is present in the refreshed catalog. If it no longer exists, the UI falls back to `Όλα τα κανάλια`.

## Validation performed

- `CatalogRefreshPolicy.kt` semantic Kotlin compilation: passed.
- Focused executable assertions: 5 passed.
- Actual `CatalogRefreshPolicyTest.kt` semantic compilation with minimal JUnit API stubs: passed.
- Atomic commit source-contract checks:
  - refresh-picker selection is not persisted before launch;
  - success commit occurs after catalog caching;
  - commit occurs before the visible state swap;
  - catch/failure path does not persist the new selection;
  - both refresh paths use visible-group restoration policy.
- Kotlin parser-risk scan of `MainViewModel.kt`: no syntax-like diagnostics (`expecting`, redeclaration, missing local symbol) were found. Android/Compose dependencies were intentionally absent, so this is not an Android semantic compile.
- Manifest/resource XML parsing: 24 files, 0 errors.
- Version contract: 1.40.36 / versionCode 80.
- Patch whitespace check: no reported whitespace errors.

## Gradle result

`:app:compileDebugKotlin` was attempted. The wrapper stopped before project compilation while trying to download Gradle 8.9 because `services.gradle.org` could not be resolved (`java.net.ConnectException` / `UnresolvedAddressException`).

This report does not claim a complete Android compilation, lint run, APK build, Gradle unit-test run or device test.
