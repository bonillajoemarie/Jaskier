package com.example.jaskier

import androidx.compose.ui.geometry.Offset
import com.example.jaskier.pet.KerkerZone
import com.example.jaskier.pet.TickleDetector
import com.example.jaskier.pet.linesFor
import com.example.jaskier.pet.zoneAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KerkerTouchTest {

    private val head = Offset(500f, 500f)
    private val r = 100f

    @Test
    fun `the top of the head is the head zone`() {
        assertEquals(KerkerZone.HEAD, zoneAt(Offset(500f, 400f), head, r))
    }

    @Test
    fun `each cheek is told apart`() {
        assertEquals(KerkerZone.CHEEK_LEFT, zoneAt(Offset(430f, 520f), head, r))
        assertEquals(KerkerZone.CHEEK_RIGHT, zoneAt(Offset(570f, 520f), head, r))
    }

    @Test
    fun `the tummy sits below the head`() {
        assertEquals(KerkerZone.BELLY, zoneAt(Offset(500f, 640f), head, r))
    }

    @Test
    fun `the feet sit below the tummy`() {
        assertEquals(KerkerZone.FEET, zoneAt(Offset(500f, 760f), head, r))
    }

    @Test
    fun `a tap well away from him hits nothing`() {
        assertEquals(KerkerZone.NONE, zoneAt(Offset(50f, 50f), head, r))
    }

    @Test
    fun `the middle of the face still counts as a poke`() {
        assertTrue(zoneAt(Offset(500f, 500f), head, r) != KerkerZone.NONE)
    }

    @Test
    fun `every zone has something to say, and never repeats itself`() {
        for (zone in KerkerZone.entries) {
            val lines = linesFor(zone)
            assertTrue("${zone.name} has no lines", lines.isNotEmpty())
            assertEquals("${zone.name} repeats a line", lines.size, lines.toSet().size)
        }
    }

    @Test
    fun `a straight swipe is not a tickle`() {
        val detector = TickleDetector()
        var tickled = false
        for (i in 0 until 20) {
            tickled = tickled || detector.onMove(i * 10f, i * 30L)
        }
        assertFalse("a one-way drag must not tickle", tickled)
    }

    @Test
    fun `rubbing back and forth tickles`() {
        val detector = TickleDetector()
        var tickled = false
        val positions = listOf(0f, 40f, 0f, 40f, 0f, 40f, 0f, 40f)
        positions.forEachIndexed { i, x ->
            tickled = tickled || detector.onMove(x, i * 60L)
        }
        assertTrue("a back-and-forth rub should tickle", tickled)
    }

    @Test
    fun `slow reversals spread over a long time do not tickle`() {
        val detector = TickleDetector()
        var tickled = false
        val positions = listOf(0f, 40f, 0f, 40f, 0f, 40f)
        positions.forEachIndexed { i, x ->
            // One move every two seconds: a kid poking about, not tickling.
            tickled = tickled || detector.onMove(x, i * 2_000L)
        }
        assertFalse(tickled)
    }
}
