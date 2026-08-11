package com.example.jaskier

import com.example.jaskier.speech.VoiceOption
import com.example.jaskier.speech.pickWarmestVoice
import com.example.jaskier.speech.scoreVoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceSelectionTest {

    private fun voice(
        name: String,
        language: String = "en",
        quality: Int = 300,
        latency: Int = 300,
        networkOnly: Boolean = false,
    ) = VoiceOption(name, language, quality, latency, networkOnly)

    @Test
    fun `a different language is never usable`() {
        assertTrue(scoreVoice(voice("de-DE-x-nfh", language = "de"), "en") < 0)
    }

    @Test
    fun `higher quality scores higher`() {
        val good = scoreVoice(voice("en-us-a", quality = 500), "en")
        val poor = scoreVoice(voice("en-us-b", quality = 100), "en")
        assertTrue("expected $good > $poor", good > poor)
    }

    @Test
    fun `lower latency scores higher`() {
        val quick = scoreVoice(voice("en-us-a", latency = 100), "en")
        val slow = scoreVoice(voice("en-us-b", latency = 500), "en")
        assertTrue("expected $quick > $slow", quick > slow)
    }

    @Test
    fun `a network-only voice loses to any local voice`() {
        // A kid tapping fast must never wait on the network, however nice it sounds.
        val network = scoreVoice(voice("en-us-x", quality = 500, networkOnly = true), "en")
        val local = scoreVoice(voice("en-us-y", quality = 100, networkOnly = false), "en")
        assertTrue("expected local $local to beat network $network", local > network)
    }

    @Test
    fun `a higher-register voice wins among otherwise equal voices`() {
        val warm = scoreVoice(voice("en-us-x-sfg#female_1"), "en")
        val plain = scoreVoice(voice("en-us-x-sfg#male_1"), "en")
        assertTrue("expected $warm > $plain", warm > plain)
    }

    @Test
    fun `picks the best usable voice`() {
        val best = voice("en-us-best#female_2", quality = 500, latency = 100)
        val options = listOf(
            voice("de-DE-x", language = "de", quality = 500, latency = 100),
            voice("en-us-meh", quality = 100, latency = 500),
            best,
            voice("en-us-network", quality = 500, latency = 100, networkOnly = true),
        )
        assertEquals(best, pickWarmestVoice(options, "en"))
    }

    @Test
    fun `no usable voice yields null so the caller keeps the default`() {
        assertNull(pickWarmestVoice(listOf(voice("de-DE-x", language = "de")), "en"))
        assertNull(pickWarmestVoice(emptyList(), "en"))
    }
}
