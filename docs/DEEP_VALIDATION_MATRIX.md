# v1.40.43 Deep validation matrix

| Area | Automated gate completed in this release | Physical/online gate still required |
|---|---|---|
| Public compatibility | Frozen v1.40.41 contracts: PlayerActivity 10/10 and MainViewModel 89/89 members preserved | Upgrade install from the signed production APK |
| Pure behavior | 179 JVM assertions across player input/session, catalog refresh/normalization, source deletion, TV Home, EPG, library, parsing and cancellation | Provider-specific behavior against real services |
| Playlist routing | Exhaustive deletion matrix for all valid sizes 1..100; duplicate-source ownership; inactive edit/delete preservation | Rapid source switching while real provider calls are in flight |
| Android TV focus | Static focus boundaries, visible dialog actions and five Compose instrumentation scenarios | Execute on Google TV/Android TV with multiple physical remotes |
| TV shortcuts | Focused tests for DPAD, media, channel, menu/info, captions and repeat behavior | Vendor-specific key-code matrix and long-press behavior |
| TV Home security | Strict canonical opaque routes and provider-row ownership checks | Launcher/provider integration on real Google TV devices |
| Structured cancellation | Shared ProviderCancellation policy; TMDB, subtitles, M3U probe, Xtream and Stalker fallbacks preserve cancellation | Network disconnect/background/source-switch soak |
| Large catalogs | 50,000 live rows and 20,000 series episodes in JVM stress fixtures | Memory, jank and scroll profiling on low-RAM TV hardware |
| Lifecycle | Player private Handler clears named and anonymous callbacks; provider jobs are generation guarded | Two-hour playback/zapping soak, PiP, screen off/on and process recreation |
| Storage | SAF null-stream crashes removed; source cleanup protects duplicate references | Real document providers, low-space and permission-revocation scenarios |
| XML/resources | Every manifest/resource XML parses successfully | Visual layout audit across TV/mobile resolutions and font scales |
| Security | No production `!!`, GlobalScope, runBlocking, Thread.sleep or process exits; credential-log scan; opaque TV routes | Proxy-based traffic inspection and HTTP-source warning UX |
| CI | Valid action majors and static gates before Gradle jobs | Execute workflow with internet, Android SDK and artifact retention |
| Build | Full clean/unit/lint/debug/release Gradle command attempted | Successful online Gradle 8.9 download, lint, R8 and APK install |

## Current gate result

- Static/compatibility/deep/risk gates: pass.
- Broad policy suite: 179 pass, 0 fail.
- Compose instrumentation tests: authored but not executed in this environment.
- Full Gradle build/lint/APK: blocked before Gradle startup by unavailable Gradle 8.9 distribution/network resolution.
- Global cleartext remains one documented compatibility warning because arbitrary user IPTV endpoints may be HTTP.
