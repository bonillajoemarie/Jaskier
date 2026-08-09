package com.example.jaskier.songs

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/** Low-latency player for the short animal clips (SoundPool-backed). */
class AnimalSoundPlayer(context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val loaded = mutableSetOf<Int>()
    private val soundIds: Map<String, Int> = AnimalSounds.associate { animal ->
        animal.id to soundPool.load(context.applicationContext, animal.resId, 1)
    }

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded += sampleId
        }
    }

    fun play(animalId: String) {
        val soundId = soundIds[animalId] ?: return
        if (soundId in loaded) soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() = soundPool.release()
}
