# Documentation maintenance

The source of truth for the app version is `app/build.gradle.kts`. Do not hard-code
the current version in validation scripts.

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
