# Android TV focus and shortcut contract — v1.40.43

| Context | Initial focus | Direction keys | OK / Center | Back | Media / channel shortcuts |
|---|---|---|---|---|---|
| App shell / Home | First visible primary action or last restored item | Move only among visible destinations/content | Open focused destination/item | Exit top overlay, then app route | No duplicate action on repeated key-down |
| Live rail/grid | Selected channel or first visible channel | Scroll and retain visible focus | Open/play channel | Return to prior route | Channel Up/Down zap only in player context |
| Movies/Series | Selected card or first visible card | Grid-safe movement; no off-screen trap | Details/play | Close detail before route | Play/Pause delegated only when playback active |
| Search | Search action/input then first result | Predictable input/results transition | Activate focused result/action | Close keyboard/results state first | Number keys must not trigger live-channel entry |
| Settings | First setting/action | Sequential visible focus | Open/toggle focused setting | Close dialog/sub-route first | No playback shortcut leakage |
| Load/refresh dialogs | Safe default (`All` or existing selection) | Down reaches alternate action; no invisible buttons | Confirm focused action | Cancel only current dialog | Repeated keys ignored |
| Category picker | Select-all/first category | List scroll follows focus; Right reaches Load | Toggle category / confirm Load | Return to load-mode choice or catalog | No shortcut leakage |
| Player controls visible | Current primary control | Android focus system owns arrows | Activate focused control | Hide overlay, then exit | Media keys remain direct; arrows do not seek/zap |
| Player controls hidden | Player surface | Horizontal seek for VOD; vertical reveal/zap policy | Reveal/toggle controls according to policy | Exit player | Captions/Menu/Info/Play/Pause/Stop/Channel keys use tested policy |
| EPG sheet | First programme row | Programme list scroll follows focus | Expand/collapse programme | Close sheet | Player shortcuts do not leak through modal |

## Invariants

- Focus must never remain attached to a removed, disabled or invisible node.
- A dialog must always expose a visible focus target on Android TV.
- Focus restoration uses bounded retries after lazy layout, dialog dismissal and Activity resume.
- Key handling acts only on first key-down unless a deliberate repeat behavior is specified.
- Back unwinds overlays and nested routes before leaving the parent destination.
