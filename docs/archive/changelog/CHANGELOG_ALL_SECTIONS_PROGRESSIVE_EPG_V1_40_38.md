# v1.40.38 — All sections, progressive catalogs, source switching and EPG discovery

## User-visible changes

- Added **Όλα** to the section chooser.
- Live TV, Movies and Series load sequentially to protect provider sessions.
- Completed/partial catalog items are published while loading continues, so the user can browse immediately.
- Xtream and Stalker selected categories publish accumulated results after each category.
- Switching playlist clears the previous visible catalog and EPG state, then requests fresh data for the target source.
- Category/source full-screen overlays hide the mobile bottom navigation and respect navigation-bar insets.
- Multiview panes use `fit` instead of `zoom` to avoid cropping channel video.
- EPG search checks playlist/provider hints and then the public iptv-org guide index, matching locally by `tvg-id` without sending provider URLs or credentials.

## Scope boundaries

- No database schema changes.
- No provider credentials are added to public EPG requests.
- No unrelated playback or navigation refactor.
