package com.example.jaskier.songs

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Plays one song at a time; tapping the playing song stops it. */
class SongPlayer(context: Context) {

    private val appContext = context.applicationContext
    private var player: MediaPlayer? = null

    private val _playingSongId = MutableStateFlow<String?>(null)
    val playingSongId: StateFlow<String?> = _playingSongId

    fun toggle(song: Song) {
        if (_playingSongId.value == song.id) {
            stop()
            return
        }
        stop()
        player = MediaPlayer.create(appContext, song.resId)?.apply {
            setOnCompletionListener { stop() }
            start()
        }
        _playingSongId.value = if (player != null) song.id else null
    }

    fun stop() {
        player?.release()
        player = null
        _playingSongId.value = null
    }

    fun positionMs(): Int = runCatching { player?.currentPosition ?: 0 }.getOrDefault(0)

    fun durationMs(): Int = runCatching { player?.duration ?: 0 }.getOrDefault(0)

    fun release() = stop()
}
