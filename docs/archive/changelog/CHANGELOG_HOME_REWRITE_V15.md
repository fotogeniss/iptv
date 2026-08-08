# Home Rewrite v15

- Added a dedicated `AdaptiveCatalogHome` entry point.
- Mobile and Android TV now render separate Home compositions.
- Mobile uses a touch-first cinematic hero plus portrait/landscape catalog rails.
- TV uses a DPAD-first cinematic hero plus larger focused media rails.
- Continue Watching opens playback directly; other catalog rails open Details.
- Removed the legacy channel-list fallback from the catalog-home execution path.
- IPTV clients, EPG, player engines and persistence remain unchanged.
