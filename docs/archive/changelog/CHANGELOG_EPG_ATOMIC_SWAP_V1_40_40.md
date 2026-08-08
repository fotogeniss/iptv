# v1.40.40 — EPG atomic swap hardening

- Parses replacement XMLTV guides into an isolated snapshot.
- Keeps the currently working EPG visible while a replacement downloads/parses.
- Publishes a replacement only after source/generation validation succeeds.
- A failed replacement no longer clears the previous EPG.
- Disk-cache candidates are read without briefly publishing a guide for another source.
- Cache writes use the committed immutable snapshot rather than mutable global state.
- Version: 1.40.40 / versionCode 84.
