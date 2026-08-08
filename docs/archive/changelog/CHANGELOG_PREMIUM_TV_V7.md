# Premium TV v7 — Premium Live TV

## New TV dashboard
- Dedicated Live TV experience instead of reusing the generic content list.
- Three filters: all channels, favorites and recently watched.
- Channel rows show logo, channel number, Now programme and live progress.
- Moving DPAD focus updates the selected channel details without starting playback.
- Pressing OK starts the selected channel.

## Now / Next panel
- Large channel identity area with neutral black scrims.
- Current programme title, description, start/end time and progress.
- Next programme and a compact three-programme schedule.
- Primary white Watch button, secondary EPG and Favorite actions.

## TV categories
- Provider groups remain available above the dashboard.
- Existing parental locking rules continue to apply.

## Engineering
- No second preview player is created, avoiding extra bandwidth and decoder usage.
- Added pure `LiveTvPolicy` for filters and EPG progress calculations.
- Added JVM tests for provider-order filtering and progress clamping.
- App version updated to 1.7.0 / versionCode 7.
