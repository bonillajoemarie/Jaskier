package com.example.jaskier.minigames

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaskier.R
import com.example.jaskier.songs.AnimalSoundPlayer
import com.example.jaskier.songs.AnimalSounds
import com.example.jaskier.speech.TtsManager
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TileColors = listOf(
    Color(0xFFFFE3C2), // dog
    Color(0xFFE6ECF5), // cat
    Color(0xFFEFF7E9), // cow
    Color(0xFFFFF6CB), // duck
    Color(0xFFF3EFE8), // sheep
    Color(0xFFFFE0D6), // rooster
)

@Composable
fun Match3Screen(
    animalPlayer: AnimalSoundPlayer,
    tts: TtsManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val logic = remember { Match3Logic() }
    var board by remember { mutableStateOf(logic.newBoard()) }
    var score by remember { mutableIntStateOf(0) }
    var clearing by remember { mutableStateOf(emptySet<Int>()) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { tts.speak(MatchGame.intro) }

    suspend fun resolveCascades() {
        busy = true
        while (true) {
            val matches = logic.findMatches(board)
            if (matches.isEmpty()) break
            // Celebrate with the matched animal's real sound.
            val matchedType = board[matches.first()]
            animalPlayer.play(AnimalSounds[matchedType % AnimalSounds.size].id)
            score += matches.size
            clearing = matches
            delay(350)
            clearing = emptySet()
            board = logic.clearAndFall(board, matches)
            delay(200)
        }
        busy = false
    }

    // drag-to-swap state
    var dragStartCell by remember { mutableStateOf(-1) }
    var dragConsumed by remember { mutableStateOf(false) }
    var dragAccum by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 96.dp, start = 12.dp, end = 12.dp, bottom = 12.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { start ->
                            val cell = cellAt(start, logic)
                            dragStartCell = cell
                            dragConsumed = false
                            dragAccum = Offset.Zero
                        },
                        onDrag = { _, amount ->
                            if (busy || dragConsumed || dragStartCell < 0) return@detectDragGestures
                            dragAccum += amount
                            val threshold = size.width / logic.columns * 0.4f
                            val target = when {
                                dragAccum.x > threshold -> dragStartCell + 1
                                dragAccum.x < -threshold -> dragStartCell - 1
                                dragAccum.y > threshold -> dragStartCell + logic.columns
                                dragAccum.y < -threshold -> dragStartCell - logic.columns
                                else -> -1
                            }
                            if (target in 0 until logic.size) {
                                dragConsumed = true
                                val next = board.copyOf()
                                if (logic.trySwap(next, dragStartCell, target)) {
                                    board = next
                                    scope.launch { resolveCascades() }
                                }
                            }
                        },
                        onDragEnd = { dragStartCell = -1 },
                        onDragCancel = { dragStartCell = -1 },
                    )
                },
        ) {
            drawBoard(logic, board, clearing, dragStartCell)
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
                color = Color(0xFF3D3D3D),
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        val backLabel = stringResource(R.string.back_button)
        Box(
            modifier = Modifier
                .padding(12.dp)
                .size(56.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onBack)
                .semantics { contentDescription = backLabel },
            contentAlignment = Alignment.Center,
        ) {
            Text("←", fontSize = 30.sp, color = Color(0xFF3D3D3D))
        }
    }
}

private fun androidx.compose.ui.input.pointer.PointerInputScope.cellAt(pos: Offset, logic: Match3Logic): Int {
    val tile = size.width.toFloat() / logic.columns
    val col = (pos.x / tile).toInt().coerceIn(0, logic.columns - 1)
    val row = (pos.y / tile).toInt().coerceIn(0, logic.rows - 1)
    return logic.index(col, row)
}

private fun DrawScope.drawBoard(logic: Match3Logic, board: IntArray, clearing: Set<Int>, heldCell: Int) {
    val tile = size.width / logic.columns
    for (i in 0 until logic.size) {
        val col = i % logic.columns
        val row = i / logic.columns
        val topLeft = Offset(col * tile, row * tile)
        val center = topLeft + Offset(tile / 2f, tile / 2f)
        val type = board[i]
        val popping = i in clearing
        val held = i == heldCell

        val pad = tile * if (popping) 0.16f else 0.045f
        drawRoundRect(
            if (held) lerp(TileColors[type % TileColors.size], Color.White, 0.4f) else TileColors[type % TileColors.size],
            topLeft = topLeft + Offset(pad, pad),
            size = Size(tile - pad * 2f, tile - pad * 2f),
            cornerRadius = CornerRadius(tile * 0.22f),
        )
        val r = tile * if (popping) 0.24f else 0.32f
        drawMiniAnimal(type, center, r)
        if (popping) {
            drawStar(center + Offset(tile * 0.3f, -tile * 0.3f), tile * 0.1f)
            drawStar(center + Offset(-tile * 0.32f, tile * 0.28f), tile * 0.08f)
        }
    }
}

private fun DrawScope.drawStar(at: Offset, r: Float) {
    val star = Path().apply {
        moveTo(at.x, at.y - r)
        quadraticTo(at.x + r * 0.2f, at.y - r * 0.2f, at.x + r, at.y)
        quadraticTo(at.x + r * 0.2f, at.y + r * 0.2f, at.x, at.y + r)
        quadraticTo(at.x - r * 0.2f, at.y + r * 0.2f, at.x - r, at.y)
        quadraticTo(at.x - r * 0.2f, at.y - r * 0.2f, at.x, at.y - r)
        close()
    }
    drawPath(star, Color(0xFFFFC93C))
}

// Compact animal faces for the small tiles — distinctive silhouettes first.
private fun DrawScope.drawMiniAnimal(type: Int, c: Offset, r: Float) {
    when (type % 6) {
        0 -> { // dog: brown, floppy ears, muzzle
            val brown = Color(0xFFB98356)
            for (side in listOf(-1f, 1f)) {
                drawOval(
                    lerp(brown, Color(0xFF6D4326), 0.5f),
                    topLeft = c + Offset(side * r * 0.85f - r * 0.28f, -r * 0.55f),
                    size = Size(r * 0.56f, r * 1.0f),
                )
            }
            shaded(brown, c, r)
            drawOval(lerp(brown, Color.White, 0.45f), topLeft = c + Offset(-r * 0.42f, r * 0.05f), size = Size(r * 0.84f, r * 0.62f))
            drawOval(Color(0xFF241812), topLeft = c + Offset(-r * 0.14f, r * 0.1f), size = Size(r * 0.28f, r * 0.2f))
            eyes(c, r)
        }
        1 -> { // cat: gray, pointy ears, whisker dots
            val gray = Color(0xFF9AA5B1)
            for (side in listOf(-1f, 1f)) {
                val ear = Path().apply {
                    moveTo(c.x + side * r * 0.35f, c.y - r * 0.7f)
                    lineTo(c.x + side * r * 0.95f, c.y - r * 1.25f)
                    lineTo(c.x + side * r * 0.95f, c.y - r * 0.45f)
                    close()
                }
                drawPath(ear, gray)
            }
            shaded(gray, c, r)
            val nose = Path().apply {
                moveTo(c.x - r * 0.1f, c.y + r * 0.18f)
                lineTo(c.x + r * 0.1f, c.y + r * 0.18f)
                lineTo(c.x, c.y + r * 0.34f)
                close()
            }
            drawPath(nose, Color(0xFFF08CA0))
            eyes(c, r)
        }
        2 -> { // cow: white with black patch and horns
            val hide = Color(0xFFF3EFE8)
            for (side in listOf(-1f, 1f)) {
                drawOval(
                    Color(0xFFE0C9A0),
                    topLeft = c + Offset(side * r * 0.75f - r * 0.16f, -r * 1.25f),
                    size = Size(r * 0.32f, r * 0.5f),
                )
            }
            shaded(hide, c, r)
            drawOval(Color(0xFF3B3B3B), topLeft = c + Offset(-r * 0.95f, -r * 0.8f), size = Size(r * 0.75f, r * 0.6f))
            drawOval(Color(0xFFF2B8C6), topLeft = c + Offset(-r * 0.5f, r * 0.15f), size = Size(r * 1.0f, r * 0.55f))
            eyes(c, r)
        }
        3 -> { // duck: yellow with orange beak
            val yellow = Color(0xFFF7D154)
            shaded(yellow, c, r)
            drawOval(Color(0xFFF59B23), topLeft = c + Offset(-r * 0.5f, r * 0.1f), size = Size(r * 1.0f, r * 0.42f))
            eyes(c, r)
        }
        4 -> { // sheep: wool cloud with tan face
            val wool = Color(0xFFFDFBF6)
            for (angle in 0 until 7) {
                val a = angle / 7f * 2f * Math.PI.toFloat()
                drawCircle(wool, r * 0.42f, c + Offset(kotlin.math.cos(a), kotlin.math.sin(a)) * r * 0.75f)
            }
            drawCircle(wool, r * 0.85f, c)
            shaded(Color(0xFFD8B48F), c + Offset(0f, r * 0.1f), r * 0.62f)
            eyes(c + Offset(0f, r * 0.05f), r * 0.8f)
        }
        else -> { // rooster: red comb + beak
            val body = Color(0xFFC96A3B)
            for ((i, dx) in listOf(-0.4f, 0f, 0.4f).withIndex()) {
                drawCircle(
                    Color(0xFFE8323F),
                    radius = r * (if (i == 1) 0.32f else 0.25f),
                    center = c + Offset(dx * r, -r * 1.0f - (if (i == 1) r * 0.12f else 0f)),
                )
            }
            shaded(body, c, r)
            val beak = Path().apply {
                moveTo(c.x - r * 0.2f, c.y + r * 0.15f)
                lineTo(c.x + r * 0.2f, c.y + r * 0.15f)
                lineTo(c.x, c.y + r * 0.48f)
                close()
            }
            drawPath(beak, Color(0xFFF5A623))
            eyes(c, r)
        }
    }
}

private fun DrawScope.shaded(color: Color, c: Offset, r: Float) {
    drawCircle(
        Brush.radialGradient(
            colors = listOf(lerp(color, Color.White, 0.4f), color, lerp(color, Color(0xFF33221A), 0.15f)),
            center = c - Offset(r * 0.35f, r * 0.4f),
            radius = r * 1.8f,
        ),
        radius = r,
        center = c,
    )
}

private fun DrawScope.eyes(c: Offset, r: Float) {
    for (side in listOf(-1f, 1f)) {
        val eye = c + Offset(side * r * 0.38f, -r * 0.22f)
        drawCircle(Color.White, r * 0.2f, eye)
        drawCircle(Color(0xFF241812), r * 0.11f, eye)
        drawCircle(Color.White, r * 0.04f, eye + Offset(r * 0.035f, -r * 0.04f))
    }
}
