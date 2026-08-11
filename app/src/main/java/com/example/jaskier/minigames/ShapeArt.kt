package com.example.jaskier.minigames

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The ten main shapes a small kid learns first. */
enum class ShapeKind {
    CIRCLE,
    SQUARE,
    TRIANGLE,
    RECTANGLE,
    OVAL,
    STAR,
    HEART,
    DIAMOND,
    PENTAGON,
    HEXAGON,
}

/**
 * Corner points of a regular polygon, starting at [startAngleDeg] — -90° by
 * default, which puts the first point at the top so pentagons and hexagons
 * point upward the way a kid draws them.
 */
fun polygonPoints(
    sides: Int,
    center: Offset,
    radius: Float,
    startAngleDeg: Float = -90f,
): List<Offset> {
    val start = startAngleDeg * PI.toFloat() / 180f
    val stepAngle = 2f * PI.toFloat() / sides
    return List(sides) { i ->
        val angle = start + i * stepAngle
        Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
    }
}

private fun polygonPath(points: List<Offset>): Path = Path().apply {
    moveTo(points.first().x, points.first().y)
    for (point in points.drop(1)) lineTo(point.x, point.y)
    close()
}

/**
 * Draws [kind] centred on [center], [size] wide, in the soft-flat style: one
 * flat fill, chunky proportions, no gradients.
 *
 * The `when` is exhaustive on purpose — adding a [ShapeKind] should be a
 * compile error here, not a blank tile in the game.
 */
fun DrawScope.drawShape(kind: ShapeKind, center: Offset, size: Float, color: Color) {
    val r = size / 2f
    when (kind) {
        ShapeKind.CIRCLE -> drawCircle(color, r, center)

        ShapeKind.SQUARE -> drawRect(
            color,
            topLeft = Offset(center.x - r, center.y - r),
            size = Size(size, size),
        )

        ShapeKind.RECTANGLE -> drawRect(
            color,
            topLeft = Offset(center.x - r, center.y - r * 0.6f),
            size = Size(size, size * 0.6f),
        )

        ShapeKind.OVAL -> drawOval(
            color,
            topLeft = Offset(center.x - r, center.y - r * 0.7f),
            size = Size(size, size * 0.7f),
        )

        ShapeKind.TRIANGLE -> drawPath(polygonPath(polygonPoints(3, center, r)), color)

        ShapeKind.PENTAGON -> drawPath(polygonPath(polygonPoints(5, center, r)), color)

        ShapeKind.HEXAGON -> drawPath(polygonPath(polygonPoints(6, center, r)), color)

        ShapeKind.DIAMOND -> drawPath(
            polygonPath(
                listOf(
                    Offset(center.x, center.y - r),
                    Offset(center.x + r * 0.75f, center.y),
                    Offset(center.x, center.y + r),
                    Offset(center.x - r * 0.75f, center.y),
                ),
            ),
            color,
        )

        // A five-pointed star: outer points alternating with inner ones.
        ShapeKind.STAR -> {
            val outer = polygonPoints(5, center, r)
            val inner = polygonPoints(5, center, r * 0.45f, startAngleDeg = -54f)
            val points = outer.indices.flatMap { listOf(outer[it], inner[it]) }
            drawPath(polygonPath(points), color)
        }

        // Two lobes over a point, drawn as one closed Bezier path.
        ShapeKind.HEART -> {
            val heart = Path().apply {
                val top = center.y - r * 0.55f
                val bottom = center.y + r * 0.85f
                moveTo(center.x, bottom)
                cubicTo(
                    center.x - r * 1.35f, center.y - r * 0.15f,
                    center.x - r * 0.55f, top - r * 0.55f,
                    center.x, top,
                )
                cubicTo(
                    center.x + r * 0.55f, top - r * 0.55f,
                    center.x + r * 1.35f, center.y - r * 0.15f,
                    center.x, bottom,
                )
                close()
            }
            drawPath(heart, color)
        }
    }
}
