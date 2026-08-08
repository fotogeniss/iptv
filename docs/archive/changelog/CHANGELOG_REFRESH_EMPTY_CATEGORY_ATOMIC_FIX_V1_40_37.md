# v1.40.37 — Empty-category refresh atomicity fix

- Fixed the no-category provider fallback in **Refresh + choose new groups**.
- When a provider returns an empty category list, the app still falls back to loading all items, but now keeps the refresh operation transactional.
- The previous source/section group choice remains untouched until the all-items fetch, normalization and session-cache write succeed.
- Failure or cancellation during this fallback leaves both the visible catalog and the saved selection unchanged.
- Successful fallback commits `all groups` only after the refreshed catalog is ready.
- Existing-selection refresh, normal initial loading, provider logic, playback and Multiview behavior are unchanged.
- Version bumped to 1.40.37 / versionCode 81.
