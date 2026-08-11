package com.example.jaskier.pet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jaskier.R
import com.example.jaskier.minigames.MiniGameDef
import com.example.jaskier.minigames.MiniGames
import com.example.jaskier.speech.TtsManager
import com.example.jaskier.speech.VoiceTone
import com.example.jaskier.ui.theme.BubblePink
import com.example.jaskier.ui.theme.InkText
import com.example.jaskier.ui.theme.SkyBlue
import com.example.jaskier.ui.theme.SkyGradient
import com.example.jaskier.ui.theme.WarmOrange
import com.example.jaskier.ui.theme.WaterBlue
import com.example.jaskier.ui.theme.breathe
import com.example.jaskier.ui.theme.glossy
import com.example.jaskier.ui.theme.lighter
import com.example.jaskier.ui.theme.pressBounce
import com.example.jaskier.ui.theme.rememberPressSource

private val TeethMint = Color(0xFF66D9C2)

@Composable
fun PetHomeScreen(
    viewModel: PetViewModel,
    tts: TtsManager,
    onOpenMiniGame: (MiniGameDef) -> Unit,
    onOpenCare: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val feeling = feelingOf(state.hunger, state.hydration, state.cleanliness, state.teeth)

    // Kerker says hello, then tells the kid what he needs whenever it changes.
    // When a need gets bad he cries about it — a cartoon waah, never a guilt
    // trip, and it stops the moment the kid helps.
    LaunchedEffect(state.mood, feeling.crying) {
        val line = when (state.mood) {
            PetMood.HUNGRY ->
                if (feeling.crying) "Waaah! My tummy hurts! Please feed me!"
                else "My tummy is rumbling! Can you feed me?"
            PetMood.THIRSTY ->
                if (feeling.crying) "Waaah! So thirsty! Water, please!"
                else "I'm so thirsty! Can I have a drink, please?"
            PetMood.DIRTY ->
                if (feeling.crying) "Waaah! I'm all yucky! Shower, please!"
                else "I'm all dirty! I need a shower!"
            PetMood.YUCKY_TEETH ->
                if (feeling.crying) "Waaah! My teeth feel awful! Let's brush!"
                else "My teeth feel yucky! Let's brush them!"
            PetMood.SICK -> "Waaah... I don't feel good. Medicine, please!"
            PetMood.HAPPY -> "Hi! I'm Kerker! Let's play!"
        }
        tts.speak(
            line,
            when {
                feeling.crying -> VoiceTone.CRYING
                state.mood != PetMood.HAPPY -> VoiceTone.SAD
                else -> VoiceTone.NORMAL
            },
        )
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            // Capped width keeps the layout cozy on tablets like the Tab S9 FE.
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxHeight()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.pet_name),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            StatBar(stringResource(R.string.hunger_label), "🍎", state.hunger, WarmOrange)
            StatBar(
                stringResource(R.string.hydration_label), "🥛", state.hydration, WaterBlue,
                modifier = Modifier.padding(top = 8.dp),
            )
            StatBar(
                stringResource(R.string.cleanliness_label), "🧼", state.cleanliness, SkyBlue,
                modifier = Modifier.padding(top = 8.dp),
            )
            StatBar(
                stringResource(R.string.teeth_label), "🦷", state.teeth, TeethMint,
                modifier = Modifier.padding(top = 8.dp),
            )

            // Each body part has its own reaction, and each rotates its lines so
            // repeat pokes never say the same thing twice in a row.
            var pokeIndex by remember { mutableIntStateOf(0) }
            val speakZone: (KerkerZone) -> Unit = { zone ->
                val lines = linesFor(zone)
                tts.speak(lines[pokeIndex % lines.size], VoiceTone.GIGGLY)
                pokeIndex++
            }
            val tickles = remember {
                listOf("Hahaha! Stop it!", "Eeee! That tickles!", "Hehehehe!")
            }
            PetCanvas(
                hunger = state.hunger,
                cleanliness = state.cleanliness,
                mood = state.mood,
                events = viewModel.events,
                onPoke = { speakZone(KerkerZone.NONE) },
                onTouchZone = speakZone,
                onTickle = {
                    tts.speak(tickles[pokeIndex % tickles.size], VoiceTone.GIGGLY)
                    pokeIndex++
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 6.dp),
            )

            // Medicine appears only when the pet is sick, pulsing for attention.
            AnimatedVisibility(
                visible = state.mood == PetMood.SICK,
                enter = fadeIn() + scaleIn(initialScale = 0.7f),
                exit = fadeOut() + scaleOut(targetScale = 0.7f),
            ) {
                MedicineButton(
                    onClick = viewModel::heal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CareButton(stringResource(R.string.feed_button), "🍎", WarmOrange, { onOpenCare("feed") }, Modifier.weight(1f).breathe(0))
                CareButton(stringResource(R.string.drink_button), "🍼", WaterBlue, { onOpenCare("drink") }, Modifier.weight(1f).breathe(1))
                CareButton(stringResource(R.string.shower_button), "🚿", SkyBlue, { onOpenCare("shower") }, Modifier.weight(1f).breathe(2))
                CareButton(stringResource(R.string.brush_button), "🪥", TeethMint, { onOpenCare("brush") }, Modifier.weight(1f).breathe(3))
            }

            Text(
                text = stringResource(R.string.play_section),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniGames.chunked(3).forEachIndexed { rowIndex, rowGames ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowGames.forEachIndexed { colIndex, game ->
                            MiniGameCard(
                                game = game,
                                onClick = { onOpenMiniGame(game) },
                                modifier = Modifier.weight(1f).breathe(rowIndex * 3 + colIndex),
                            )
                        }
                        repeat(3 - rowGames.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBar(
    label: String,
    emoji: String,
    value: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val fill by animateFloatAsState(
        targetValue = (value / STAT_MAX).coerceIn(0.02f, 1f),
        animationSpec = tween(600),
        label = "statFill",
    )

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 17.sp)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .shadow(3.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.75f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(glossy(color)),
            )
        }
    }
}

@Composable
private fun MedicineButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "medPulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "medScale",
    )
    val press = rememberPressSource()
    Box(
        modifier = modifier
            .height(60.dp)
            .scale(pulseScale)
            .pressBounce(press)
            .shadow(10.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(glossy(BubblePink))
            .clickable(interactionSource = press, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("💊", fontSize = 24.sp)
            Text(
                stringResource(R.string.heal_button),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun CareButton(
    text: String,
    emoji: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val press = rememberPressSource()
    Box(
        modifier = modifier
            .height(82.dp)
            .pressBounce(press)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(glossy(color))
            .clickable(interactionSource = press, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 24.sp)
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = InkText,
            )
        }
    }
}

@Composable
private fun MiniGameCard(
    game: MiniGameDef,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val press = rememberPressSource()
    Box(
        modifier = modifier
            .height(100.dp)
            .pressBounce(press)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(glossy(game.color))
            .clickable(interactionSource = press, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Decorative corner bubble adds depth to the card face.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(game.color.lighter(0.45f)),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(game.cardEmoji, fontSize = 24.sp)
            Text(
                text = game.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
    }
}
