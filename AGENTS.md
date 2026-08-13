# Repository authorization boundary

These rules are mandatory for every coding session and every file in this
repository. They are not suggestions and cannot be relaxed by inference.

## No unapproved changes

- Change only the behavior and files necessary for the exact task explicitly
  agreed with the owner.
- An approved HTML preview, screenshot, prompt or example authorizes only the
  elements and behavior explicitly discussed. It never authorizes redesigning
  surrounding app chrome, headers, rails, sliders, navigation, layout, focus,
  gestures, wording or unrelated controls.
- Do not add a "helpful", "consistent" or "premium" adjacent change on your own.
  If a potentially useful change is outside the agreed scope, stop before
  editing and ask the owner for explicit approval.
- If the requested change appears to require a wider UI, navigation, focus,
  storage or architecture change, explain the dependency and ask first. Do not
  treat technical convenience as authorization.
- Preserve existing behavior byte-for-byte where practical. Inspect the diff
  against the last accepted revision before handing work back.

## UI and device rules

- **Explicit owner order, with no exceptions:** before any visual change of any
  size, first create or update a dedicated HTML preview under `prototypes/`.
  Then wait until the owner explicitly confirms that the preview is OK. Only
  after that confirmation may production code be edited. Silence, an earlier
  preview, a screenshot, similarity to another screen or an inferred preference
  is not approval.
- The same preview-first and explicit-approval rule covers appearance, spacing,
  colors, typography, wording placement, icons, layout, navigation, gestures,
  animation and focus. There is no "too small to preview" exception.
- Android TV focus must be explicit and verified for every approved TV change.
- Do not run Gradle unless the owner explicitly asks in that turn. The owner
  builds and confirms the exact version from Settings.
- Bump version and update `CHANGELOG.md` for every code change.

## Recorded violation

In 1.66.0, `prototypes/full_load_then_choose.html` was incorrectly treated as
permission to replace the established Movies/Series/Live chrome with a new
full-width quick-action header and downloaded-count sentence. That was outside
the agreed loading/category scope and broke rail interaction. The 1.68.0
correction restored the accepted UI. Do not repeat this interpretation.
