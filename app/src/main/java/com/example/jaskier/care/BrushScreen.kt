package com.example.jaskier.care

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaskier.R
import com.example.jaskier.pet.PetViewModel
import com.example.jaskier.pet.drawBall
import com.example.jaskier.pet.drawCurl
import com.example.jaskier.pet.drawSoftBlush
import com.example.jaskier.speech.TtsManager
import com.example.jaskier.ui.theme.InkText
import kotlinx.coroutines.delay

private enum class BrushStep { PASTE, BRUSH, DONE }
private const val TOOTH_COUNT = 6

// Tuned for toddlers: bigger targets, faster fills, and taps that just work.
private const val TOOTH_SCRUB_RADIUS = 0.20f
private const val TOOTH_TAP_RADIUS = 0.24f
private const val TOOTH_FILL_MILLIS = 300f

@Composable
fun BrushScreen(
    viewModel: PetViewModel,
    tts: TtsManager,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(BrushStep.PASTE) }
    val teethClean = remember { mutableStateListOf(*Array(TOOTH_COUNT) { 0f }) }
    var pasteAmount by remember { mutableFloatStateOf(0f) }
    var brushPos by remember { mutableStateOf<Offset?>(null) }
    var brushHeld by remember { mutableStateOf(false) }
    var pastePos by remember { mutableStateOf<Offset?>(null) }
    var pasteHeld by remember { mutableStateOf(false) }
    var lastTickMs by remember { mutableStateOf(0L) }
    var sparkAt by remember { mutableStateOf<Offset?>(null) }
    val spark = remember { Animatable(0f) }
    var lastProgressMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var stalled by remember { mutableStateOf(false) }

    LaunchedEffect(sparkAt) {
        if (sparkAt != null) {
            spark.snapTo(0f)
            spark.animateTo(1f, tween(420))
        }
    }

    // Demonstrate rather than instruct: a pre-reader cannot use written help.
    LaunchedEffect(step, lastProgressMs) {
        stalled = false
        delay(DEMO_AFTER_MILLIS)
        stalled = true
    }

    val hint = when (step) {
        BrushStep.PASTE -> "Put toothpaste on the toothbrush!"
        BrushStep.BRUSH -> "Now brush every tooth! Brush brush brush!"
        BrushStep.DONE -> "Yay! My teeth are sparkly clean!"
    }
    LaunchedEffect(step) { tts.speak(hint) }

    LaunchedEffect(step) {
        if (step == BrushStep.DONE) {
            viewModel.brush()
            delay(2600)
            onDone()
        }
    }

    val idle = rememberInfiniteTransition(label = "brushIdle")
    val pulse by idle.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // Tap-first: a two-year-old cannot hold a drag, so every step
                // must be completable with taps alone. Dragging still works.
                .pointerInput(step) {
                    detectTapGestures { tap ->
                        // No tap is ever silent.
                        sparkAt = tap
                        lastProgressMs = System.currentTimeMillis()
                        when (step) {
                            BrushStep.PASTE -> {
                                pasteAmount = 1f
                                step = BrushStep.BRUSH
                            }
                            BrushStep.BRUSH -> {
                                for (i in 0 until TOOTH_COUNT) {
                                    if ((tap - toothCenter(i)).getDistance() < minDim() * TOOTH_TAP_RADIUS) {
                                        teethClean[i] = 1f
                                    }
                                }
                                if (teethClean.all { it >= 1f }) step = BrushStep.DONE
                            }
                            BrushStep.DONE -> Unit
                        }
                    }
                }
                .pointerInput(step) {
                    detectDragGestures(
                        onDragStart = { start ->
                            lastTickMs = 0L
                            when (step) {
                                // Any drag holds the tube; squeezing it over the
                                // brush is the lesson of step one.
                                BrushStep.PASTE -> {
                                    pasteHeld = true
                                    pastePos = start
                                }
                                // Once brushing, any drag holds the brush.
                                BrushStep.BRUSH -> {
                                    brushHeld = true
                                    brushPos = start
                                }
                                BrushStep.DONE -> Unit
                            }
                        },
                        onDrag = { change, _ ->
                            // Progress is time-in-zone, not event count, so it feels
                            // the same on every device and input speed.
                            val now = change.uptimeMillis
                            val dt = if (lastTickMs == 0L) 0f else (now - lastTickMs).coerceAtMost(100L).toFloat()
                            lastTickMs = now
                            if (pasteHeld) {
                                pastePos = change.position
                                // Squeeze paste while the tube is over the brush head.
                                if ((change.position - brushRestPos()).getDistance() < minDim() * 0.30f) {
                                    pasteAmount = (pasteAmount + dt / 600f).coerceAtMost(1f)
                                    if (pasteAmount >= 1f && step == BrushStep.PASTE) {
                                        step = BrushStep.BRUSH
                                        pasteHeld = false
                                        pastePos = null
                                    }
                                }
                            }
                            if (brushHeld) {
                                brushPos = change.position
                                if (step == BrushStep.BRUSH) {
                                    // Scrub whichever tooth the bristles touch, and
                                    // credit its neighbours too — dragging across the
                                    // mouth should visibly help everywhere.
                                    for (i in 0 until TOOTH_COUNT) {
                                        val distance = (change.position - toothCenter(i)).getDistance()
                                        teethClean[i] = scrubProgress(
                                            current = teethClean[i],
                                            dtMillis = dt,
                                            fillMillis = TOOTH_FILL_MILLIS,
                                            onTarget = distance < minDim() * TOOTH_SCRUB_RADIUS,
                                            nearTarget = distance < minDim() * TOOTH_SCRUB_RADIUS * 1.8f,
                                            toolMoving = true,
                                        )
                                    }
                                    if (teethClean.all { it >= 1f }) {
                                        step = BrushStep.DONE
                                        brushHeld = false
                                        brushPos = null
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            pasteHeld = false
                            brushHeld = false
                            pastePos = null
                            brushPos = null
                        },
                        onDragCancel = {
                            pasteHeld = false
                            brushHeld = false
                            pastePos = null
                            brushPos = null
                        },
                    )
                },
        ) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFBFE8F2), Color(0xFFE7F6FA))))
            // bathroom tiles, like the shower room
            val tileColor = Color(0xFFA8D8E8).copy(alpha = 0.35f)
            val tileSize = size.minDimension * 0.18f
            var ty = 0f
            while (ty < size.height) {
                drawLine(tileColor, Offset(0f, ty), Offset(size.width, ty), strokeWidth = 3f)
                ty += tileSize
            }
            var tx = 0f
            while (tx < size.width) {
                drawLine(tileColor, Offset(tx, 0f), Offset(tx, size.height), strokeWidth = 3f)
                tx += tileSize
            }
            drawZoomedFace(teethClean.toList(), sparkle = step == BrushStep.DONE)
            if (step == BrushStep.PASTE) {
                drawPasteTube(pastePos ?: pasteHomeDraw(), pulse = if (pastePos == null) pulse else 1f)
            }
            drawToothbrush(
                at = brushPos ?: brushRestDraw(),
                paste = pasteAmount,
                scrubbing = brushHeld && step == BrushStep.BRUSH,
                pulse = if (brushPos == null && step == BrushStep.BRUSH) pulse else 1f,
            )

            // Progress as a picture: paste, then one star per tooth.
            drawCareStars(
                filled = if (step == BrushStep.PASTE) 0 else 1 + teethClean.count { it >= 1f },
                total = 1 + TOOTH_COUNT,
            )

            // Stalled? Show the kid exactly what to touch, on a loop.
            if (stalled && step != BrushStep.DONE) {
                val target = if (step == BrushStep.PASTE) {
                    brushRestDraw()
                } else {
                    val next = (0 until TOOTH_COUNT).firstOrNull { teethClean[it] < 1f } ?: 0
                    toothCenterDraw(next)
                }
                drawTargetHalo(target, size.minDimension * 0.16f, pulse)
                drawGhostHand(target, ((pulse - 0.94f) / 0.14f).coerceIn(0f, 1f))
            }

            sparkAt?.let { at -> drawTapSpark(at, spark.value) }
        }

        Text(
            text = hint,
            style = MaterialTheme.typography.titleLarge.copy(
                shadow = androidx.compose.ui.graphics.Shadow(Color(0x99000000), blurRadius = 12f),
            ),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 84.dp, start = 24.dp, end = 24.dp),
        )

        val backLabel = stringResource(R.string.back_button)
        Box(
            modifier = Modifier
                .padding(12.dp)
                .size(56.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onDone)
                .semantics { contentDescription = backLabel },
            contentAlignment = Alignment.Center,
        ) {
            Text("←", fontSize = 30.sp, color = InkText)
        }
    }
}

// ---- geometry shared by input and drawing ------------------------------------

private fun PointerInputScope.minDim(): Float = kotlin.math.min(size.width, size.height).toFloat()

private fun PointerInputScope.pasteHome() = Offset(size.width * 0.2f, size.height * 0.85f)
private fun DrawScope.pasteHomeDraw() = Offset(size.width * 0.2f, size.height * 0.85f)

private fun PointerInputScope.brushRestPos() = Offset(size.width * 0.72f, size.height * 0.85f)
private fun DrawScope.brushRestDraw() = Offset(size.width * 0.72f, size.height * 0.85f)

private fun PointerInputScope.toothCenter(i: Int): Offset = toothCenterOf(i, size.width.toFloat(), size.height.toFloat())
private fun DrawScope.toothCenterDraw(i: Int): Offset = toothCenterOf(i, size.width, size.height)

private fun toothCenterOf(i: Int, w: Float, h: Float): Offset {
    val mouthCx = w * 0.5f
    val mouthY = h * 0.47f
    val rowW = w * 0.52f
    val x = mouthCx - rowW / 2f + rowW * (i + 0.5f) / TOOTH_COUNT
    return Offset(x, mouthY)
}

// ---- drawing -------------------------------------------------------------------

private fun DrawScope.drawZoomedFace(teethClean: List<Float>, sparkle: Boolean) {
    val d = size.minDimension
    val cx = size.width / 2f
    val skin = Color(0xFFF0C09A)

    // Giant head fills most of the screen — a full sphere, zoomed in on the mouth.
    drawBall(Offset(cx, size.height * 0.44f), size.width * 0.48f, skin)
    // hair across the top, glossy curls
    for (dx in listOf(-0.34f, -0.12f, 0.12f, 0.34f)) {
        drawCurl(Offset(cx + dx * size.width, size.height * 0.145f), size.width * 0.14f)
    }
    // huge sparkly chibi eyes looking down at the brush
    for (side in listOf(-1f, 1f)) {
        val eye = Offset(cx + side * size.width * 0.2f, size.height * 0.28f)
        val eyeR = d * 0.105f
        drawCircle(Color.White, eyeR, eye)
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFF54331B), Color(0xFF241206)),
                center = eye - Offset(0f, eyeR * 0.05f),
                radius = eyeR * 0.85f,
            ),
            radius = eyeR * 0.7f,
            center = eye + Offset(0f, eyeR * 0.15f),
        )
        drawCircle(Color.White, eyeR * 0.24f, eye + Offset(eyeR * 0.2f, -eyeR * 0.1f))
        drawCircle(Color.White.copy(alpha = 0.8f), eyeR * 0.11f, eye + Offset(-eyeR * 0.22f, eyeR * 0.32f))
    }
    // blush
    for (side in listOf(-1f, 1f)) {
        drawSoftBlush(Offset(cx + side * size.width * 0.3f, size.height * 0.375f), d * 0.19f, d * 0.11f)
    }

    // Big open mouth showing the tooth row — darker toward the throat.
    val mouthY = size.height * 0.47f
    drawOval(
        Brush.radialGradient(
            listOf(Color(0xFF551F1F), Color(0xFF320F0F)),
            center = Offset(cx, mouthY + size.height * 0.05f),
            radius = size.width * 0.4f,
        ),
        topLeft = Offset(cx - size.width * 0.32f, mouthY - size.height * 0.075f),
        size = Size(size.width * 0.64f, size.height * 0.2f),
    )
    // tongue
    drawOval(
        Color(0xFFE86A80),
        topLeft = Offset(cx - size.width * 0.18f, mouthY + size.height * 0.05f),
        size = Size(size.width * 0.36f, size.height * 0.07f),
    )
    // gums above the tooth row
    val gumTop = toothCenterDraw(0).y - size.height * 0.05f
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFFEE8B9C), Color(0xFFE0798D))),
        topLeft = Offset(cx - size.width * 0.29f, gumTop),
        size = Size(size.width * 0.58f, size.height * 0.035f),
        cornerRadius = CornerRadius(size.width * 0.02f),
    )
    // teeth: realistic crowns (flat at the gum, rounded biting edge),
    // yellow → white as each is scrubbed
    for (i in 0 until TOOTH_COUNT) {
        val tooth = toothCenterDraw(i)
        val clean = teethClean[i]
        val toothW = size.width * 0.078f
        val toothH = size.height * 0.06f
        val tx = tooth.x - toothW / 2f
        val ty = tooth.y - toothH * 0.45f
        val color = lerp(Color(0xFFDECB6B), Color(0xFFFDFDF8), clean)
        val crown = Path().apply {
            moveTo(tx, ty)
            lineTo(tx + toothW * 0.94f, ty)
            lineTo(tx + toothW * 0.94f, ty + toothH * 0.62f)
            quadraticTo(tx + toothW * 0.72f, ty + toothH * 1.05f, tx + toothW * 0.47f, ty + toothH * 1.0f)
            quadraticTo(tx + toothW * 0.2f, ty + toothH * 1.05f, tx, ty + toothH * 0.62f)
            close()
        }
        drawPath(
            crown,
            Brush.verticalGradient(
                listOf(lerp(color, Color(0xFFB9AE8D), 0.3f), color),
                startY = ty,
                endY = ty + toothH,
            ),
        )
        drawPath(crown, Color(0x22000000), style = Stroke(width = toothW * 0.05f))
        // germs sit on dirty teeth
        if (clean < 0.5f) {
            drawCircle(
                Color(0xFF7C9B44).copy(alpha = (1f - clean * 2f) * 0.8f),
                toothW * 0.14f,
                tooth + Offset(toothW * 0.2f, -toothH * 0.2f),
            )
        }
        // sparkle on freshly cleaned teeth
        if (clean >= 1f) {
            drawCircle(Color.White, toothW * 0.1f, tooth + Offset(-toothW * 0.22f, -toothH * 0.24f))
        }
    }

    if (sparkle) {
        for ((sx, sy) in listOf(0.28f to 0.42f, 0.72f to 0.42f, 0.5f to 0.6f)) {
            val at = Offset(size.width * sx, size.height * sy)
            val star = Path().apply {
                moveTo(at.x, at.y - d * 0.035f)
                quadraticTo(at.x + d * 0.007f, at.y - d * 0.007f, at.x + d * 0.035f, at.y)
                quadraticTo(at.x + d * 0.007f, at.y + d * 0.007f, at.x, at.y + d * 0.035f)
                quadraticTo(at.x - d * 0.007f, at.y + d * 0.007f, at.x - d * 0.035f, at.y)
                quadraticTo(at.x - d * 0.007f, at.y - d * 0.007f, at.x, at.y - d * 0.035f)
                close()
            }
            drawPath(star, Color.White)
        }
    }
}

private fun DrawScope.drawPasteTube(at: Offset, pulse: Float) {
    val w = size.minDimension * 0.24f * pulse
    val h = size.minDimension * 0.11f * pulse
    rotate(degrees = -20f, pivot = at) {
        drawRoundRect(
            Brush.horizontalGradient(listOf(Color(0xFF6FCF97), Color(0xFF41A66B))),
            topLeft = Offset(at.x - w / 2f, at.y - h / 2f),
            size = Size(w, h),
            cornerRadius = CornerRadius(h * 0.3f),
        )
        // cap
        drawRoundRect(
            Color(0xFFE6EEF2),
            topLeft = Offset(at.x + w * 0.5f, at.y - h * 0.3f),
            size = Size(w * 0.16f, h * 0.6f),
            cornerRadius = CornerRadius(h * 0.12f),
        )
        // white paste stripe
        drawRoundRect(
            Color.White.copy(alpha = 0.7f),
            topLeft = Offset(at.x - w * 0.4f, at.y - h * 0.12f),
            size = Size(w * 0.55f, h * 0.24f),
            cornerRadius = CornerRadius(h * 0.12f),
        )
    }
}

private fun DrawScope.drawToothbrush(at: Offset, paste: Float, scrubbing: Boolean, pulse: Float) {
    val len = size.minDimension * 0.34f * pulse
    val handleH = size.minDimension * 0.05f * pulse
    rotate(degrees = -35f, pivot = at) {
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFFFF8FA0), Color(0xFFE86A80))),
            topLeft = Offset(at.x - len * 0.1f, at.y - handleH / 2f),
            size = Size(len, handleH),
            cornerRadius = CornerRadius(handleH * 0.5f),
        )
        // bristles
        drawRoundRect(
            Color.White,
            topLeft = Offset(at.x - len * 0.28f, at.y - handleH * 0.9f),
            size = Size(len * 0.24f, handleH * 1.3f),
            cornerRadius = CornerRadius(handleH * 0.2f),
        )
        // paste blob on the bristles
        if (paste > 0.02f) {
            drawOval(
                Color(0xFF6FE3E1),
                topLeft = Offset(at.x - len * 0.28f, at.y - handleH * (0.9f + 0.8f * paste)),
                size = Size(len * 0.24f, handleH * 0.8f * paste),
            )
        }
    }
    // foam while scrubbing
    if (scrubbing && paste > 0f) {
        for (i in 0..2) {
            drawCircle(
                Color.White.copy(alpha = 0.8f),
                size.minDimension * (0.018f + i * 0.008f),
                at + Offset((i - 1) * size.minDimension * 0.05f, -size.minDimension * 0.06f),
            )
        }
    }
}
