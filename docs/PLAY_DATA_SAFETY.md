# Google Play Data safety worksheet

Status: release draft for Prelude+ 1.42.0 (`com.prelude.iptv`)  
Last technical audit: 2 August 2026

This worksheet records the code-level facts needed for the Google Play Data
safety form. It is not a form submission and not legal advice. The publisher is
responsible for the final declaration and must re-audit every shipped dependency
and behavior.

## Current product facts

- No Prelude+ account, cloud sync or publisher backend.
- No advertising SDK.
- No analytics SDK.
- Firebase Crashlytics is present for optional crash/ANR diagnostics. Collection
  defaults off and Firebase is not initialized before consent or **Send once**.
- Android system backup and device transfer of private app data are disabled.
- Source/API credentials are encrypted locally using Android Keystore.
- Portable backup is explicit, password protected and controlled by the user.
- User-configured IPTV endpoints may use HTTP, so not every transmission is
  encrypted in transit.

## Data-flow decisions

| Data or activity | Stored by Prelude+ | Sent off device | Recipient / purpose | Play form review |
|---|---:|---:|---|---|
| IPTV URL and provider credentials | Yes, encrypted locally | Yes | User-selected provider; authentication/catalog/playback | Review as user-provided identifiers; do not assume a blanket exemption |
| OpenSubtitles credentials/API key | Yes, encrypted locally | Yes | OpenSubtitles; authentication/search/download | Review Personal info / User IDs under current Play definitions |
| TMDB API key | Yes, encrypted locally | Yes | TMDB; API authentication | Developer credential, normally not end-user data |
| Media title, year, season, episode and language | Cached locally | Yes, when feature used | TMDB/OpenSubtitles; metadata or subtitle search | Conservatively review App activity / Search history |
| Favorites, history and watch progress | Yes | No | Local personalization only | Not collected while it remains only on device |
| Profiles, parental settings and layout preferences | Yes | No | Local features only | Not collected while it remains only on device |
| Purchase state and purchase evidence | Yes, locally | Google Play Billing handles transaction | Google Play; purchase/restore/entitlement | Review latest Play Billing disclosure; no Prelude+ server receives it |
| EPG and catalog cache | Yes | Requests go to configured provider | User-selected provider; catalog/guide | Usually service data, but credentials and request metadata still require review |
| Local playlist file | User-selected import | No by Prelude+ | Local parsing | Not collected unless a later feature uploads it |
| Android TV recommendations / Play Next | Local TV Provider | No Prelude+ cloud transfer | Android system on the device | Local device integration |
| Crash/ANR diagnostics | One redacted pending summary may be stored locally | Only after opt-in or Send once | Firebase Crashlytics; app stability | Review Crash logs, Diagnostics and Device or other IDs under the current form |
| IP address and network metadata | Not deliberately stored by publisher | Inherent in every network connection | Selected provider/TMDB/OpenSubtitles/Google | Disclose in privacy policy; evaluate Device or other IDs under current form guidance |

## Answers that are unsafe to select

- **"No data is collected or shared"** must not be selected without a Play
  policy review. Google defines collection broadly as transmission off device,
  including through third-party code or services.
- **"All user data is encrypted in transit"** is false while arbitrary HTTP IPTV
  endpoints are supported.
- **"Users can request account deletion"** is not applicable while Prelude+
  offers no account. If account creation is added, both in-app and web deletion
  flows are required before release.

## Store form checklist

1. Audit the exact release AAB, merged manifest and complete dependency tree.
2. Reconcile every transmitted field with the current Play data-type taxonomy.
3. Check current SDK data-safety guidance for Google Play Billing.
4. Check current Firebase Crashlytics disclosure guidance and verify that the
   shipped default remains opt-in with Analytics absent.
5. Confirm whether every third party is a service provider or a data-sharing
   recipient under the current definitions.
6. Answer encryption-in-transit **No** unless HTTP provider support is removed or
   technically isolated in a way accepted by the policy.
7. Publish the final privacy policy at an active, public, non-PDF URL and link it
   both in Play Console and inside the app.
8. Keep the form accurate whenever code, SDKs, permissions or data practices
   change.

## External release blockers

- Publisher legal name and privacy contact email are not configured.
- Public privacy-policy URL is not deployed.
- Firebase project ownership and `app/google-services.json` are not configured;
  until they are, crash reports remain local and cannot reach the owner console.
- TMDB states that revenue-generating use is commercial and should use a
  commercial licence. Confirm the licence before monetized release.
- Confirm the OpenSubtitles API/account terms that apply to the published and
  monetized app.
- Complete final publisher/legal review for target countries and audience.

## Authoritative references

- Google Play Data safety:
  https://support.google.com/googleplay/android-developer/answer/10787469
- Google Play Developer Program privacy-policy requirements:
  https://support.google.com/googleplay/android-developer/answer/17190352
- Google Play app review/privacy guidance:
  https://support.google.com/googleplay/android-developer/answer/9859455
- TMDB API FAQ and attribution/licensing:
  https://developer.themoviedb.org/docs/faq
