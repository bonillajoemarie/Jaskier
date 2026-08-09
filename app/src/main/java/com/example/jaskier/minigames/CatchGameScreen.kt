package com.example.jaskier.minigames

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp as lerpFloat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.jaskier.R
import com.example.jaskier.speech.TtsManager
import com.example.jaskier.ui.theme.CleanTeal
import com.example.jaskier.ui.theme.InkText
import com.example.jaskier.ui.theme.RainbowCells
import com.example.jaskier.ui.theme.SkyGradient
import com.example.jaskier.ui.theme.SunYellow
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.runtime.withFrameNanos

private class FallingItem(
    val item: AnnounceItem,
    val color: Color,
    val x: Float, // fraction of width
    var y: Float, // fraction of height, 0 = top
    val speed: Float, // fractions of height per second
)

private const val PET_Y = 0.88f // pet mouth line as a fraction of height
private const val CATCH_HALF_WIDTH = 0.11f

@Composable
fun CatchGameScreen(
    tts: TtsManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var score by remember { mutableIntStateOf(0) }
    var petX by remember { mutableFloatStateOf(0.5f) }
    var frame by remember { mutableFloatStateOf(0f) } // drives canvas invalidation
    val items = remember { mutableStateListOf<FallingItem>() }
    val eatPop = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val random = remember { Random(System.nanoTime()) }
    var running by remember { mutableStateOf(true) }

    // Pause the fall while the app is backgrounded.
    LifecycleResumeEffect(Unit) {
        running = true
        onPauseOrDispose { running = false }
    }

    // Spoken how-to for pre-readers.
    LaunchedEffect(Unit) { tts.speak(CatchGame.intro) }

    // Game loop: spawn, fall, catch. Missed items simply drift away — no
    // fail state for little players; the score only ever counts up.
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        var spawnCooldown = 0.4f
        while (isActive) {
            withFrameNanos { now ->
                val dt = if (lastNanos == 0L) 0f else ((now - lastNanos) / 1e9f).coerceAtMost(0.05f)
                lastNanos = now
                if (!running) return@withFrameNanos

                spawnCooldown -= dt
                if (spawnCooldown <= 0f) {
                    // Speeds up gently as the score grows, capped kid-friendly.
                    val difficulty = (score / 10f).coerceAtMost(1f)
                    items += FallingItem(
                        item = CatchPool[random.nextInt(CatchPool.size)],
                        color = RainbowCells[random.nextInt(RainbowCells.size)],
                        x = 0.12f + random.nextFloat() * 0.76f,
                        y = -0.05f,
                        speed = lerpFloat(0.13f, 0.22f, difficulty) * (0.85f + random.nextFloat() * 0.3f),
                    )
                    spawnCooldown = lerpFloat(1.7f, 1.1f, difficulty)
                }

                val iterator = items.listIterator()
                while (iterator.hasNext()) {
                    val falling = iterator.next()
                    falling.y += falling.speed * dt
                    val caught = falling.y >= PET_Y - 0.04f && falling.y <= PET_Y + 0.05f &&
                        abs(falling.x - petX) <= CATCH_HALF_WIDTH
                    when {
                        caught -> {
                            iterator.remove()
                            score += 1
                            tts.speak(falling.item.utterance)
                            scope.launch {
                                eatPop.snapTo(0f)
                                eatPop.animateTo(1f, tween(450))
                                eatPop.snapTo(0f)
                            }
                        }
                        falling.y > 1.1f -> iterator.remove()
                    }
                }
                frame = now / 1e9f
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // Blobby follows the finger anywhere on screen — drag or tap.
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        petX = (change.position.x / size.width).coerceIn(0.1f, 0.9f)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onPress = { offset ->
                        petX = (offset.x / size.width).coerceIn(0.1f, 0.9f)
                    })
                },
        ) {
            frame // read so each game-loop frame invalidates the canvas
            drawGround()
            drawBlobby(petX, eatPop.value, frame)
        }

        // Falling glyph tiles drawn above the scene canvas.
        val textMeasurer = rememberTextMeasurer()
        Canvas(modifier = Modifier.fillMaxSize()) {
            frame
            for (falling in items) {
                val tile = size.minDimension * 0.13f
                val pos = Offset(falling.x * size.width - tile / 2f, falling.y * size.height - tile / 2f)
                val wobble = sin((frame * 2f + falling.x * 10f) * 2f) * 6f
                drawRoundRect(
                    Brush.verticalGradient(
                        listOf(lerp(falling.color, Color.White, 0.35f), falling.color),
                        startY = pos.y,
                        endY = pos.y + tile,
                    ),
                    topLeft = pos + Offset(wobble, 0f),
                    size = Size(tile, tile),
                    cornerRadius = CornerRadius(tile * 0.3f),
                )
                val layout = textMeasurer.measure(
                    AnnotatedString(falling.item.display),
                    style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White),
                )
                drawText(
                    layout,
                    topLeft = pos + Offset(
                        wobble + (tile - layout.size.width) / 2f,
                        (tile - layout.size.height) / 2f,
                    ),
                )
            }
        }

        // Score chip.
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .shadow(6.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⭐", fontSize = 20.sp)
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineMedium,
                color = InkText,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        val backLabel = stringResource(R.string.back_button)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(56.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onBack)
                .semantics { contentDescription = backLabel },
            contentAlignment = Alignment.Center,
        ) {
            Text("←", fontSize = 30.sp, color = InkText)
        }
    }
}

private fun DrawScope.drawGround() {
    val groundTop = size.height * 0.93f
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFFB9E88F), Color(0xFF8FD05F)), startY = groundTop),
        topLeft = Offset(-20f, groundTop),
        size = Size(size.width + 40f, size.height - groundTop + 20f),
        cornerRadius = CornerRadius(40f),
    )
}

// A compact Kerker with an always-open hungry mouth, mirroring the pet's styling.
private fun DrawScope.drawBlobby(petX: Float, eatPop: Float, frame: Float) {
    val r = size.minDimension * 0.13f
    val cx = petX * size.width
    val bob = sin(frame * 3f) * r * 0.05f
    val pop = 1f + sin(eatPop * Math.PI.toFloat()) * 0.15f
    val cy = size.height * PET_Y + bob
    val skin = Color(0xFFF0C09A)
    val hair = Color(0xFF32241B)

    // Contact shadow on the grass.
    drawOval(
        Color(0x335B4A26),
        topLeft = Offset(cx - r * 1.0f, size.height * 0.935f),
        size = Size(r * 2.0f, r * 0.3f),
    )

    // Body with the same top-left light source as the home pet.
    drawOval(
        Brush.radialGradient(
            colors = listOf(
                lerp(skin, Color.White, 0.4f),
                skin,
                lerp(skin, Color(0xFF4A2F1E), 0.18f),
            ),
            center = Offset(cx - r * 0.4f, cy - r * 0.8f),
            radius = r * 2.4f,
        ),
        topLeft = Offset(cx - r * pop, cy - r * 1.25f * pop + r * 0.15f),
        size = Size(r * 2f * pop, r * 2.3f * pop),
    )

    // White tank top hint at the bottom of the body.
    drawArc(
        Color.White.copy(alpha = 0.95f),
        startAngle = 15f,
        sweepAngle = 150f,
        useCenter = true,
        topLeft = Offset(cx - r * pop, cy - r * 1.25f * pop + r * 0.15f),
        size = Size(r * 2f * pop, r * 2.3f * pop),
    )

    // Curly hair mop on top.
    val hairY = cy - r * 1.02f
    for ((i, dx) in listOf(-0.55f, -0.2f, 0.2f, 0.55f).withIndex()) {
        drawCircle(
            hair,
            radius = r * (if (i == 1 || i == 2) 0.30f else 0.24f),
            center = Offset(cx + dx * r, hairY - (if (i == 1 || i == 2) r * 0.08f else 0f)),
        )
    }

    // Eyes looking up at the falling goodies.
    val eyeY = cy - r * 0.55f
    for (side in listOf(-1f, 1f)) {
        val eyeCenter = Offset(cx + side * r * 0.42f, eyeY)
        drawCircle(Color.White, r * 0.26f, eyeCenter)
        drawCircle(Color(0xFF1C1C1C), r * 0.14f, eyeCenter + Offset(0f, -r * 0.06f))
        drawCircle(Color.White, r * 0.05f, eyeCenter + Offset(r * 0.05f, -r * 0.11f))
    }

    // Big open mouth — the catch zone.
    val mouthW = r * (0.7f + 0.25f * sin(eatPop * Math.PI.toFloat()))
    drawOval(
        Color(0xFF2C2C2C),
        topLeft = Offset(cx - mouthW / 2f, cy - r * 0.15f),
        size = Size(mouthW, r * 0.55f),
    )
    val tongue = Path().apply {
        moveTo(cx - mouthW * 0.3f, cy + r * 0.25f)
        quadraticTo(cx, cy + r * 0.45f, cx + mouthW * 0.3f, cy + r * 0.25f)
        close()
    }
    drawPath(tongue, Color(0xFFFF6B81))

    // Sparkle when something is eaten.
    if (eatPop > 0f) {
        val alpha = sin(eatPop * Math.PI.toFloat())
        drawCircle(SunYellow.copy(alpha = alpha * 0.7f), r * 0.1f, Offset(cx - r * 0.9f, cy - r * 1.3f))
        drawCircle(SunYellow.copy(alpha = alpha * 0.7f), r * 0.07f, Offset(cx + r * 0.95f, cy - r * 1.1f))
    }
}
