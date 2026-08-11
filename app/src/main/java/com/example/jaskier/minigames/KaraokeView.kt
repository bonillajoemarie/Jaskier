package com.example.jaskier.minigames

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaskier.songs.Song
import com.example.jaskier.songs.SongPlayer
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TWO_PI = 2f * Math.PI.toFloat()

@Composable
fun KaraokeView(
    song: Song,
    player: SongPlayer,
    isPlaying: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onPokeKerker: () -> Unit = {},
) {
    // Follow the recording: highlight advances with playback position,
    // weighting each line by its length.
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying, song.id) {
        while (isActive && isPlaying) {
            val duration = player.durationMs()
            progress = if (duration > 0) player.positionMs().toFloat() / duration else 0f
            delay(100)
        }
    }
    val lineIndex = lineIndexFor(song.lyrics, progress)

    val ambient = rememberInfiniteTransition(label = "karaokeAmbient")
    val t by ambient.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Restart),
        label = "t",
    )
    val beat by ambient.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Restart),
        label = "beat",
    )

    // Tap-anywhere star bursts so kids can "feel" the song.
    val burst = remember { Animatable(0f) }
    var burstAt by remember { mutableStateOf<Offset?>(null) }
    val scope = rememberCoroutineScope()

    // Poking him squishes him, volume-preserving: wider means shorter.
    val squish = remember { Animatable(1f) }

    // Kerker is a physical toy the kid can fling around the scene.
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var toy by remember { mutableStateOf(ToyState()) }
    var grabbed by remember { mutableStateOf(false) }

    val toyRadius = if (canvasSize == IntSize.Zero) {
        0f
    } else {
        minOf(canvasSize.width, canvasSize.height) * 0.09f
    }
    // Bounds constrain his centre: inset by his drawn size, with the floor at 90%
    // of the screen so he never lands under the "tap for stars" caption.
    val playground = remember(canvasSize, toyRadius) {
        if (toyRadius <= 0f) {
            Rect.Zero
        } else {
            Rect(
                left = toyRadius,
                top = toyRadius * 1.2f,
                right = canvasSize.width - toyRadius,
                bottom = canvasSize.height * 0.90f,
            )
        }
    }

    // Drop him into his home corner once the layout size is known.
    LaunchedEffect(playground) {
        if (playground != Rect.Zero && toy.pos == Offset.Zero) {
            toy = ToyState(
                pos = Offset(canvasSize.width * 0.82f, playground.bottom),
                resting = true,
            )
        }
    }

    // Frame loop: only runs the integrator while he is actually in flight.
    LaunchedEffect(playground) {
        var lastFrame = 0L
        while (isActive) {
            withFrameNanos { now ->
                val dt = if (lastFrame == 0L) {
                    0f
                } else {
                    ((now - lastFrame) / 1_000_000_000f).coerceAtMost(0.05f)
                }
                lastFrame = now
                if (!grabbed && !toy.resting) toy = step(toy, dt, playground)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(playground) {
                detectTapGestures { offset ->
                    burstAt = offset
                    val hitKerker = playground != Rect.Zero &&
                        (offset - toy.pos).getDistance() < toyRadius * 1.6f
                    if (hitKerker) onPokeKerker()
                    // Two launches so the squish and the stars run concurrently —
                    // a cosmetic animation must never block the next tap.
                    scope.launch {
                        if (hitKerker) {
                            squish.snapTo(1.28f)
                            squish.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 380f))
                        }
                    }
                    scope.launch {
                        burst.snapTo(0f)
                        burst.animateTo(1f, tween(600))
                        burst.snapTo(0f)
                    }
                }
            }
            .pointerInput(playground) {
                val tracker = VelocityTracker()
                detectDragGestures(
                    onDragStart = { start ->
                        // Generous grab radius — little fingers aren't precise.
                        if (playground != Rect.Zero &&
                            (start - toy.pos).getDistance() < toyRadius * 1.8f
                        ) {
                            grabbed = true
                            tracker.resetTracking()
                            toy = toy.copy(vel = Offset.Zero, spin = 0f, resting = false)
                        }
                    },
                    onDrag = { change, _ ->
                        if (grabbed) {
                            tracker.addPosition(change.uptimeMillis, change.position)
                            // He leans in the direction he's being pulled.
                            val lean = ((change.position.x - toy.pos.x) * 3f).coerceIn(-18f, 18f)
                            toy = toy.copy(pos = change.position, angle = lean)
                        }
                    },
                    onDragEnd = {
                        if (grabbed) {
                            grabbed = false
                            val v = tracker.calculateVelocity()
                            toy = toy.copy(
                                vel = Offset(v.x, v.y),
                                spin = (v.x / 8f).coerceIn(-900f, 900f),
                                resting = false,
                            )
                        }
                    },
                    onDragCancel = {
                        if (grabbed) {
                            grabbed = false
                            toy = toy.copy(vel = Offset.Zero, spin = 0f, resting = false)
                        }
                    },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawSongBackground(song.id, t)
            if (playground != Rect.Zero) {
                drawKerkerToy(
                    center = toy.pos,
                    radius = toyRadius,
                    rotation = toy.angle,
                    squish = squish.value,
                    airborne = !toy.resting,
                    groundY = playground.bottom,
                    beat = beat,
                    isPlaying = isPlaying,
                )
            }
            val at = burstAt
            if (burst.value > 0f && at != null) drawStarBurst(at, burst.value)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Start)
                    .size(56.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onClose)
                    .semantics { contentDescription = "Back to songs" },
                contentAlignment = Alignment.Center,
            ) {
                Text("←", fontSize = 30.sp, color = Color(0xFF3D3D3D))
            }

            Text(
                text = "${song.emoji}  ${song.title}",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )

            // Three-line karaoke window: previous, current (big), next.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val lyrics = song.lyrics
                val shadowStyle = Shadow(Color(0x66000000), Offset(0f, 4f), blurRadius = 10f)
                if (lineIndex > 0) {
                    Text(
                        lyrics[lineIndex - 1],
                        fontSize = 19.sp,
                        color = Color.White.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    text = lyrics.getOrElse(lineIndex) { "" },
                    style = TextStyle(
                        fontSize = 31.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        shadow = shadowStyle,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 14.dp),
                )
                if (lineIndex < lyrics.lastIndex) {
                    Text(
                        lyrics[lineIndex + 1],
                        fontSize = 19.sp,
                        color = Color.White.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center,
                    )
                }
                if (!isPlaying) {
                    Text(
                        "▶  Tap to play again",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .padding(top = 22.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                            .clickable { player.toggle(song) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            }

            Text(
                "Tap anywhere for stars ✨",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
    }
}

private fun lineIndexFor(lyrics: List<String>, progress: Float): Int {
    if (lyrics.isEmpty()) return 0
    val weights = lyrics.map { it.length + 6f }
    val total = weights.sum()
    var cumulative = 0f
    weights.forEachIndexed { index, weight ->
        cumulative += weight
        if (progress * total < cumulative) return index
    }
    return lyrics.lastIndex
}

// ---- themed backgrounds ------------------------------------------------------

private fun DrawScope.drawSongBackground(songId: String, t: Float) {
    when (songId) {
        "twinkle", "sleeping", "hush_baby" -> drawNightSky(t)
        "alphabet" -> drawFloatingGlyphs(t, ('A'..'Z').map { "$it" })
        "one_two" -> drawFloatingGlyphs(t, (1..10).map { "$it" })
        "bingo" -> drawFarm(t)
        "hickory" -> drawClockRoom(t)
        "mary_lamb" -> drawMeadow(t)
        "hokey" -> drawParty(t)
        "teapot" -> drawTeaSteam(t)
        "river_woods" -> drawSnowWoods(t)
        else -> drawSunshine(t) // happy, head_shoulders
    }
}

private fun DrawScope.drawNightSky(t: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF232A5C), Color(0xFF3C3F8F), Color(0xFF6B5CA8))))
    // moon
    drawCircle(Color(0xFFFFF3C4), size.minDimension * 0.09f, Offset(size.width * 0.82f, size.height * 0.14f))
    drawCircle(Color(0xFF3C3F8F).copy(alpha = 0.35f), size.minDimension * 0.075f, Offset(size.width * 0.85f, size.height * 0.125f))
    // twinkling stars
    for (i in 0 until 26) {
        val x = (i * 137 % 100) / 100f * size.width
        val y = (i * 61 % 100) / 100f * size.height
        val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * TWO_PI * 3f + i))
        drawCircle(Color.White.copy(alpha = twinkle * 0.9f), size.minDimension * 0.008f * (1f + i % 3), Offset(x, y))
    }
}

private fun DrawScope.drawFloatingGlyphs(t: Float, glyphs: List<String>) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF3AA7CF), Color(0xFF6BC7E0), Color(0xFF9FDCEB))))
    // Letters/numbers drift slowly upward; drawn with simple text paint.
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        for (i in glyphs.indices) {
            val x = (i * 89 % 100) / 100f * size.width
            val phase = (t + i / glyphs.size.toFloat()) % 1f
            val y = size.height * (1.1f - phase * 1.2f)
            paint.textSize = size.minDimension * (0.06f + (i % 3) * 0.02f)
            paint.color = android.graphics.Color.argb((60 + (i % 4) * 25), 255, 255, 255)
            canvas.nativeCanvas.drawText(glyphs[i], x, y, paint)
        }
    }
}

private fun DrawScope.drawFarm(t: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF7FCBEF), Color(0xFFB6E4F5)), endY = size.height * 0.62f))
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF8FD05F), Color(0xFF5FB344)), startY = size.height * 0.62f),
        topLeft = Offset(0f, size.height * 0.62f),
        size = Size(size.width, size.height * 0.38f),
    )
    drawCircle(Color(0xFFFECA57), size.minDimension * 0.09f, Offset(size.width * 0.16f, size.height * 0.12f))
    // barn
    val barnLeft = size.width * 0.6f
    val barnTop = size.height * 0.47f
    val barnW = size.width * 0.26f
    val barnH = size.height * 0.15f
    drawRect(Color(0xFFC0392B), topLeft = Offset(barnLeft, barnTop), size = Size(barnW, barnH))
    val roof = Path().apply {
        moveTo(barnLeft - barnW * 0.08f, barnTop)
        lineTo(barnLeft + barnW / 2f, barnTop - barnH * 0.55f)
        lineTo(barnLeft + barnW * 1.08f, barnTop)
        close()
    }
    drawPath(roof, Color(0xFF8E2B20))
    drawRect(Color(0xFF7A4E2D), topLeft = Offset(barnLeft + barnW * 0.38f, barnTop + barnH * 0.35f), size = Size(barnW * 0.24f, barnH * 0.65f))
    // drifting cloud
    val cloudX = (t * 1.4f % 1.4f - 0.2f) * size.width
    drawCircle(Color.White, size.minDimension * 0.05f, Offset(cloudX, size.height * 0.18f))
    drawCircle(Color.White, size.minDimension * 0.04f, Offset(cloudX + size.minDimension * 0.06f, size.height * 0.19f))
    drawCircle(Color.White, size.minDimension * 0.04f, Offset(cloudX - size.minDimension * 0.05f, size.height * 0.195f))
}

private fun DrawScope.drawClockRoom(t: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF6B4F3A), Color(0xFF977052))))
    val clock = Offset(size.width * 0.5f, size.height * 0.24f)
    val r = size.minDimension * 0.17f
    drawCircle(Color(0xFFF6EFDD), r, clock)
    drawCircle(Color(0xFF4A382A), r, clock, style = Stroke(size.minDimension * 0.02f))
    // ticking hands
    val minuteAngle = t * TWO_PI * 4f
    rotate(degrees = Math.toDegrees(minuteAngle.toDouble()).toFloat(), pivot = clock) {
        drawLine(Color(0xFF4A382A), clock, clock + Offset(0f, -r * 0.75f), strokeWidth = size.minDimension * 0.012f, cap = StrokeCap.Round)
    }
    rotate(degrees = Math.toDegrees((minuteAngle / 12f).toDouble()).toFloat(), pivot = clock) {
        drawLine(Color(0xFF4A382A), clock, clock + Offset(0f, -r * 0.45f), strokeWidth = size.minDimension * 0.018f, cap = StrokeCap.Round)
    }
    // swinging pendulum
    val swing = sin(t * TWO_PI * 4f) * 0.4f
    val pivot = clock + Offset(0f, r)
    val bob = pivot + Offset(sin(swing) * r * 1.3f, cos(swing) * r * 1.6f)
    drawLine(Color(0xFFD9B98C), pivot, bob, strokeWidth = size.minDimension * 0.012f)
    drawCircle(Color(0xFFE8C766), size.minDimension * 0.045f, bob)
}

private fun DrawScope.drawMeadow(t: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF8ED4F2), Color(0xFFC6EBF7)), endY = size.height * 0.55f))
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF9BDB6C), Color(0xFF6FBF52)), startY = size.height * 0.55f),
        topLeft = Offset(0f, size.height * 0.55f),
        size = Size(size.width, size.height * 0.45f),
    )
    // flowers swaying
    for (i in 0 until 7) {
        val x = (0.08f + i * 0.14f) * size.width
        val y = size.height * (0.68f + (i % 3) * 0.09f)
        val sway = sin(t * TWO_PI * 2f + i) * size.minDimension * 0.008f
        drawLine(Color(0xFF4E9440), Offset(x, y + size.minDimension * 0.05f), Offset(x + sway, y), strokeWidth = size.minDimension * 0.008f)
        for (p in 0 until 5) {
            val angle = p / 5f * TWO_PI
            drawCircle(
                if (i % 2 == 0) Color(0xFFFF8FA0) else Color(0xFFFECA57),
                size.minDimension * 0.014f,
                Offset(x + sway + cos(angle) * size.minDimension * 0.02f, y + sin(angle) * size.minDimension * 0.02f),
            )
        }
        drawCircle(Color.White, size.minDimension * 0.01f, Offset(x + sway, y))
    }
    // fluffy sheep in the corner
    val sheep = Offset(size.width * 0.78f, size.height * 0.62f)
    for (angle in 0 until 6) {
        val a = angle / 6f * TWO_PI
        drawCircle(Color.White, size.minDimension * 0.035f, sheep + Offset(cos(a), sin(a)) * size.minDimension * 0.05f)
    }
    drawCircle(Color.White, size.minDimension * 0.07f, sheep)
    drawCircle(Color(0xFFD8B48F), size.minDimension * 0.038f, sheep + Offset(size.minDimension * 0.055f, size.minDimension * 0.01f))
}

private fun DrawScope.drawParty(t: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF6C3FA8), Color(0xFF9B59B6), Color(0xFFC168C9))))
    val confettiColors = listOf(Color(0xFFFF6B6B), Color(0xFFFECA57), Color(0xFF2ED573), Color(0xFF54A0FF), Color(0xFFFF6B81))
    for (i in 0 until 22) {
        val x = (i * 47 % 100) / 100f * size.width
        val phase = (t * (1f + i % 3 * 0.4f) + i * 0.13f) % 1f
        val y = phase * size.height * 1.1f - size.height * 0.05f
        rotate(degrees = phase * 720f + i * 30f, pivot = Offset(x, y)) {
            drawRoundRect(
                confettiColors[i % confettiColors.size],
                topLeft = Offset(x - size.minDimension * 0.012f, y - size.minDimension * 0.02f),
                size = Size(size.minDimension * 0.024f, size.minDimension * 0.04f),
                cornerRadius = CornerRadius(size.minDimension * 0.006f),
            )
        }
    }
}

private fun DrawScope.drawTeaSteam(t: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFFE58F7E), Color(0xFFF2B49B), Color(0xFFF7D6B8))))
    // rising steam swirls
    for (i in 0 until 4) {
        val baseX = size.width * (0.25f + i * 0.17f)
        val phase = (t * 2f + i * 0.25f) % 1f
        val y = size.height * (0.95f - phase * 0.75f)
        val alpha = (1f - phase) * 0.5f
        val steam = Path().apply {
            moveTo(baseX, y)
            cubicTo(
                baseX + size.minDimension * 0.05f, y - size.minDimension * 0.06f,
                baseX - size.minDimension * 0.05f, y - size.minDimension * 0.12f,
                baseX, y - size.minDimension * 0.18f,
            )
        }
        drawPath(steam, Color.White.copy(alpha = alpha), style = Stroke(size.minDimension * 0.02f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawSnowWoods(t: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF7FA8CF), Color(0xFFB7CFE4), Color(0xFFE8F0F7))))
    // fir trees
    for (i in 0 until 4) {
        val x = size.width * (0.12f + i * 0.25f)
        val baseY = size.height * 0.8f
        val h = size.minDimension * (0.16f + (i % 2) * 0.05f)
        for (layer in 0 until 3) {
            val w = h * (1f - layer * 0.25f)
            val topY = baseY - h * (0.5f + layer * 0.35f)
            val tree = Path().apply {
                moveTo(x - w / 2f, topY + h * 0.5f)
                lineTo(x, topY)
                lineTo(x + w / 2f, topY + h * 0.5f)
                close()
            }
            drawPath(tree, Color(0xFF2E6B4F))
        }
    }
    // falling snow
    for (i in 0 until 30) {
        val x = (i * 53 % 100) / 100f * size.width + sin(t * TWO_PI + i) * size.minDimension * 0.02f
        val phase = (t * (1.5f + i % 3 * 0.5f) + i * 0.07f) % 1f
        drawCircle(Color.White.copy(alpha = 0.85f), size.minDimension * (0.005f + (i % 3) * 0.003f), Offset(x, phase * size.height))
    }
}

private fun DrawScope.drawSunshine(t: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFFFFB74D), Color(0xFFFECA57), Color(0xFFFFE29A))))
    val sun = Offset(size.width * 0.5f, size.height * 0.2f)
    rotate(degrees = t * 360f, pivot = sun) {
        for (i in 0 until 12) {
            rotate(degrees = i * 30f, pivot = sun) {
                drawRoundRect(
                    Color(0xFFFFF3C4).copy(alpha = 0.6f),
                    topLeft = sun + Offset(-size.minDimension * 0.012f, -size.minDimension * 0.26f),
                    size = Size(size.minDimension * 0.024f, size.minDimension * 0.09f),
                    cornerRadius = CornerRadius(size.minDimension * 0.01f),
                )
            }
        }
    }
    drawCircle(Color(0xFFFFDE59), size.minDimension * 0.13f, sun)
    drawCircle(Color(0xFFFFF3C4), size.minDimension * 0.10f, sun)
}

// ---- foreground extras -------------------------------------------------------

// Kerker as a draggable toy: he bops in place when settled, and spins freely
// while airborne. Geometry is proportional to [radius] so the same code serves
// any size.
private fun DrawScope.drawKerkerToy(
    center: Offset,
    radius: Float,
    rotation: Float,
    squish: Float,
    airborne: Boolean,
    groundY: Float,
    beat: Float,
    isPlaying: Boolean,
) {
    val r = radius
    val dancing = isPlaying && !airborne
    val bounce = if (dancing) kotlin.math.abs(sin(beat * TWO_PI)) * r * 0.35f else 0f
    val tilt = if (dancing) sin(beat * TWO_PI) * 8f else 0f
    val c = Offset(center.x, center.y - bounce)
    val skin = Color(0xFFF0C09A)

    // Contact shadow, fading out the higher he flies.
    val lift = (groundY - center.y).coerceAtLeast(0f)
    val shadowAlpha = 0.2f * (1f - lift / (r * 8f)).coerceIn(0f, 1f)
    if (shadowAlpha > 0.01f) {
        drawOval(
            Color.Black.copy(alpha = shadowAlpha),
            topLeft = Offset(c.x - r * 0.9f, groundY + r * 1.05f),
            size = Size(r * 1.8f, r * 0.3f),
        )
    }

    scale(scaleX = squish, scaleY = 1f / squish, pivot = c) {
        rotate(degrees = rotation + tilt, pivot = c) {
            drawOval(
                Brush.radialGradient(
                    listOf(Color(0xFFFAD9B8), skin, Color(0xFFC99669)),
                    center = c - Offset(r * 0.3f, r * 0.5f),
                    radius = r * 2.2f,
                ),
                topLeft = Offset(c.x - r, c.y - r * 1.15f),
                size = Size(r * 2f, r * 2.3f),
            )
            // vest
            drawArc(
                Color.White,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = true,
                topLeft = Offset(c.x - r, c.y - r * 1.15f),
                size = Size(r * 2f, r * 2.3f),
            )
            // hair
            for (dx in listOf(-0.5f, -0.17f, 0.17f, 0.5f)) {
                drawCircle(Color(0xFF32241B), r * 0.26f, Offset(c.x + dx * r, c.y - r * 0.95f))
            }
            // eyes + open singing mouth
            for (side in listOf(-1f, 1f)) {
                drawCircle(Color.White, r * 0.2f, Offset(c.x + side * r * 0.38f, c.y - r * 0.4f))
                drawCircle(Color(0xFF2A180C), r * 0.1f, Offset(c.x + side * r * 0.38f, c.y - r * 0.42f))
            }
            drawOval(
                Color(0xFF2C2C2C),
                topLeft = Offset(c.x - r * 0.22f, c.y - r * 0.05f),
                size = Size(r * 0.44f, r * 0.4f),
            )
        }
    }

    // Floating music notes, only while he is settled and singing.
    if (dancing) {
        for (i in 0 until 3) {
            val phase = (beat + i / 3f) % 1f
            val nx = c.x - r * (1.6f + i * 0.5f)
            val ny = c.y - r * (1.2f + phase * 2.2f)
            val alpha = 1f - phase
            drawCircle(Color.White.copy(alpha = alpha), r * 0.12f, Offset(nx, ny))
            drawLine(
                Color.White.copy(alpha = alpha),
                Offset(nx + r * 0.11f, ny),
                Offset(nx + r * 0.11f, ny - r * 0.4f),
                strokeWidth = r * 0.06f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawStarBurst(at: Offset, t: Float) {
    val d = size.minDimension
    for (i in 0 until 6) {
        val angle = i / 6f * TWO_PI
        val dist = d * 0.12f * t
        val star = at + Offset(cos(angle) * dist, sin(angle) * dist - d * 0.03f * t)
        val alpha = 1f - t
        val r = d * 0.022f * (1f - t * 0.4f)
        val path = Path().apply {
            moveTo(star.x, star.y - r)
            quadraticTo(star.x + r * 0.2f, star.y - r * 0.2f, star.x + r, star.y)
            quadraticTo(star.x + r * 0.2f, star.y + r * 0.2f, star.x, star.y + r)
            quadraticTo(star.x - r * 0.2f, star.y + r * 0.2f, star.x - r, star.y)
            quadraticTo(star.x - r * 0.2f, star.y - r * 0.2f, star.x, star.y - r)
            close()
        }
        drawPath(path, Color(0xFFFFE066).copy(alpha = alpha))
    }
}
