package com.example.jaskier.care

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin

// Feedback every care routine owes a pre-reader.
//
// Two rules from kids-ux drive this file: silence teaches a kid the app is
// broken, so no tap may ever do nothing; and instructions should be
// demonstrated rather than written, because the user cannot read.

private const val TWO_PI = 2f * Math.PI.toFloat()

/** Show the demo hand once a step has stalled this long. */
const val DEMO_AFTER_MILLIS = 3_000L

/**
 * A translucent hand that taps [target] on a loop, showing the kid exactly
 * what to do. [t] runs 0..1 and repeats.
 */
fun DrawScope.drawGhostHand(target: Offset, t: Float) {
    val d = size.minDimension
    // Down on the first half of the cycle, back up on the second.
    val press = if (t < 0.5f) t * 2f else (1f - t) * 2f
    val at = target + Offset(d * 0.06f, d * 0.10f - press * d * 0.045f)
    val alpha = 0.5f + press * 0.3f

    // tap ripple under the fingertip
    if (press > 0.7f) {
        val ripple = (press - 0.7f) / 0.3f
        drawCircle(
            Color.White.copy(alpha = 0.55f * (1f - ripple)),
            d * 0.05f * (0.4f + ripple),
            target,
        )
    }

    val handWhite = Color.White.copy(alpha = alpha)
    val ink = Color(0xFF3A2A1E).copy(alpha = alpha * 0.5f)
    // palm
    drawCircle(handWhite, d * 0.045f, at + Offset(0f, d * 0.045f))
    drawCircle(ink, d * 0.045f, at + Offset(0f, d * 0.045f), style = androidx.compose.ui.graphics.drawscope.Stroke(d * 0.005f))
    // pointing finger
    val finger = Path().apply {
        moveTo(at.x - d * 0.012f, at.y + d * 0.05f)
        lineTo(at.x - d * 0.012f, at.y - d * 0.03f)
        quadraticTo(at.x, at.y - d * 0.05f, at.x + d * 0.012f, at.y - d * 0.03f)
        lineTo(at.x + d * 0.012f, at.y + d * 0.05f)
        close()
    }
    drawPath(finger, handWhite)
}

/**
 * A drag demo: the ghost hand slides from [from] to [to] and starts over,
 * for steps where dragging is the richer (but never required) path.
 */
fun DrawScope.drawGhostDrag(from: Offset, to: Offset, t: Float) {
    val eased = if (t < 0.75f) t / 0.75f else 1f
    val at = Offset(
        from.x + (to.x - from.x) * eased,
        from.y + (to.y - from.y) * eased,
    )
    drawGhostHand(at, if (t < 0.75f) 0.8f else 0.2f)
}

/** Every tap does something, even one that lands on nothing. */
fun DrawScope.drawTapSpark(at: Offset, t: Float) {
    if (t <= 0f || t >= 1f) return
    val d = size.minDimension
    for (i in 0 until 6) {
        val angle = i / 6f * TWO_PI
        val dist = d * 0.055f * t
        drawCircle(
            Color.White.copy(alpha = (1f - t) * 0.9f),
            d * 0.013f * (1f - t * 0.5f),
            at + Offset(cos(angle) * dist, sin(angle) * dist),
        )
    }
}

/** Progress as a picture, never a number: a filling row of stars. */
fun DrawScope.drawCareStars(filled: Int, total: Int) {
    if (total <= 0) return
    val d = size.minDimension
    val r = d * 0.03f
    val gap = r * 2.7f
    val startX = size.width / 2f - (total - 1) * gap / 2f
    for (i in 0 until total) {
        val at = Offset(startX + i * gap, size.height * 0.055f)
        val done = i < filled
        drawStar(at, r, if (done) Color(0xFFFFD54F) else Color(0x33000000))
    }
}

private fun DrawScope.drawStar(center: Offset, r: Float, color: Color) {
    val path = Path()
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) r else r * 0.45f
        val angle = -Math.PI.toFloat() / 2f + i * TWO_PI / 10f
        val x = center.x + cos(angle) * radius
        val y = center.y + sin(angle) * radius
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

/** A soft glow that marks whatever the kid should touch next. */
fun DrawScope.drawTargetHalo(at: Offset, radius: Float, pulse: Float) {
    drawCircle(Color.White.copy(alpha = 0.22f), radius * pulse, at)
    drawCircle(
        Color.White.copy(alpha = 0.4f),
        radius * pulse,
        at,
        style = androidx.compose.ui.graphics.drawscope.Stroke(radius * 0.06f),
    )
}

/** Size helper so pointer-space hit tests can share draw-space maths. */
fun sizeOf(width: Int, height: Int) = Size(width.toFloat(), height.toFloat())
