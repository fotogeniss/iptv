# Player surface decisions

Why the mini player showed sound with no picture, what actually fixed it, and
what must never be tried again. Written after the 1.71.0 → 1.78.0 sequence,
which cost six owner builds because the symptom was diagnosed five times before
it was measured once.

## The rule

**Both playback engines must render into a `TextureView`. Never a `SurfaceView`
inside the Compose player.**

- ExoPlayer: `PlayerVideoSurface(preferSmoothResize = true)`.
- libVLC: `attachViews(layout, null, false, VLC_USE_TEXTURE_VIEW)` in
  `VlcBackend`, where that constant is `true`.

A `SurfaceView` lives in its own system layer with its own geometry. It does not
follow the resize, clipping or z-order of the Compose tree around it. The moment
the player collapses to the 121x68dp strip, a SurfaceView-backed output stays at
full-screen geometry and nothing is visible, while audio continues normally.

If someone changes either engine back to a SurfaceView to "reduce GPU cost" or
"use the recommended surface", this defect returns immediately, and it returns
only in the mini player, which is where nobody looks.

## The signature

Sound plays, picture is black, and the failure is **surface-shaped**, not
engine-shaped:

- Full screen works, the strip does not.
- Expanding back sometimes restores the picture and sometimes does not.
- Recorded video works, live channels do not — or the reverse, depending on
  which engine each stream lands on.

That last point is the trap that cost the most time. `PlaybackBackendPolicy`
routes bare MPEG-TS to libVLC and `m3u8`/`mp4`/`mkv`/`mpd` to ExoPlayer, so
"sometimes it works" almost always means "it depends which engine that stream
used", not "there is a race". Establish the engine before theorising about
timing.

## Measure before theorising

The QA build carries a readout inside the strip's video area, gated on
`BuildConfig.PREMIUM_QA_OVERRIDE`, in `MobileMiniPlayer`:

```
VLC f=3
363x204
Text#5737 ar=1.78
```

| Field | Meaning | What it decides |
| --- | --- | --- |
| `EXO` / `VLC` | active engine for this stream | which path to inspect at all |
| `f=` | frames rendered on the **current** surface | the whole diagnosis, see below |
| `363x204` | measured surface size in pixels | `0x0` means the surface was never laid out |
| `Text#…` | identity of the attached surface | only meaningful for ExoPlayer |
| `ar=` | aspect ratio reported by the stream | sanity check that video exists at all |

`f=` splits the problem in two, and the two halves need opposite fixes:

- **`f=0`** — no frame ever reached the strip's surface. The failure is in
  surface delivery: attach/detach, engine ownership, or output configuration.
- **`f>0` and still black** — frames arrive and something above them hides the
  picture. The failure is in compositing: clipping, layer alpha, z-order, or a
  cover drawn over the video.

One photograph of the strip answers this. Three rounds of reasoning did not.
Take the photograph first.

## What was tried, and what it was worth

| Attempt | Verdict |
| --- | --- |
| 1.71.0 — identity guard on the libVLC detach | **Keep.** A real defect: the departing full-screen surface tore down the strip's freshly attached layout. Not the owner's bug, but correct and still required. |
| 1.74.0 — one `movableContentOf` surface shared by both layouts | **Keep.** Removes the ExoPlayer output swap entirely, so `MediaCodec.setOutputSurface` is never called mid-playback and cannot be refused. |
| 1.75.0 — re-attach libVLC on a material size change | **Removed.** It worked, but rebuilding the video output forces a live MPEG-TS stream to wait for the next keyframe: three to four seconds of black on every collapse. |
| 1.77.0 — report the new window size instead of re-attaching | **Removed.** Cheap, but it did not restore the picture at all, and the safety net never fired because `vlcFrameWasRendered` is sticky: libVLC never reported the output as lost, so the fallback saw a healthy state that was not there. |
| 1.78.0 — render libVLC into a TextureView | **The fix.** The surface now follows the layout by itself, so no manual resize handling exists at all. |

## Things that are correct — do not "simplify" them

- `PlaybackEngine.attachSurface` / `detachSurface` detach **only** if the view
  passed in is still the active one. Two surfaces briefly coexist while Compose
  swaps layouts; an unconditional detach tears down the wrong one.
- `VlcBackend.detachLayout(layout)` has the same guard for the same reason. The
  ExoPlayer path had it from the first commit; the libVLC path did not, which is
  what 1.71.0 corrected.
- The video surface is a single `movableContentOf` owned by
  `MobilePlaybackOverlay` and handed to whichever layout is showing. Do not give
  the strip its own `PlayerVideoSurface` again. Two surfaces competing for one
  output is how this started.
- `MobilePlaybackOverlay` does not leave the composition when it collapses. The
  engine lives in a `remember` inside it; removing the overlay releases the
  engine and kills the audio too.

## Where git history could not help

`MobileMiniPlayer`, the engine's surface attach/detach and `PlayerVideoSurface`
were byte-identical to `85de40a`, this repository's **root** commit, and the
uncommitted 1.67.0–1.69.0 work never touched the player. The baseline is a
squashed import, so nothing before it exists. Any regression older than the
first commit is invisible here. Do not spend a session looking for it again —
measure the running build instead.

## Removing the QA readout

It is deliberately temporary. Delete the `BuildConfig.PREMIUM_QA_OVERRIDE` block
in `MobileMiniPlayer` and `PlaybackEngine.attachedSurfaceLabel()` once the strip
is confirmed on device. Keep this document; it is the part worth keeping.
