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
