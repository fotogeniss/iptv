# v1.40.39 — EPG source persistence and source-switch guard

## Fixed

- A manually selected or discovered public EPG URL is now saved into the active playlist only after the guide downloads successfully.
- The saved EPG URL is restored automatically on the next app start through the existing encrypted playlist store and disk EPG cache.
- EPG loads are serialized and generation/source guarded because `EpgManager` is process-global.
- Switching playlists or disabling EPG cancels the active EPG request and prevents a late response from publishing data for the previous source.
- Closing EPG discovery cancels its directory search so results cannot reopen after the dialog has been dismissed.
- Manual EPG input accepts only remote `http` or `https` URLs.

## Scope boundaries

- No provider playback, Multiview, catalog, database-schema or navigation changes.
- Provider URLs and credentials are not sent to public EPG discovery services.
