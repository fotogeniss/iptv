# v1.40.36 — Refresh atomic commit hardening

- New group selections made through **Refresh + choose new groups** are no longer persisted before provider loading succeeds.
- Failed or cancelled refreshes keep both the visible catalog and the previously saved group selection unchanged.
- Successful refreshes commit the new source/section-scoped group choice only after the refreshed catalog has been normalized and cached.
- Existing-selection refresh and fresh-group refresh preserve the currently visible group filter when that group still exists.
- If the visible group disappeared, the UI safely falls back to **All channels**.
- No provider, playback, Multiview, mobile navigation or database schema refactor.
- Version bumped to 1.40.36 / versionCode 80.
