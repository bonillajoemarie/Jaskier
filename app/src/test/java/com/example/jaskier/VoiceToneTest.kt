package com.example.jaskier

import com.example.jaskier.speech.VoiceTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceToneTest {

    @Test
    fun `every tone is within the range Android accepts`() {
        for (tone in VoiceTone.entries) {
            assertTrue("${tone.name} pitch ${tone.pitch}", tone.pitch in 0.5f..2.0f)
            assertTrue("${tone.name} rate ${tone.rate}", tone.rate in 0.5f..2.0f)
        }
    }

    @Test
    fun `normal narration keeps the established kid-friendly pace`() {
        assertEquals(0.8f, VoiceTone.NORMAL.rate, 0.0001f)
    }

    @Test
    fun `normal sits above default pitch so he sounds like a small character`() {
        assertTrue(VoiceTone.NORMAL.pitch > 1.0f)
    }

    @Test
    fun `excitement is higher and faster than normal`() {
        assertTrue(VoiceTone.EXCITED.pitch > VoiceTone.NORMAL.pitch)
        assertTrue(VoiceTone.EXCITED.rate > VoiceTone.NORMAL.rate)
    }

    @Test
    fun `sleepy and sad are slower than normal`() {
        assertTrue(VoiceTone.SLEEPY.rate < VoiceTone.NORMAL.rate)
        assertTrue(VoiceTone.SAD.rate < VoiceTone.NORMAL.rate)
    }

    @Test
    fun `giggling is the highest and fastest tone`() {
        assertEquals(VoiceTone.GIGGLY, VoiceTone.entries.maxBy { it.pitch })
    }
}
