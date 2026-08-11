package com.example.jaskier

import com.example.jaskier.pet.Emotion
import com.example.jaskier.pet.Feeling
import com.example.jaskier.pet.PetMood
import com.example.jaskier.pet.emotionOf
import com.example.jaskier.pet.feelingOf
import com.example.jaskier.pet.isSleepyHour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetFeelingTest {

    @Test
    fun `a mildly unmet need is sad, not tearful`() {
        val feeling = feelingOf(hunger = 40f, hydration = 100f, cleanliness = 100f, teeth = 100f)
        assertEquals(PetMood.HUNGRY, feeling.mood)
        assertFalse("40 is sad, not crying", feeling.crying)
    }

    @Test
    fun `each need brings tears once it gets bad`() {
        assertTrue(feelingOf(25f, 100f, 100f, 100f).crying)
        assertTrue(feelingOf(100f, 25f, 100f, 100f).crying)
        assertTrue(feelingOf(100f, 100f, 25f, 100f).crying)
        assertTrue(feelingOf(100f, 100f, 100f, 25f).crying)
    }

    @Test
    fun `being sick always cries`() {
        val feeling = feelingOf(25f, 25f, 100f, 100f)
        assertEquals(PetMood.SICK, feeling.mood)
        assertTrue(feeling.crying)
    }

    @Test
    fun `a happy pet never cries`() {
        val feeling = feelingOf(100f, 100f, 100f, 100f)
        assertEquals(PetMood.HAPPY, feeling.mood)
        assertFalse(feeling.crying)
    }

    @Test
    fun `crying outranks every other feeling`() {
        val crying = Feeling(PetMood.HUNGRY, crying = true)
        assertEquals(
            Emotion.CRYING,
            emotionOf(crying, excited = true, laughing = true, sleepy = true, bored = true),
        )
    }

    @Test
    fun `an unmet need outranks sleepy and bored`() {
        val needy = Feeling(PetMood.THIRSTY, crying = false)
        assertEquals(Emotion.NEEDY, emotionOf(needy, sleepy = true, bored = true))
    }

    @Test
    fun `celebrating wins over laughing, sleepy and bored`() {
        val fine = Feeling(PetMood.HAPPY, crying = false)
        assertEquals(
            Emotion.EXCITED,
            emotionOf(fine, excited = true, laughing = true, sleepy = true, bored = true),
        )
    }

    @Test
    fun `boredom is the last thing he shows`() {
        val fine = Feeling(PetMood.HAPPY, crying = false)
        assertEquals(Emotion.BORED, emotionOf(fine, bored = true))
        assertEquals(Emotion.SLEEPY, emotionOf(fine, sleepy = true, bored = true))
        assertEquals(Emotion.HAPPY, emotionOf(fine))
    }

    @Test
    fun `evenings and nights are sleepy, daytime is not`() {
        assertTrue(isSleepyHour(21))
        assertTrue(isSleepyHour(2))
        assertFalse(isSleepyHour(10))
        assertFalse(isSleepyHour(18))
    }
}
