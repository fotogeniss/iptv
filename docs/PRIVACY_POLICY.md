# Prelude+ Privacy Policy (release draft)

Effective date: 2 August 2026  
Policy version: 1.1-draft  
Application: Prelude+ (`com.prelude.iptv`)

> Release blocker: replace **[PUBLISHER LEGAL NAME]** and
> **[PRIVACY CONTACT EMAIL]** before publishing this policy. The publisher name
> must match the entity shown on the Google Play listing.

This policy explains how **[PUBLISHER LEGAL NAME]** ("we", "us") handles data
when you use Prelude+. Prelude+ is an independent media player. It does not
provide, host, sell or bundle TV channels, playlists, subscriptions or media.

## 1. Current privacy model

Prelude+ currently has no Prelude+ account, cloud synchronization or
publisher-operated backend. The publisher does not receive your playlists,
provider credentials, favorites, playback history or watch progress.

The production code does not include advertising or behavioral analytics. It
includes an optional Firebase Crashlytics stability-reporting SDK. Crashlytics
does not initialize or transmit a report until you enable crash reporting or
explicitly send one pending report from the Diagnostics screen.

## 2. Data stored locally

The app stores the following information on your Android device so its features
work:

- playlists and source configuration that you add;
- IPTV provider connection details and credentials;
- optional TMDB and OpenSubtitles API/account credentials;
- favorites, recently watched items, watch progress and playback preferences;
- profiles, parental-control preferences, Home/category layouts and other app
  settings;
- cached catalog, artwork, metadata, subtitle and EPG information;
- locally cached Google Play Premium entitlement state.

Sensitive source and API credentials are encrypted at rest with AES-GCM and a
key held by Android Keystore. Private app data is excluded from Android system
backup and device transfer.

If you explicitly export a portable backup, Prelude+ creates a user-controlled,
password-protected AES-256-GCM backup file. You choose where that file is stored
or shared. Anyone who obtains both the file and its password may be able to
restore its contents.

## 3. Network requests and external recipients

Prelude+ sends network requests only to provide features you request or enable.

### Your IPTV provider

The app connects directly to playlist URLs, Xtream servers, Stalker/MAC portals,
stream URLs and EPG endpoints you configure. These services receive the network
information and credentials needed to authenticate and serve the requested
catalog or media. Their own privacy terms apply. We do not control these
providers.

### The Movie Database (TMDB)

When TMDB metadata is configured, Prelude+ may send media titles, years, media
types, season/episode identifiers and language preferences to TMDB to retrieve
artwork, ratings, cast and descriptions. TMDB's own privacy terms apply.

### OpenSubtitles

When subtitle search is configured or requested, Prelude+ may send a media title,
year, season, episode, language, file identity and optional OpenSubtitles account
credentials to OpenSubtitles to search for and download subtitles.

### Google Play

When you view, buy or restore Prelude+ Premium, the app communicates with Google
Play Billing. Google processes the transaction under its own terms. Prelude+
receives purchase state and purchase evidence needed to grant the local
entitlement. The current app does not send purchase tokens to a Prelude+ server.

### Optional stability diagnostics

Crash and ANR reporting is disabled by default. While it is disabled, Prelude+
may keep one redacted crash summary in its private local storage so you can
choose to send or delete it. If you enable reporting or choose **Send once**, the
app may send crash or ANR diagnostics to Firebase Crashlytics, a Google service.

These diagnostics can include exception type and message, stack trace, app
version, device model, operating-system version, process state and technical ANR
information. Prelude+ does not deliberately attach playlist URLs, IPTV
credentials, media titles, profile identifiers, advertising identifiers or
analytics/navigation events. Exception and platform-generated diagnostic data
can nevertheless contain technical values produced at the point of failure.

### Android TV Home

If you enable Android TV Home recommendations or Play Next, the app writes the
selected local program metadata to Android's TV Provider on that device. This
does not create a Prelude+ cloud account.

## 4. Encryption in transit

TMDB, OpenSubtitles and Google Play use encrypted HTTPS connections. Many
user-supplied IPTV providers still use unencrypted HTTP. Prelude+ permits such
connections for compatibility. When you use an HTTP source, network operators
may be able to observe or modify traffic between your device and that provider.
Use HTTPS sources whenever available.

Because user-configured HTTP sources are supported, Prelude+ must not claim in
Google Play Data safety that **all** transmitted data is encrypted in transit.

## 5. Retention and deletion

Local data remains until it is removed through the relevant app control, the
source is deleted, app storage is cleared in Android settings, or the app is
uninstalled. Cache-clearing controls remove only the identified cache.

A locally pending diagnostic report can be deleted from the Diagnostics screen.
Reports sent to Firebase are retained according to Firebase Crashlytics' current
retention controls and the publisher's Firebase project settings.

Data already sent to an IPTV provider, TMDB, OpenSubtitles, Google Play or
Firebase Crashlytics is governed by that recipient's retention and deletion
practices. Contact that provider for requests concerning data it controls.

Prelude+ currently offers no app account, so there is no Prelude+ cloud account
to delete. This section and an account-deletion mechanism must be added before
account creation or cloud synchronization is released.

## 6. Permissions

Prelude+ may request or declare network access, network/Wi-Fi state,
notifications, a foreground data-sync service and Android TV EPG/Home provider
access. These permissions support catalog loading, playback, user-visible
background work and Android TV integration. Prelude+ does not request device
location, contacts, microphone, camera or advertising ID access.

## 7. Children

Prelude+ is a general-purpose media player and is not directed to children. The
local profile/PIN controls are convenience features, not identity or age
verification. Users and guardians are responsible for the sources and content
they add.

## 8. Security and limitations

We use platform security controls and encrypted local credential storage, but no
device, network or software can be guaranteed completely secure. Rooted devices,
malicious software, compromised providers and unencrypted HTTP sources can
increase risk.

## 9. Changes

We will update the effective date and policy version when data practices change.
Material changes must be reflected both in the in-app policy and the public
policy URL before the corresponding app update is released.

## 10. Contact

Publisher: **[PUBLISHER LEGAL NAME]**  
Privacy contact: **[PRIVACY CONTACT EMAIL]**

This repository draft is not the public policy URL. Google Play requires an
active, globally accessible, non-PDF policy page linked from Play Console and
available inside the app.
