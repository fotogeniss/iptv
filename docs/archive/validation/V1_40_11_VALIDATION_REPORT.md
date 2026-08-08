# v1.40.11 Validation Report

## Scope

This release fixes three device-reported presentation issues:

1. Invisible player icons/labels on a white TV focus surface.
2. Large TV hero titles clipped at the top.
3. Mobile Live quick guide showing only six channels from the active group.

## Kotlin PSI syntax validation

Command:

```bash
java -cp 'syntaxcheck.jar:<kotlin-compiler-libs>' SyntaxCheckKt app/src
```

Result:

```text
FILES=157 ERRORS=0
```

This validates Kotlin syntax only; it does not resolve Android/Compose symbols.

## Contract audit

Passed checks:

- shared player focus defines a dark focused foreground;
- focused player content is retinted recursively for images and labels;
- central play/pause control changes tint on focus;
- central control uses `CENTER_INSIDE` and reduced padding;
- TV hero title line height is larger than its font size;
- Mobile Live no longer contains `channels.take(6)`;
- Mobile Live guide rows use `itemsIndexed` lazy emission;
- guide header reports the real active-channel count;
- the old problematic explicit `foundation.layout.weight` import is absent;
- version is `1.40.11` / code `55`.

## Gradle build status

A full Android build was attempted via the included wrapper, but this environment cannot resolve/download the Gradle distribution:

```text
java.nio.channels.UnresolvedAddressException
```

Therefore this report does not claim that `compileDebugKotlin` or `assembleDebug` passed. Build and device verification are still required in Android Studio.

## Device checks

### Android TV player

- Focus the central play/pause control: the icon must be dark and fully visible.
- Focus Audio, Subtitles, Quality, Aspect and List: icon and label must remain visible.
- Move focus repeatedly between center and toolbar controls; no blank white buttons should appear.

### Android TV Home

- Test one-line and two-line titles at 720p and 1080p.
- Confirm the tops of capital letters are fully visible.

### Mobile Live

- Open a group with more than six channels.
- Scroll the quick guide to the final channel.
- Confirm the displayed count matches the active filtered list.
