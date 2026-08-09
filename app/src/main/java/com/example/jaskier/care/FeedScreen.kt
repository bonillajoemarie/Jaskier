package com.example.jaskier.care

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.jaskier.pet.drawContactShadow
import com.example.jaskier.pet.drawCurl
import com.example.jaskier.pet.drawEyeBall
import com.example.jaskier.pet.drawShadedRoundRect
import com.example.jaskier.pet.drawSoftBlush
import com.example.jaskier.speech.TtsManager
import com.example.jaskier.ui.theme.InkText
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

private const val TWO_PI = 2f * Math.PI.toFloat()

private enum class FeedStep { CHOOSE, EAT, DONE }

private data class FoodChoice(val id: String, val name: String, val color: Color, val darker: Color)

private val FoodChoices = listOf(
    FoodChoice("apple", "Apple", Color(0xFFFF6B6B), Color(0xFFE8323F)),
    FoodChoice("banana", "Banana", Color(0xFFFBE47A), Color(0xFFF2C230)),
    FoodChoice("broccoli", "Broccoli", Color(0xFF6FCF97), Color(0xFF41A66B)),
    FoodChoice("berry", "Berry", Color(0xFF9B8CDB), Color(0xFF7461C4)),
)

private class PlateFood(val choice: FoodChoice, var bites: Int = 2)

@Composable
fun FeedScreen(
    viewModel: PetViewModel,
    tts: TtsManager,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(FeedStep.CHOOSE) }
    val plate = remember { mutableStateListOf<PlateFood>() }
    var spoonPos by remember { mutableStateOf<Offset?>(null) }
    var spoonHeld by remember { mutableStateOf(false) }
    var spoonLoad by remember { mutableStateOf<FoodChoice?>(null) }
    var mouthOpen by remember { mutableStateOf(false) }

    val hint = when (step) {
        FeedStep.CHOOSE -> "Pick some yummy food for the plate!"
        FeedStep.EAT -> "Scoop with the spoon and feed Kerker!"
        FeedStep.DONE -> "Mmm! All eaten, thank you!"
    }
    LaunchedEffect(step) { tts.speak(hint) }

    LaunchedEffect(step) {
        if (step == FeedStep.DONE) {
            delay(2600)
            onDone()
        }
    }

    val idle = rememberInfiniteTransition(label = "feedIdle")
    val bob by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Restart),
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
                        if (step == FeedStep.CHOOSE) {
                            FoodChoices.forEachIndexed { index, choice ->
                                if ((tap - shelfSlot(index)).getDistance() < minDim() * 0.12f && plate.size < 3) {
                                    plate += PlateFood(choice)
                                    tts.speak(choice.name)
                                    if (plate.size >= 3) step = FeedStep.EAT
                                }
                            }
                            // Tapping the plate starts eating once anything is on it.
                            if (plate.isNotEmpty() && (tap - plateCenter()).getDistance() < minDim() * 0.2f) {
                                step = FeedStep.EAT
                            }
                        }
                    }
                }
                .pointerInput(step) {
                    detectDragGestures(
                        onDragStart = { start ->
                            // Any drag during mealtime holds the spoon — little
                            // fingers aren't precise.
                            if (step == FeedStep.EAT) {
                                spoonHeld = true
                                spoonPos = start
                            }
                        },
                        onDrag = { change, _ ->
                            if (!spoonHeld) return@detectDragGestures
                            spoonPos = change.position
                            // Scoop a bite from the plate.
                            if (spoonLoad == null && plate.isNotEmpty() &&
                                (change.position - plateCenter()).getDistance() < minDim() * 0.18f
                            ) {
                                val food = plate.first()
                                spoonLoad = food.choice
                                food.bites -= 1
                                if (food.bites <= 0) plate.removeAt(0)
                            }
                            // Bring the loaded spoon to Kerker's mouth.
                            val nearMouth = (change.position - mouthCenter()).getDistance() < minDim() * 0.15f
                            mouthOpen = spoonLoad != null && nearMouth
                            if (spoonLoad != null && nearMouth) {
                                viewModel.feed()
                                tts.speak(if (plate.isEmpty()) "Yum yum!" else "Mmm, yummy!")
                                spoonLoad = null
                                mouthOpen = false
                                if (plate.isEmpty()) {
                                    step = FeedStep.DONE
                                    spoonHeld = false
                                    spoonPos = null
                                }
                            }
                        },
                        onDragEnd = {
                            spoonHeld = false
                            mouthOpen = false
                        },
                        onDragCancel = {
                            spoonHeld = false
                            mouthOpen = false
                        },
                    )
                },
        ) {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFFFE9C7), Color(0xFFFFF8E7))))
            drawTable()
            drawEatingKerker(bob, mouthOpen = mouthOpen || step == FeedStep.DONE, happy = step == FeedStep.DONE)
            drawPlate(plate.toList())
            if (step == FeedStep.CHOOSE) drawShelf(pulse)
            if (step == FeedStep.EAT) drawSpoon(spoonPos ?: spoonRestDraw(), spoonLoad, pulse = if (spoonPos == null) pulse else 1f)
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

// ---- geometry ------------------------------------------------------------------

private fun PointerInputScope.minDim(): Float = kotlin.math.min(size.width, size.height).toFloat()

private fun PointerInputScope.shelfSlot(i: Int) = shelfSlotOf(i, size.width.toFloat(), size.height.toFloat())
private fun DrawScope.shelfSlotDraw(i: Int) = shelfSlotOf(i, size.width, size.height)
private fun shelfSlotOf(i: Int, w: Float, h: Float) =
    Offset(w * (0.14f + 0.24f * i), h * 0.85f)

private fun PointerInputScope.plateCenter() = Offset(size.width * 0.5f, size.height * 0.68f)
private fun DrawScope.plateCenterDraw() = Offset(size.width * 0.5f, size.height * 0.68f)

private fun PointerInputScope.spoonRest() = Offset(size.width * 0.82f, size.height * 0.68f)
private fun DrawScope.spoonRestDraw() = Offset(size.width * 0.82f, size.height * 0.68f)

private fun PointerInputScope.mouthCenter() = Offset(size.width * 0.5f, size.height * 0.37f)
private fun DrawScope.mouthCenterDraw() = Offset(size.width * 0.5f, size.height * 0.37f)

// ---- drawing --------------------------------------------------------------------

private fun DrawScope.drawTable() {
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFFDFA968), Color(0xFFB8813F)), startY = size.height * 0.58f),
        topLeft = Offset(0f, size.height * 0.58f),
        size = Size(size.width, size.height * 0.42f),
    )
    // table edge catches the light
    drawRect(
        Brush.verticalGradient(
            listOf(Color(0xFFF0C083), Color.Transparent),
            startY = size.height * 0.58f,
            endY = size.height * 0.6f,
        ),
        topLeft = Offset(0f, size.height * 0.58f),
        size = Size(size.width, size.height * 0.02f),
    )
    // wood grain
    for (i in 0 until 4) {
        val y = size.height * (0.66f + i * 0.08f)
        drawLine(
            Color(0xFF9A6A30).copy(alpha = 0.35f),
            Offset(size.width * (0.05f + (i % 2) * 0.1f), y),
            Offset(size.width * (0.85f + (i % 2) * 0.1f), y),
            strokeWidth = size.minDimension * 0.005f,
        )
    }
    // placemat under the plate
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFFFF8B9E), Color(0xFFE86A80)), startY = size.height * 0.6f),
        topLeft = Offset(size.width * 0.2f, size.height * 0.615f),
        size = Size(size.width * 0.6f, size.height * 0.14f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.05f),
    )
    // plate's soft shadow on the mat
    drawOval(
        Color(0xFF5B3A1A).copy(alpha = 0.2f),
        topLeft = Offset(size.width * 0.27f, size.height * 0.665f),
        size = Size(size.width * 0.46f, size.height * 0.055f),
    )
}

private fun DrawScope.drawEatingKerker(bob: Float, mouthOpen: Boolean, happy: Boolean) {
    val d = size.minDimension
    val cx = size.width / 2f
    val r = size.width * 0.24f
    val bobOffset = sin(bob * TWO_PI) * r * 0.03f
    val skin = Color(0xFFF6CBA4)
    // Head positioned so the mouth lands exactly on the spoon target.
    val head = Offset(cx, mouthCenterDraw().y - r * 0.48f + bobOffset)
    val skinBrush = Brush.radialGradient(
        listOf(lerp(skin, Color.White, 0.42f), skin, lerp(skin, Color(0xFF4A2F1E), 0.16f)),
        center = head - Offset(r * 0.35f, r * 0.45f),
        radius = r * 2.2f,
    )

    // vest shoulders peeking above the table, shaded like fabric
    drawShadedRoundRect(
        topLeft = Offset(cx - r * 0.85f, head.y + r * 0.8f),
        size = Size(r * 1.7f, size.height * 0.58f - (head.y + r * 0.8f) + 20f),
        cornerRadius = r * 0.35f,
        base = Color(0xFFEFEBE2),
    )
    // stubby arms resting beside the plate
    for (side in listOf(-1f, 1f)) {
        drawBall(Offset(cx + side * r * 1.02f, head.y + r * 1.15f), r * 0.2f, skin, specular = false)
    }
    // big round head, sphere-shaded and grounded on the shirt
    drawContactShadow(head + Offset(0f, r * 1.1f), r * 1.4f, r * 0.45f, alpha = 0.2f)
    drawBall(head, r, skin)

    // curly mop + wisps
    for (angleDeg in listOf(-152f, -124f, -96f, -68f, -40f, -12f)) {
        val angle = Math.toRadians(angleDeg.toDouble()).toFloat()
        drawCurl(head + Offset(cos(angle) * r * 0.82f, sin(angle) * r * 0.82f), r * 0.4f)
    }
    for ((i, wx) in listOf(-0.62f, 0f, 0.62f).withIndex()) {
        val dir = if (i % 2 == 0) 1f else -1f
        val wisp = head + Offset(r * wx, -r * (if (i == 1) 1.18f else 1.0f))
        val curl = Path().apply {
            moveTo(wisp.x, wisp.y + r * 0.1f)
            cubicTo(
                wisp.x + dir * r * 0.07f, wisp.y - r * 0.03f,
                wisp.x - dir * r * 0.08f, wisp.y - r * 0.12f,
                wisp.x + dir * r * 0.07f, wisp.y - r * 0.18f,
            )
        }
        drawPath(curl, Color(0xFF32241B), style = Stroke(width = r * 0.045f))
    }

    // huge sparkly eyes (watching the spoon)
    val eyeY = head.y - r * 0.08f
    for (side in listOf(-1f, 1f)) {
        val eye = Offset(cx + side * r * 0.42f, eyeY)
        val eyeR = r * 0.28f
        drawEyeBall(eye, eyeR)
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFF54331B), Color(0xFF241206)),
                center = eye - Offset(0f, eyeR * 0.1f),
                radius = eyeR * 0.85f,
            ),
            radius = eyeR * 0.7f,
            center = eye + Offset(0f, eyeR * 0.08f),
        )
        drawCircle(Color.White, eyeR * 0.24f, eye + Offset(eyeR * 0.2f, -eyeR * 0.18f))
        drawCircle(Color.White.copy(alpha = 0.8f), eyeR * 0.11f, eye + Offset(-eyeR * 0.22f, eyeR * 0.26f))
    }
    // blush
    for (side in listOf(-1f, 1f)) {
        drawSoftBlush(head + Offset(side * r * 0.62f, r * 0.33f), r * 0.44f, r * 0.26f)
    }
    // mouth: opens wide when the spoon comes close
    val mouth = mouthCenterDraw()
    if (mouthOpen) {
        drawOval(
            Color(0xFF551F1F),
            topLeft = Offset(mouth.x - d * 0.08f, mouth.y - d * 0.055f),
            size = Size(d * 0.16f, d * 0.11f),
        )
        drawOval(
            Color(0xFFE86A80),
            topLeft = Offset(mouth.x - d * 0.05f, mouth.y + d * 0.015f),
            size = Size(d * 0.1f, d * 0.035f),
        )
    } else if (happy) {
        val smile = Path().apply {
            moveTo(mouth.x - d * 0.09f, mouth.y - d * 0.01f)
            quadraticTo(mouth.x, mouth.y + d * 0.07f, mouth.x + d * 0.09f, mouth.y - d * 0.01f)
            close()
        }
        drawPath(smile, Color(0xFF2C2C2C))
    } else {
        val smile = Path().apply {
            moveTo(mouth.x - d * 0.06f, mouth.y)
            quadraticTo(mouth.x, mouth.y + d * 0.04f, mouth.x + d * 0.06f, mouth.y)
        }
        drawPath(smile, Color(0xFF2C2C2C), style = Stroke(width = d * 0.014f))
    }
}

private fun DrawScope.drawPlate(plate: List<PlateFood>) {
    val c = plateCenterDraw()
    val d = size.minDimension
    drawOval(
        Brush.radialGradient(listOf(Color.White, Color(0xFFD8DEE2)), center = c - Offset(0f, d * 0.02f), radius = d * 0.3f),
        topLeft = Offset(c.x - d * 0.24f, c.y - d * 0.085f),
        size = Size(d * 0.48f, d * 0.17f),
    )
    drawOval(
        Color(0xFFEDF1F3),
        topLeft = Offset(c.x - d * 0.17f, c.y - d * 0.06f),
        size = Size(d * 0.34f, d * 0.12f),
    )
    // food on the plate, scaled by how many bites remain
    plate.forEachIndexed { index, food ->
        val at = c + Offset((index - (plate.size - 1) / 2f) * d * 0.1f, -d * 0.02f)
        val scale = 0.5f + 0.5f * (food.bites / 2f)
        drawFoodBlob(food.choice, at, d * 0.055f * scale)
    }
}

private fun DrawScope.drawShelf(pulse: Float) {
    val d = size.minDimension
    // shelf board
    drawRoundRect(
        Color(0xFFA9763B),
        topLeft = Offset(size.width * 0.04f, size.height * 0.85f + d * 0.075f),
        size = Size(size.width * 0.92f, d * 0.03f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(d * 0.015f),
    )
    FoodChoices.forEachIndexed { index, choice ->
        drawFoodBlob(choice, shelfSlotDraw(index), d * 0.07f * pulse)
    }
}

private fun DrawScope.drawFoodBlob(choice: FoodChoice, at: Offset, r: Float) {
    when (choice.id) {
        "banana" -> {
            rotate(degrees = -30f, pivot = at) {
                drawOval(
                    Brush.verticalGradient(listOf(choice.color, choice.darker)),
                    topLeft = Offset(at.x - r * 1.4f, at.y - r * 0.55f),
                    size = Size(r * 2.8f, r * 1.1f),
                )
            }
        }
        "broccoli" -> {
            drawRoundRect(
                Color(0xFF9BC98F),
                topLeft = Offset(at.x - r * 0.2f, at.y),
                size = Size(r * 0.4f, r * 1.0f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.2f),
            )
            for ((dx, dy) in listOf(-0.55f to -0.1f, 0f to -0.45f, 0.55f to -0.1f)) {
                drawCircle(
                    Brush.radialGradient(listOf(choice.color, choice.darker), center = at + Offset(dx * r - r * 0.2f, dy * r - r * 0.2f), radius = r),
                    r * 0.55f,
                    at + Offset(dx * r, dy * r),
                )
            }
        }
        else -> {
            drawCircle(
                Brush.radialGradient(
                    listOf(lerp(choice.color, Color.White, 0.3f), choice.color, choice.darker),
                    center = at - Offset(r * 0.35f, r * 0.4f),
                    radius = r * 1.9f,
                ),
                r,
                at,
            )
            if (choice.id == "apple") {
                drawLine(Color(0xFF6D4C41), at + Offset(0f, -r), at + Offset(0f, -r * 1.4f), strokeWidth = r * 0.16f)
            }
        }
    }
}

private fun DrawScope.drawSpoon(at: Offset, load: FoodChoice?, pulse: Float) {
    val d = size.minDimension
    val len = d * 0.26f * pulse
    rotate(degrees = -50f, pivot = at) {
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFFE6EEF2), Color(0xFFAAB9C0))),
            topLeft = Offset(at.x, at.y - d * 0.016f),
            size = Size(len, d * 0.032f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(d * 0.016f),
        )
        drawOval(
            Brush.radialGradient(listOf(Color(0xFFF2F7F9), Color(0xFFB9C6CC)), center = at - Offset(d * 0.02f, d * 0.02f), radius = d * 0.09f),
            topLeft = Offset(at.x - d * 0.11f, at.y - d * 0.045f),
            size = Size(d * 0.12f, d * 0.09f),
        )
        if (load != null) {
            drawOval(
                Brush.radialGradient(listOf(lerp(load.color, Color.White, 0.2f), load.darker), center = at - Offset(d * 0.06f, d * 0.03f), radius = d * 0.07f),
                topLeft = Offset(at.x - d * 0.095f, at.y - d * 0.05f),
                size = Size(d * 0.09f, d * 0.06f),
            )
        }
    }
}
