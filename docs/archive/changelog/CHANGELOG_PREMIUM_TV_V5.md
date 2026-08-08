# Premium TV v5 — Details, Episodes & Resume

## User-facing changes

- Movie details now show a real **Continue** action with percentage and remaining time.
- A watched movie can be restarted from the beginning without manually clearing app data.
- Series details now include a TV-first horizontal episode rail with:
  - season selector,
  - stable DPAD focus,
  - episode artwork,
  - per-episode progress,
  - remaining-time labels.
- Series details automatically select the season containing the most recent unfinished episode.
- The primary series action resumes the latest unfinished episode or starts episode 1.
- Details metadata remains in a safe left column while episode rails use the full TV width.
- Hero playback displays resume progress when the featured movie is unfinished.
- Share is now a real Android share action; the previous dead feedback action was removed.

## Playback correctness

- Opening an episode now passes the complete ordered episode list to the player.
- Previous/next and autoplay-next therefore move through episodes instead of the parent series catalog.
- Episode queues continue across season boundaries.
- Episodes reached through next/autoplay are added to recents and can resume correctly.

## Persistence and architecture

- Added `WatchProgressPolicy` as the single source of truth for resumable progress.
- Added `PlaybackQueuePolicy` as a tested pure queue-selection policy.
- Recent items now preserve cast, director, genre and duration metadata.
- Added explicit recent removal for restart/continue flows.
- Added JVM tests for watch progress and playback queue selection.
