package com.example.jaskier.care

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.jaskier.ui.theme.WaterBlue
import kotlin.math.sin
import kotlinx.coroutines.delay

private const val TWO_PI = 2f * Math.PI.toFloat()

/** Which drink the kid picked. */
private enum class DrinkChoice { NONE, CUP, BOTTLE }

/** Cup route: fill it, then give it to Kerker. */
private enum class CupStep { FILL, DRINK, DONE }

/** Bottle route: the real steps a parent takes, in order. */
private enum class BottleStep { FILL, POWDER, CAP, SHAKE, DRINK, DONE }

private const val POWDER_SCOOPS = 3
private const val SHAKES_NEEDED = 6
private const val GULPS_NEEDED = 4

@Composable
fun DrinkScreen(
    viewModel: PetViewModel,
    tts: TtsManager,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var choice by remember { mutableStateOf(DrinkChoice.NONE) }

    when (choice) {
        DrinkChoice.NONE -> DrinkPicker(
            tts = tts,
            onPick = { choice = it },
            modifier = modifier,
        )
        DrinkChoice.CUP -> CupRoutine(viewModel, tts, onDone, modifier)
        DrinkChoice.BOTTLE -> BottleRoutine(viewModel, tts, onDone, modifier)
    }
}

// ---- picker ------------------------------------------------------------------

@Composable
private fun DrinkPicker(
    tts: TtsManager,
    onPick: (DrinkChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        tts.speak("What shall we drink? Tap the cup of water, or the baby bottle!")
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        // Two enormous halves: nothing to miss.
                        onPick(if (tap.x < size.width / 2f) DrinkChoice.CUP else DrinkChoice.BOTTLE)
                    }
                },
        ) {
            drawKitchen()
            val y = size.height * 0.52f
            drawCup(
                center = Offset(size.width * 0.27f, y),
                width = size.minDimension * 0.3f,
                fill = 1f,
                milky = false,
            )
            drawBottle(
                center = Offset(size.width * 0.73f, y),
                width = size.minDimension * 0.26f,
                fill = 1f,
                milky = true,
                capped = true,
                tilt = 0f,
            )
        }

        Text(
            text = "Water          Bottle",
            style = MaterialTheme.typography.titleLarge,
            color = InkText,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
        )
    }
}

// ---- cup of water ------------------------------------------------------------

@Composable
private fun CupRoutine(
    viewModel: PetViewModel,
    tts: TtsManager,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(CupStep.FILL) }
    var fill by remember { mutableFloatStateOf(0f) }
    var gulps by remember { mutableIntStateOf(0) }
    var cupPos by remember { mutableStateOf<Offset?>(null) }

    val hint = when (step) {
        CupStep.FILL -> "Tap the tap to fill the cup!"
        CupStep.DRINK -> "Now tap Kerker's mouth to give him a drink!"
        CupStep.DONE -> "Glug glug! Thank you! All better!"
    }
    LaunchedEffect(step) {
        tts.speak(hint, if (step == CupStep.DONE) VoiceTone.EXCITED else VoiceTone.NORMAL)
    }

    LaunchedEffect(step) {
        if (step == CupStep.DONE) {
            viewModel.drink()
            delay(1600)
            onDone()
        }
    }

    RoutineScaffold(
        hint = hint,
        starsFilled = when (step) {
            CupStep.FILL -> 0
            CupStep.DRINK -> 1
            CupStep.DONE -> 2
        },
        starsTotal = 2,
        onTap = { tap, canvas ->
            when (step) {
                CupStep.FILL -> if (isNearTap(tap, canvas)) {
                    fill = (fill + 0.34f).coerceAtMost(1f)
                    if (fill >= 1f) step = CupStep.DRINK
                }
                CupStep.DRINK -> if (isNearMouth(tap, canvas)) {
                    gulps++
                    if (gulps >= GULPS_NEEDED) step = CupStep.DONE
                }
                CupStep.DONE -> Unit
            }
        },
        onDrag = { at, canvas ->
            // Dragging the cup works too, for kids who prefer it.
            if (step == CupStep.DRINK) {
                cupPos = at
                if (isNearMouth(at, canvas)) {
                    gulps++
                    if (gulps >= GULPS_NEEDED) step = CupStep.DONE
                }
            }
        },
        modifier = modifier,
    ) { pulse ->
        drawKitchen()
        drawThirstyKerker(
            drinking = step == CupStep.DRINK || step == CupStep.DONE,
            happy = step == CupStep.DONE,
        )
        drawTap(pouring = step == CupStep.FILL && fill > 0f, pulse = if (step == CupStep.FILL) pulse else 1f)

        val remaining = 1f - gulps / GULPS_NEEDED.toFloat()
        val home = Offset(size.width * 0.22f, size.height * 0.55f)
        drawCup(
            center = cupPos ?: home,
            width = size.minDimension * 0.2f,
            fill = if (step == CupStep.FILL) fill else remaining.coerceAtLeast(0f),
            milky = false,
        )
    }
}

// ---- baby bottle -------------------------------------------------------------

@Composable
private fun BottleRoutine(
    viewModel: PetViewModel,
    tts: TtsManager,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(BottleStep.FILL) }
    var fill by remember { mutableFloatStateOf(0f) }
    var scoops by remember { mutableIntStateOf(0) }
    var shakes by remember { mutableIntStateOf(0) }
    var gulps by remember { mutableIntStateOf(0) }

    val shakeWobble = remember { Animatable(0f) }

    val hint = when (step) {
        BottleStep.FILL -> "Tap the tap to fill the bottle with water!"
        BottleStep.POWDER -> "Now tap the milk tin to add three scoops!"
        BottleStep.CAP -> "Tap the cap to close the bottle!"
        BottleStep.SHAKE -> "Shake it! Tap the bottle again and again!"
        BottleStep.DRINK -> "The milk is ready! Tap Kerker's mouth to feed him!"
        BottleStep.DONE -> "Glug glug glug... burp! Thank you!"
    }
    LaunchedEffect(step) {
        tts.speak(hint, if (step == BottleStep.DONE) VoiceTone.EXCITED else VoiceTone.NORMAL)
    }

    LaunchedEffect(step) {
        if (step == BottleStep.DONE) {
            viewModel.bottleFeed()
            delay(1800)
            onDone()
        }
    }

    // Milk turns creamy as it is shaken.
    val milkiness = (shakes / SHAKES_NEEDED.toFloat()).coerceIn(0f, 1f)

    RoutineScaffold(
        hint = hint,
        starsFilled = when (step) {
            BottleStep.FILL -> 0
            BottleStep.POWDER -> 1
            BottleStep.CAP -> 2
            BottleStep.SHAKE -> 3
            BottleStep.DRINK -> 4
            BottleStep.DONE -> 5
        },
        starsTotal = 5,
        onTap = { tap, canvas ->
            when (step) {
                BottleStep.FILL -> if (isNearTap(tap, canvas)) {
                    fill = (fill + 0.34f).coerceAtMost(1f)
                    if (fill >= 1f) step = BottleStep.POWDER
                }
                BottleStep.POWDER -> if (isNearTin(tap, canvas)) {
                    scoops++
                    if (scoops >= POWDER_SCOOPS) step = BottleStep.CAP
                }
                BottleStep.CAP -> if (isNearBottle(tap, canvas)) step = BottleStep.SHAKE
                BottleStep.SHAKE -> if (isNearBottle(tap, canvas)) {
                    shakes++
                    if (shakes >= SHAKES_NEEDED) step = BottleStep.DRINK
                }
                BottleStep.DRINK -> if (isNearMouth(tap, canvas)) {
                    gulps++
                    if (gulps >= GULPS_NEEDED) step = BottleStep.DONE
                }
                BottleStep.DONE -> Unit
            }
        },
        onDrag = { at, canvas ->
            // Dragging the bottle to his mouth also feeds him.
            if (step == BottleStep.DRINK && isNearMouth(at, canvas)) {
                gulps++
                if (gulps >= GULPS_NEEDED) step = BottleStep.DONE
            }
        },
        modifier = modifier,
    ) { pulse ->
        drawKitchen()
        drawThirstyKerker(
            drinking = step == BottleStep.DRINK || step == BottleStep.DONE,
            happy = step == BottleStep.DONE,
        )
        drawTap(
            pouring = step == BottleStep.FILL && fill > 0f,
            pulse = if (step == BottleStep.FILL) pulse else 1f,
        )
        drawMilkTin(
            scoops = scoops,
            pulse = if (step == BottleStep.POWDER) pulse else 1f,
        )

        val wobble = if (step == BottleStep.SHAKE) sin(shakes * 2.1f) * 12f else 0f
        val remaining = 1f - gulps / GULPS_NEEDED.toFloat()
        drawBottle(
            center = bottleHome(),
            width = size.minDimension * 0.2f,
            fill = when (step) {
                BottleStep.FILL -> fill
                BottleStep.DRINK, BottleStep.DONE -> remaining.coerceAtLeast(0f)
                else -> 1f
            },
            milky = milkiness > 0f,
            milkiness = milkiness,
            capped = step >= BottleStep.CAP,
            tilt = wobble + shakeWobble.value,
            pulse = if (step == BottleStep.CAP || step == BottleStep.SHAKE) pulse else 1f,
        )
    }
}

// ---- shared routine chrome ---------------------------------------------------

/**
 * Every drink routine shares the same shell: a spoken hint, a picture progress
 * row, a back-safe tap surface where **no tap is ever silent**, and a gentle
 * pulse that draws the eye to whatever is next.
 */
@Composable
private fun RoutineScaffold(
    hint: String,
    starsFilled: Int,
    starsTotal: Int,
    onTap: (Offset, Size) -> Unit,
    onDrag: (Offset, Size) -> Unit,
    modifier: Modifier = Modifier,
    content: DrawScope.(pulse: Float) -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "drinkPulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            androidx.compose.animation.core.tween(700, easing = LinearEasing),
            RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    // Sparkles for taps that hit nothing, so the app never feels broken.
    var sparkleAt by remember { mutableStateOf<Offset?>(null) }
    val sparkle = remember { Animatable(0f) }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onTap) {
                    detectTapGestures { tap ->
                        sparkleAt = tap
                        onTap(tap, size.toSize())
                    }
                }
                .pointerInput(onDrag) {
                    detectDragGestures { change, _ ->
                        onDrag(change.position, size.toSize())
                    }
                },
        ) {
            content(pulse)
            sparkleAt?.let { at -> drawTapSparkle(at, sparkle.value) }
            drawProgressStars(starsFilled, starsTotal)
        }

        LaunchedEffect(sparkleAt) {
            if (sparkleAt != null) {
                sparkle.snapTo(0f)
                sparkle.animateTo(1f, androidx.compose.animation.core.tween(450))
            }
        }

        Text(
            text = hint,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = InkText,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 22.dp),
        )
    }
}

private fun androidx.compose.ui.unit.IntSize.toSize() = Size(width.toFloat(), height.toFloat())

// ---- hit zones ---------------------------------------------------------------
//
// All generous: toddler fingers are not precise, and no step may need accuracy.

private fun isNearTap(at: Offset, canvas: Size) =
    (at - Offset(canvas.width * 0.5f, canvas.height * 0.17f)).getDistance() < canvas.minDimension * 0.28f

private fun isNearTin(at: Offset, canvas: Size) =
    (at - Offset(canvas.width * 0.82f, canvas.height * 0.62f)).getDistance() < canvas.minDimension * 0.26f

private fun DrawScope.bottleHome() = Offset(size.width * 0.22f, size.height * 0.58f)

private fun isNearBottle(at: Offset, canvas: Size) =
    (at - Offset(canvas.width * 0.22f, canvas.height * 0.58f)).getDistance() < canvas.minDimension * 0.28f

private fun isNearMouth(at: Offset, canvas: Size) =
    (at - Offset(canvas.width * 0.5f, canvas.height * 0.46f)).getDistance() < canvas.minDimension * 0.28f

// ---- drawing -----------------------------------------------------------------

private fun DrawScope.drawKitchen() {
    drawRect(Brush.verticalGradient(listOf(Color(0xFFFFF3D9), Color(0xFFFFE7C2))))
    // counter
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFFD9A46B), Color(0xFFB57C42)), startY = size.height * 0.78f),
        topLeft = Offset(0f, size.height * 0.78f),
        size = Size(size.width, size.height * 0.22f),
    )
}

private fun DrawScope.drawTap(pouring: Boolean, pulse: Float) {
    val d = size.minDimension
    val base = Offset(size.width * 0.5f, size.height * 0.12f)
    val w = d * 0.06f * pulse
    drawShadedRoundRect(
        topLeft = Offset(base.x - w / 2f, base.y - d * 0.08f),
        size = Size(w, d * 0.16f),
        cornerRadius = w * 0.4f,
        base = Color(0xFFB0BEC5),
    )
    drawShadedRoundRect(
        topLeft = Offset(base.x - w * 1.6f, base.y + d * 0.05f),
        size = Size(w * 2.2f, w * 0.9f),
        cornerRadius = w * 0.45f,
        base = Color(0xFF90A4AE),
    )
    if (pouring) {
        drawRect(
            WaterBlue.copy(alpha = 0.6f),
            topLeft = Offset(base.x - w * 0.25f, base.y + d * 0.08f),
            size = Size(w * 0.5f, size.height * 0.3f),
        )
    }
}

private fun DrawScope.drawMilkTin(scoops: Int, pulse: Float) {
    val d = size.minDimension
    val c = Offset(size.width * 0.82f, size.height * 0.62f)
    val w = d * 0.17f * pulse
    val h = w * 1.25f
    drawContactShadow(Offset(c.x, c.y + h * 0.55f), w * 0.9f, h * 0.14f)
    drawShadedRoundRect(
        topLeft = Offset(c.x - w / 2f, c.y - h / 2f),
        size = Size(w, h),
        cornerRadius = w * 0.16f,
        base = Color(0xFFFFF6E5),
    )
    drawShadedRoundRect(
        topLeft = Offset(c.x - w / 2f, c.y - h / 2f),
        size = Size(w, h * 0.24f),
        cornerRadius = w * 0.16f,
        base = Color(0xFF7EC8E3),
    )
    // one dot per scoop already added, so the kid can count them
    for (i in 0 until POWDER_SCOOPS) {
        val filled = i < scoops
        drawCircle(
            if (filled) Color(0xFF43A047) else Color(0x33000000),
            w * 0.07f,
            Offset(c.x - w * 0.24f + i * w * 0.24f, c.y + h * 0.24f),
        )
    }
}

private fun DrawScope.drawCup(center: Offset, width: Float, fill: Float, milky: Boolean) {
    val h = width * 1.15f
    drawContactShadow(Offset(center.x, center.y + h * 0.55f), width * 0.85f, h * 0.12f)
    // glass
    drawShadedRoundRect(
        topLeft = Offset(center.x - width / 2f, center.y - h / 2f),
        size = Size(width, h),
        cornerRadius = width * 0.14f,
        base = Color(0xFFE8F4F8),
    )
    if (fill > 0f) {
        val liquidH = h * 0.86f * fill
        drawRoundRect(
            if (milky) Color(0xFFFFFDF7) else WaterBlue.copy(alpha = 0.85f),
            topLeft = Offset(center.x - width * 0.42f, center.y + h * 0.43f - liquidH),
            size = Size(width * 0.84f, liquidH),
            cornerRadius = CornerRadius(width * 0.1f),
        )
    }
}

private fun DrawScope.drawBottle(
    center: Offset,
    width: Float,
    fill: Float,
    milky: Boolean,
    capped: Boolean,
    tilt: Float,
    milkiness: Float = 1f,
    pulse: Float = 1f,
) {
    val w = width * pulse
    val h = w * 1.9f
    drawContactShadow(Offset(center.x, center.y + h * 0.55f), w * 0.8f, h * 0.09f)
    rotate(degrees = tilt, pivot = center) {
        drawShadedRoundRect(
            topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
            size = Size(w, h),
            cornerRadius = w * 0.34f,
            base = Color(0xFFEFF7FA),
        )
        if (fill > 0f) {
            val liquidH = h * 0.72f * fill
            drawRoundRect(
                if (milky) lerp(WaterBlue.copy(alpha = 0.8f), Color(0xFFFFFBF2), milkiness)
                else WaterBlue.copy(alpha = 0.8f),
                topLeft = Offset(center.x - w * 0.38f, center.y + h * 0.36f - liquidH),
                size = Size(w * 0.76f, liquidH),
                cornerRadius = CornerRadius(w * 0.24f),
            )
        }
        // teat and collar
        val teatBase = center.y - h / 2f
        drawShadedRoundRect(
            topLeft = Offset(center.x - w * 0.34f, teatBase - w * 0.22f),
            size = Size(w * 0.68f, w * 0.26f),
            cornerRadius = w * 0.1f,
            base = if (capped) Color(0xFF66BB6A) else Color(0xFFCFD8DC),
        )
        if (capped) {
            drawShadedRoundRect(
                topLeft = Offset(center.x - w * 0.16f, teatBase - w * 0.62f),
                size = Size(w * 0.32f, w * 0.44f),
                cornerRadius = w * 0.16f,
                base = Color(0xFFFFCC80),
            )
        }
    }
}

/** Kerker, waiting for his drink. Mouth opens while he is being fed. */
private fun DrawScope.drawThirstyKerker(drinking: Boolean, happy: Boolean) {
    val d = size.minDimension
    val c = Offset(size.width * 0.5f, size.height * 0.42f)
    val r = d * 0.16f
    val skin = Color(0xFFF6CBA4)

    drawContactShadow(Offset(c.x, c.y + r * 1.5f), r * 1.1f, r * 0.2f)
    // curls first so the head overlaps them
    for (angle in listOf(-150f, -120f, -90f, -60f, -30f)) {
        val a = angle * TWO_PI / 360f
        drawCurl(c + Offset(kotlin.math.cos(a), sin(a)) * r * 0.98f, r * 0.26f)
    }
    drawBall(c, r, skin)
    drawSoftBlush(c + Offset(-r * 0.6f, r * 0.28f), r * 0.42f, r * 0.26f)
    drawSoftBlush(c + Offset(r * 0.6f, r * 0.28f), r * 0.42f, r * 0.26f)

    for (side in listOf(-1f, 1f)) {
        val eye = c + Offset(side * r * 0.36f, -r * 0.12f)
        drawEyeBall(eye, r * 0.19f)
        drawCircle(Color(0xFF3A2313), r * 0.1f, eye + Offset(0f, r * 0.01f))
        drawCircle(Color.White, r * 0.035f, eye + Offset(-r * 0.04f, -r * 0.05f))
    }

    val mouth = c + Offset(0f, r * 0.45f)
    if (drinking) {
        drawOval(
            Color(0xFF7A3B32),
            topLeft = Offset(mouth.x - r * 0.16f, mouth.y - r * 0.12f),
            size = Size(r * 0.32f, r * 0.3f),
        )
    } else {
        drawArc(
            Color(0xFF7A3B32),
            startAngle = if (happy) 0f else 20f,
            sweepAngle = if (happy) 180f else 140f,
            useCenter = false,
            topLeft = Offset(mouth.x - r * 0.22f, mouth.y - r * 0.2f),
            size = Size(r * 0.44f, r * 0.34f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.06f),
        )
    }
}

/** Progress as a picture, never a number: a filling row of stars. */
private fun DrawScope.drawProgressStars(filled: Int, total: Int) {
    val d = size.minDimension
    val r = d * 0.028f
    val gap = r * 2.6f
    val startX = size.width / 2f - (total - 1) * gap / 2f
    for (i in 0 until total) {
        val at = Offset(startX + i * gap, size.height * 0.06f)
        drawCircle(
            if (i < filled) Color(0xFFFFD54F) else Color(0x33000000),
            r,
            at,
        )
    }
}

/** Every tap does something, even one that hits nothing. */
private fun DrawScope.drawTapSparkle(at: Offset, t: Float) {
    if (t <= 0f || t >= 1f) return
    val d = size.minDimension
    for (i in 0 until 5) {
        val a = i / 5f * TWO_PI
        val dist = d * 0.05f * t
        drawCircle(
            Color.White.copy(alpha = 1f - t),
            d * 0.012f * (1f - t),
            at + Offset(kotlin.math.cos(a) * dist, sin(a) * dist),
        )
    }
}

/** Back button shared with the other care routines. */
@Composable
fun DrinkBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val backLabel = stringResource(R.string.back_button)
    Box(
        modifier = modifier
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
