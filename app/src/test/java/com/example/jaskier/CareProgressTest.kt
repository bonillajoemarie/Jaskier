package com.example.jaskier

import com.example.jaskier.care.SNAP_TO_CLEAN
import com.example.jaskier.care.scrubProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CareProgressTest {

    private val fill = 300f // milliseconds to clean one target outright

    @Test
    fun `scrubbing the target makes progress`() {
        assertEquals(0.5f, scrubProgress(0f, dtMillis = 150f, fill, onTarget = true), 0.001f)
    }

    @Test
    fun `scrubbing beside a target still helps its neighbour`() {
        val bled = scrubProgress(0f, dtMillis = 150f, fill, onTarget = false, nearTarget = true)
        assertTrue("expected some bleed, got $bled", bled > 0f)
        assertTrue("bleed must be slower than a direct scrub", bled < 0.5f)
    }

    @Test
    fun `just moving the tool always earns something`() {
        val trickle = scrubProgress(0f, dtMillis = 100f, fill, onTarget = false, toolMoving = true)
        assertTrue("a moving tool must never earn nothing, got $trickle", trickle > 0f)
    }

    @Test
    fun `a tool that touches nothing and does not move earns nothing`() {
        assertEquals(0.2f, scrubProgress(0.2f, dtMillis = 100f, fill, onTarget = false), 0.0001f)
    }

    @Test
    fun `nearly-clean snaps clean so no one hunts the last sliver`() {
        val nearlyDone = SNAP_TO_CLEAN - 0.01f
        assertEquals(1f, scrubProgress(nearlyDone, dtMillis = 20f, fill, onTarget = true), 0.0001f)
    }

    @Test
    fun `progress never goes backwards and never exceeds one`() {
        var progress = 0f
        repeat(200) {
            val next = scrubProgress(progress, dtMillis = 16f, fill, onTarget = true)
            assertTrue("went backwards: $progress -> $next", next >= progress)
            assertTrue("overshot: $next", next <= 1f)
            progress = next
        }
        assertEquals(1f, progress, 0.0001f)
    }

    @Test
    fun `an already-clean target stays clean`() {
        assertEquals(1f, scrubProgress(1f, dtMillis = 999f, fill, onTarget = false), 0.0001f)
    }
}
