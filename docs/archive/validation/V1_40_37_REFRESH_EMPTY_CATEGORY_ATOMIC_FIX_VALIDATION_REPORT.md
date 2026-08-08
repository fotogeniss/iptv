# v1.40.37 Empty-category refresh atomicity validation report

## Scope

Small release-hardening change on top of v1.40.36. No new UI, provider, playback, Multiview or persistence-schema surface was added.

## Fixed behavior

The **Refresh + choose new groups** path normally downloads a fresh category list and opens the picker. Some providers or sections can legitimately return an empty category list. In v1.40.36 that fallback called the all-items loader without preserving the refresh origin, so it could use normal first-load persistence semantics.

v1.40.37 keeps the operation transactional in that fallback:

1. An empty fresh category list falls back to loading all items, as before.
2. The loader receives an explicit refresh-origin override.
3. The previous source/section choice remains saved while fetching and normalizing.
4. `all groups` is committed only after the refreshed catalog has been cached successfully.
5. Failure or cancellation leaves both the old visible catalog and old saved selection unchanged.

Normal first load and explicit category-picker refresh behavior remain unchanged.

## Validation performed

- `CatalogRefreshPolicy.kt` semantic Kotlin compilation: passed.
- Focused executable policy assertions: 8 passed.
- Actual `CatalogRefreshPolicyTest.kt` semantic compilation with minimal JUnit API stubs: passed.
- Source contracts:
  - empty-category branch forwards `refreshSelectionOverride = openPickerDirectly`;
  - the override defaults to false for all existing call sites;
  - transactional-origin policy combines explicit refresh picker and direct refresh fallback;
  - pre-load persistence remains guarded by `!refreshSelection`;
  - success commit occurs after `cacheCatalog(...)` and before the visible state swap;
  - failure path contains no selection persistence;
  - empty-category fallback contains no direct persistence.
- Kotlin parser/signature-risk scan of `MainViewModel.kt`: no syntax-like or changed-signature diagnostics found. Android/Compose dependencies were intentionally absent, so this is not an Android semantic compile.
- Manifest/resource XML parsing: 24 files, 0 errors.
- Version contract: 1.40.37 / versionCode 81.

## Gradle result

`:app:compileDebugKotlin` was attempted. The Gradle wrapper stopped before project compilation while trying to download Gradle 8.9 because `services.gradle.org` could not be resolved (`java.net.ConnectException` / `UnresolvedAddressException`).

This report does not claim a complete Android compilation, lint run, APK build, Gradle unit-test run or device test.
