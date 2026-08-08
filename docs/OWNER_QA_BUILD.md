# Owner QA build

The `qa` variant is the owner's full-access release-candidate build. It exists so
all Premium screens and flows can be tested before the Play Console product or
backend is available.

## Contract

| Variant | Application ID | Premium access | Purpose |
| --- | --- | --- | --- |
| `debug` | `com.prelude.iptv` | Full QA override | Local development |
| `qa` | `com.prelude.iptv.qa` | Full QA override | Owner device testing |
| `release` | `com.prelude.iptv` | Play entitlement only | Public distribution |

The `qa` variant inherits release minification and resource shrinking. It is
non-debuggable (matching release behavior) but debug-signed for local owner
installation, has a separate application ID and displays as **Prelude+ QA**, so
it can be installed beside the public app. Use the ordinary `debug` variant when
an attached debugger is required.

## Build and install

```powershell
.\gradlew.bat :app:assembleQa
adb install -r app\build\outputs\apk\qa\app-qa-universal.apk
```

If ABI splits change the filename, inspect `app/build/outputs/apk/qa/` and choose
the matching device APK or the universal APK.

## Important limitation

The QA override deliberately bypasses Premium entitlement checks. It is suitable
for testing premium functionality, but it does **not** test Google Play Billing.
Billing itself must be validated separately with a production-signed bundle from
an internal Play testing track and a license tester account.

Never change the release value of `PREMIUM_QA_OVERRIDE` to `true`. The static
release audit fails if the QA/release boundary is removed.
