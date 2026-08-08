# Mobile Movies & Series Navigation — v1.40.1

## Change

The premium mobile bottom navigation now contains seven destinations:

1. Αρχική
2. Ταινίες
3. Σειρές
4. Live
5. Αναζήτηση
6. Λίστα
7. Ρυθμίσεις

## Routing

- `Ταινίες` uses the existing `MainViewModel.setContentType("vod")` flow.
- `Σειρές` uses the existing `MainViewModel.setContentType("series")` flow.
- `Αρχική` returns to the premium catalog home.
- Existing loading/category-selection behavior remains unchanged when a section is not cached.
- The same navigation destinations are available from Mobile Home, Live TV, and Library.
- Android TV navigation was not changed.

## UI

- Seven equally weighted items fit within the existing 72dp bottom bar.
- Icons use 20dp sizing and compact 7sp labels.
- The active destination keeps the existing white state and red indicator.

## Compatibility fix

No `import androidx.compose.foundation.layout.weight` import is present. `Modifier.weight(...)` is resolved only from the correct `RowScope`/`ColumnScope` context, preserving the v1.39.1 build fix.
