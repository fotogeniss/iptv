# v1.40.35 — Refresh mode and fresh group selection

- Added a shared refresh choice for Android TV and mobile:
  1. Refresh the current saved group selection.
  2. Refresh the provider group list and choose groups again.
- Applied the flow independently to Live TV, Movies and Series through the existing source/section-scoped selection keys.
- Fresh-group refresh opens the category picker directly and preselects groups that are still available.
- New provider groups remain visible but unselected until the user chooses them.
- Removed provider groups are dropped from the preselection safely.
- Cancelling the fresh-group picker keeps the previously visible catalog unchanged.
- Existing-selection refresh also keeps the old catalog visible until fresh data succeeds.
- Added the same flow to Browse and Settings on both TV and mobile layouts.
- Added focused JVM policy tests.
- Bumped version to 1.40.35 / versionCode 79.
