# v1.40.13 Validation Report

## Scope

Provider request cancellation and serialization only. No visual redesign, parser format change, playback-engine change or persistent catalog cache was added.

## Static validation performed

- Parsed all production and JVM-test Kotlin files through Kotlin PSI.
- Result: `SYNTAX_ERRORS=0`.
- Verified all Xtream and Stalker provider HTTP calls use the dedicated provider OkHttp client.
- Verified the M3U catalog download uses the dedicated provider client.
- Verified catalog/category jobs are owned by `catalogLoadJob` and series-detail work by `seriesLoadJob`.
- Verified high-level provider loading is guarded by `providerLoadMutex`.
- Verified source/tab/refresh cancellation calls `Http.cancelProviderRequests()`.
- Verified no `ChannelDiskCache.save/put` or `SeriesDiskCache.save/put` path was introduced.
- Verified version `1.40.13 / 57`.
- Verified final ZIP integrity with `unzip -t`.

## Behavioral contracts implemented

1. A newer content-tab request cancels the previous catalog job.
2. Refresh cancels the previous job, invalidates the session snapshot and starts a fresh request.
3. Source switching cancels the old source before dropping the old Stalker client reference.
4. Old progress callbacks and late results are rejected by the existing generation check.
5. Only one high-level provider catalog operation owns the provider mutex at a time.
6. Stalker internal page concurrency remains bounded to its existing six-thread pool.
7. Subtitle/TMDB/general HTTP traffic is not cancelled by catalog cancellation.

## Full Android build status

A real `:app:compileDebugKotlin` was attempted. The Gradle bootstrap stopped before Android compilation because this environment cannot resolve/download `gradle-8.9-bin.zip` from `services.gradle.org` (`UnresolvedAddressException`). Therefore this report does not claim a successful Android Gradle build.

The user should run:

```bash
./scripts/verify-debug.sh
```

or on Windows:

```bat
scripts\verify-debug.bat
```

and send the first compiler/build error if one appears.
