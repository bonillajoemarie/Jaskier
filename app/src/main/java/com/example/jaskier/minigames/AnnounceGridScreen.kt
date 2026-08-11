package com.example.jaskier.minigames

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jaskier.R
import com.example.jaskier.speech.TtsManager
import com.example.jaskier.ui.theme.InkText
import com.example.jaskier.ui.theme.RainbowCells
import com.example.jaskier.ui.theme.SkyGradient
import com.example.jaskier.ui.theme.glossy
import kotlinx.coroutines.launch

@Composable
fun AnnounceGridScreen(
    game: AnnounceGame,
    tts: TtsManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Spoken how-to for pre-readers.
    LaunchedEffect(game.id) { tts.speak(game.intro) }

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
                text = game.title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp),
            )
        }

        LazyVerticalGrid(
            // Adaptive cells: 4 columns on phones, more on tablets.
            columns = GridCells.Adaptive(minSize = 88.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(game.items) { index, item ->
                AnnounceCell(
                    item = item,
                    // A color tile shows its own color; everything else takes
                    // the next color off the rainbow.
                    color = (item.art as? TileArt.Swatch)?.color
                        ?: RainbowCells[index % RainbowCells.size],
                    onAnnounce = { tts.speak(item.utterance) },
                )
            }
        }
    }
}

@Composable
private fun AnnounceCell(
    item: AnnounceItem,
    color: Color,
    onAnnounce: () -> Unit,
) {
    val rotation = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(pop.value)
            .rotate(rotation.value)
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(glossy(color))
            // Without this the white swatch reads as a hole in the grid.
            .border(2.dp, Color(0x22000000), RoundedCornerShape(24.dp))
            .clickable {
                onAnnounce()
                scope.launch {
                    launch {
                        pop.snapTo(1.15f)
                        pop.animateTo(1f)
                    }
                    rotation.animateTo(
                        0f,
                        keyframes {
                            durationMillis = 350
                            0f at 0
                            -8f at 90
                            8f at 180
                            -4f at 260
                            0f at 350
                        },
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        when (val art = item.art) {
            // Letters and numbers: the glyph is the whole tile.
            TileArt.Glyph -> Text(
                text = item.display,
                fontSize = 34.sp,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )

            // Colors: the tile *is* the color, so the name has to survive on
            // both a white and a black background.
            is TileArt.Swatch -> TileLabel(item.display, labelColorOn(art.color))

            // Shapes: a white silhouette on the rainbow tile.
            is TileArt.Shape -> {
                Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 18.dp)) {
                    drawShape(
                        kind = art.kind,
                        center = center,
                        size = size.minDimension * 0.62f,
                        color = Color.White,
                    )
                }
                TileLabel(item.display, Color.White)
            }
        }
    }
}

/** The written name, small and low on the tile: decoration for parents, per kids-ux. */
@Composable
private fun BoxScope.TileLabel(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 8.dp),
    )
}

/** Dark ink on pale swatches, white on dark ones, so every color name stays legible. */
private fun labelColorOn(swatch: Color): Color =
    if (swatch.luminance() > 0.55f) InkText else Color.White
