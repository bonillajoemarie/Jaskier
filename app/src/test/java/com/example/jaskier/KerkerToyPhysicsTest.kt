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
