# v1.34 — Screen 03 Premium Search

## UI

- Replaced the inline adaptive Search implementation with independent Mobile
  and Android TV compositions.
- Added a cinematic TMDB-backed focused-result presentation.
- Added reusable filter, empty-state, result-card and keyboard components.
- Mobile Search now has a sticky touch search field, voice action, discovery
  suggestions, featured result and two-column result rows.
- Android TV Search now has a DPAD-first remote keyboard with Greek/Latin
  layouts, recent/suggested terms, filter chips, a focused-result hero and a
  five-column results grid.
- Focus uses scale, brightness and shadow rather than red borders.

## Integration

- Existing `LibraryPolicy.search`, repositories, playback routing, favorites
  and watch progress remain unchanged.
- Search now reuses the existing TMDB memory/disk cache for backdrop metadata.
- Voice search uses Android's `RecognizerIntent` and fails gracefully when no
  recognition activity is installed.
- The approved HTML prototype is included under `prototypes/search/`.

## Structure

```text
ui/components/search/
ui/mobile/search/
ui/tv/search/
```
