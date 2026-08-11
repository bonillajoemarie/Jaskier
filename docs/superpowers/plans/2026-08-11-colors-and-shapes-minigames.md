# Colors and Shapes Mini-Games Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Two new tap-to-announce mini-games — 11 main colors and 10 main shapes — that speak their name when tapped, exactly like the existing ABC and 123 games.

**Architecture:** `AnnounceItem` gains a `TileArt` describing how its tile is drawn (`Glyph`, `Swatch`, `Shape`), defaulting to `Glyph` so ABC and 123 are untouched. `AnnounceGridScreen` switches on it. Shapes are code-drawn in a new `ShapeArt.kt` from a `ShapeKind` enum, with the polygon maths extracted as a pure function so it can be unit-tested.

**Tech Stack:** Kotlin, Jetpack Compose Canvas, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-08-11-colors-and-shapes-minigames-design.md`

## Global Constraints

- Kotlin 2.2.10 built-in via AGP 9.3.1. No `org.jetbrains.kotlin.android` plugin. No new dependencies.
- Canvas geometry proportional to the drawing size — never hardcoded pixels.
- No scoring, no fail state, no timing — self-paced tap-to-hear, like ABC and 123.
- Touch targets stay ≥64dp (the grid's 88dp adaptive cells already satisfy this).
- Every tap speaks, via `TtsManager`.
- Tests live in `app/src/test/java/com/example/jaskier/`, package `com.example.jaskier`, JUnit 4.

## File Structure

| File | Responsibility |
|---|---|
| `app/src/main/java/com/example/jaskier/minigames/ShapeArt.kt` | **Create.** `ShapeKind` enum, pure `polygonPoints`, and `drawShape`. |
| `app/src/test/java/com/example/jaskier/ShapeArtTest.kt` | **Create.** Polygon maths and enum completeness. |
| `app/src/main/java/com/example/jaskier/minigames/MiniGame.kt` | **Modify.** `TileArt`, `AnnounceItem.art`, the two new games. |
| `app/src/test/java/com/example/jaskier/MiniGameCatalogTest.kt` | **Create.** Catalog integrity. |
| `app/src/main/java/com/example/jaskier/minigames/AnnounceGridScreen.kt` | **Modify.** Render tiles by art. |
| `CLAUDE.md` | **Modify.** Mention the two new games. |

---

### Task 1: Shape drawing

**Files:**
- Create: `app/src/main/java/com/example/jaskier/minigames/ShapeArt.kt`
- Test: `app/src/test/java/com/example/jaskier/ShapeArtTest.kt`

**Interfaces:**
- Produces: `enum class ShapeKind { CIRCLE, SQUARE, TRIANGLE, RECTANGLE, OVAL, STAR, HEART, DIAMOND, PENTAGON, HEXAGON }`, `fun polygonPoints(sides: Int, center: Offset, radius: Float, startAngleDeg: Float = -90f): List<Offset>`, and `fun DrawScope.drawShape(kind: ShapeKind, center: Offset, size: Float, color: Color)` where `size` is the shape's full width.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.example.jaskier

import androidx.compose.ui.geometry.Offset
import com.example.jaskier.minigames.ShapeKind
import com.example.jaskier.minigames.polygonPoints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ShapeArtTest {

    private val center = Offset(100f, 100f)

    @Test
    fun `there is a shape for each of the ten main shapes`() {
        assertEquals(10, ShapeKind.entries.size)
    }

    @Test
    fun `a polygon has one point per side`() {
        assertEquals(5, polygonPoints(5, center, 50f).size)
        assertEquals(6, polygonPoints(6, center, 50f).size)
    }

    @Test
    fun `every polygon point sits on the circle`() {
        for (point in polygonPoints(6, center, 50f)) {
            val distance = (point - center).getDistance()
            assertEquals(50f, distance, 0.01f)
        }
    }

    @Test
    fun `a polygon starts at the top so shapes point upward`() {
        val first = polygonPoints(5, center, 50f).first()
        assertEquals(center.x, first.x, 0.01f)
        assertTrue("expected the first point above centre", first.y < center.y)
    }

    @Test
    fun `polygon points are evenly spaced`() {
        val points = polygonPoints(6, center, 50f)
        val gaps = points.indices.map { i ->
            (points[(i + 1) % points.size] - points[i]).getDistance()
        }
        for (gap in gaps) assertTrue("uneven gap $gap vs ${gaps[0]}", abs(gap - gaps[0]) < 0.01f)
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.jaskier.ShapeArtTest"`
Expected: FAIL — unresolved `ShapeKind`, `polygonPoints`.

- [ ] **Step 3: Implement**

Create `ShapeArt.kt` with the enum, the pure `polygonPoints`, and a `drawShape` that switches exhaustively over `ShapeKind` (no `else`, so a new shape is a compile error rather than a blank tile). Shapes are drawn soft-flat: a single flat fill, chunky proportions, no gradients.

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.jaskier.ShapeArtTest"`
Expected: PASS, 5 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/jaskier/minigames/ShapeArt.kt app/src/test/java/com/example/jaskier/ShapeArtTest.kt
git commit -m "feat: add code-drawn shapes for the shapes mini-game"
```

---

### Task 2: Catalog entries

**Files:**
- Modify: `app/src/main/java/com/example/jaskier/minigames/MiniGame.kt`
- Test: `app/src/test/java/com/example/jaskier/MiniGameCatalogTest.kt`

**Interfaces:**
- Consumes: `ShapeKind` (Task 1).
- Produces: `sealed interface TileArt` with `data object Glyph`, `data class Swatch(val color: Color)`, `data class Shape(val kind: ShapeKind)`; `AnnounceItem(display, utterance, art: TileArt = TileArt.Glyph)`; games with ids `"colors"` and `"shapes"` in `MiniGames`.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.example.jaskier

import com.example.jaskier.minigames.AnnounceGame
import com.example.jaskier.minigames.MiniGames
import com.example.jaskier.minigames.TileArt
import com.example.jaskier.minigames.miniGameById
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniGameCatalogTest {

    private fun announceGame(id: String) = miniGameById(id) as AnnounceGame

    @Test
    fun `every game id is unique`() {
        val ids = MiniGames.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every game is resolvable by id`() {
        for (game in MiniGames) assertEquals(game, miniGameById(game.id))
    }

    @Test
    fun `the colors game covers the eleven main colors`() {
        assertEquals(11, announceGame("colors").items.size)
    }

    @Test
    fun `the shapes game covers the ten main shapes`() {
        assertEquals(10, announceGame("shapes").items.size)
    }

    @Test
    fun `every announce item says something`() {
        for (game in MiniGames.filterIsInstance<AnnounceGame>()) {
            for (item in game.items) {
                assertTrue("${game.id} has a blank utterance", item.utterance.isNotBlank())
            }
        }
    }

    @Test
    fun `colors are swatches and shapes are shapes`() {
        for (item in announceGame("colors").items) {
            assertTrue("${item.display} should be a swatch", item.art is TileArt.Swatch)
        }
        for (item in announceGame("shapes").items) {
            assertTrue("${item.display} should be a shape", item.art is TileArt.Shape)
        }
    }

    @Test
    fun `letters and numbers still render as plain glyphs`() {
        for (id in listOf("abc", "numbers")) {
            for (item in announceGame(id).items) {
                assertEquals(TileArt.Glyph, item.art)
            }
        }
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.jaskier.MiniGameCatalogTest"`
Expected: FAIL — unresolved `TileArt`, and no `colors`/`shapes` games.

- [ ] **Step 3: Implement**

Add `TileArt` and the `art` parameter to `AnnounceItem`, then append the two games to `MiniGames` after `numbers`, using the colors and shapes tables from the spec.

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.jaskier.MiniGameCatalogTest"`
Expected: PASS, 7 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/jaskier/minigames/MiniGame.kt app/src/test/java/com/example/jaskier/MiniGameCatalogTest.kt
git commit -m "feat: add colors and shapes to the mini-game catalog"
```

---

### Task 3: Render the new tiles

**Files:**
- Modify: `app/src/main/java/com/example/jaskier/minigames/AnnounceGridScreen.kt:90-148`
- Modify: `CLAUDE.md`

**Interfaces:**
- Consumes: `TileArt` (Task 2), `drawShape` (Task 1).

- [ ] **Step 1: Render by art**

`AnnounceCell` switches on `item.art`:

- `Glyph` — today's behaviour exactly: the display string in white at 34sp.
- `Swatch` — the cell itself becomes that color, with a thin neutral outline so white and black tiles still read as tiles, and the name beneath in small text whose color flips to dark on light swatches.
- `Shape` — the rainbow cell background as today, with the shape drawn in white on a `Canvas` and the name beneath in small white text.

The written names are for parents; the audio is the interface, per `kids-ux`.

- [ ] **Step 2: Build and run the suite**

Run: `./gradlew assembleDebug && ./gradlew test && ./gradlew lint`
Expected: all green.

- [ ] **Step 3: Verify on a device**

Open Colors: 11 tiles, each tapping speaks its name; white and black are visibly tiles, not holes. Open Shapes: 10 distinct silhouettes, each speaking its name. ABC and 123 look and behave exactly as before.

- [ ] **Step 4: Update CLAUDE.md and commit**

```bash
git add app/src/main/java/com/example/jaskier/minigames/AnnounceGridScreen.kt CLAUDE.md
git commit -m "feat: render color swatches and shape tiles in the announce grid"
```

---

## Verification

```bash
./gradlew test && ./gradlew lint && ./gradlew assembleDebug
```

Plus the device check in Task 3 Step 3.
