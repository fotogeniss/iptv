# Player surface decisions

Why the mobile strip played sound with no picture on live channels, what the
logcat actually proved, and what must never be tried again.

Rewritten at 1.84.0. The previous version of this document was written after
1.78.0 and claimed the fix was to render both engines into a `TextureView`.
That claim was wrong, it was never confirmed on a device, 1.80.0 reverted it
because it broke Android TV, and 1.83.0 reverted the whole 1.71.0-1.82.0 range.
Trust `git log` over any heading, including this one.

## The rule

**libVLC builds exactly one video output per stream, and destroying it is
permanent.** Therefore the video view must have **one instance and one parent**
for the whole life of the playback. It may be resized and repositioned. It may
never be detached, recreated, or reparented.

- `PlayerVlcSurface` hands libVLC a `VLCVideoLayout` through
  `engine.attachVlcLayout`. `player.detachViews()` tears down the vout **and the
  MediaCodec decoder**, and a later `attachViews` on a different layout does not
  rebuild either one.
- Audio survives because libVLC decodes it on a separate thread. That is the
  entire explanation for "sound but no picture".
- ExoPlayer does not have this property: `setVideoTextureView` /
  `setVideoSurfaceView` can be swapped while playing. This is why recorded video
  always worked and live never did — `PlaybackBackendPolicy` routes bare MPEG-TS
  to libVLC and `m3u8`/`mp4`/`mkv`/`mpd` to ExoPlayer.

## The measurement that settled it

Two logcat captures on the owner's device (OPPO CPH2629, Android 16), filtered
on `VLC|vout|Codec2|MediaCodec`, one stream each, collapse and expand included:

```
07.77   Codec2-DumpInput: [DumpInput] c2.mtk.avc.decoder_32
07.79   Codec2Client: setOutputSurface -- generation=13155337
07.79   Codec2Client: Surface configure completed
07.81   libvlc decoder: output: 2130708361 unknown, 1280x720      ← playing

14.175  MediaCodec: client does not own the buffer #4
14.179  Codec2-DumpInput: [stop:L476]  c2.mtk.avc.decoder_32
14.198  Codec2-DumpInput: [~DumpInput] c2.mtk.avc.decoder_32      ← destroyed

after   nothing. no second decoder, no second setOutputSurface, no new vout.
```

The first capture showed the same shape with
`MediaCodec: Pending dequeue output buffer request cancelled` in place of the
buffer-ownership error.

**Exactly one `setOutputSurface` per stream is the whole proof.** If libVLC
rebuilt its output for the strip there would be a second one with a different
`generation`. There never is. The strip's `VLCVideoLayout` never received a
surface at all.

## How to reproduce the measurement

The owner has two apps installed with the same name and icon:
`com.prelude.iptv` is 1.46.0-qa, `com.prelude.iptv.qa` is current. Stop the
other one first or the log mixes them.

```
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb shell am force-stop com.prelude.iptv
& $adb logcat -c
# on the phone: open a live channel, collapse, wait, expand
& $adb logcat -d | Select-String "VLC|vout|Codec2|MediaCodec"
```

Read it this way:

- **A second `setOutputSurface` appears** → the output was rebuilt. Any remaining
  black is then a compositing question.
- **Only one, and a `stop`/destructor after the transition** → the output was
  destroyed and not rebuilt. Something detached or reparented the view.

Noise, always present, never the cause: `libvlc window: request N not
implemented`, `can't get Subtitles Surface`, `option --rtsp-caching no longer
exists`, `Codec2Client: query -- param skipped`, and `OStatsManager_Calc` lines
mentioning `org.videolan.vlc`.

## The design that follows from it, as built in 1.84.0

Collapsing the mobile player is a **change of geometry, not a change of
content** — which is what the comment in `MobileMiniPlayer` always claimed while
the code did the opposite.

- `MobilePlaybackOverlay` never leaves the composition, and neither does the
  single `BoxWithConstraints(playerModifier)` that contains the video. There is
  no `if (collapsed) { ... return }` branch any more.
- `playerModifier` has three geometries: full screen, the 16:9 sticky slot, and
  — when collapsed — `align(BottomStart)` with the strip's paddings and
  `size(121.dp, 68.dp)` at `zIndex(2f)`.
- The geometry is **real layout** (`align`/`padding`/`size`), never
  `graphicsLayer`. A `SurfaceView` does not follow an ancestor's scale or clip;
  it follows only its own position and size. Scaling the page would leave the
  libVLC surface at full-screen geometry.
- `MobileMiniPlayer` draws a `Spacer` hole where its video used to be, and is
  rendered **before** the video slot so it stays underneath it.
- When collapsed, the inner box early-returns after the video and a
  tap-to-expand layer. One decision point instead of fourteen conditionals over
  the controls, subtitles, gestures, transition and toasts.

## What was tried, and what it was worth

| Attempt | Verdict |
| --- | --- |
| 1.71.0 — identity guard on the libVLC detach | Correctly implemented, changed nothing. The problem is not detach *ordering*, it is that any detach is fatal. Reverted with the rest in 1.83.0. |
| 1.74.0 — one `movableContentOf` surface shared by both layouts | **Made live worse.** `movableContentOf` reparents the View, and reparenting a View destroys its Surface. Right instinct, wrong mechanism. |
| 1.75.0 — re-attach libVLC on a material size change | Restored picture at the cost of 3-4 s of black per collapse: the rebuild waits for the next MPEG-TS keyframe. |
| 1.77.0 — `setWindowSize` instead of re-attaching | Did not restore the picture at all. |
| 1.78.0 — render libVLC into a `TextureView` | Never confirmed on device. Reverted in 1.80.0 because `VLCVideoLayout`'s unused `SurfaceView` stays in the hierarchy and paints black in front of the picture on Android TV. |
| 1.79.0 / 1.82.0 — rebuild on `onViewAttachedToWindow` / on `surfaceCreated` after `surfaceDestroyed` | Both asked *when* to rebuild. There is no answer; `attachViews` does not rebuild. |

**Never spend another build on these**, all ruled out by the log above rather
than by argument:

- `MainActivity: ComponentActivity → AppCompatActivity`.
- `themes.xml: android:Theme.Material.NoActionBar → Theme.AppCompat.DayNight.NoActionBar`.
- `SurfaceView` versus `TextureView` for libVLC, and z-order or compositing in
  general. A compositing fault leaves the decoder alive rendering into something
  invisible. The decoder is destroyed.

## Things that are correct — do not "simplify" them

- `PlaybackEngine.attachSurface` / `detachSurface` detach **only** if the view
  passed in is still the active one. Two surfaces can briefly coexist while
  Compose swaps layouts.
- `MobilePlaybackOverlay` does not leave the composition when it collapses. The
  engine lives in a `remember` inside it; removing the overlay releases the
  engine and kills the audio too. The same fact is now what keeps the video
  surface alive.
- Android TV is not part of any of this. Television never collapses, and
  `PlayerHost` owns its own surface at a single call site.

## This was never a regression

The owner's installed 1.46.0-qa behaves identically once measured. No released
version has ever shown a libVLC live stream inside the strip. Do not look for
the commit that broke it; there isn't one.
