package com.example.jaskier.pet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp

// Shared "3D" shading kit for Kerker: every ball gets top-left key light,
// a soft inner shadow hugging the dark rim, and a specular gleam. Contact
// shadows ground shapes against each other.

internal fun DrawScope.drawBall(
    center: Offset,
    radius: Float,
    base: Color,
    specular: Boolean = true,
) {
    drawCircle(
        Brush.radialGradient(
            listOf(
                lerp(base, Color.White, 0.5f),
                lerp(base, Color.White, 0.12f),
                base,
                lerp(base, Color(0xFF3A2313), 0.3f),
            ),
            center = center - Offset(radius * 0.4f, radius * 0.5f),
            radius = radius * 2.0f,
        ),
        radius = radius,
        center = center,
    )
    // inner shadow hugging the lower-right rim sells the sphere
    val clip = Path().apply {
        addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
    }
    clipPath(clip) {
        val shadowCenter = center - Offset(radius * 0.22f, radius * 0.3f)
        drawCircle(
            Brush.radialGradient(
                0.7f to Color.Transparent,
                1f to Color(0x4D2F1A0C),
                center = shadowCenter,
                radius = radius * 1.5f,
            ),
            radius = radius * 1.5f,
            center = shadowCenter,
        )
    }
    if (specular) {
        val gleamCenter = center + Offset(-radius * 0.42f, -radius * 0.55f)
        drawOval(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.5f), Color.Transparent),
                center = gleamCenter,
                radius = radius * 0.42f,
            ),
            topLeft = gleamCenter - Offset(radius * 0.34f, radius * 0.22f),
            size = Size(radius * 0.68f, radius * 0.44f),
        )
    }
}

/** A glossy hair curl with an under-shadow and a sheen. */
internal fun DrawScope.drawCurl(center: Offset, radius: Float) {
    drawCircle(Color(0xFF1A110A), radius, center + Offset(radius * 0.1f, radius * 0.16f))
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0xFF5C4433), Color(0xFF32241B), Color(0xFF20150D)),
            center = center - Offset(radius * 0.35f, radius * 0.42f),
            radius = radius * 1.7f,
        ),
        radius = radius,
        center = center,
    )
    drawCircle(
        Color.White.copy(alpha = 0.13f),
        radius * 0.3f,
        center + Offset(-radius * 0.3f, -radius * 0.36f),
    )
}

/** A spherical eye white with an upper-lid shadow. */
internal fun DrawScope.drawEyeBall(center: Offset, radius: Float) {
    drawCircle(
        Brush.radialGradient(
            listOf(Color.White, Color(0xFFDCE4E6)),
            center = center - Offset(0f, radius * 0.35f),
            radius = radius * 1.5f,
        ),
        radius = radius,
        center = center,
    )
    val clip = Path().apply {
        addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
    }
    clipPath(clip) {
        drawOval(
            Color(0x2E30241C),
            topLeft = center - Offset(radius * 1.05f, radius * 1.45f),
            size = Size(radius * 2.1f, radius * 0.95f),
        )
    }
}

/** Soft airbrushed blush instead of a flat oval. */
internal fun DrawScope.drawSoftBlush(center: Offset, w: Float, h: Float) {
    drawOval(
        Brush.radialGradient(
            listOf(Color(0xFFFF8B9E).copy(alpha = 0.55f), Color(0xFFFF8B9E).copy(alpha = 0f)),
            center = center,
            radius = w * 0.62f,
        ),
        topLeft = center - Offset(w / 2f, h / 2f),
        size = Size(w, h),
    )
}

/** A rounded-rect volume (tummy, shirt) shaded like a soft cushion. */
internal fun DrawScope.drawShadedRoundRect(
    topLeft: Offset,
    size: Size,
    cornerRadius: Float,
    base: Color,
) {
    val rect = Rect(topLeft.x, topLeft.y, topLeft.x + size.width, topLeft.y + size.height)
    drawRoundRect(
        Brush.radialGradient(
            listOf(
                lerp(base, Color.White, 0.42f),
                lerp(base, Color.White, 0.1f),
                base,
                lerp(base, Color(0xFF3A2313), 0.22f),
            ),
            center = Offset(rect.left + size.width * 0.32f, rect.top + size.height * 0.2f),
            radius = maxOf(size.width, size.height) * 1.15f,
        ),
        topLeft = topLeft,
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
    )
    // rim shadow along the bottom-right edge
    val clip = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(rect, cornerRadius, cornerRadius)) }
    clipPath(clip) {
        val shadowCenter = Offset(rect.left + size.width * 0.35f, rect.top + size.height * 0.3f)
        val shadowR = maxOf(size.width, size.height) * 0.95f
        drawCircle(
            Brush.radialGradient(
                0.72f to Color.Transparent,
                1f to Color(0x452F1A0C),
                center = shadowCenter,
                radius = shadowR,
            ),
            radius = shadowR,
            center = shadowCenter,
        )
    }
    // soft top gleam
    drawOval(
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.35f), Color.Transparent),
            center = Offset(rect.left + size.width * 0.3f, rect.top + size.height * 0.14f),
            radius = size.width * 0.3f,
        ),
        topLeft = Offset(rect.left + size.width * 0.12f, rect.top + size.height * 0.04f),
        size = Size(size.width * 0.4f, size.height * 0.18f),
    )
}

/** Soft shadow one shape casts on another (chin on shirt, feet on ground...). */
internal fun DrawScope.drawContactShadow(center: Offset, w: Float, h: Float, alpha: Float = 0.16f) {
    drawOval(
        Brush.radialGradient(
            listOf(Color(0xFF2F1A0C).copy(alpha = alpha), Color.Transparent),
            center = center,
            radius = w * 0.55f,
        ),
        topLeft = center - Offset(w / 2f, h / 2f),
        size = Size(w, h),
    )
}
