# v13.1 build fix

- HeroShowcase now accepts `List<Pair<Channel, Float>>`, matching `continueWatching()`.
- Continue Watching progress is rendered through `AppMediaProgress`.
- Suspend TMDB callbacks use the explicit `vm::tmdb` function reference.
- Removed explicit `foundation.layout.weight` imports from adaptive Home/Details to avoid Kotlin 2.1 RowColumnParentData resolution conflicts.
