package com.example.jaskier

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jaskier.care.BrushScreen
import com.example.jaskier.care.DrinkScreen
import com.example.jaskier.care.FeedScreen
import com.example.jaskier.care.ShowerScreen
import com.example.jaskier.minigames.AnnounceGame
import com.example.jaskier.minigames.AnnounceGridScreen
import com.example.jaskier.minigames.CatchGame
import com.example.jaskier.minigames.CatchGameScreen
import com.example.jaskier.minigames.Match3Screen
import com.example.jaskier.minigames.MatchGame
import com.example.jaskier.minigames.SingAlong
import com.example.jaskier.minigames.SongsScreen
import com.example.jaskier.minigames.ZooGame
import com.example.jaskier.minigames.ZooScreen
import com.example.jaskier.minigames.miniGameById
import com.example.jaskier.pet.PetHomeScreen
import com.example.jaskier.pet.PetRepository
import com.example.jaskier.pet.PetViewModel
import com.example.jaskier.songs.AnimalSoundPlayer
import com.example.jaskier.songs.SongPlayer
import com.example.jaskier.speech.TtsManager
import com.example.jaskier.ui.theme.JaskierTheme
import com.example.jaskier.ui.theme.SkyGradient

sealed interface Screen {
    data object PetHome : Screen
    data class MiniGamePlay(val gameId: String) : Screen
    data class Care(val careId: String) : Screen
}

private val ScreenSaver = Saver<Screen, String>(
    save = { screen ->
        when (screen) {
            Screen.PetHome -> "home"
            is Screen.MiniGamePlay -> "game:${screen.gameId}"
            is Screen.Care -> "care:${screen.careId}"
        }
    },
    restore = { value ->
        when {
            value.startsWith("game:") -> Screen.MiniGamePlay(value.removePrefix("game:"))
            value.startsWith("care:") -> Screen.Care(value.removePrefix("care:"))
            else -> Screen.PetHome
        }
    },
)

@Composable
fun JaskierApp(
    repository: PetRepository,
    tts: TtsManager,
    songPlayer: SongPlayer,
    animalPlayer: AnimalSoundPlayer,
) {
    val petViewModel: PetViewModel = viewModel(factory = PetViewModel.Factory(repository))
    var screen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf(Screen.PetHome) }

    LifecycleResumeEffect(Unit) {
        petViewModel.refresh()
        onPauseOrDispose { }
    }

    JaskierTheme {
        // The sky paints edge-to-edge (behind status/navigation bars); each
        // screen then lays its content inside the safe drawing area.
        Box(modifier = Modifier.fillMaxSize().background(SkyGradient)) {
            BackHandler(enabled = screen != Screen.PetHome) { screen = Screen.PetHome }
            val goHome = { screen = Screen.PetHome }

            when (val current = screen) {
                Screen.PetHome -> PetHomeScreen(
                    viewModel = petViewModel,
                    tts = tts,
                    onOpenMiniGame = { screen = Screen.MiniGamePlay(it.id) },
                    onOpenCare = { screen = Screen.Care(it) },
                    modifier = Modifier.safeDrawingPadding(),
                )
                is Screen.Care -> when (current.careId) {
                    "drink" -> DrinkScreen(
                        viewModel = petViewModel,
                        tts = tts,
                        onDone = goHome,
                        modifier = Modifier.safeDrawingPadding(),
                    )
                    "shower" -> ShowerScreen(
                        viewModel = petViewModel,
                        tts = tts,
                        onDone = goHome,
                        modifier = Modifier.safeDrawingPadding(),
                    )
                    "brush" -> BrushScreen(
                        viewModel = petViewModel,
                        tts = tts,
                        onDone = goHome,
                        modifier = Modifier.safeDrawingPadding(),
                    )
                    else -> FeedScreen(
                        viewModel = petViewModel,
                        tts = tts,
                        onDone = goHome,
                        modifier = Modifier.safeDrawingPadding(),
                    )
                }
                is Screen.MiniGamePlay -> when (val game = miniGameById(current.gameId)) {
                    is AnnounceGame -> AnnounceGridScreen(
                        game = game,
                        tts = tts,
                        onBack = goHome,
                        modifier = Modifier.safeDrawingPadding(),
                    )
                    CatchGame -> CatchGameScreen(
                        tts = tts,
                        onBack = goHome,
                        modifier = Modifier.safeDrawingPadding(),
                    )
                    SingAlong -> SongsScreen(
                        player = songPlayer,
                        tts = tts,
                        onBack = goHome,
                        modifier = Modifier.safeDrawingPadding(),
                    )
                    ZooGame -> ZooScreen(
                        animalPlayer = animalPlayer,
                        tts = tts,
                        onBack = goHome,
                        modifier = Modifier.safeDrawingPadding(),
                    )
                    MatchGame -> Match3Screen(
                        animalPlayer = animalPlayer,
                        tts = tts,
                        onBack = goHome,
                        modifier = Modifier.safeDrawingPadding(),
                    )
                }
            }
        }
    }
}
