package com.example.jaskier.minigames

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jaskier.R
import com.example.jaskier.songs.Song
import com.example.jaskier.songs.SongPlayer
import com.example.jaskier.songs.Songs
import com.example.jaskier.speech.TtsManager
import com.example.jaskier.ui.theme.InkText
import com.example.jaskier.ui.theme.RainbowCells
import com.example.jaskier.ui.theme.glossy
import com.example.jaskier.ui.theme.pressBounce
import com.example.jaskier.ui.theme.rememberPressSource

@Composable
fun SongsScreen(
    player: SongPlayer,
    tts: TtsManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playingId by player.playingSongId.collectAsStateWithLifecycle()
    var selectedSong by remember { mutableStateOf<Song?>(null) }

    // Spoken how-to for pre-readers.
    LaunchedEffect(Unit) { tts.speak(SingAlong.intro) }

    // Leaving the screen stops the music.
    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }

    // Karaoke "now playing" view takes over while a song is selected.
    val karaokeSong = selectedSong
    if (karaokeSong != null) {
        KaraokeView(
            song = karaokeSong,
            player = player,
            isPlaying = playingId == karaokeSong.id,
            onClose = {
                player.stop()
                selectedSong = null
            },
            modifier = modifier,
        )
        return
    }

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
                text = "Sing 🎵",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(start = 16.dp),
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            // Capped width keeps song rows readable on tablets.
            modifier = Modifier
                .widthIn(max = 640.dp)
                .align(Alignment.CenterHorizontally)
                .fillMaxSize(),
        ) {
            itemsIndexed(Songs) { index, song ->
                SongCard(
                    song = song,
                    color = RainbowCells[index % RainbowCells.size],
                    isPlaying = playingId == song.id,
                    onToggle = {
                        selectedSong = song
                        if (playingId != song.id) player.toggle(song)
                    },
                )
            }
        }
    }
}

@Composable
private fun SongCard(
    song: Song,
    color: Color,
    isPlaying: Boolean,
    onToggle: () -> Unit,
) {
    val press = rememberPressSource()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .pressBounce(press)
            .shadow(if (isPlaying) 10.dp else 6.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(glossy(color))
            .clickable(interactionSource = press, indication = null, onClick = onToggle)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(song.emoji, fontSize = 24.sp)
        }
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
        if (isPlaying) Equalizer() else Text("▶", fontSize = 26.sp, color = Color.White)
    }
}

// Three bouncing bars that show which song is playing.
@Composable
private fun Equalizer() {
    val transition = rememberInfiniteTransition(label = "eq")
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.height(28.dp),
    ) {
        listOf(0, 150, 300).forEach { delay ->
            val h by transition.animateFloat(
                initialValue = 8f,
                targetValue = 26f,
                animationSpec = infiniteRepeatable(
                    tween(280, delayMillis = delay, easing = LinearEasing),
                    RepeatMode.Reverse,
                ),
                label = "bar$delay",
            )
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White),
            )
        }
    }
}
