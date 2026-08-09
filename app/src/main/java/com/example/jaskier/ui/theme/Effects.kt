package com.example.jaskier.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Sky-to-cream background used behind every screen. */
val SkyGradient = Brush.verticalGradient(listOf(Color(0xFFAEE4FF), Color(0xFFDFF3F8), SunnyBackground))

fun Color.lighter(amount: Float): Color = lerp(this, Color.White, amount)

/** Top-lit vertical gradient that gives flat fills a soft 3D roundness. */
fun glossy(color: Color): Brush =
    Brush.verticalGradient(listOf(color.lighter(0.35f), color, lerp(color, Color(0xFF203A38), 0.12f)))

/**
 * Kid-friendly press feedback: the element springs down to 90% while held.
 * Returns the scale modifier plus the interaction source to pass to clickable().
 */
@Composable
fun Modifier.pressBounce(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return scale(pressScale)
}

@Composable
fun rememberPressSource() = remember { MutableInteractionSource() }

/**
 * A slow, staggered breathing pulse that makes tappable things look alive
 * and invites little fingers. Stagger with [phase] (0, 1, 2, ...).
 */
@Composable
fun Modifier.breathe(phase: Int): Modifier {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "breathe")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(900),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
            initialStartOffset = androidx.compose.animation.core.StartOffset(phase * 220),
        ),
        label = "breathePulse",
    )
    return scale(pulse)
}
