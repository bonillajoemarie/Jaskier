package com.example.jaskier.minigames

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaskier.R
import com.example.jaskier.songs.AnimalSoundPlayer
import com.example.jaskier.songs.AnimalSounds
import com.example.jaskier.speech.TtsManager
import com.example.jaskier.ui.theme.InkText
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch

private data class Fruit(val id: String, val name: String, val juice: Color)

private val Fruits = listOf(
    Fruit("apple", "Apple", Color(0xFFE8323F)),
    Fruit("banana", "Banana", Color(0xFFF7D154)),
    Fruit("orange", "Orange", Color(0xFFFF9F1C)),
    Fruit("strawberry", "Strawberry", Color(0xFFE84667)),
    Fruit("watermelon", "Watermelon", Color(0xFFFF5D6C)),
    Fruit("grapes", "Grapes", Color(0xFF8E5FBF)),
)

private sealed interface ZooTile {
    data class Animal(val id: String, val name: String) : ZooTile
    data class FruitTile(val fruit: Fruit) : ZooTile
}

@Composable
fun ZooScreen(
    animalPlayer: AnimalSoundPlayer,
    tts: TtsManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Fresh random arrangement of animals and fruits every visit.
    val tiles = remember {
        (AnimalSounds.map { ZooTile.Animal(it.id, it.name) } + Fruits.map { ZooTile.FruitTile(it) })
            .shuffled()
    }

    // Spoken how-to for pre-readers.
    LaunchedEffect(Unit) { tts.speak(ZooGame.intro) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val backLabel = stringResource(R.string.back_button)
            Box(
                modifier = Modifier
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
            Text(
                text = "Zoo 🐮🍎",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp),
            )
        }

        LazyVerticalGrid(
            // Adaptive cells: 2 columns on phones, more on tablets.
            columns = GridCells.Adaptive(minSize = 170.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(tiles, key = { tile -> if (tile is ZooTile.Animal) tile.id else (tile as ZooTile.FruitTile).fruit.id }) { tile ->
                when (tile) {
                    is ZooTile.Animal -> AnimalTile(tile) { animalPlayer.play(tile.id) }
                    is ZooTile.FruitTile -> FruitTile(tile.fruit) { tts.speak(tile.fruit.name) }
                }
            }
        }
    }
}

@Composable
private fun AnimalTile(animal: ZooTile.Animal, onTapped: () -> Unit) {
    val wiggle = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    TileCard(
        label = animal.name,
        modifier = Modifier.rotate(wiggle.value),
        onClick = {
            onTapped()
            scope.launch {
                wiggle.animateTo(
                    0f,
                    keyframes {
                        durationMillis = 400
                        0f at 0
                        -7f at 100
                        7f at 200
                        -3f at 300
                        0f at 400
                    },
                )
            }
        },
    ) {
        when (animal.id) {
            "dog" -> drawDog()
            "cat" -> drawCat()
            "cow" -> drawCow()
            "duck" -> drawDuck()
            "sheep" -> drawSheep()
            else -> drawRooster()
        }
    }
}

@Composable
private fun FruitTile(fruit: Fruit, onTapped: () -> Unit) {
    val squish = remember { Animatable(0f) }
    val splat = remember { Animatable(0f) }
    val splatSeed = remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val drops = remember(splatSeed.intValue) {
        val random = Random(splatSeed.intValue)
        List(12) {
            val angle = random.nextFloat() * 2f * Math.PI.toFloat()
            Triple(cos(angle), sin(angle), 0.5f + random.nextFloat())
        }
    }

    TileCard(
        label = fruit.name,
        onClick = {
            onTapped()
            scope.launch {
                squish.snapTo(0f)
                squish.animateTo(1f, tween(450))
                squish.snapTo(0f)
            }
            scope.launch {
                splatSeed.intValue++
                splat.snapTo(0f)
                splat.animateTo(1f, tween(550))
                splat.snapTo(0f)
            }
        },
    ) {
        // Squash the fruit flat and let it spring back.
        val squishNow = sin(squish.value * Math.PI.toFloat())
        scale(
            scaleX = 1f + 0.25f * squishNow,
            scaleY = 1f - 0.45f * squishNow,
            pivot = Offset(center.x, size.height * 0.8f),
        ) {
            when (fruit.id) {
                "apple" -> drawApple()
                "banana" -> drawBanana()
                "orange" -> drawOrange()
                "strawberry" -> drawStrawberry()
                "watermelon" -> drawWatermelon()
                else -> drawGrapes()
            }
        }

        // Juice splatter: droplets burst outward and fall with gravity.
        if (splat.value > 0f) {
            val t = splat.value
            val d = size.minDimension
            for ((dx, dy, speed) in drops) {
                val dist = d * 0.38f * t * speed
                val drop = Offset(
                    center.x + dx * dist,
                    center.y + dy * dist + d * 0.25f * t * t, // gravity
                )
                drawCircle(
                    fruit.juice.copy(alpha = (1f - t)),
                    radius = d * 0.035f * (1f - t * 0.5f) * speed,
                    center = drop,
                )
            }
        }
    }
}

@Composable
private fun TileCard(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    draw: DrawScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.15f)
                .padding(10.dp),
        ) { draw() }
        Text(label, style = MaterialTheme.typography.bodyLarge, color = InkText)
    }
}

// ---- shared drawing helpers -------------------------------------------------

private fun DrawScope.shadedCircle(color: Color, center: Offset, radius: Float) {
    drawCircle(
        Brush.radialGradient(
            colors = listOf(lerp(color, Color.White, 0.4f), color, lerp(color, Color(0xFF33221A), 0.15f)),
            center = center - Offset(radius * 0.35f, radius * 0.4f),
            radius = radius * 1.8f,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.face(eyeY: Float, spread: Float, pupil: Color = Color(0xFF241812)) {
    for (side in listOf(-1f, 1f)) {
        val eye = Offset(center.x + side * spread, eyeY)
        drawCircle(Color.White, size.minDimension * 0.075f, eye)
        drawCircle(pupil, size.minDimension * 0.042f, eye)
        drawCircle(Color.White, size.minDimension * 0.014f, eye + Offset(size.minDimension * 0.012f, -size.minDimension * 0.014f))
    }
}

// ---- animals ----------------------------------------------------------------

private fun DrawScope.drawDog() {
    val d = size.minDimension
    val head = center
    val brown = Color(0xFFB98356)
    // floppy ears
    for (side in listOf(-1f, 1f)) {
        drawOval(
            lerp(brown, Color(0xFF6D4326), 0.5f),
            topLeft = Offset(head.x + side * d * 0.34f - d * 0.11f, head.y - d * 0.28f),
            size = Size(d * 0.22f, d * 0.42f),
        )
    }
    shadedCircle(brown, head, d * 0.36f)
    // muzzle
    drawOval(
        lerp(brown, Color.White, 0.45f),
        topLeft = Offset(head.x - d * 0.16f, head.y + d * 0.02f),
        size = Size(d * 0.32f, d * 0.24f),
    )
    drawOval(Color(0xFF241812), topLeft = Offset(head.x - d * 0.05f, head.y + d * 0.04f), size = Size(d * 0.10f, d * 0.075f))
    // tongue
    drawOval(Color(0xFFFF6B81), topLeft = Offset(head.x - d * 0.04f, head.y + d * 0.19f), size = Size(d * 0.08f, d * 0.09f))
    face(eyeY = head.y - d * 0.1f, spread = d * 0.14f)
}

private fun DrawScope.drawCat() {
    val d = size.minDimension
    val head = center
    val gray = Color(0xFF9AA5B1)
    // pointy ears
    for (side in listOf(-1f, 1f)) {
        val ear = Path().apply {
            moveTo(head.x + side * d * 0.13f, head.y - d * 0.26f)
            lineTo(head.x + side * d * 0.33f, head.y - d * 0.48f)
            lineTo(head.x + side * d * 0.35f, head.y - d * 0.18f)
            close()
        }
        drawPath(ear, gray)
        val inner = Path().apply {
            moveTo(head.x + side * d * 0.19f, head.y - d * 0.27f)
            lineTo(head.x + side * d * 0.31f, head.y - d * 0.41f)
            lineTo(head.x + side * d * 0.32f, head.y - d * 0.22f)
            close()
        }
        drawPath(inner, Color(0xFFF3B8C3))
    }
    shadedCircle(gray, head, d * 0.35f)
    // nose + whiskers
    val nose = Path().apply {
        moveTo(head.x - d * 0.035f, head.y + d * 0.06f)
        lineTo(head.x + d * 0.035f, head.y + d * 0.06f)
        lineTo(head.x, head.y + d * 0.115f)
        close()
    }
    drawPath(nose, Color(0xFFF08CA0))
    val whisker = Stroke(width = d * 0.012f, cap = StrokeCap.Round)
    for (side in listOf(-1f, 1f)) {
        for (i in 0..1) {
            drawLine(
                Color(0xFF5F6B76),
                start = Offset(head.x + side * d * 0.12f, head.y + d * (0.07f + i * 0.05f)),
                end = Offset(head.x + side * d * 0.38f, head.y + d * (0.03f + i * 0.08f)),
                strokeWidth = whisker.width,
                cap = StrokeCap.Round,
            )
        }
    }
    face(eyeY = head.y - d * 0.08f, spread = d * 0.14f, pupil = Color(0xFF2E5E32))
}

private fun DrawScope.drawCow() {
    val d = size.minDimension
    val head = center
    val hide = Color(0xFFF3EFE8)
    // horns
    for (side in listOf(-1f, 1f)) {
        drawOval(
            Color(0xFFE0C9A0),
            topLeft = Offset(head.x + side * d * 0.28f - d * 0.06f, head.y - d * 0.44f),
            size = Size(d * 0.12f, d * 0.18f),
        )
        // ears
        drawOval(
            Color(0xFFD9D2C6),
            topLeft = Offset(head.x + side * d * 0.38f - d * 0.09f, head.y - d * 0.18f),
            size = Size(d * 0.18f, d * 0.12f),
        )
    }
    shadedCircle(hide, head, d * 0.36f)
    // black patch
    drawOval(
        Color(0xFF3B3B3B),
        topLeft = Offset(head.x - d * 0.34f, head.y - d * 0.30f),
        size = Size(d * 0.26f, d * 0.20f),
    )
    // pink muzzle with nostrils
    drawOval(
        Color(0xFFF2B8C6),
        topLeft = Offset(head.x - d * 0.20f, head.y + d * 0.06f),
        size = Size(d * 0.40f, d * 0.22f),
    )
    for (side in listOf(-1f, 1f)) {
        drawOval(
            Color(0xFFAD5F77),
            topLeft = Offset(head.x + side * d * 0.09f - d * 0.03f, head.y + d * 0.13f),
            size = Size(d * 0.06f, d * 0.05f),
        )
    }
    face(eyeY = head.y - d * 0.08f, spread = d * 0.15f)
}

private fun DrawScope.drawDuck() {
    val d = size.minDimension
    val head = center
    val yellow = Color(0xFFF7D154)
    shadedCircle(yellow, head, d * 0.34f)
    // hair tuft
    drawOval(yellow, topLeft = Offset(head.x - d * 0.05f, head.y - d * 0.44f), size = Size(d * 0.1f, d * 0.12f))
    // beak
    drawOval(
        Color(0xFFF59B23),
        topLeft = Offset(head.x - d * 0.18f, head.y + d * 0.04f),
        size = Size(d * 0.36f, d * 0.15f),
    )
    drawLine(
        Color(0xFFC77607),
        start = Offset(head.x - d * 0.16f, head.y + d * 0.115f),
        end = Offset(head.x + d * 0.16f, head.y + d * 0.115f),
        strokeWidth = d * 0.012f,
        cap = StrokeCap.Round,
    )
    face(eyeY = head.y - d * 0.08f, spread = d * 0.13f)
}

private fun DrawScope.drawSheep() {
    val d = size.minDimension
    val head = center
    // wool cloud
    val wool = Color(0xFFF6F3EC)
    for (i in 0..7) {
        val angle = i / 8f * 2f * Math.PI.toFloat()
        drawCircle(wool, d * 0.14f, head + Offset(cos(angle) * d * 0.26f, sin(angle) * d * 0.26f))
    }
    drawCircle(wool, d * 0.3f, head)
    // tan face
    shadedCircle(Color(0xFFD8B48F), head + Offset(0f, d * 0.04f), d * 0.22f)
    // ears
    for (side in listOf(-1f, 1f)) {
        drawOval(
            Color(0xFFC49B72),
            topLeft = Offset(head.x + side * d * 0.24f - d * 0.07f, head.y - d * 0.02f),
            size = Size(d * 0.14f, d * 0.08f),
        )
    }
    val smile = Path().apply {
        moveTo(head.x - d * 0.05f, head.y + d * 0.14f)
        quadraticTo(head.x, head.y + d * 0.19f, head.x + d * 0.05f, head.y + d * 0.14f)
    }
    drawPath(smile, Color(0xFF6B4B31), style = Stroke(width = d * 0.014f, cap = StrokeCap.Round))
    face(eyeY = head.y - d * 0.015f, spread = d * 0.1f)
}

private fun DrawScope.drawRooster() {
    val d = size.minDimension
    val head = center
    val body = Color(0xFFC96A3B)
    // comb
    for ((i, dx) in listOf(-0.12f, 0f, 0.12f).withIndex()) {
        drawCircle(
            Color(0xFFE8323F),
            radius = d * (if (i == 1) 0.11f else 0.085f),
            center = Offset(head.x + dx * d, head.y - d * 0.36f - (if (i == 1) d * 0.04f else 0f)),
        )
    }
    shadedCircle(body, head, d * 0.34f)
    // wattle
    drawOval(
        Color(0xFFE8323F),
        topLeft = Offset(head.x - d * 0.05f, head.y + d * 0.16f),
        size = Size(d * 0.1f, d * 0.14f),
    )
    // beak
    val beak = Path().apply {
        moveTo(head.x - d * 0.07f, head.y + d * 0.05f)
        lineTo(head.x + d * 0.07f, head.y + d * 0.05f)
        lineTo(head.x, head.y + d * 0.16f)
        close()
    }
    drawPath(beak, Color(0xFFF5A623))
    face(eyeY = head.y - d * 0.08f, spread = d * 0.13f)
}

// ---- fruits -----------------------------------------------------------------

private fun DrawScope.drawApple() {
    val d = size.minDimension
    shadedCircle(Color(0xFFE8323F), center + Offset(0f, d * 0.04f), d * 0.32f)
    drawRoundRect(
        Color(0xFF6D4C41),
        topLeft = Offset(center.x - d * 0.015f, center.y - d * 0.40f),
        size = Size(d * 0.03f, d * 0.14f),
        cornerRadius = CornerRadius(d * 0.01f),
    )
    rotate(degrees = -25f, pivot = center + Offset(d * 0.08f, -d * 0.34f)) {
        drawOval(
            Color(0xFF2ED573),
            topLeft = Offset(center.x + d * 0.02f, center.y - d * 0.40f),
            size = Size(d * 0.16f, d * 0.08f),
        )
    }
}

private fun DrawScope.drawBanana() {
    val d = size.minDimension
    val banana = Path().apply {
        moveTo(center.x - d * 0.30f, center.y - d * 0.16f)
        quadraticTo(center.x - d * 0.05f, center.y + d * 0.38f, center.x + d * 0.32f, center.y + d * 0.06f)
        quadraticTo(center.x + d * 0.34f, center.y + d * 0.16f, center.x + d * 0.26f, center.y + d * 0.20f)
        quadraticTo(center.x - d * 0.14f, center.y + d * 0.40f, center.x - d * 0.36f, center.y - d * 0.08f)
        close()
    }
    drawPath(
        banana,
        Brush.verticalGradient(listOf(Color(0xFFFBE47A), Color(0xFFF2C230))),
    )
    for (tip in listOf(Offset(center.x - d * 0.33f, center.y - d * 0.13f), Offset(center.x + d * 0.30f, center.y + d * 0.12f))) {
        drawCircle(Color(0xFF8A6D3B), d * 0.03f, tip)
    }
}

private fun DrawScope.drawOrange() {
    val d = size.minDimension
    shadedCircle(Color(0xFFFF9F1C), center + Offset(0f, d * 0.02f), d * 0.32f)
    // dimple texture
    for (i in 0..5) {
        val angle = i / 6f * 2f * Math.PI.toFloat()
        drawCircle(
            Color(0xFFE07E00).copy(alpha = 0.5f),
            d * 0.012f,
            center + Offset(cos(angle) * d * 0.15f, d * 0.02f + sin(angle) * d * 0.15f),
        )
    }
    drawOval(
        Color(0xFF2ED573),
        topLeft = Offset(center.x - d * 0.02f, center.y - d * 0.38f),
        size = Size(d * 0.14f, d * 0.07f),
    )
}

private fun DrawScope.drawStrawberry() {
    val d = size.minDimension
    val berry = Path().apply {
        moveTo(center.x - d * 0.26f, center.y - d * 0.12f)
        quadraticTo(center.x - d * 0.28f, center.y + d * 0.14f, center.x, center.y + d * 0.34f)
        quadraticTo(center.x + d * 0.28f, center.y + d * 0.14f, center.x + d * 0.26f, center.y - d * 0.12f)
        quadraticTo(center.x, center.y - d * 0.26f, center.x - d * 0.26f, center.y - d * 0.12f)
        close()
    }
    drawPath(
        berry,
        Brush.radialGradient(
            colors = listOf(Color(0xFFFF7B8D), Color(0xFFE84667)),
            center = center - Offset(d * 0.1f, d * 0.12f),
            radius = d * 0.5f,
        ),
    )
    // seeds
    for ((sx, sy) in listOf(-0.12f to 0.0f, 0.0f to 0.1f, 0.12f to 0.0f, -0.06f to 0.18f, 0.06f to 0.18f, 0.0f to -0.06f)) {
        drawOval(
            Color(0xFFFBE47A),
            topLeft = center + Offset(sx * d - d * 0.012f, sy * d - d * 0.018f),
            size = Size(d * 0.024f, d * 0.036f),
        )
    }
    // leafy crown
    for (dx in listOf(-0.12f, 0f, 0.12f)) {
        drawOval(
            Color(0xFF2ED573),
            topLeft = center + Offset(dx * d - d * 0.05f, -d * 0.30f),
            size = Size(d * 0.1f, d * 0.09f),
        )
    }
}

private fun DrawScope.drawWatermelon() {
    val d = size.minDimension
    val c = center + Offset(0f, d * 0.05f)
    // rind
    drawArc(
        Color(0xFF2E9E4F),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(c.x - d * 0.34f, c.y - d * 0.34f),
        size = Size(d * 0.68f, d * 0.68f),
    )
    drawArc(
        Color(0xFFEFF7E9),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(c.x - d * 0.30f, c.y - d * 0.30f),
        size = Size(d * 0.60f, d * 0.60f),
    )
    drawArc(
        Brush.verticalGradient(listOf(Color(0xFFFF7B8D), Color(0xFFFF5D6C)), endY = c.y),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(c.x - d * 0.27f, c.y - d * 0.27f),
        size = Size(d * 0.54f, d * 0.54f),
    )
    for ((sx, sy) in listOf(-0.14f to -0.08f, 0.0f to -0.16f, 0.14f to -0.08f, -0.06f to -0.04f, 0.07f to -0.05f)) {
        drawOval(
            Color(0xFF33221A),
            topLeft = c + Offset(sx * d - d * 0.014f, sy * d - d * 0.02f),
            size = Size(d * 0.028f, d * 0.04f),
        )
    }
}

private fun DrawScope.drawGrapes() {
    val d = size.minDimension
    val purple = Color(0xFF8E5FBF)
    drawRoundRect(
        Color(0xFF6D4C41),
        topLeft = Offset(center.x - d * 0.015f, center.y - d * 0.42f),
        size = Size(d * 0.03f, d * 0.14f),
        cornerRadius = CornerRadius(d * 0.01f),
    )
    val positions = listOf(
        Offset(-0.14f, -0.18f), Offset(0.14f, -0.18f), Offset(0f, -0.2f),
        Offset(-0.2f, -0.02f), Offset(0f, -0.02f), Offset(0.2f, -0.02f),
        Offset(-0.1f, 0.14f), Offset(0.1f, 0.14f),
        Offset(0f, 0.28f),
    )
    for (pos in positions) {
        shadedCircle(purple, center + Offset(pos.x * d, pos.y * d), d * 0.105f)
    }
}
