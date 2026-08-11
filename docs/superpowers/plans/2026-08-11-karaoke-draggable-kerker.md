# Karaoke Draggable Kerker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** In the karaoke view, let a kid drag the mini-Kerker anywhere and fling him so he tumbles — spinning, falling, and bouncing off walls and floor — until he settles and resumes dancing where he landed.

**Architecture:** A pure, Compose-free physics module (`KerkerToyPhysics.kt`) holds the toy's position, velocity, angle, and spin, and exposes a single `step()` function. `KaraokeView` owns the state, advances it in a `withFrameNanos` loop, feeds it finger positions during a drag, and hands it a fling velocity on release. The existing `drawDancingKerker` becomes a parameterised `drawKerkerToy` that accepts a center, rotation, and squish.

**Tech Stack:** Kotlin, Jetpack Compose Canvas, `withFrameNanos`, `androidx.compose.ui.input.pointer.util.VelocityTracker`, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-08-11-karaoke-draggable-kerker-design.md`

## Global Constraints

- Kotlin 2.2.10 built-in via AGP 9.3.1. Do **not** add the `org.jetbrains.kotlin.android` plugin.
- New dependencies go in `gradle/libs.versions.toml` and are referenced as `libs.*`. This plan needs no new dependencies.
- All Canvas geometry is proportional to `size.minDimension` or the passed radius — never hardcoded pixels.
- Drag progress and timing are derived from `change.uptimeMillis` deltas, never event counts.
- No fail state, no score, no time pressure — this is a kids' app.
- Physics tunables are expressed as fractions of the playground height per second, so behaviour is identical on every screen size.
- Tests live in `app/src/test/java/com/example/jaskier/` in package `com.example.jaskier`, JUnit 4 style, matching `PetStatsTest.kt`.

## File Structure

| File | Responsibility |
|---|---|
| `app/src/main/java/com/example/jaskier/minigames/KerkerToyPhysics.kt` | **Create.** Pure toy state + `step()`. No Compose runtime, no Android types. |
| `app/src/test/java/com/example/jaskier/KerkerToyPhysicsTest.kt` | **Create.** Unit tests for `step()`. |
| `app/src/main/java/com/example/jaskier/minigames/KaraokeView.kt` | **Modify.** Owns toy state, frame loop, drag and tap handling; `drawDancingKerker` → `drawKerkerToy`. |
| `app/src/main/java/com/example/jaskier/minigames/SongsScreen.kt` | **Modify.** Passes the new `onPokeKerker` callback wired to TTS giggles. |
| `CLAUDE.md` | **Modify.** Update the karaoke bullet to mention the draggable, tumbling Kerker. |

---

### Task 1: Pure toy physics

**Files:**
- Create: `app/src/main/java/com/example/jaskier/minigames/KerkerToyPhysics.kt`
- Test: `app/src/test/java/com/example/jaskier/KerkerToyPhysicsTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `data class ToyState(pos: Offset, vel: Offset, angle: Float, spin: Float, resting: Boolean)` and `fun step(state: ToyState, dt: Float, bounds: Rect): ToyState`. `bounds` constrains the toy's **center** and is expected to be pre-inset by the caller for the character's draw size. Velocities are pixels per second; `angle` and `spin` are degrees and degrees per second.

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/example/jaskier/KerkerToyPhysicsTest.kt`:

```kotlin
package com.example.jaskier

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.jaskier.minigames.ToyState
import com.example.jaskier.minigames.step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class KerkerToyPhysicsTest {

    // A 1000x2000 playground, matching a typical phone in pixels.
    private val bounds = Rect(left = 100f, top = 100f, right = 900f, bottom = 1800f)
    private val dt = 1f / 60f

    private fun flung(vel: Offset, spin: Float = 0f) = ToyState(
        pos = Offset(500f, 900f),
        vel = vel,
        angle = 0f,
        spin = spin,
        resting = false,
    )

    @Test
    fun `a resting toy is untouched by stepping`() {
        val resting = ToyState(pos = Offset(500f, 1800f), resting = true)
        assertEquals(resting, step(resting, dt, bounds))
    }

    @Test
    fun `gravity pulls a flung toy downward`() {
        val after = step(flung(Offset(0f, 0f)), dt, bounds)
        assertTrue("expected downward velocity, got ${after.vel.y}", after.vel.y > 0f)
    }

    @Test
    fun `a fling carries the toy sideways`() {
        var toy = flung(Offset(600f, -400f))
        repeat(10) { toy = step(toy, dt, bounds) }
        assertTrue("expected rightward travel, got ${toy.pos.x}", toy.pos.x > 500f)
    }

    @Test
    fun `spin advances the angle`() {
        val after = step(flung(Offset.Zero, spin = 360f), dt, bounds)
        assertTrue("expected angle to advance, got ${after.angle}", after.angle > 0f)
    }

    @Test
    fun `a floor bounce loses energy`() {
        // Drop it just above the floor moving fast downward.
        val falling = ToyState(pos = Offset(500f, 1790f), vel = Offset(0f, 2000f), resting = false)
        val after = step(falling, dt, bounds)
        assertTrue("expected an upward rebound, got ${after.vel.y}", after.vel.y < 0f)
        assertTrue("rebound must be slower than impact", abs(after.vel.y) < 2000f)
    }

    @Test
    fun `a wall bounce reverses horizontal travel`() {
        val intoWall = ToyState(pos = Offset(895f, 900f), vel = Offset(1500f, 0f), resting = false)
        val after = step(intoWall, dt, bounds)
        assertTrue("expected leftward rebound, got ${after.vel.x}", after.vel.x < 0f)
        assertTrue("must stay inside the right wall", after.pos.x <= bounds.right)
    }

    @Test
    fun `the toy never escapes its bounds`() {
        val launches = listOf(
            Offset(4000f, -4000f), Offset(-4000f, -4000f),
            Offset(0f, -6000f), Offset(5000f, 500f),
        )
        for (launch in launches) {
            var toy = flung(launch, spin = 900f)
            repeat(600) {
                toy = step(toy, dt, bounds)
                assertTrue("x escaped: ${toy.pos.x} for launch $launch", toy.pos.x in bounds.left..bounds.right)
                assertTrue("y escaped: ${toy.pos.y} for launch $launch", toy.pos.y in bounds.top..bounds.bottom)
            }
        }
    }

    @Test
    fun `a flung toy settles on the floor`() {
        var toy = flung(Offset(900f, -1200f), spin = 720f)
        repeat(1200) { toy = step(toy, dt, bounds) }
        assertTrue("expected the toy to come to rest", toy.resting)
        assertEquals("should settle on the floor", bounds.bottom, toy.pos.y, 1f)
        assertEquals("should settle upright", 0f, toy.angle, 0.001f)
    }

    @Test
    fun `a settled toy stays put`() {
        var toy = flung(Offset(500f, -800f))
        repeat(1200) { toy = step(toy, dt, bounds) }
        val settled = toy
        assertTrue(settled.resting)
        repeat(60) { toy = step(toy, dt, bounds) }
        assertEquals(settled, toy)
    }

    @Test
    fun `stepping with no elapsed time changes nothing`() {
        val toy = flung(Offset(300f, -300f))
        val after = step(toy, 0f, bounds)
        assertEquals(toy.pos, after.pos)
        assertEquals(toy.vel, after.vel)
        assertFalse(after.resting)
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.jaskier.KerkerToyPhysicsTest"`
Expected: FAIL — compilation error, `ToyState` and `step` are unresolved references.

- [ ] **Step 3: Write the implementation**

Create `app/src/main/java/com/example/jaskier/minigames/KerkerToyPhysics.kt`:

```kotlin
package com.example.jaskier.minigames

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs

// Kerker as a physical toy: the kid flings him and he tumbles until he settles.
//
// Tunables are fractions of the playground height per second, so the toy behaves
// identically on every screen size. Pure Kotlin plus Compose geometry only — no
// Compose runtime, no Android types — so it runs on the JVM under unit test.

/** Downward acceleration, in playground-heights per second squared. */
const val TOY_GRAVITY = 2.2f

/** Fraction of speed kept after hitting the floor. */
const val TOY_FLOOR_BOUNCE = 0.55f

/** Fraction of speed kept after hitting a wall or the ceiling. */
const val TOY_WALL_BOUNCE = 0.6f

/** Fraction of horizontal speed kept per floor contact — scuffing along the ground. */
const val TOY_FLOOR_FRICTION = 0.85f

/** Fraction of spin kept per bounce. */
const val TOY_SPIN_BOUNCE_DAMPING = 0.7f

/** Air resistance applied per second to both travel and spin. */
const val TOY_AIR_DRAG = 0.35f

/** Below this speed (playground-heights per second) while touching the floor, he settles. */
const val TOY_REST_SPEED = 0.35f

/** Below this spin (degrees per second) while touching the floor, he settles. */
const val TOY_REST_SPIN = 60f

data class ToyState(
    val pos: Offset = Offset.Zero,
    /** Pixels per second. */
    val vel: Offset = Offset.Zero,
    /** Degrees. */
    val angle: Float = 0f,
    /** Degrees per second. */
    val spin: Float = 0f,
    /** True once he has come to rest; a resting toy is left alone until flung again. */
    val resting: Boolean = true,
)

/**
 * Advances the toy by [dt] seconds inside [bounds], which constrains the toy's
 * **center** and must already be inset by the caller for the character's draw size.
 */
fun step(state: ToyState, dt: Float, bounds: Rect): ToyState {
    if (state.resting || dt <= 0f || bounds.width <= 0f || bounds.height <= 0f) return state

    val height = bounds.height
    val drag = (1f - TOY_AIR_DRAG * dt).coerceIn(0f, 1f)

    var vel = Offset(state.vel.x, state.vel.y + TOY_GRAVITY * height * dt) * drag
    var spin = state.spin * drag
    var pos = state.pos + vel * dt
    var angle = state.angle + spin * dt

    if (pos.x < bounds.left) {
        pos = Offset(bounds.left, pos.y)
        vel = Offset(-vel.x * TOY_WALL_BOUNCE, vel.y)
        spin *= TOY_SPIN_BOUNCE_DAMPING
    } else if (pos.x > bounds.right) {
        pos = Offset(bounds.right, pos.y)
        vel = Offset(-vel.x * TOY_WALL_BOUNCE, vel.y)
        spin *= TOY_SPIN_BOUNCE_DAMPING
    }

    if (pos.y < bounds.top) {
        pos = Offset(pos.x, bounds.top)
        vel = Offset(vel.x, -vel.y * TOY_WALL_BOUNCE)
        spin *= TOY_SPIN_BOUNCE_DAMPING
    }

    var onFloor = false
    if (pos.y > bounds.bottom) {
        pos = Offset(pos.x, bounds.bottom)
        vel = Offset(vel.x * TOY_FLOOR_FRICTION, -vel.y * TOY_FLOOR_BOUNCE)
        spin *= TOY_SPIN_BOUNCE_DAMPING
        onFloor = true
    }

    // Settle: once he is barely moving on the ground, stand him back up and stop
    // simulating. Without this he jitters on the floor forever.
    if (onFloor && vel.getDistance() < TOY_REST_SPEED * height && abs(spin) < TOY_REST_SPIN) {
        return ToyState(pos = pos, vel = Offset.Zero, angle = 0f, spin = 0f, resting = true)
    }

    return ToyState(pos = pos, vel = vel, angle = angle, spin = spin, resting = false)
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.jaskier.KerkerToyPhysicsTest"`
Expected: PASS, 10 tests.

If `a flung toy settles on the floor` fails because the toy is still bouncing after 1200 steps (20 seconds), the rest thresholds are too tight — this is the one number most likely to need adjusting. Raise `TOY_REST_SPEED` toward `0.5f` rather than weakening `TOY_FLOOR_BOUNCE`, so flings still feel bouncy.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/jaskier/minigames/KerkerToyPhysics.kt app/src/test/java/com/example/jaskier/KerkerToyPhysicsTest.kt
git commit -m "feat: add pure tumble physics for the karaoke Kerker toy"
```

---

### Task 2: Parameterise the karaoke Kerker drawing

Rendering change only — no behaviour change yet. Afterwards Kerker still sits in his corner and bops exactly as before, but the drawing function now accepts a position, rotation, and squish.

**Files:**
- Modify: `app/src/main/java/com/example/jaskier/minigames/KaraokeView.kt` (replace `drawDancingKerker` at lines 450–515 and its call site at line 122)

**Interfaces:**
- Consumes: nothing from Task 1 yet.
- Produces: `private fun DrawScope.drawKerkerToy(center: Offset, radius: Float, rotation: Float, squish: Float, airborne: Boolean, groundY: Float, beat: Float, isPlaying: Boolean)`. `squish` is a horizontal scale factor; the vertical scale is its reciprocal, which preserves volume. `groundY` is the y of the floor, used to place and fade the contact shadow.

- [ ] **Step 1: Replace the drawing function**

In `KaraokeView.kt`, delete `drawDancingKerker` entirely and put this in its place:

```kotlin
// Kerker as a draggable toy: he bops in place when settled, and spins freely
// while airborne. Geometry is proportional to [radius] so the same code serves
// any size.
private fun DrawScope.drawKerkerToy(
    center: Offset,
    radius: Float,
    rotation: Float,
    squish: Float,
    airborne: Boolean,
    groundY: Float,
    beat: Float,
    isPlaying: Boolean,
) {
    val r = radius
    val dancing = isPlaying && !airborne
    val bounce = if (dancing) kotlin.math.abs(sin(beat * TWO_PI)) * r * 0.35f else 0f
    val tilt = if (dancing) sin(beat * TWO_PI) * 8f else 0f
    val c = Offset(center.x, center.y - bounce)
    val skin = Color(0xFFF0C09A)

    // Contact shadow, fading out the higher he flies.
    val lift = (groundY - center.y).coerceAtLeast(0f)
    val shadowAlpha = 0.2f * (1f - lift / (r * 8f)).coerceIn(0f, 1f)
    if (shadowAlpha > 0.01f) {
        drawOval(
            Color.Black.copy(alpha = shadowAlpha),
            topLeft = Offset(c.x - r * 0.9f, groundY + r * 1.05f),
            size = Size(r * 1.8f, r * 0.3f),
        )
    }

    scale(scaleX = squish, scaleY = 1f / squish, pivot = c) {
        rotate(degrees = rotation + tilt, pivot = c) {
            drawOval(
                Brush.radialGradient(
                    listOf(Color(0xFFFAD9B8), skin, Color(0xFFC99669)),
                    center = c - Offset(r * 0.3f, r * 0.5f),
                    radius = r * 2.2f,
                ),
                topLeft = Offset(c.x - r, c.y - r * 1.15f),
                size = Size(r * 2f, r * 2.3f),
            )
            // vest
            drawArc(
                Color.White,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = true,
                topLeft = Offset(c.x - r, c.y - r * 1.15f),
                size = Size(r * 2f, r * 2.3f),
            )
            // hair
            for (dx in listOf(-0.5f, -0.17f, 0.17f, 0.5f)) {
                drawCircle(Color(0xFF32241B), r * 0.26f, Offset(c.x + dx * r, c.y - r * 0.95f))
            }
            // eyes + open singing mouth
            for (side in listOf(-1f, 1f)) {
                drawCircle(Color.White, r * 0.2f, Offset(c.x + side * r * 0.38f, c.y - r * 0.4f))
                drawCircle(Color(0xFF2A180C), r * 0.1f, Offset(c.x + side * r * 0.38f, c.y - r * 0.42f))
            }
            drawOval(
                Color(0xFF2C2C2C),
                topLeft = Offset(c.x - r * 0.22f, c.y - r * 0.05f),
                size = Size(r * 0.44f, r * 0.4f),
            )
        }
    }

    // Floating music notes, only while he is settled and singing.
    if (dancing) {
        for (i in 0 until 3) {
            val phase = (beat + i / 3f) % 1f
            val nx = c.x - r * (1.6f + i * 0.5f)
            val ny = c.y - r * (1.2f + phase * 2.2f)
            val alpha = 1f - phase
            drawCircle(Color.White.copy(alpha = alpha), r * 0.12f, Offset(nx, ny))
            drawLine(
                Color.White.copy(alpha = alpha),
                Offset(nx + r * 0.11f, ny),
                Offset(nx + r * 0.11f, ny - r * 0.4f),
                strokeWidth = r * 0.06f,
                cap = StrokeCap.Round,
            )
        }
    }
}
```

- [ ] **Step 2: Add the import for `scale`**

In the import block of `KaraokeView.kt`, beside the existing `rotate` import:

```kotlin
import androidx.compose.ui.graphics.drawscope.scale
```

- [ ] **Step 3: Update the call site to preserve current behaviour**

In the `Canvas` block, replace `drawDancingKerker(beat, isPlaying)` with:

```kotlin
val toyRadius = size.minDimension * 0.09f
drawKerkerToy(
    center = Offset(size.width * 0.82f, size.height * 0.86f),
    radius = toyRadius,
    rotation = 0f,
    squish = 1f,
    airborne = false,
    groundY = size.height * 0.86f,
    beat = beat,
    isPlaying = isPlaying,
)
```

- [ ] **Step 4: Verify it builds and looks unchanged**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Install and open any song's karaoke view. Kerker must look and behave exactly as he did before this task — same corner, same bop, same notes, same shadow. This is a pure refactor; any visible difference is a bug.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/jaskier/minigames/KaraokeView.kt
git commit -m "refactor: parameterise the karaoke Kerker drawing by position and rotation"
```

---

### Task 3: Drag, fling, and tumble

**Files:**
- Modify: `app/src/main/java/com/example/jaskier/minigames/KaraokeView.kt`

**Interfaces:**
- Consumes: `ToyState` and `step(state, dt, bounds)` from Task 1; `drawKerkerToy(...)` from Task 2.
- Produces: nothing for later tasks beyond the `toy` state that Task 4's tap handler hit-tests against.

- [ ] **Step 1: Add the imports**

```kotlin
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
```

- [ ] **Step 2: Add the toy state and playground geometry**

Inside `KaraokeView`, after the `burstAt` declaration:

```kotlin
    // Kerker is a physical toy the kid can fling around the scene.
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var toy by remember { mutableStateOf(ToyState()) }
    var grabbed by remember { mutableStateOf(false) }

    val toyRadius = if (canvasSize == IntSize.Zero) 0f else {
        minOf(canvasSize.width, canvasSize.height) * 0.09f
    }
    // Bounds constrain his centre: inset by his drawn size, with the floor at 90%
    // of the screen so he never lands under the "tap for stars" caption.
    val playground = remember(canvasSize, toyRadius) {
        if (toyRadius <= 0f) Rect.Zero else Rect(
            left = toyRadius,
            top = toyRadius * 1.2f,
            right = canvasSize.width - toyRadius,
            bottom = canvasSize.height * 0.90f,
        )
    }

    // Drop him into his home corner once the layout size is known.
    LaunchedEffect(playground) {
        if (playground != Rect.Zero && toy.pos == Offset.Zero) {
            toy = ToyState(
                pos = Offset(canvasSize.width * 0.82f, playground.bottom),
                resting = true,
            )
        }
    }

    // Frame loop: only runs the integrator while he is actually in flight.
    LaunchedEffect(playground) {
        var lastFrame = 0L
        while (isActive) {
            withFrameNanos { now ->
                val dt = if (lastFrame == 0L) 0f else {
                    ((now - lastFrame) / 1_000_000_000f).coerceAtMost(0.05f)
                }
                lastFrame = now
                if (!grabbed && !toy.resting) toy = step(toy, dt, playground)
            }
        }
    }
```

- [ ] **Step 3: Add the drag handler**

Add `.onSizeChanged { canvasSize = it }` to the root `Box`'s modifier chain, and a second `pointerInput` block after the existing tap one:

```kotlin
            .onSizeChanged { canvasSize = it }
            .pointerInput(playground) {
                val tracker = VelocityTracker()
                detectDragGestures(
                    onDragStart = { start ->
                        // Generous grab radius — little fingers aren't precise.
                        if (playground != Rect.Zero &&
                            (start - toy.pos).getDistance() < toyRadius * 1.8f
                        ) {
                            grabbed = true
                            tracker.resetTracking()
                            toy = toy.copy(vel = Offset.Zero, spin = 0f, resting = false)
                        }
                    },
                    onDrag = { change, _ ->
                        if (grabbed) {
                            tracker.addPosition(change.uptimeMillis, change.position)
                            // He leans in the direction he's being pulled.
                            val lean = ((change.position.x - toy.pos.x) * 3f).coerceIn(-18f, 18f)
                            toy = toy.copy(pos = change.position, angle = lean)
                        }
                    },
                    onDragEnd = {
                        if (grabbed) {
                            grabbed = false
                            val v = tracker.calculateVelocity()
                            toy = toy.copy(
                                vel = Offset(v.x, v.y),
                                spin = (v.x / 8f).coerceIn(-900f, 900f),
                                resting = false,
                            )
                        }
                    },
                    onDragCancel = {
                        if (grabbed) {
                            grabbed = false
                            toy = toy.copy(vel = Offset.Zero, spin = 0f, resting = false)
                        }
                    },
                )
            },
```

Note: `ToyState`'s properties are `pos` and `angle`, not `position` and `lean` — the lean *is* the angle while he is held.

- [ ] **Step 4: Draw him at his live position**

Replace the Task 2 call site inside the `Canvas` block with:

```kotlin
            if (playground != Rect.Zero) {
                drawKerkerToy(
                    center = toy.pos,
                    radius = toyRadius,
                    rotation = toy.angle,
                    squish = 1f,
                    airborne = !toy.resting,
                    groundY = playground.bottom,
                    beat = beat,
                    isPlaying = isPlaying,
                )
            }
```

- [ ] **Step 5: Build and verify by hand**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

Install, open a song, and check each of these on a device or the `jaskier_test` AVD (launch headless emulators with `-gpu swangle_indirect`; the default `swiftshader_indirect` segfaults on this machine):

1. Kerker starts in his usual corner, bopping.
2. Dragging him picks him up and he follows the finger with a lean.
3. Releasing while moving flings him — he spins, arcs, and falls.
4. He bounces off the left and right walls, the top, and the floor, losing energy each time.
5. He settles upright somewhere and resumes bopping there, with his shadow beneath him.
6. He never leaves the screen or lands under the bottom caption.
7. Dragging on empty space does nothing to him.
8. Tapping empty space still produces a star burst.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/jaskier/minigames/KaraokeView.kt
git commit -m "feat: let kids drag and fling Kerker around the karaoke scene"
```

---

### Task 4: Tap-to-poke, giggles, and docs

**Files:**
- Modify: `app/src/main/java/com/example/jaskier/minigames/KaraokeView.kt`
- Modify: `app/src/main/java/com/example/jaskier/minigames/SongsScreen.kt` (the `KaraokeView(...)` call at lines 79–89)
- Modify: `CLAUDE.md` (the `SongsScreen.kt` + `KaraokeView.kt` bullet in the Architecture section)

**Interfaces:**
- Consumes: `toy` and `toyRadius` from Task 3.
- Produces: `KaraokeView` gains a parameter `onPokeKerker: () -> Unit = {}`, called when a tap lands on Kerker.

- [ ] **Step 1: Add the parameter and the squish animation**

Add to `KaraokeView`'s signature, after `onClose`:

```kotlin
    onPokeKerker: () -> Unit = {},
```

And beside the existing `burst` Animatable:

```kotlin
    // Poking him squishes him, volume-preserving: wider means shorter.
    val squish = remember { Animatable(1f) }
```

- [ ] **Step 2: Extend the tap handler**

Replace the body of the existing `detectTapGestures { offset -> ... }` with:

```kotlin
                detectTapGestures { offset ->
                    burstAt = offset
                    val hitKerker = playground != Rect.Zero &&
                        (offset - toy.pos).getDistance() < toyRadius * 1.6f
                    if (hitKerker) onPokeKerker()
                    scope.launch {
                        if (hitKerker) {
                            squish.snapTo(1.28f)
                            squish.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 380f))
                        }
                    }
                    scope.launch {
                        burst.snapTo(0f)
                        burst.animateTo(1f, tween(600))
                        burst.snapTo(0f)
                    }
                }
```

Two separate `launch` blocks so the squish and the star burst run concurrently — a single block would make the stars wait for the squish, and neither may block input.

Add the import:

```kotlin
import androidx.compose.animation.core.spring
```

- [ ] **Step 3: Feed the squish into the drawing**

In the `drawKerkerToy(...)` call from Task 3, change `squish = 1f` to:

```kotlin
                    squish = squish.value,
```

- [ ] **Step 4: Wire the giggles in SongsScreen**

In `SongsScreen.kt`, above the `if (karaokeSong != null)` block:

```kotlin
    // Rotating giggles so repeat pokes don't say the same thing, mirroring PetHomeScreen.
    val giggles = remember { listOf("Hehe! That tickles!", "Wheee!", "Hi hi hi!", "Do it again!") }
    var giggleIndex by remember { mutableIntStateOf(0) }
```

Add the imports `androidx.compose.runtime.mutableIntStateOf` and `androidx.compose.runtime.setValue` if they are not already present, then pass the callback in the `KaraokeView(...)` call:

```kotlin
            onPokeKerker = {
                tts.speak(giggles[giggleIndex % giggles.size])
                giggleIndex++
            },
```

- [ ] **Step 5: Build, test, and verify by hand**

Run: `./gradlew assembleDebug && ./gradlew test`
Expected: BUILD SUCCESSFUL and all unit tests pass, including `KerkerToyPhysicsTest`.

On a device: tapping Kerker squishes him, speaks a giggle, and bursts stars; the giggle line changes each tap; tapping elsewhere still only bursts stars; tapping him repeatedly and fast never stalls or queues up.

- [ ] **Step 6: Update CLAUDE.md**

Replace the `minigames/SongsScreen.kt` + `KaraokeView.kt` bullet in the Architecture section with:

```markdown
- `minigames/SongsScreen.kt` + `KaraokeView.kt` — song list → full-screen karaoke: line highlight
  synced to `player.positionMs()` (lines weighted by length), themed animated background chosen by
  song id (night sky / floating glyphs / farm / clock / meadow / confetti / steam / snow / sun),
  tap-anywhere star bursts, and a draggable Kerker: kids fling him and he tumbles around the scene
  — spinning, falling, and bouncing off the walls and floor — until he settles and dances where he
  landed. Physics live in `minigames/KerkerToyPhysics.kt` as a pure `step()` function
  (unit-tested in `KerkerToyPhysicsTest`); tapping him squishes him and speaks a giggle.
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/jaskier/minigames/KaraokeView.kt app/src/main/java/com/example/jaskier/minigames/SongsScreen.kt CLAUDE.md
git commit -m "feat: poke the karaoke Kerker for a squish and a giggle"
```

---

## Verification

After all four tasks:

```bash
./gradlew test          # all unit tests, including KerkerToyPhysicsTest
./gradlew lint          # Android Lint
./gradlew assembleDebug # debug APK
```

Then run through the Task 3 Step 5 checklist plus the Task 4 Step 5 checks once more on a real device, since the physics feel is the whole point and cannot be asserted in a unit test.
