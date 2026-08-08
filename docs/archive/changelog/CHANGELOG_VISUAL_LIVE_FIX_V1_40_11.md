# v1.40.11 — TV Player Focus, Hero Title & Full Live Group

## TV player focus

- Focused player controls now keep their icons and labels visible.
- Focus uses a light premium surface with dark foreground content.
- The central play/pause icon changes tint together with its focused surface.
- The central icon padding was reduced and `CENTER_INSIDE` was set to avoid drawable clipping.
- Selection-sheet rows use the same foreground/background focus contract.

## Android TV Home hero

- Increased the hero title line height so large bold glyphs are not clipped at the top.
- Added additional title top padding and TV-safe horizontal/top spacing.
- Increased the hero height and top content padding to preserve the full title area on TV layouts.

## Mobile Live guide

- Removed the hard `take(6)` limit from the quick guide.
- The selected group/filter now exposes every matching channel.
- Channel rows are emitted as lazy list items instead of composing the whole group eagerly.
- The guide header displays the actual channel count.

## Compatibility

- No provider, playback engine, history, source cache, EPG matching or stream-resolution behavior was changed.
