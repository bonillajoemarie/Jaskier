package com.example.jaskier.care

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
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
import com.example.jaskier.pet.drawContactShadow
import com.example.jaskier.pet.drawCurl
import com.example.jaskier.pet.drawEyeBall
import com.example.jaskier.pet.drawShadedRoundRect
import com.example.jaskier.pet.drawSoftBlush
import com.example.jaskier.speech.TtsManager
import com.example.jaskier.speech.VoiceTone
import com.example.jaskier.ui.theme.InkText
import com.example.jaskier.ui.theme.SkyBlue
import kotlin.math.sin
import kotlinx.coroutines.delay

private const val TWO_PI = 2f * Math.PI.toFloat()

private enum class ShowerStep { TURN_ON, GET_WET, SOAP, RINSE_HINT, RINSING, DONE }

// Dirt spots the kid has to scrub, as fractions of the body area.
private val DirtSpots = listOf(Offset(0.36f, 0.42f), Offset(0.62f, 0.55f), Offset(0.47f, 0.72f))

// Tuned for toddlers: bigger targets and faster fills than the originals.
private const val DIRT_SCRUB_RADIUS = 0.26f
private const val DIRT_TAP_RADIUS = 0.30f
private const val DIRT_FILL_MILLIS = 800f

@Composable
fun ShowerScreen(
    viewModel: PetViewModel,
    tts: TtsManager,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(ShowerStep.TURN_ON) }
    val scrubbed = remember { mutableStateListOf(0f, 0f, 0f) }
    var soapPos by remember { mutableStateOf<Offset?>(null) }
    var soapHeld by remember { mutableStateOf(false) }
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
    val waterT = remember { Animatable(0f) }
    var rinseT by remember { mutableFloatStateOf(0f) }

    val hint = when (step) {
        ShowerStep.TURN_ON -> "Tap the knob to turn on the shower!"
        ShowerStep.GET_WET -> "Splash splash! Getting all wet!"
        ShowerStep.SOAP -> "Grab the soap and scrub the dirt!"
        ShowerStep.RINSE_HINT -> "Great scrubbing! Tap the knob to rinse!"
        ShowerStep.RINSING -> "Rinse, rinse, rinse!"
        ShowerStep.DONE -> "Yay! I'm all clean! Thank you!"
    }
    // Speak every instruction so pre-readers can follow the routine.
    LaunchedEffect(step) { tts.speak(hint) }

    // Water runs for a moment to get wet, then stops for soaping.
    LaunchedEffect(step) {
        when (step) {
            ShowerStep.GET_WET -> {
                waterT.snapTo(0f)
                waterT.animateTo(1f, tween(2600, easing = LinearEasing))
                waterT.snapTo(0f)
                step = ShowerStep.SOAP
            }
            ShowerStep.RINSING -> {
                waterT.snapTo(0f)
                waterT.animateTo(1f, tween(2800, easing = LinearEasing)) { rinseT = value }
                waterT.snapTo(0f)
                viewModel.shower()
                step = ShowerStep.DONE
            }
            ShowerStep.DONE -> {
                delay(2600)
                onDone()
            }
            else -> Unit
        }
    }

    val idle = rememberInfiniteTransition(label = "showerIdle")
    val bob by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "bob",
    )
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
                .pointerInput(step) {
                    detectTapGestures { tap ->
                        // No tap is ever silent.
                        sparkAt = tap
                        lastProgressMs = System.currentTimeMillis()
                        if (isOnKnob(tap) && (step == ShowerStep.TURN_ON || step == ShowerStep.RINSE_HINT)) {
                            step = if (step == ShowerStep.TURN_ON) ShowerStep.GET_WET else ShowerStep.RINSING
                        }
                        // Tap-first: tapping a dirt spot soaps it outright, so the
                        // routine never depends on holding a drag.
                        if (step == ShowerStep.SOAP) {
                            DirtSpots.forEachIndexed { index, spot ->
                                if ((tap - bodySpot(spot)).getDistance() < minDim() * DIRT_TAP_RADIUS) {
                                    if (scrubbed[index] < 1f) tts.speak("Scrub, scrub!", VoiceTone.EXCITED)
                                    scrubbed[index] = 1f
                                }
                            }
                            if (scrubbed.all { it >= 1f }) step = ShowerStep.RINSE_HINT
                        }
                    }
                }
                .pointerInput(step) {
                    detectDragGestures(
                        onDragStart = { start ->
                            // Little fingers aren't precise: any drag during the
                            // soap step picks the soap up.
                            if (step == ShowerStep.SOAP) {
                                soapHeld = true
                                soapPos = start
                                lastTickMs = 0L
                            }
                        },
                        onDrag = { change, _ ->
                            if (soapHeld) {
                                soapPos = change.position
                                // Time-in-zone progress: works for slow little hands
                                // and sparse synthetic input alike.
                                val now = change.uptimeMillis
                                val dt = if (lastTickMs == 0L) 0f else (now - lastTickMs).coerceAtMost(100L).toFloat()
                                lastTickMs = now
                                // Scrub any dirt spot the soap touches.
                                DirtSpots.forEachIndexed { index, spot ->
                                    val distance = (change.position - bodySpot(spot)).getDistance()
                                    val before = scrubbed[index]
                                    scrubbed[index] = scrubProgress(
                                        current = before,
                                        dtMillis = dt,
                                        fillMillis = DIRT_FILL_MILLIS,
                                        onTarget = distance < minDim() * DIRT_SCRUB_RADIUS,
                                        nearTarget = distance < minDim() * DIRT_SCRUB_RADIUS * 1.8f,
                                        toolMoving = true,
                                    )
                                    if (before < 1f && scrubbed[index] >= 1f) {
                                        tts.speak("Scrub, scrub!", VoiceTone.EXCITED)
                                    }
                                }
                                if (scrubbed.all { it >= 1f } && step == ShowerStep.SOAP) {
                                    step = ShowerStep.RINSE_HINT
                                    soapHeld = false
                                    soapPos = null
                                }
                            }
                        },
                        onDragEnd = {
                            soapHeld = false
                            soapPos = null
                        },
                        onDragCancel = {
                            soapHeld = false
                            soapPos = null
                        },
                    )
                },
        ) {
            drawBathroom()
            val soapAmount = scrubbed.toList()
            drawWetKerker(
                bob = bob,
                wet = step >= ShowerStep.GET_WET,
                dirt = soapAmount.map { 1f - it },
                bubbleAmount = soapAmount,
                rinse = if (step >= ShowerStep.RINSING) rinseT else 0f,
                sparkle = step == ShowerStep.DONE,
            )
            drawShowerHead(waterOn = waterT.value > 0f && waterT.value < 1f)
            drawKnob(highlight = (step == ShowerStep.TURN_ON || step == ShowerStep.RINSE_HINT), pulse = pulse)
            if (step == ShowerStep.SOAP) {
                drawSoap(soapPos ?: soapHomeDraw(), held = soapHeld, pulse = if (soapPos == null) pulse else 1f)
            }

            // Picture progress: wet, three scrubbed spots, rinsed.
            drawCareStars(
                filled = when (step) {
                    ShowerStep.TURN_ON -> 0
                    ShowerStep.GET_WET -> 1
                    ShowerStep.SOAP -> 1 + scrubbed.count { it >= 1f }
                    ShowerStep.RINSE_HINT -> 4
                    ShowerStep.RINSING -> 4
                    ShowerStep.DONE -> 5
                },
                total = 5,
            )

            // Stalled? Show the kid exactly what to touch, on a loop.
            if (stalled && step != ShowerStep.DONE && step != ShowerStep.RINSING) {
                val target = when (step) {
                    ShowerStep.SOAP -> {
                        val next = DirtSpots.indices.firstOrNull { scrubbed[it] < 1f } ?: 0
                        bodySpotDraw(DirtSpots[next])
                    }
                    else -> knobCenter()
                }
                drawTargetHalo(target, size.minDimension * 0.15f, pulse)
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

// ---- geometry helpers (shared between gestures and drawing) ------------------

private fun androidx.compose.ui.input.pointer.PointerInputScope.minDim(): Float =
    kotlin.math.min(size.width, size.height).toFloat()

private fun DrawScope.knobCenter() = Offset(size.width * 0.9f, size.height * 0.42f)
private fun androidx.compose.ui.input.pointer.PointerInputScope.knobCenterPx() =
    Offset(size.width * 0.9f, size.height * 0.42f)

private fun androidx.compose.ui.input.pointer.PointerInputScope.isOnKnob(tap: Offset) =
    (tap - knobCenterPx()).getDistance() < size.width * 0.14f

private fun androidx.compose.ui.input.pointer.PointerInputScope.soapHome() =
    Offset(size.width * 0.14f, size.height * 0.82f)

// Chibi geometry shared by gestures and drawing: big round head up top, a
// round tummy-body below (the scrub zone), stubby arms and feet.
private fun chibiSpotOf(frac: Offset, w: Float, h: Float): Offset {
    val r = w * 0.26f
    val bodyTop = h * 0.30f + r * 0.8f
    val bodyBottom = h * 0.82f
    return Offset(w / 2f - r * 0.75f + frac.x * r * 1.5f, bodyTop + frac.y * (bodyBottom - bodyTop))
}

private fun androidx.compose.ui.input.pointer.PointerInputScope.bodySpot(frac: Offset): Offset =
    chibiSpotOf(frac, size.width.toFloat(), size.height.toFloat())

private fun DrawScope.bodySpotDraw(frac: Offset): Offset =
    chibiSpotOf(frac, size.width, size.height)

private fun DrawScope.soapHomeDraw() = Offset(size.width * 0.14f, size.height * 0.82f)

// ---- drawing ------------------------------------------------------------------

private fun DrawScope.drawBathroom() {
    // tiled wall + floor
    drawRect(Brush.verticalGradient(listOf(Color(0xFFBFE8F2), Color(0xFFD8F1F7)), endY = size.height * 0.85f))
    val tile = Color(0xFFA8D8E8).copy(alpha = 0.4f)
    val tileSize = size.minDimension * 0.18f
    var y = 0f
    while (y < size.height * 0.85f) {
        drawLine(tile, Offset(0f, y), Offset(size.width, y), strokeWidth = 3f)
        y += tileSize
    }
    var x = 0f
    while (x < size.width) {
        drawLine(tile, Offset(x, 0f), Offset(x, size.height * 0.85f), strokeWidth = 3f)
        x += tileSize
    }
    drawRect(
        Brush.verticalGradient(
            listOf(Color(0xFFE8DFCE), Color(0xFFD4C6AC)),
            startY = size.height * 0.85f,
        ),
        topLeft = Offset(0f, size.height * 0.85f),
        size = Size(size.width, size.height * 0.15f),
    )
    // wall meets floor with a soft shadow line
    drawRect(
        Brush.verticalGradient(
            listOf(Color(0x33403017), Color.Transparent),
            startY = size.height * 0.85f,
            endY = size.height * 0.885f,
        ),
        topLeft = Offset(0f, size.height * 0.85f),
        size = Size(size.width, size.height * 0.035f),
    )
    // bath mat under Kerker
    drawOval(
        Brush.radialGradient(
            listOf(Color(0xFF8FD0E8), Color(0xFF6FB6D4)),
            center = Offset(size.width * 0.5f, size.height * 0.875f),
            radius = size.width * 0.35f,
        ),
        topLeft = Offset(size.width * 0.18f, size.height * 0.845f),
        size = Size(size.width * 0.64f, size.height * 0.06f),
    )
}

private fun DrawScope.drawShowerHead(waterOn: Boolean) {
    val headCenter = Offset(size.width * 0.5f, size.height * 0.09f)
    val headW = size.width * 0.3f
    // pipe from the knob wall
    drawLine(
        Color(0xFFAAB9C0),
        Offset(size.width, size.height * 0.05f),
        Offset(headCenter.x, size.height * 0.05f),
        strokeWidth = size.minDimension * 0.03f,
    )
    drawLine(
        Color(0xFFAAB9C0),
        Offset(headCenter.x, size.height * 0.05f),
        headCenter,
        strokeWidth = size.minDimension * 0.03f,
    )
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFFE6EEF2), Color(0xFFAAB9C0))),
        topLeft = Offset(headCenter.x - headW / 2f, headCenter.y),
        size = Size(headW, size.height * 0.035f),
        cornerRadius = CornerRadius(size.minDimension * 0.03f),
    )
    if (waterOn) {
        val water = SkyBlue.copy(alpha = 0.6f)
        for (i in 0..5) {
            val streamX = headCenter.x - headW / 2f + headW * (0.08f + 0.168f * i)
            var streamY = headCenter.y + size.height * 0.04f
            while (streamY < size.height * 0.8f) {
                drawLine(
                    water,
                    Offset(streamX, streamY),
                    Offset(streamX, streamY + size.height * 0.03f),
                    strokeWidth = size.minDimension * 0.014f,
                )
                streamY += size.height * 0.05f
            }
        }
    }
}

private fun DrawScope.drawKnob(highlight: Boolean, pulse: Float) {
    val knob = knobCenter()
    val r = size.width * 0.1f * (if (highlight) pulse else 1f)
    if (highlight) drawCircle(Color(0xFFFECA57).copy(alpha = 0.45f), r * 1.5f, knob)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFE6EEF2), Color(0xFF9FB0B8)), center = knob - Offset(r * 0.3f, r * 0.3f), radius = r * 2f), r, knob)
    rotate(degrees = 40f, pivot = knob) {
        drawRoundRect(
            Color(0xFF7E9096),
            topLeft = Offset(knob.x - r * 0.8f, knob.y - r * 0.16f),
            size = Size(r * 1.6f, r * 0.32f),
            cornerRadius = CornerRadius(r * 0.16f),
        )
    }
}

private fun DrawScope.drawSoap(at: Offset, held: Boolean, pulse: Float) {
    if (!held) {
        // soap dish
        drawOval(
            Color(0xFFC9BFAE),
            topLeft = Offset(soapHomeDraw().x - size.minDimension * 0.14f, soapHomeDraw().y + size.minDimension * 0.045f),
            size = Size(size.minDimension * 0.28f, size.minDimension * 0.06f),
        )
    }
    val w = size.minDimension * 0.22f * pulse
    val h = size.minDimension * 0.12f * pulse
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFFFFB7C9), Color(0xFFF08CA0))),
        topLeft = Offset(at.x - w / 2f, at.y - h / 2f),
        size = Size(w, h),
        cornerRadius = CornerRadius(h * 0.5f),
    )
    drawOval(
        Color.White.copy(alpha = 0.5f),
        topLeft = Offset(at.x - w * 0.3f, at.y - h * 0.32f),
        size = Size(w * 0.35f, h * 0.28f),
    )
}

private fun DrawScope.drawWetKerker(
    bob: Float,
    wet: Boolean,
    dirt: List<Float>,
    bubbleAmount: List<Float>,
    rinse: Float,
    sparkle: Boolean,
) {
    val d = size.minDimension
    val cx = size.width / 2f
    val r = size.width * 0.26f
    val headCy = size.height * 0.30f
    val bobOffset = sin(bob * TWO_PI) * d * 0.008f
    val head = Offset(cx, headCy + bobOffset)
    val bodyTop = headCy + r * 0.8f
    val bodyBottom = size.height * 0.82f
    val bodyW = r * 1.5f
    val skin = Color(0xFFF6CBA4)
    val skinBrush = Brush.radialGradient(
        listOf(lerp(skin, Color.White, 0.4f), skin, lerp(skin, Color(0xFF4A2F1E), 0.15f)),
        center = head - Offset(r * 0.35f, r * 0.45f),
        radius = size.height * 0.6f,
    )

    // feet
    for (side in listOf(-1f, 1f)) {
        drawOval(
            skinBrush,
            topLeft = Offset(cx + side * bodyW * 0.22f - r * 0.19f, bodyBottom - r * 0.08f),
            size = Size(r * 0.38f, r * 0.24f),
        )
    }
    // stubby arms
    for (side in listOf(-1f, 1f)) {
        drawCircle(
            skinBrush,
            radius = r * 0.23f,
            center = Offset(cx + side * bodyW * 0.62f, bodyTop + (bodyBottom - bodyTop) * 0.3f),
        )
    }
    // round tummy body (bath time — no shirt!), shaded like a soft cushion
    drawShadedRoundRect(
        topLeft = Offset(cx - bodyW / 2f, bodyTop + bobOffset),
        size = Size(bodyW, bodyBottom - bodyTop),
        cornerRadius = r * 0.5f,
        base = skin,
    )
    // big round head, shaded like a sphere, grounded with a cast shadow
    drawContactShadow(head + Offset(0f, r * 1.1f), r * 1.4f, r * 0.45f, alpha = 0.2f)
    drawBall(head, r, skin)
    if (wet) {
        drawOval(
            Color.White.copy(alpha = 0.25f),
            topLeft = head + Offset(-r * 0.72f, -r * 0.72f),
            size = Size(r * 0.5f, r * 0.3f),
        )
    }

    // curly mop + wisps
    for (angleDeg in listOf(-152f, -124f, -96f, -68f, -40f, -12f)) {
        val angle = Math.toRadians(angleDeg.toDouble()).toFloat()
        drawCurl(head + Offset(kotlin.math.cos(angle) * r * 0.82f, sin(angle) * r * 0.82f), r * 0.4f)
    }
    for ((i, wx) in listOf(-0.62f, 0f, 0.62f).withIndex()) {
        val dir = if (i % 2 == 0) 1f else -1f
        val wisp = head + Offset(r * wx, -r * (if (i == 1) 1.18f else 1.0f))
        val curl = androidx.compose.ui.graphics.Path().apply {
            moveTo(wisp.x, wisp.y + r * 0.1f)
            cubicTo(
                wisp.x + dir * r * 0.07f, wisp.y - r * 0.03f,
                wisp.x - dir * r * 0.08f, wisp.y - r * 0.12f,
                wisp.x + dir * r * 0.07f, wisp.y - r * 0.18f,
            )
        }
        drawPath(curl, Color(0xFF32241B), style = Stroke(width = r * 0.045f))
    }

    // huge sparkly eyes
    val eyeY = head.y - r * 0.08f
    for (side in listOf(-1f, 1f)) {
        val eye = Offset(cx + side * r * 0.42f, eyeY)
        val eyeR = r * 0.28f
        drawEyeBall(eye, eyeR)
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFF54331B), Color(0xFF241206)),
                center = eye - Offset(0f, eyeR * 0.2f),
                radius = eyeR * 0.85f,
            ),
            radius = eyeR * 0.7f,
            center = eye,
        )
        drawCircle(Color.White, eyeR * 0.24f, eye + Offset(eyeR * 0.2f, -eyeR * 0.26f))
        drawCircle(Color.White.copy(alpha = 0.8f), eyeR * 0.11f, eye + Offset(-eyeR * 0.22f, eyeR * 0.2f))
    }
    // blush
    for (side in listOf(-1f, 1f)) {
        drawSoftBlush(head + Offset(side * r * 0.62f, r * 0.33f), r * 0.44f, r * 0.26f)
    }
    // sweet smile
    val mouthY = head.y + r * 0.5f
    val smile = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx - r * 0.2f, mouthY)
        quadraticTo(cx, mouthY + r * 0.18f, cx + r * 0.2f, mouthY)
    }
    drawPath(smile, Color(0xFF3A2E28), style = Stroke(width = r * 0.05f))

    // dirt spots fade as they're scrubbed
    DirtSpots.forEachIndexed { index, spot ->
        val at = bodySpotDraw(spot)
        val alpha = dirt[index].coerceIn(0f, 1f) * 0.55f
        if (alpha > 0.01f) {
            drawOval(
                Color(0xFF57431F).copy(alpha = alpha),
                topLeft = Offset(at.x - d * 0.07f, at.y - d * 0.045f),
                size = Size(d * 0.14f, d * 0.09f),
            )
        }
        // soap bubbles mark the scrubbed spots, sliding down as they rinse
        val bubbles = bubbleAmount[index] * (1f - rinse)
        if (bubbles > 0.02f) {
            val slide = rinse * size.height * 0.2f
            for (i in 0..3) {
                val angle = i / 4f * TWO_PI
                drawCircle(
                    Color.White.copy(alpha = 0.85f * bubbles),
                    d * (0.035f - i * 0.004f),
                    Offset(
                        at.x + kotlin.math.cos(angle) * d * 0.05f,
                        at.y + kotlin.math.sin(angle) * d * 0.04f + slide,
                    ),
                )
            }
        }
    }

    if (sparkle) {
        for ((sx, sy) in listOf(0.3f to 0.35f, 0.7f to 0.45f, 0.5f to 0.65f)) {
            val at = Offset(size.width * sx, size.height * sy)
            val star = androidx.compose.ui.graphics.Path().apply {
                moveTo(at.x, at.y - d * 0.03f)
                quadraticTo(at.x + d * 0.006f, at.y - d * 0.006f, at.x + d * 0.03f, at.y)
                quadraticTo(at.x + d * 0.006f, at.y + d * 0.006f, at.x, at.y + d * 0.03f)
                quadraticTo(at.x - d * 0.006f, at.y + d * 0.006f, at.x - d * 0.03f, at.y)
                quadraticTo(at.x - d * 0.006f, at.y - d * 0.006f, at.x, at.y - d * 0.03f)
                close()
            }
            drawPath(star, Color.White)
        }
    }
}
