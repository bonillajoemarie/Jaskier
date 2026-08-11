package com.example.jaskier.pet

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp

// Shared soft-flat drawing kit — the whole app's visual language in one place.
//
// Toca Boca is the reference: flat fills, chunky rounded shapes, and exactly
// one soft shadow per object for readability. No gradients inside shapes, no
// rim light, no specular gleam. Form reads from silhouette and value contrast.
//
// Every scene draws Kerker and his props through these primitives, so changing
// the style here changes it everywhere at once.

/** How much darker a shape's flat shade band is than its base colour. */
private const val SHADE_MIX = 0.14f

/** The single drop shadow every object gets, offset down-right. */
private const val DROP_ALPHA = 0.13f

private val ShadowInk = Color(0xFF3A2A1E)

/**
 * A flat ball: one fill, one shade crescent along the lower-right, one soft
 * drop shadow. [specular] is kept for call-site compatibility and now controls
 * only whether a small flat catchlight dot is drawn.
 */
internal fun DrawScope.drawBall(
    center: Offset,
    radius: Float,
    base: Color,
    specular: Boolean = true,
) {
    // one soft shadow, down and right
    drawCircle(
        ShadowInk.copy(alpha = DROP_ALPHA),
        radius * 1.02f,
        center + Offset(radius * 0.09f, radius * 0.11f),
    )
    drawCircle(base, radius, center)

    // flat shade band hugging the lower-right, clipped to the ball
    val clip = Path().apply {
        addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
    }
    clipPath(clip) {
        drawCircle(
            lerp(base, ShadowInk, SHADE_MIX),
            radius,
            center + Offset(radius * 0.30f, radius * 0.34f),
        )
        drawCircle(base, radius, center + Offset(radius * 0.10f, radius * 0.12f))
    }

    if (specular) {
        drawCircle(
            Color.White.copy(alpha = 0.5f),
            radius * 0.13f,
            center + Offset(-radius * 0.42f, -radius * 0.5f),
        )
    }
}

/** A chunky flat hair curl with a soft shadow beneath it. */
internal fun DrawScope.drawCurl(center: Offset, radius: Float) {
    drawCircle(
        ShadowInk.copy(alpha = DROP_ALPHA),
        radius,
        center + Offset(radius * 0.12f, radius * 0.16f),
    )
    drawCircle(Color(0xFF32241B), radius, center)
    drawCircle(Color(0xFF241A12), radius * 0.42f, center + Offset(radius * 0.3f, radius * 0.32f))
}

/** A flat eye white with a soft lid shade across the top. */
internal fun DrawScope.drawEyeBall(center: Offset, radius: Float) {
    drawCircle(Color.White, radius, center)
    val clip = Path().apply {
        addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
    }
    clipPath(clip) {
        drawOval(
            Color(0x1A30241C),
            topLeft = center - Offset(radius * 1.05f, radius * 1.5f),
            size = Size(radius * 2.1f, radius * 0.9f),
        )
    }
}

/** Flat blush: a solid soft-edged oval, no airbrush gradient. */
internal fun DrawScope.drawSoftBlush(center: Offset, w: Float, h: Float) {
    drawOval(
        Color(0xFFFF8B9E).copy(alpha = 0.4f),
        topLeft = center - Offset(w / 2f, h / 2f),
        size = Size(w, h),
    )
}

/** A chunky flat rounded rect (tummy, shirt, props) with one soft shadow. */
internal fun DrawScope.drawShadedRoundRect(
    topLeft: Offset,
    size: Size,
    cornerRadius: Float,
    base: Color,
) {
    val corner = CornerRadius(cornerRadius)
    drawRoundRect(
        ShadowInk.copy(alpha = DROP_ALPHA),
        topLeft = topLeft + Offset(size.width * 0.03f, size.height * 0.035f),
        size = size,
        cornerRadius = corner,
    )
    drawRoundRect(base, topLeft = topLeft, size = size, cornerRadius = corner)

    // flat shade along the bottom edge, clipped to the shape
    val rect = Rect(topLeft.x, topLeft.y, topLeft.x + size.width, topLeft.y + size.height)
    clipPath(Path().apply { addRoundRect(RoundRect(rect, cornerRadius, cornerRadius)) }) {
        drawRect(
            lerp(base, ShadowInk, SHADE_MIX),
            topLeft = Offset(rect.left, rect.top + size.height * 0.74f),
            size = Size(size.width, size.height * 0.26f),
        )
    }
}

/** The single soft shadow an object casts on what is beneath it. */
internal fun DrawScope.drawContactShadow(center: Offset, w: Float, h: Float, alpha: Float = 0.16f) {
    drawOval(
        ShadowInk.copy(alpha = alpha),
        topLeft = center - Offset(w / 2f, h / 2f),
        size = Size(w, h),
    )
}

/**
 * Paper grain over a finished scene. Deterministic — a grain that reshuffles
 * every frame reads as television static, not paper.
 */
internal fun DrawScope.drawPaperGrain(alpha: Float = 0.035f) {
    val step = size.minDimension * 0.045f
    if (step <= 0f) return
    var i = 0
    var y = 0f
    while (y < size.height) {
        var x = 0f
        while (x < size.width) {
            // A fixed hash, so the same speck lands in the same place every frame.
            val hash = (i * 1103515245 + 12345) and 0x7FFFFFFF
            if (hash % 3 == 0) {
                val r = step * (0.10f + (hash % 7) / 40f)
                drawCircle(
                    if (hash % 2 == 0) Color.White.copy(alpha = alpha)
                    else ShadowInk.copy(alpha = alpha),
                    r,
                    Offset(x + (hash % 11) / 11f * step, y + (hash % 13) / 13f * step),
                )
            }
            x += step
            i++
        }
        y += step
    }
}

/** A chunky hand-drawn outline, for shapes that need to pop off the background. */
internal fun DrawScope.drawFlatOutline(center: Offset, radius: Float, width: Float) {
    drawCircle(ShadowInk.copy(alpha = 0.18f), radius, center, style = Stroke(width))
}
