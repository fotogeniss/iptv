# v1.40.42 — Controlled Player Session Refactor

## Scope

This release performs one controlled architecture extraction only: player session and channel-transition ownership. It does not move ExoPlayer/VLC lifecycle ownership and does not redesign the player UI.

## Production changes

- Added `PlayerSessionController`, `PlayerSessionState` and the `PlayerSessionQueue` boundary.
- Added `PlaybackQueueSessionAdapter` so the existing global queue remains compatible while PlayerActivity stops owning duplicate session fields.
- Moved the following state out of `PlayerActivity`:
  - playback URL,
  - current title/type/EPG identity,
  - source identity,
  - resume-position key,
  - async channel-load generation,
  - committed versus pending queue position.
- Removed the legacy `loadToken` and mutable duplicate `streamUrl/title/kind/tvgId/posKey/sourceId` fields.

## Behavior fixes discovered by the extraction

- A failed rapid zap now returns to the last channel that actually started playing, not to an unresolved intermediate selection.
- A stale provider response cannot replace a newer channel selection.
- Resume position is saved under the committed item while another URL is still resolving.
- A failed channel resolve does not restart the stream that was already playing.
- Movies/episodes are added to recents only after their playback URL resolves successfully.
- Channel-panel highlight and player chrome are restored together after a failed transition.

## Compatibility guardrails

- Frozen v1.40.41 public-member snapshots for `PlayerActivity` and `MainViewModel`.
- Repeatable `scripts/compatibility_contracts.py` audit.
- Existing architecture/focus/routing audit extended with session-ownership contracts.
- No public PlayerActivity or MainViewModel member removed.
- No database, Manifest, provider, playback-format or navigation contract changed.
