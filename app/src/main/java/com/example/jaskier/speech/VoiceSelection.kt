package com.example.jaskier.speech

// Picking a voice, kept free of Android types so it can be unit-tested.
//
// Android's default voice is rarely the best one installed. We score every voice
// the engine offers and take the warmest usable one, falling back to the default
// when nothing qualifies — the app must never go mute.

/** Mirrors android.speech.tts.Voice.QUALITY_* — higher is better. */
const val VOICE_QUALITY_HIGH = 400

/** Network-only voices stall when a kid taps fast, so they lose to any local voice. */
private const val NETWORK_PENALTY = 10_000

/** Nudge toward a higher register, which reads as warmer for this character. */
private const val HIGHER_REGISTER_BONUS = 150

private val HIGHER_REGISTER_MARKERS = listOf("female", "#f", "-f-", "_f_")

data class VoiceOption(
    val name: String,
    /** Language tag only, e.g. "en" — country variants are not a preference. */
    val language: String,
    /** android.speech.tts.Voice.getQuality(), 100..500. */
    val quality: Int,
    /** android.speech.tts.Voice.getLatency(), 100..500; lower is better. */
    val latency: Int,
    val isNetworkOnly: Boolean,
)

/** Higher is better. A negative score means the voice is unusable. */
fun scoreVoice(option: VoiceOption, language: String): Int {
    if (!option.language.equals(language, ignoreCase = true)) return -1

    var score = option.quality - option.latency
    if (option.isNetworkOnly) score -= NETWORK_PENALTY
    val name = option.name.lowercase()
    if (HIGHER_REGISTER_MARKERS.any { name.contains(it) }) score += HIGHER_REGISTER_BONUS
    if (option.quality >= VOICE_QUALITY_HIGH) score += 100
    return score
}

/** The warmest usable voice, or null to keep the engine's default. */
fun pickWarmestVoice(options: List<VoiceOption>, language: String): VoiceOption? =
    options.map { it to scoreVoice(it, language) }
        .filter { (_, score) -> score >= 0 }
        .maxByOrNull { (_, score) -> score }
        ?.first
