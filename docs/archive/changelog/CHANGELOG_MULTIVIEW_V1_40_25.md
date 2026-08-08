# v1.40.25 — Multiview hardening

- Added process-private, one-shot Multiview launch tokens; provider URLs and credentials are not placed in Multiview Intent extras.
- Added per-source provider resolution serialization for session-sensitive Stalker/Xtream launches.
- Added isolated two-pane Media3 playback with the inactive pane's audio track type disabled.
- Added independent secondary-stream failure handling so the primary keeps playing.
- Added lifecycle release of both players on background/Home.
- Added DPAD left/right pane selection, audio ownership transfer, Back/Exit handling and duplicate-launch suppression.
- Added focused token/policy tests and Manifest/configuration contracts.
- Bumped version to 1.40.25 (versionCode 69).
