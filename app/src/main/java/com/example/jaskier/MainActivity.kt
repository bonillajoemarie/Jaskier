package com.example.jaskier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.jaskier.pet.PetRepository
import com.example.jaskier.songs.AnimalSoundPlayer
import com.example.jaskier.songs.SongPlayer
import com.example.jaskier.speech.TtsManager

class MainActivity : ComponentActivity() {

    private lateinit var tts: TtsManager
    private lateinit var songPlayer: SongPlayer
    private lateinit var animalPlayer: AnimalSoundPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TtsManager(this)
        songPlayer = SongPlayer(this)
        animalPlayer = AnimalSoundPlayer(this)
        val repository = PetRepository(this)
        setContent {
            JaskierApp(
                repository = repository,
                tts = tts,
                songPlayer = songPlayer,
                animalPlayer = animalPlayer,
            )
        }
    }

    override fun onPause() {
        super.onPause()
        songPlayer.stop()
    }

    override fun onDestroy() {
        tts.shutdown()
        songPlayer.release()
        animalPlayer.release()
        super.onDestroy()
    }
}
