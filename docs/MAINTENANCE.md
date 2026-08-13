# Documentation maintenance

The source of truth for the app version is `app/build.gradle.kts`. Do not hard-code
the current version in validation scripts.

## Authorization boundary — mandatory

The owner authorizes only the exact change explicitly agreed in the current
task. This is a hard scope boundary:

- Never infer permission for adjacent cleanup, redesign, consistency work or
  "helpful" additions.
- A preview or screenshot defines only the discussed element. It does not grant
  permission to replace surrounding headers, chrome, rails/sliders, navigation,
  layout, focus, gestures, wording or unrelated controls.
- If implementation would require any behavior outside that boundary, stop
  before editing and request explicit approval.
- **Every visual change, without a small-change exception, follows this strict
  sequence:** create/update a dedicated HTML preview in `prototypes/`; show it
  to the owner; wait for the owner's explicit statement that it is OK; only
  then edit production code. An older approval, screenshot, silence or inferred
  intent is not approval for the current visual change.
- This sequence also covers spacing, colors, typography, wording placement,
  icons, layout, navigation, gestures, animation and focus.
- Before handoff, compare all touched shared UI files with the last accepted
  revision and prove that unrelated behavior stayed unchanged.

The 1.66.0 incident is the permanent counterexample: an approved loading and
category-flow preview was incorrectly expanded into a full-width catalog-header
redesign. That exceeded authorization and broke the rails. See root `AGENTS.md`;
the documentation gate deliberately requires this rule to remain present.

## For every change

| Change | Required documentation |
|---|---|
| User-visible behavior or bug fix | Add a short entry under `Unreleased` in `CHANGELOG.md`. |
| New/removed capability | Update the matching feature section in `README.md` and the changelog. |
| Architecture or ownership change | Update `docs/ARCHITECTURE_REFACTOR_PLAN.md` if the documented boundaries changed. |
| SDK, dependency or device-support change | Update `Toolchain` in `README.md`. |
| Security/storage/network change | Update the README security section and the relevant decision document. |
| Data field, permission, SDK, external service, analytics, ads or account change | Re-audit `docs/PRIVACY_POLICY.md` and `docs/PLAY_DATA_SAFETY.md`; update the public policy and Play Console form before release. |
| Purchase, refund or subscription behavior change | Re-audit `docs/TERMS_OF_USE.md`, the privacy policy and Play Billing declarations. |
| Build/release process change | Update the README verification/release sections. |
| Version bump | Update the README version line and add a matching released section to `CHANGELOG.md`. |

## Before merging

Run:

```bash
python scripts/documentation_contracts.py
```

CI runs the same check. It verifies that README and CHANGELOG match the Gradle
version and rejects historical/generated report files placed in the root.

Before a store release, also search the release dependency tree and merged
manifest for new SDKs, permissions and network recipients. Documentation checks
can detect stale files, but they cannot prove that a Play Console declaration or
public policy matches runtime behavior.

## File placement

- `README.md`: concise, current product and contributor entry point.
- `CHANGELOG.md`: concise user-visible release history.
- `docs/`: current detailed technical documentation.
- `docs/PRIVACY_POLICY.md`: release privacy source; publish the approved content
  to the public policy URL and expose it in-app.
- `docs/PLAY_DATA_SAFETY.md`: code-audited worksheet for the Play Console form.
- `docs/TERMS_OF_USE.md`: publisher-reviewed terms source.
- `docs/archive/changelog/`: old per-phase changelog files.
- `docs/archive/build/`: historical build-fix notes.
- `docs/archive/validation/`: old validation reports and generated logs.
- `docs/archive/history/`: other superseded project notes.
- `validation/`: temporary output from the current validation cycle only.
- `prototypes/`: HTML design prototypes, never the repository root.

Do not create one Markdown report per small fix. The code, tests and the single
CHANGELOG entry should normally be enough.
