# Draggable, tumbling Kerker in the karaoke view

Date: 2026-08-11

## Goal

In the karaoke view (`minigames/KaraokeView.kt`), the mini-Kerker is currently pinned at a fixed
spot (82% width, 86% height) and only bops to the beat. Make him a physical toy: a kid can drag
him anywhere on the scene and fling him, after which he tumbles — spinning through the air, falling
under gravity, bouncing off the walls and floor with damping — until he settles and resumes dancing
wherever he landed.

## Behaviour

- **Drag**: pressing within roughly 1.4x his radius grabs him. While held he follows the finger
  directly (physics paused) with a slight lean in the direction of motion. A drag that starts
  anywhere else is ignored, so the rest of the screen behaves exactly as it does today.
- **Fling**: on release he keeps the finger's velocity, gains spin proportional to horizontal
  speed, and falls under gravity.
- **Bounce**: he collides with the left/right walls, a ceiling at the top of the screen, and a
  floor at 90% of screen height. Each bounce loses energy; the floor also applies friction and
  damps spin.
- **Settle**: below a small speed-and-spin threshold he snaps upright, marks himself resting, and
  resumes the existing beat bounce, tilt, and floating music notes at his new location.
- **Tap**: a tap anywhere still produces the star burst. A tap that lands on Kerker additionally
  squishes him and fires a giggle via TTS.
- **Bounds**: the whole screen is his playground; he may pass over the lyrics. He is drawn in the
  background `Canvas`, so the lyric `Column` renders on top of him and the words stay readable.
- No fail state, no score — this is pure play, consistent with the rest of the app.

## Architecture

### 1. `minigames/KerkerToyPhysics.kt` (new, pure)

```kotlin
data class ToyState(
    val pos: Offset,
    val vel: Offset,     // pixels per second
    val angle: Float,    // degrees
    val spin: Float,     // degrees per second
    val resting: Boolean,
)

fun step(state: ToyState, dt: Float, bounds: Rect, radius: Float): ToyState
```

Only `Offset` and `Rect` from Compose geometry — no Compose runtime, no Android types, so it runs
on the JVM. Constants (all as fractions of screen height per second, so behaviour is
resolution-independent):

- gravity ~2.2 screen-heights/s^2
- restitution 0.55 on the floor, 0.6 on walls and ceiling
- floor friction 0.85 applied to horizontal velocity per floor contact
- spin damped 0.7 per bounce, plus mild per-second air damping
- rest threshold: speed and spin both below a small epsilon while touching the floor; on rest the
  angle eases to 0 and `resting` becomes true

This mirrors how `pet/PetStats.kt` and `minigames/Match3Logic.kt` isolate pure rules from Compose.

### 2. `minigames/KaraokeView.kt` (modified)

- `Modifier.onSizeChanged` on the root `Box` supplies the playground `Rect` (full width inset by
  his radius; top at 0, floor at 90% height). His `ToyState` is initialised once at the current
  home spot.
- A `withFrameNanos` loop (same shape as `CatchGameScreen`) advances the physics with the real
  frame delta, and idles cheaply while he is resting or grabbed.
- `detectDragGestures` handles grab/move/release. Release velocity comes from a Compose
  `VelocityTracker` fed with pointer positions and `uptimeMillis` — time-based, per the repo rule
  that drag progress must never be derived from event counts.
- `detectTapGestures` keeps the star burst and adds the on-Kerker squish plus a new
  `onPokeKerker: () -> Unit` parameter.
- `drawDancingKerker` becomes `drawKerkerToy(center, rotation, squish, beat, isPlaying)`. Resting:
  today's beat bounce, tilt, contact shadow, and music notes. Airborne: shadow fades out with
  height and the notes stop.

### 3. `minigames/SongsScreen.kt` (modified)

Passes `onPokeKerker` to `KaraokeView`, wired to a rotating list of TTS giggles — the same pattern
`pet/PetHomeScreen.kt` uses for `PetCanvas.onPoke`.

## Testing

`app/src/test/.../KerkerToyPhysicsTest.kt` covers the pure `step` function:

- a fling arcs downward: after several steps, vertical velocity increased and the position moved
  in the direction of the initial horizontal velocity
- a bounce loses energy: speed after a floor collision is strictly less than before
- containment: from a variety of starting velocities, the position never leaves `bounds` after any
  number of steps
- settling: a toy dropped from rest reaches `resting = true` within a bounded number of steps, and
  a resting toy left alone stays put

Compose-side behaviour (drag, tap, drawing) is not unit-tested, consistent with the rest of the
app; verified by building and running.

## Out of scope

- Any physics for the home-screen `PetCanvas` — that Kerker stays as he is.
- Collisions with background scenery, multiple Kerkers, or persisting his position across songs.
