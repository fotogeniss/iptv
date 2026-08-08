# Network security decision — v1.40.43

## Decision

`android:usesCleartextTraffic="true"` remains enabled in this release as an explicit compatibility exception.

Ultimate Playlist Loader accepts arbitrary user-configured M3U, Xtream and Stalker endpoints. A material portion of IPTV deployments still exposes only plain HTTP. Replacing the global flag with a fixed domain allow-list would break those user-supplied hosts because their domains are not known at build time.

## Compensating controls

- Provider credentials and resolved stream URLs are not written to application logs.
- Exported TV Home playback routes carry opaque UUID tokens, never provider credentials or media URLs.
- TV Home routes accept only the exact `upl://play-next/<uuid>` and `upl://my-list/<uuid>` shapes.
- External EPG URLs accept only HTTP/HTTPS and are stored per source after successful validation.
- Internal player and Multiview launches use process-private state or typed intent contracts.

## Follow-up

A later security UX phase should visibly label HTTP sources as unencrypted and offer an opt-in HTTPS-only policy. The default cannot be changed until migration behavior for existing HTTP playlists is designed and device-tested.
