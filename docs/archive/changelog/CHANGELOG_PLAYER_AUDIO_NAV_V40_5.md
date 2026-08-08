# v1.40.5 — Player audio selection & navigation guard

## Player audio
- Mobile/TV player selection rows now execute the track action before dismissing the sheet.
- The sheet panel consumes its own touches so taps do not fall through to the player surface.
- VLC audio options use the real VLC track id and mark the actual active track.
- `Disable` is presented as `Χωρίς ήχο`.
- ExoPlayer audio options mark the currently selected track and explicitly enable audio before applying an override.
- A short result message confirms whether VLC accepted the selected track.

## Navigation
- Opening Settings from the loaded catalog records the catalog as the return destination.
- Back from Settings returns to the loaded catalog instead of the source-selection screen.
- Back / Sources from the loaded catalog shows a confirmation dialog before opening source selection.
- Cancelling the dialog keeps the current source and screen unchanged.

## Version
- versionName: 1.40.5
- versionCode: 49
