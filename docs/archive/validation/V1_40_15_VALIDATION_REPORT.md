# v1.40.15 Validation Report

## Scope

Στοχευμένη διόρθωση δύο θεμάτων πάνω στο πραγματικό v1.40.14:

1. μετατόπιση/κόψιμο τίτλου στον TV Home Hero,
2. μη αξιόπιστη εφαρμογή μεγέθους υποτίτλων.

## Checks completed

### Kotlin PSI syntax validation

```text
FILES=159 ERRORS=0
```

Ο έλεγχος περιλαμβάνει όλα τα production και test Kotlin αρχεία κάτω από `app/src`.

### Static integration contracts

Επαληθεύτηκαν στον τελικό κώδικα:

- `versionName = 1.40.15`, `versionCode = 59`.
- Το premium TV Home διατηρεί `FocusRequester`, αλλά επαναφέρει το `LazyColumn` στο item 0 μετά το focus request.
- Ο premium Hero έχει ασφαλές ύψος/padding και αυξημένο line height.
- Ο legacy TV Hero δεν χρησιμοποιεί ανεξέλεγκτο `rememberInitialFocus`.
- Το subtitle slider αποθηκεύει την τιμή μέσω `PlaylistStore.subtitleSizePercent`.
- Εξωτερικοί υπότιτλοι: `setTextSize`, `requestLayout`, `invalidate`.
- ExoPlayer: `setApplyEmbeddedFontSizes(false)` και `setFractionalTextSize(..., true)`.
- Reapply hooks: player creation, `onTracksChanged`, `onRenderedFirstFrame`, `onVideoSizeChanged`, αλλαγή embedded track.
- Άμεσο subtitle size preview πάνω στο video frame.

### Gradle compile attempt

Εκτελέστηκε:

```text
./gradlew :app:compileDebugKotlin --no-daemon --stacktrace
```

Το Android compilation δεν ξεκίνησε, επειδή το isolated περιβάλλον δεν μπόρεσε να επιλύσει/συνδεθεί στο `services.gradle.org` για τη διανομή Gradle 8.9 (`UnresolvedAddressException`). Αυτό δεν αποτελεί επιτυχημένο full build.

## Device verification still required

- TV 720p και 1080p: αρχική θέση Hero και DPAD focus.
- ExoPlayer embedded subtitle track: αλλαγή 70% → 180%.
- OpenSubtitles/SRT με ExoPlayer και VLC: αλλαγή 70% → 180%.
- Επιβεβαίωση ότι οι embedded VLC υπότιτλοι εμφανίζουν τον δηλωμένο περιορισμό.
