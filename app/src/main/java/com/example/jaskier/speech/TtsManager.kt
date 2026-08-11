package com.example.jaskier.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context.applicationContext, this)

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    // Setting pitch and rate on every utterance is wasteful; only change on switch.
    private var appliedTone: VoiceTone? = null

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return

        val locale = if (tts.setLanguage(Locale.US) >= TextToSpeech.LANG_AVAILABLE) {
            Locale.US
        } else {
            tts.language = Locale.getDefault()
            Locale.getDefault()
        }

        selectWarmestVoice(locale.language)
        applyTone(VoiceTone.NORMAL)
        _isReady.value = true
    }

    /**
     * The engine default is rarely the nicest voice installed. Anything unexpected
     * here is swallowed: a plain default voice beats a silent app.
     */
    private fun selectWarmestVoice(language: String) {
        val available = runCatching { tts.voices }.getOrNull() ?: return
        val options = available.mapNotNull { voice ->
            val tag = voice.locale?.language ?: return@mapNotNull null
            VoiceOption(
                name = voice.name.orEmpty(),
                language = tag,
                quality = voice.quality,
                latency = voice.latency,
                isNetworkOnly = voice.isNetworkConnectionRequired ||
                    voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true,
            )
        }
        val best = pickWarmestVoice(options, language) ?: return
        available.firstOrNull { it.name == best.name }?.let { chosen: Voice ->
            runCatching { tts.voice = chosen }
        }
    }

    fun speak(text: String, tone: VoiceTone = VoiceTone.NORMAL) {
        if (!_isReady.value) return
        applyTone(tone)
        // QUEUE_FLUSH so rapid taps always announce the latest item.
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    private fun applyTone(tone: VoiceTone) {
        if (tone == appliedTone) return
        tts.setPitch(tone.pitch)
        tts.setSpeechRate(tone.rate)
        appliedTone = tone
    }

    fun shutdown() {
        _isReady.value = false
        tts.shutdown()
    }

    private companion object {
        const val UTTERANCE_ID = "jaskier"
    }
}
