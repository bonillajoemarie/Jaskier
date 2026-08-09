package com.example.jaskier.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context.applicationContext, this)

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            if (tts.setLanguage(Locale.US) < TextToSpeech.LANG_AVAILABLE) {
                tts.language = Locale.getDefault()
            }
            tts.setSpeechRate(KID_SPEECH_RATE)
            _isReady.value = true
        }
    }

    fun speak(text: String) {
        if (!_isReady.value) return
        // QUEUE_FLUSH so rapid taps always announce the latest item.
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun shutdown() {
        _isReady.value = false
        tts.shutdown()
    }

    private companion object {
        const val KID_SPEECH_RATE = 0.8f
        const val UTTERANCE_ID = "jaskier"
    }
}
