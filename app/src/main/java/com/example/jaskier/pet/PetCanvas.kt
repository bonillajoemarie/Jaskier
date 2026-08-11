package com.example.jaskier.pet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import com.example.jaskier.ui.theme.BubblePink
import com.example.jaskier.ui.theme.LeafGreen
import com.example.jaskier.ui.theme.SkyBlue
import com.example.jaskier.ui.theme.SunYellow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private const val TWO_PI = 2f * Math.PI.toFloat()
private val Ink = Color(0xFF3A2E28)
private val SickGreen = Color(0xFFA9C7A1)

// Kerker's look, translated from the reference photo into a chibi cartoon:
// big round head, huge sparkly brown eyes, dark curls, tiny body in his
// white tank top, stubby arms and feet.
val KerkerSkin = Color(0xFFF6CBA4)
private val KerkerGrime = Color(0xFF9A8163)
private val HairBrown = Color(0xFF32241B)
private val PupilBrown = Color(0xFF54331B)

@Composable
fun PetCanvas(
    hunger: Float,
    cleanliness: Float,
    mood: PetMood,
    events: Flow<PetEvent>,
    modifier: Modifier = Modifier,
    emotion: Emotion = Emotion.HAPPY,
    onPoke: () -> Unit = {},
    onTouchZone: (KerkerZone) -> Unit = {},
    onTickle: () -> Unit = {},
) {
    val idle = rememberInfiniteTransition(label = "idle")
    val bobT by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "bob",
    )
    val cloudT by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "clouds",
    )
    val blinkT by idle.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3500
                1f at 0
                1f at 3260
                0.1f at 3320
                0.1f at 3440
                1f at 3500
            },
        ),
        label = "blink",
    )

    val feedT = remember { Animatable(0f) }
    val showerT = remember { Animatable(0f) }
    val brushT = remember { Animatable(0f) }
    val healT = remember { Animatable(0f) }
    val squishT = remember { Animatable(0f) }
    val pokeHeartsT = remember { Animatable(0f) }
    val bubbleSeed = remember { mutableIntStateOf(0) }
    var touch by remember { mutableStateOf<Offset?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(events) {
        events.collect { event ->
            suspend fun run(anim: Animatable<Float, *>, durationMs: Int) {
                anim.snapTo(0f)
                anim.animateTo(1f, tween(durationMs))
                anim.snapTo(0f)
            }
            when (event) {
                // Drinking reuses the eating animation: mouth open, happy gulp.
                PetEvent.FED, PetEvent.DRANK -> run(feedT, 900)
                PetEvent.SHOWERED -> {
                    bubbleSeed.intValue++
                    run(showerT, 1800)
                }
                PetEvent.BRUSHED -> run(brushT, 1600)
                PetEvent.HEALED -> run(healT, 1200)
            }
        }
    }

    val bubbles = remember(bubbleSeed.intValue) {
        val random = Random(bubbleSeed.intValue)
        List(10) {
            Bubble(
                x = 0.26f + random.nextFloat() * 0.48f,
                radius = 0.028f + random.nextFloat() * 0.04f,
                speed = 0.5f + random.nextFloat() * 0.5f,
            )
        }
    }

    Canvas(
        modifier = modifier
            // Poke Kerker: he squishes and giggles hearts. Eyes follow the finger.
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        touch = offset
                        tryAwaitRelease()
                        touch = null
                    },
                    onTap = { at ->
                        // Different body parts react differently, Talking-Tom style.
                        val minDim = minOf(size.width, size.height).toFloat()
                        val zone = zoneAt(
                            at,
                            Offset(size.width / 2f, size.height * 0.40f),
                            minDim * 0.30f,
                        )
                        if (zone == KerkerZone.NONE) onPoke() else onTouchZone(zone)
                        scope.launch {
                            squishT.snapTo(0f)
                            squishT.animateTo(1f, tween(450))
                            squishT.snapTo(0f)
                        }
                        scope.launch {
                            pokeHeartsT.snapTo(0f)
                            pokeHeartsT.animateTo(1f, tween(700))
                            pokeHeartsT.snapTo(0f)
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                val tickle = TickleDetector()
                detectDragGestures(
                    onDragStart = { tickle.reset() },
                    onDrag = { change, _ ->
                        touch = change.position
                        // A rub back and forth over him is a tickle, not a drag.
                        if (tickle.onMove(change.position.x, change.uptimeMillis)) {
                            onTickle()
                            scope.launch {
                                squishT.snapTo(0f)
                                squishT.animateTo(1f, tween(450))
                                squishT.snapTo(0f)
                            }
                        }
                    },
                    onDragEnd = {
                        touch = null
                        tickle.reset()
                    },
                    onDragCancel = {
                        touch = null
                        tickle.reset()
                    },
                )
            },
    ) {
        val bobPhase = sin(bobT * TWO_PI)
        // Excitement hops, laughter shakes, sleepiness breathes slow and deep.
        val hop = if (emotion == Emotion.EXCITED) {
            -kotlin.math.abs(sin(bobT * TWO_PI * 3f)) * size.minDimension * 0.05f
        } else {
            0f
        }
        val shake = if (emotion == Emotion.LAUGHING) {
            sin(bobT * TWO_PI * 9f) * size.minDimension * 0.012f
        } else {
            0f
        }
        val breath = if (emotion == Emotion.SLEEPY) 0.5f else 1f
        val bob = bobPhase * size.minDimension * 0.015f * breath + hop
        val pokeSquish = sin(squishT.value * Math.PI.toFloat()) * 0.10f
        val emotionSquish = if (emotion == Emotion.EXCITED) hop / size.minDimension * 0.8f else 0f
        val squashY = 1f + bobPhase * 0.015f * breath - pokeSquish - emotionSquish
        val squashX = 2f - (1f + bobPhase * 0.015f * breath) + pokeSquish + emotionSquish

        drawBackdrop(cloudT)
        // Paper grain over the flat scene, deterministic so it never crawls.
        drawPaperGrain()

        val eating = feedT.value > 0f
        val brushing = brushT.value > 0f
        var skin = lerp(KerkerGrime, KerkerSkin, (cleanliness / STAT_MAX).coerceIn(0f, 1f))
        if (mood == PetMood.SICK) skin = lerp(skin, SickGreen, 0.55f)

        // Contact shadow under his feet.
        val shadowScale = 1f - bobPhase * 0.05f
        drawOval(
            Color(0x2E5B4A26),
            topLeft = Offset(center.x - headR() * 1.05f * shadowScale, feetY() + headR() * 0.12f),
            size = Size(headR() * 2.1f * shadowScale, headR() * 0.28f),
        )

        translate(top = bob) {
            scale(scaleX = squashX, scaleY = squashY, pivot = Offset(center.x, feetY())) {
                drawChibiBody(skin)
                drawHead(skin)
                drawHair(bobT)
                if (cleanliness < MOOD_THRESHOLD) drawDirt()
                // Sick pets have droopy, half-closed eyes.
                val lid = when {
                    emotion == Emotion.LAUGHING -> 0.12f
                    emotion == Emotion.SLEEPY -> blinkT.coerceAtMost(0.35f)
                    mood == PetMood.SICK -> blinkT.coerceAtMost(0.55f)
                    else -> blinkT
                }
                drawEyes(blinkScale = lid, mood = mood, touch = touch)
                drawBlush(mood)
                drawMouth(mood, eating = eating, brushing = brushing, brushT = brushT.value)
                if (mood == PetMood.SICK) drawSweatDrop(bobT)
                drawEmotionOverlay(emotion, bobT, shake)
            }
        }

        if (feedT.value > 0f) drawFeedAnimation(feedT.value)
        if (showerT.value > 0f) drawShowerAnimation(showerT.value, bubbles)
        if (brushT.value > 0f) drawBrushAnimation(brushT.value)
        if (healT.value > 0f) drawHealAnimation(healT.value)
        if (pokeHeartsT.value > 0f) drawPokeHearts(pokeHeartsT.value)
    }
}

private data class Bubble(val x: Float, val radius: Float, val speed: Float)

// ---- chibi anchors ------------------------------------------------------------

private fun DrawScope.headR() = size.minDimension * 0.30f
private fun DrawScope.headC() = Offset(size.width / 2f, size.height * 0.40f)
private fun DrawScope.mouthAnchor() = headC() + Offset(0f, headR() * 0.48f)
private fun DrawScope.feetY() = (headC().y + headR() * 2.05f).coerceAtMost(size.height * 0.94f)

// Sunny sky scene behind Kerker: sun with glow halo and two drifting clouds.
private fun DrawScope.drawBackdrop(cloudT: Float) {
    val d = size.minDimension
    val sun = Offset(size.width * 0.85f, size.height * 0.12f)
    drawCircle(SunYellow.copy(alpha = 0.25f), radius = d * 0.13f, center = sun)
    drawCircle(SunYellow, radius = d * 0.08f, center = sun)

    val drift = cloudT * size.width
    drawCloud(Offset((drift + size.width * 0.15f) % (size.width * 1.3f) - size.width * 0.15f, size.height * 0.10f), d * 0.055f)
    drawCloud(Offset((drift * 0.6f + size.width * 0.65f) % (size.width * 1.3f) - size.width * 0.15f, size.height * 0.22f), d * 0.04f)
}

private fun DrawScope.drawCloud(at: Offset, r: Float) {
    val cloud = Color.White.copy(alpha = 0.85f)
    drawCircle(cloud, r, at)
    drawCircle(cloud, r * 0.8f, at + Offset(-r * 1.1f, r * 0.25f))
    drawCircle(cloud, r * 0.85f, at + Offset(r * 1.1f, r * 0.2f))
    drawOval(cloud, topLeft = at + Offset(-r * 1.8f, r * 0.1f), size = Size(r * 3.6f, r * 1.1f))
}

// Small body in a white tank top with stubby arms and little feet.
private fun DrawScope.drawChibiBody(skin: Color) {
    val r = headR()
    val c = headC()
    val bodyTop = c.y + r * 0.75f
    val bodyW = r * 1.55f
    val bodyH = feetY() - bodyTop + r * 0.05f

    // feet peeking out
    for (side in listOf(-1f, 1f)) {
        drawOval(
            skin.shade(),
            topLeft = Offset(c.x + side * bodyW * 0.22f - r * 0.19f, feetY() - r * 0.10f),
            size = Size(r * 0.38f, r * 0.24f),
        )
    }

    // stubby arms, shaded like little spheres
    for (side in listOf(-1f, 1f)) {
        drawBall(
            Offset(c.x + side * bodyW * 0.62f, bodyTop + bodyH * 0.35f),
            r * 0.23f,
            skin,
            specular = false,
        )
    }

    // tank-top body
    drawRoundRect(
        Brush.verticalGradient(
            colors = listOf(Color.White, Color(0xFFE2DED4)),
            startY = bodyTop,
            endY = bodyTop + bodyH,
        ),
        topLeft = Offset(c.x - bodyW / 2f, bodyTop),
        size = Size(bodyW, bodyH),
        cornerRadius = CornerRadius(r * 0.45f),
    )
    // fabric turns away from the light on the right side
    drawRoundRect(
        Brush.horizontalGradient(
            listOf(Color.White.copy(alpha = 0.25f), Color.Transparent, Color(0xFF6B5A44).copy(alpha = 0.14f)),
            startX = c.x - bodyW / 2f,
            endX = c.x + bodyW / 2f,
        ),
        topLeft = Offset(c.x - bodyW / 2f, bodyTop),
        size = Size(bodyW, bodyH),
        cornerRadius = CornerRadius(r * 0.45f),
    )
}

private fun Color.shade(): Brush = Brush.radialGradient(
    colors = listOf(lerp(this, Color.White, 0.35f), this, lerp(this, Color(0xFF4A2F1E), 0.15f)),
)

// Big round head — the star of the chibi silhouette, shaded like a sphere.
private fun DrawScope.drawHead(skin: Color) {
    val r = headR()
    val c = headC()
    // the head grounds itself on the body with a soft cast shadow
    drawContactShadow(c + Offset(0f, r * 1.12f), r * 1.5f, r * 0.5f, alpha = 0.22f)
    drawBall(c, r, skin)
}

// Dark curly mop hugging the top of the head, with flyaway wisps.
private fun DrawScope.drawHair(bobT: Float) {
    val r = headR()
    val c = headC()

    val curls = listOf(
        Triple(-124f, 0.42f, 1f),
        Triple(-152f, 0.38f, 1f),
        Triple(-96f, 0.44f, 1f),
        Triple(-68f, 0.44f, 1f),
        Triple(-40f, 0.38f, 1f),
        Triple(-12f, 0.34f, 1f),
    )
    for ((angleDeg, sizeFrac, _) in curls) {
        val angle = Math.toRadians(angleDeg.toDouble()).toFloat()
        val curlCenter = c + Offset(cos(angle) * r * 0.82f, sin(angle) * r * 0.82f)
        drawCurl(curlCenter, r * sizeFrac)
    }

    // Flyaway curl wisps that sway with the idle bob.
    val sway = sin(bobT * TWO_PI) * r * 0.02f
    val wisps = listOf(
        c + Offset(-r * 0.62f, -r * 1.02f),
        c + Offset(0f, -r * 1.18f),
        c + Offset(r * 0.62f, -r * 1.0f),
    )
    for ((i, wisp) in wisps.withIndex()) {
        val dir = if (i % 2 == 0) 1f else -1f
        val curl = Path().apply {
            moveTo(wisp.x, wisp.y + r * 0.1f)
            cubicTo(
                wisp.x + dir * r * 0.07f + sway, wisp.y - r * 0.03f,
                wisp.x - dir * r * 0.08f + sway, wisp.y - r * 0.12f,
                wisp.x + dir * r * 0.07f + sway, wisp.y - r * 0.18f,
            )
        }
        drawPath(curl, HairBrown, style = Stroke(width = r * 0.045f))
    }
}

private fun DrawScope.drawDirt() {
    val r = headR()
    val c = headC()
    val smudge = Color(0x33402D1A)
    // one on the cheek, two on the shirt
    drawOval(smudge, topLeft = c + Offset(r * 0.35f, r * 0.28f), size = Size(r * 0.34f, r * 0.2f))
    drawOval(smudge, topLeft = c + Offset(-r * 0.6f, r * 1.15f), size = Size(r * 0.4f, r * 0.22f))
    drawOval(smudge, topLeft = c + Offset(r * 0.18f, r * 1.5f), size = Size(r * 0.34f, r * 0.2f))

    val stink = Color(0x55707C4A)
    for (side in listOf(-1f, 1f)) {
        val x = c.x + side * r * 0.5f
        val baseY = c.y - r * 1.25f
        val wave = Path().apply {
            moveTo(x, baseY)
            cubicTo(x - r * 0.1f, baseY - r * 0.1f, x + r * 0.1f, baseY - r * 0.2f, x, baseY - r * 0.3f)
        }
        drawPath(wave, stink, style = Stroke(width = r * 0.04f))
    }
}

// Huge glossy eyes: the cuteness engine. Pupils track the finger.
private fun DrawScope.drawEyes(blinkScale: Float, mood: PetMood, touch: Offset?) {
    val r = headR()
    val c = headC()
    val eyeY = c.y - r * 0.08f
    val eyeOffsetX = r * 0.42f
    val eyeR = r * 0.30f

    for (side in listOf(-1f, 1f)) {
        val eyeCenter = Offset(c.x + side * eyeOffsetX, eyeY)

        drawEyeBall(eyeCenter, eyeR)

        val maxShift = eyeR * 0.26f
        val pupilShift = if (touch != null) {
            val angle = atan2(touch.y - eyeCenter.y, touch.x - eyeCenter.x)
            Offset(cos(angle), sin(angle)) * min(maxShift, (touch - eyeCenter).getDistance() * 0.1f)
        } else {
            Offset(0f, 0f)
        }
        val pupilCenter = eyeCenter + pupilShift

        scale(scaleX = 1f, scaleY = blinkScale, pivot = eyeCenter) {
            // Big warm-brown pupil filling most of the eye = instantly lovable.
            drawCircle(
                Brush.radialGradient(
                    colors = listOf(PupilBrown, Color(0xFF241206)),
                    center = pupilCenter - Offset(0f, eyeR * 0.2f),
                    radius = eyeR * 0.85f,
                ),
                radius = eyeR * 0.72f,
                center = pupilCenter,
            )
            // iris rim ring adds glassy depth
            drawCircle(
                Color(0xFF1B0D04).copy(alpha = 0.7f),
                radius = eyeR * 0.72f,
                center = pupilCenter,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = eyeR * 0.07f),
            )
            // twin sparkles
            drawCircle(Color.White, radius = eyeR * 0.24f, center = pupilCenter + Offset(eyeR * 0.2f, -eyeR * 0.26f))
            drawCircle(Color.White.copy(alpha = 0.8f), radius = eyeR * 0.11f, center = pupilCenter + Offset(-eyeR * 0.22f, eyeR * 0.2f))
        }

        if (mood == PetMood.HUNGRY || mood == PetMood.SICK) {
            val browY = eyeY - eyeR * 1.45f
            rotate(degrees = side * 16f, pivot = Offset(eyeCenter.x, browY)) {
                drawLine(
                    Ink,
                    start = Offset(eyeCenter.x - eyeR * 0.6f, browY),
                    end = Offset(eyeCenter.x + eyeR * 0.6f, browY),
                    strokeWidth = r * 0.05f,
                )
            }
        }
    }
}

private fun DrawScope.drawBlush(mood: PetMood) {
    if (mood == PetMood.SICK) return
    val r = headR()
    val c = headC()
    for (side in listOf(-1f, 1f)) {
        drawSoftBlush(c + Offset(side * r * 0.62f, r * 0.33f), r * 0.44f, r * 0.26f)
    }
}

private fun DrawScope.drawSweatDrop(bobT: Float) {
    val r = headR()
    val c = headC()
    val wobble = sin(bobT * TWO_PI) * r * 0.02f
    val at = c + Offset(r * 0.78f, -r * 0.62f + wobble)
    val drop = Path().apply {
        moveTo(at.x, at.y - r * 0.14f)
        quadraticTo(at.x + r * 0.11f, at.y + r * 0.05f, at.x, at.y + r * 0.11f)
        quadraticTo(at.x - r * 0.11f, at.y + r * 0.05f, at.x, at.y - r * 0.14f)
        close()
    }
    drawPath(drop, SkyBlue.copy(alpha = 0.85f))
    drawCircle(Color.White.copy(alpha = 0.6f), radius = r * 0.025f, center = at + Offset(-r * 0.025f, 0f))
}

private fun DrawScope.drawMouth(mood: PetMood, eating: Boolean, brushing: Boolean, brushT: Float) {
    val r = headR()
    val m = mouthAnchor()
    val stroke = Stroke(width = r * 0.055f)

    when {
        // Wide open while eating or being brushed, so the action reads clearly.
        eating || (brushing && brushT < 0.8f) -> {
            drawOval(
                Ink,
                topLeft = m + Offset(-r * 0.24f, -r * 0.16f),
                size = Size(r * 0.48f, r * 0.38f),
            )
            if (brushing) drawTeethRow(m.x, m.y - r * 0.14f, r, Color.White)
        }
        // Right after brushing: a proud sparkling-white grin.
        brushing -> {
            drawSmile(m, r)
            drawTeethRow(m.x, m.y - r * 0.05f, r, Color.White)
        }
        mood == PetMood.HAPPY -> {
            // Kerker's signature laugh: open smile, tiny teeth, pink tongue.
            drawSmile(m, r)
            drawTeethRow(m.x, m.y - r * 0.04f, r, Color.White)
            val tongue = Path().apply {
                moveTo(m.x - r * 0.17f, m.y + r * 0.14f)
                quadraticTo(m.x, m.y + r * 0.3f, m.x + r * 0.17f, m.y + r * 0.14f)
                close()
            }
            drawPath(tongue, BubblePink)
        }
        mood == PetMood.HUNGRY -> {
            val frown = Path().apply {
                moveTo(m.x - r * 0.24f, m.y + r * 0.12f)
                quadraticTo(m.x, m.y - r * 0.1f, m.x + r * 0.24f, m.y + r * 0.12f)
            }
            drawPath(frown, Ink, style = stroke)
        }
        mood == PetMood.YUCKY_TEETH -> {
            drawOval(
                Ink,
                topLeft = m + Offset(-r * 0.27f, -r * 0.13f),
                size = Size(r * 0.54f, r * 0.32f),
            )
            drawTeethRow(m.x, m.y - r * 0.11f, r, Color(0xFFE8D77A))
        }
        mood == PetMood.SICK -> {
            val wobble = Path().apply {
                moveTo(m.x - r * 0.24f, m.y + r * 0.06f)
                cubicTo(
                    m.x - r * 0.1f, m.y - r * 0.06f,
                    m.x + r * 0.06f, m.y + r * 0.14f,
                    m.x + r * 0.24f, m.y,
                )
            }
            drawPath(wobble, Ink, style = stroke)
        }
        else -> {
            // DIRTY: wiggly unimpressed mouth
            val wiggle = Path().apply {
                moveTo(m.x - r * 0.28f, m.y)
                cubicTo(
                    m.x - r * 0.12f, m.y - r * 0.08f,
                    m.x + r * 0.12f, m.y + r * 0.08f,
                    m.x + r * 0.28f, m.y,
                )
            }
            drawPath(wiggle, Ink, style = stroke)
        }
    }
}

private fun DrawScope.drawSmile(m: Offset, r: Float) {
    val smile = Path().apply {
        moveTo(m.x - r * 0.36f, m.y - r * 0.08f)
        quadraticTo(m.x, m.y + r * 0.34f, m.x + r * 0.36f, m.y - r * 0.08f)
        close()
    }
    drawPath(smile, Ink)
}

// A gently arced row of individually shaped teeth under a thin gum line —
// reads as a real little smile rather than flat blocks.
private fun DrawScope.drawTeethRow(cx: Float, y: Float, r: Float, color: Color) {
    val toothW = r * 0.11f
    drawRoundRect(
        Color(0xFFF4A0AC),
        topLeft = Offset(cx - toothW * 2.8f, y - r * 0.025f),
        size = Size(toothW * 5.6f, r * 0.045f),
        cornerRadius = CornerRadius(r * 0.02f),
    )
    for (i in -2..2) {
        val tx = cx + i * toothW * 1.08f - toothW / 2f
        val ty = y + i * i * r * 0.008f
        val crownH = r * (if (i == -2 || i == 2) 0.075f else 0.095f)
        val tooth = Path().apply {
            moveTo(tx, ty)
            lineTo(tx + toothW * 0.92f, ty)
            lineTo(tx + toothW * 0.92f, ty + crownH * 0.6f)
            quadraticTo(tx + toothW * 0.46f, ty + crownH * 1.45f, tx, ty + crownH * 0.6f)
            close()
        }
        drawPath(
            tooth,
            Brush.verticalGradient(
                listOf(lerp(color, Color(0xFFB9B39F), 0.25f), color),
                startY = ty,
                endY = ty + crownH,
            ),
        )
        drawPath(tooth, Color(0x1A000000), style = Stroke(width = r * 0.008f))
    }
}

// Apple arcs up to the mouth with a tumble, shrinks away, then hearts float up.
private fun DrawScope.drawFeedAnimation(t: Float) {
    val r = headR()
    val m = mouthAnchor()

    if (t < 0.6f) {
        val rise = t / 0.6f
        val appleX = m.x + r * 1.2f * (1f - rise) * (1f - rise)
        val appleY = size.height - (size.height - m.y) * rise
        val appleR = r * 0.24f * (if (rise > 0.85f) (1f - rise) / 0.15f else 1f)
        if (appleR > 0f) {
            rotate(degrees = rise * 260f, pivot = Offset(appleX, appleY)) {
                drawCircle(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFF7B85), Color(0xFFE8323F)),
                        center = Offset(appleX - appleR * 0.3f, appleY - appleR * 0.35f),
                        radius = appleR * 1.5f,
                    ),
                    radius = appleR,
                    center = Offset(appleX, appleY),
                )
                drawRect(
                    Color(0xFF6D4C41),
                    topLeft = Offset(appleX - appleR * 0.08f, appleY - appleR * 1.35f),
                    size = Size(appleR * 0.16f, appleR * 0.45f),
                )
                drawOval(
                    LeafGreen,
                    topLeft = Offset(appleX + appleR * 0.1f, appleY - appleR * 1.45f),
                    size = Size(appleR * 0.55f, appleR * 0.3f),
                )
            }
        }
    } else {
        val floatT = (t - 0.6f) / 0.4f
        val alpha = 1f - floatT
        for (side in listOf(-1f, 1f)) {
            val hx = m.x + side * r * 0.55f
            val hy = m.y - r * (0.5f + 0.7f * floatT)
            drawHeart(Offset(hx, hy), r * 0.17f, BubblePink.copy(alpha = alpha))
        }
    }
}

// Shower: a showerhead slides in overhead, water streams down, bubbles foam
// up, and Kerker comes out sparkling.
private fun DrawScope.drawShowerAnimation(t: Float, bubbles: List<Bubble>) {
    val d = size.minDimension
    val cx = size.width / 2f

    val headIn = (t / 0.15f).coerceAtMost(1f)
    val headY = size.height * 0.02f - d * 0.15f * (1f - headIn)
    val headW = d * 0.24f

    drawRoundRect(
        Color(0xFFB9C6CC),
        topLeft = Offset(cx - d * 0.02f, headY - d * 0.02f),
        size = Size(d * 0.04f, d * 0.05f),
        cornerRadius = CornerRadius(d * 0.01f),
    )
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFFE6EEF2), Color(0xFFAAB9C0))),
        topLeft = Offset(cx - headW / 2f, headY + d * 0.02f),
        size = Size(headW, d * 0.05f),
        cornerRadius = CornerRadius(d * 0.025f),
    )

    if (t in 0.1f..0.8f) {
        val streamAlpha = if (t > 0.7f) (0.8f - t) * 10f else 1f
        val water = SkyBlue.copy(alpha = 0.55f * streamAlpha.coerceIn(0f, 1f))
        val scroll = (t * 6f) % 1f
        for (i in 0..4) {
            val x = cx - headW / 2f + headW * (0.1f + 0.2f * i)
            var y = headY + d * 0.08f + scroll * d * 0.09f
            while (y < size.height * 0.85f) {
                drawLine(water, Offset(x, y), Offset(x, y + d * 0.05f), strokeWidth = d * 0.014f)
                y += d * 0.09f
            }
        }
    }

    if (t > 0.2f) {
        val foamT = ((t - 0.2f) / 0.8f).coerceIn(0f, 1f)
        val fade = 1f - foamT
        for (bubble in bubbles) {
            val y = size.height * (1f - foamT * bubble.speed)
            val bubbleCenter = Offset(size.width * bubble.x, y)
            val r = d * bubble.radius
            drawCircle(
                Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.9f * fade), Color(0xB3D9F3FF).copy(alpha = fade)),
                    center = bubbleCenter - Offset(r * 0.3f, r * 0.3f),
                    radius = r * 1.4f,
                ),
                radius = r,
                center = bubbleCenter,
            )
        }
    }

    if (t > 0.72f) {
        val sparkleT = (t - 0.72f) / 0.28f
        val alpha = sin(sparkleT * Math.PI.toFloat())
        drawSparkle(headC() + Offset(-headR() * 0.9f, -headR() * 0.4f), d * 0.035f, alpha)
        drawSparkle(headC() + Offset(headR() * 0.95f, headR() * 0.3f), d * 0.028f, alpha)
        drawSparkle(headC() + Offset(headR() * 0.1f, -headR() * 1.3f), d * 0.03f, alpha)
    }
}

// A big toothbrush scrubs side to side over the open mouth with foam,
// finishing on a sparkling grin.
private fun DrawScope.drawBrushAnimation(t: Float) {
    val d = size.minDimension
    val m = mouthAnchor()

    if (t < 0.8f) {
        val scrub = sin(t * TWO_PI * 3f) * d * 0.09f
        val brushAt = Offset(m.x + scrub, m.y - d * 0.02f)

        val foam = Color.White.copy(alpha = 0.8f)
        val foamR = d * (0.02f + 0.02f * sin(t * TWO_PI * 5f).coerceAtLeast(0f))
        drawCircle(foam, foamR, brushAt + Offset(-d * 0.10f, -d * 0.015f))
        drawCircle(foam, foamR * 1.2f, brushAt + Offset(d * 0.11f, -d * 0.03f))
        drawCircle(foam, foamR * 0.9f, brushAt + Offset(0f, d * 0.045f))

        rotate(degrees = -35f, pivot = brushAt) {
            drawRoundRect(
                Brush.verticalGradient(listOf(Color(0xFFFF8FA0), BubblePink)),
                topLeft = Offset(brushAt.x, brushAt.y),
                size = Size(d * 0.20f, d * 0.035f),
                cornerRadius = CornerRadius(d * 0.017f),
            )
            drawRoundRect(
                Color.White,
                topLeft = Offset(brushAt.x - d * 0.055f, brushAt.y - d * 0.012f),
                size = Size(d * 0.06f, d * 0.045f),
                cornerRadius = CornerRadius(d * 0.008f),
            )
        }
    } else {
        val sparkleT = (t - 0.8f) / 0.2f
        val alpha = sin(sparkleT * Math.PI.toFloat())
        drawSparkle(m + Offset(-d * 0.1f, -d * 0.05f), d * 0.03f, alpha)
        drawSparkle(m + Offset(d * 0.1f, d * 0.01f), d * 0.026f, alpha)
    }
}

// Medicine: a green healing cross floats up with sparkles.
private fun DrawScope.drawHealAnimation(t: Float) {
    val d = size.minDimension
    val cx = size.width / 2f
    val alpha = (1f - t).coerceIn(0f, 1f)
    val rise = headC().y - d * 0.05f - d * 0.18f * t
    val crossR = d * 0.05f

    val cross = Color(0xFF41C97A).copy(alpha = alpha)
    drawRoundRect(
        cross,
        topLeft = Offset(cx - crossR * 0.3f, rise - crossR),
        size = Size(crossR * 0.6f, crossR * 2f),
        cornerRadius = CornerRadius(crossR * 0.2f),
    )
    drawRoundRect(
        cross,
        topLeft = Offset(cx - crossR, rise - crossR * 0.3f),
        size = Size(crossR * 2f, crossR * 0.6f),
        cornerRadius = CornerRadius(crossR * 0.2f),
    )

    val sparkleAlpha = sin(t * Math.PI.toFloat())
    drawSparkle(Offset(cx - d * 0.22f, rise + d * 0.05f), d * 0.028f, sparkleAlpha)
    drawSparkle(Offset(cx + d * 0.24f, rise - d * 0.03f), d * 0.032f, sparkleAlpha)
}

// Small hearts that pop when Kerker is poked.
private fun DrawScope.drawPokeHearts(t: Float) {
    val r = headR()
    val c = headC()
    val alpha = 1f - t
    for ((i, side) in listOf(-1f, 0f, 1f).withIndex()) {
        val hx = c.x + side * r * 0.7f
        val hy = c.y - r * (1.15f + 0.55f * t) - i % 2 * r * 0.12f
        drawHeart(Offset(hx, hy), r * 0.13f, BubblePink.copy(alpha = alpha))
    }
}

private fun DrawScope.drawHeart(center: Offset, r: Float, color: Color) {
    drawCircle(color, r * 0.55f, center + Offset(-r * 0.4f, -r * 0.25f))
    drawCircle(color, r * 0.55f, center + Offset(r * 0.4f, -r * 0.25f))
    val v = Path().apply {
        moveTo(center.x - r * 0.9f, center.y - r * 0.1f)
        lineTo(center.x, center.y + r)
        lineTo(center.x + r * 0.9f, center.y - r * 0.1f)
        close()
    }
    drawPath(v, color)
}

private fun DrawScope.drawSparkle(at: Offset, r: Float, alpha: Float) {
    val color = Color.White.copy(alpha = alpha.coerceIn(0f, 1f))
    val star = Path().apply {
        moveTo(at.x, at.y - r)
        quadraticTo(at.x + r * 0.18f, at.y - r * 0.18f, at.x + r, at.y)
        quadraticTo(at.x + r * 0.18f, at.y + r * 0.18f, at.x, at.y + r)
        quadraticTo(at.x - r * 0.18f, at.y + r * 0.18f, at.x - r, at.y)
        quadraticTo(at.x - r * 0.18f, at.y - r * 0.18f, at.x, at.y - r)
        close()
    }
    drawPath(star, color)
}

/**
 * The emotion layer, drawn over the finished character.
 *
 * Crying is deliberately a *cartoon* waah — two fat tears and a wobbly lip,
 * nothing that reads as real distress — and it disappears the instant the kid
 * meets the need, per the kids-ux rule that the pet may look grubby or sleepy
 * but must never appear to suffer.
 */
private fun DrawScope.drawEmotionOverlay(emotion: Emotion, bobT: Float, shake: Float) {
    val r = headR()
    val head = headC() + Offset(shake, 0f)

    when (emotion) {
        Emotion.CRYING -> {
            // Fat tears rolling down both cheeks, on a loop.
            for (side in listOf(-1f, 1f)) {
                for (i in 0 until 2) {
                    val phase = ((bobT * 1.6f) + i * 0.5f) % 1f
                    val x = head.x + side * r * 0.42f
                    val y = head.y - r * 0.02f + phase * r * 0.95f
                    val fade = (1f - phase).coerceIn(0f, 1f)
                    drawCircle(SkyBlue.copy(alpha = 0.85f * fade), r * 0.075f, Offset(x, y))
                    drawPath(
                        Path().apply {
                            moveTo(x, y - r * 0.13f)
                            lineTo(x - r * 0.07f, y + r * 0.02f)
                            lineTo(x + r * 0.07f, y + r * 0.02f)
                            close()
                        },
                        SkyBlue.copy(alpha = 0.85f * fade),
                    )
                }
            }
        }

        Emotion.EXCITED -> {
            // Sparkles bursting outward from the celebration.
            for (i in 0 until 7) {
                val angle = i / 7f * TWO_PI + bobT * TWO_PI * 0.5f
                val dist = r * (1.25f + sin(bobT * TWO_PI * 2f + i) * 0.14f)
                val at = head + Offset(cos(angle) * dist, sin(angle) * dist * 0.85f)
                drawCircle(SunYellow, r * 0.06f, at)
                drawCircle(Color.White.copy(alpha = 0.8f), r * 0.026f, at)
            }
        }

        Emotion.LAUGHING -> {
            // Laugh lines flicking off both cheeks.
            for (side in listOf(-1f, 1f)) {
                for (i in 0 until 3) {
                    val start = head + Offset(side * r * (0.95f + i * 0.1f), -r * 0.1f + i * r * 0.16f)
                    drawLine(
                        Ink.copy(alpha = 0.35f),
                        start,
                        start + Offset(side * r * 0.16f, -r * 0.05f),
                        strokeWidth = r * 0.028f,
                    )
                }
            }
        }

        Emotion.SLEEPY -> {
            // Zzz drifting up and fading out.
            for (i in 0 until 3) {
                val phase = ((bobT * 0.7f) + i / 3f) % 1f
                val at = head + Offset(r * (0.75f + phase * 0.5f), -r * (0.85f + phase * 1.1f))
                val zzz = r * (0.13f + i * 0.035f)
                val fade = (1f - phase).coerceIn(0f, 1f)
                val ink = Ink.copy(alpha = 0.55f * fade)
                val w = r * 0.02f
                drawLine(ink, at, at + Offset(zzz, 0f), strokeWidth = w)
                drawLine(ink, at + Offset(zzz, 0f), at + Offset(0f, zzz), strokeWidth = w)
                drawLine(ink, at + Offset(0f, zzz), at + Offset(zzz, zzz), strokeWidth = w)
            }
        }

        Emotion.BORED -> {
            // A slow sigh puffing out to one side. Charming, never nagging.
            val phase = (bobT * 0.9f) % 1f
            val at = mouthAnchor() + Offset(shake + r * (0.4f + phase * 0.55f), phase * r * 0.18f)
            drawCircle(Color.White.copy(alpha = 0.4f * (1f - phase)), r * 0.1f * (1f + phase), at)
        }

        Emotion.NEEDY, Emotion.HAPPY -> Unit
    }
}
