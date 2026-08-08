# v1.40.10 Validation Report

## Implemented step

Performance Stabilization Step 1: bounded session-only catalog cache.

## Static syntax validation

Command:

```bash
java -cp 'syntaxcheck.jar:<kotlin-compiler-libs>' SyntaxCheckKt app/src
```

Result:

```text
FILES=157 ERRORS=0
```

This is Kotlin PSI syntax validation. It does not resolve Android or Compose symbols.

## Session cache contract compilation

`SessionCatalogCache.kt` was compiled with `kotlinc` against a minimal `Channel` contract and executed with runtime assertions.

Result:

```text
SESSION_CACHE_CONTRACTS=PASS
```

Covered contracts:

- stable category signatures,
- source/type/category isolation,
- source-specific invalidation,
- LRU eviction,
- distinction between all categories and an empty category selection.

## Integration audit

Ten source checks passed:

- bounded catalog cache,
- bounded M3U payload cache,
- restore-before-network on tab changes,
- restore-before-network on source return,
- real manual refresh invalidation,
- both network load paths populate the cache,
- category invalidation occurs after confirmation,
- Stalker URL resolution occurs at Play time,
- no persistent catalog cache reads,
- old disk catalog files are still removed.

Result:

```text
CHECKS=10 FAILURES=0
```

## Gradle build status

A full Android Gradle build was not completed in this environment. The Gradle wrapper attempted to download Gradle 8.9, but outbound DNS/network access is unavailable:

```text
java.nio.channels.UnresolvedAddressException
```

Therefore this report does not claim that `compileDebugKotlin` or `assembleDebug` passed. The project must still be built in Android Studio or with an installed Android SDK and network-accessible Gradle/Maven dependencies.

## Device test checklist

After installation, verify:

1. Open Movies and wait for loading to finish.
2. Open Series, then return to Movies. Movies should appear immediately without progress/download.
3. Open Live, then return to Series. Series should appear immediately.
4. Press Refresh. The active source must show real progress and download again.
5. Close the app process and reopen it. The first tab load should download again, proving the catalog cache is not persistent.
6. On Stalker/MAC, play a channel after changing tabs or sources. The URL should resolve without requiring a catalog reload.
