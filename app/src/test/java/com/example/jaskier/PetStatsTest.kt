package com.example.jaskier

import com.example.jaskier.pet.AWAY_FLOOR
import com.example.jaskier.pet.BOTTLE_FEED_AMOUNT
import com.example.jaskier.pet.HUNGER_FULL_TO_FLOOR_HOURS
import com.example.jaskier.pet.MEDICINE_RECOVERY
import com.example.jaskier.pet.PetMood
import com.example.jaskier.pet.PetStats
import com.example.jaskier.pet.STAT_MAX
import com.example.jaskier.pet.applyDecay
import com.example.jaskier.pet.bottleFed
import com.example.jaskier.pet.brushed
import com.example.jaskier.pet.decayedTo
import com.example.jaskier.pet.drank
import com.example.jaskier.pet.fed
import com.example.jaskier.pet.medicined
import com.example.jaskier.pet.moodOf
import com.example.jaskier.pet.showered
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetStatsTest {

    private val hourMs = 3_600_000L

    @Test
    fun `zero elapsed leaves value unchanged`() {
        assertEquals(80f, applyDecay(80f, 0L, HUNGER_FULL_TO_FLOOR_HOURS), 0.0001f)
    }

    @Test
    fun `negative elapsed (clock moved back) leaves value unchanged`() {
        assertEquals(80f, applyDecay(80f, -hourMs, HUNGER_FULL_TO_FLOOR_HOURS), 0.0001f)
    }

    @Test
    fun `decays at expected rate`() {
        // 16h full-to-floor over an 80-point span = 5 points per hour.
        val after8h = applyDecay(STAT_MAX, 8 * hourMs, HUNGER_FULL_TO_FLOOR_HOURS)
        assertEquals(60f, after8h, 0.001f)
    }

    @Test
    fun `long absence clamps at floor - pet never dies`() {
        val after30days = applyDecay(STAT_MAX, 30 * 24 * hourMs, HUNGER_FULL_TO_FLOOR_HOURS)
        assertEquals(AWAY_FLOOR, after30days, 0.0001f)
    }

    @Test
    fun `decayedTo updates all stats and timestamp`() {
        val stats = PetStats(lastUpdatedMillis = 0L)
        val later = stats.decayedTo(8 * hourMs)
        assertEquals(60f, later.hunger, 0.001f)          // 5 pts/h
        assertEquals(73.333f, later.cleanliness, 0.01f)  // 80/24 pts/h
        assertEquals(46.666f, later.teeth, 0.01f)        // 80/12 pts/h
        assertEquals(8 * hourMs, later.lastUpdatedMillis)
    }

    @Test
    fun `decayedTo with past timestamp keeps newest timestamp`() {
        val stats = PetStats(hunger = 50f, cleanliness = 50f, teeth = 50f, lastUpdatedMillis = hourMs)
        val result = stats.decayedTo(0L)
        assertEquals(50f, result.hunger, 0.0001f)
        assertEquals(hourMs, result.lastUpdatedMillis)
    }

    @Test
    fun `feeding raises hunger and clamps at max`() {
        assertEquals(50f, PetStats(hunger = 20f).fed().hunger, 0.0001f)
        assertEquals(STAT_MAX, PetStats(hunger = 95f).fed().hunger, 0.0001f)
    }

    @Test
    fun `shower and brush restore their stats fully`() {
        assertEquals(STAT_MAX, PetStats(cleanliness = AWAY_FLOOR).showered().cleanliness, 0.0001f)
        assertEquals(STAT_MAX, PetStats(teeth = AWAY_FLOOR).brushed().teeth, 0.0001f)
    }

    @Test
    fun `medicine lifts every low stat to recovery level`() {
        val sick = PetStats(hunger = 20f, cleanliness = 25f, teeth = 90f).medicined()
        assertEquals(MEDICINE_RECOVERY, sick.hunger, 0.0001f)
        assertEquals(MEDICINE_RECOVERY, sick.cleanliness, 0.0001f)
        assertEquals(90f, sick.teeth, 0.0001f) // healthy stats keep their value
    }

    @Test
    fun `mood boundaries`() {
        assertEquals(PetMood.HUNGRY, moodOf(44.9f, 100f, 100f, 100f))
        assertEquals(PetMood.DIRTY, moodOf(100f, 100f, 44.9f, 100f))
        assertEquals(PetMood.YUCKY_TEETH, moodOf(100f, 100f, 100f, 44.9f))
        assertEquals(PetMood.HUNGRY, moodOf(44f, 100f, 44f, 100f)) // hunger wins ties
        assertEquals(PetMood.HAPPY, moodOf(70f, 70f, 70f, 70f))
        assertEquals(PetMood.HAPPY, moodOf(45f, 45f, 45f, 45f))   // threshold itself is still happy
    }

    @Test
    fun `sick when at least two needs are badly neglected`() {
        assertEquals(PetMood.SICK, moodOf(29f, 100f, 29f, 100f))
        assertEquals(PetMood.SICK, moodOf(29f, 100f, 100f, 29f))
        assertEquals(PetMood.SICK, moodOf(100f, 100f, 29f, 29f))
        assertEquals(PetMood.SICK, moodOf(20f, 20f, 20f, 20f))
        // One badly neglected need alone is not sickness.
        assertEquals(PetMood.HUNGRY, moodOf(29f, 100f, 100f, 100f))
        assertEquals(PetMood.SICK, PetStats(hunger = 25f, cleanliness = 25f, teeth = 80f).mood)
    }

    @Test
    fun `thirst outpaces hunger`() {
        val stats = PetStats(lastUpdatedMillis = 0L).decayedTo(5 * hourMs)
        assertTrue(
            "expected thirst (${stats.hydration}) below hunger (${stats.hunger})",
            stats.hydration < stats.hunger,
        )
        // 10h full-to-floor over an 80-point span = 8 points per hour.
        assertEquals(60f, stats.hydration, 0.01f)
    }

    @Test
    fun `thirst is announced right after hunger`() {
        assertEquals(PetMood.THIRSTY, moodOf(100f, 44.9f, 100f, 100f))
        // Hunger still wins when both are low.
        assertEquals(PetMood.HUNGRY, moodOf(44f, 44f, 100f, 100f))
        // ...but thirst beats a dirty body and yucky teeth.
        assertEquals(PetMood.THIRSTY, moodOf(100f, 44f, 44f, 44f))
    }

    @Test
    fun `sickness counts all four needs`() {
        assertEquals(PetMood.SICK, moodOf(100f, 29f, 29f, 100f))
        assertEquals(PetMood.SICK, moodOf(29f, 29f, 100f, 100f))
        // Thirst alone is not sickness.
        assertEquals(PetMood.THIRSTY, moodOf(100f, 29f, 100f, 100f))
    }

    @Test
    fun `drinking refills hydration completely`() {
        assertEquals(STAT_MAX, PetStats(hydration = AWAY_FLOOR).drank().hydration, 0.0001f)
    }

    @Test
    fun `a bottle is both a drink and a meal`() {
        val fedBottle = PetStats(hunger = 30f, hydration = AWAY_FLOOR).bottleFed()
        assertEquals(STAT_MAX, fedBottle.hydration, 0.0001f)
        assertEquals(30f + BOTTLE_FEED_AMOUNT, fedBottle.hunger, 0.0001f)
        // Hunger still clamps at the maximum.
        assertEquals(STAT_MAX, PetStats(hunger = 95f).bottleFed().hunger, 0.0001f)
    }

    @Test
    fun `milk feeds and hydrates at once`() {
        val after = PetStats(hunger = 40f, hydration = 40f).fed(hunger = 30f, hydration = 25f)
        assertEquals(70f, after.hunger, 0.0001f)
        assertEquals(65f, after.hydration, 0.0001f)
    }

    @Test
    fun `medicine lifts hydration too`() {
        assertEquals(
            MEDICINE_RECOVERY,
            PetStats(hydration = 20f).medicined().hydration,
            0.0001f,
        )
    }

    @Test
    fun `long total neglect ends sick and medicine cures it`() {
        val neglected = PetStats(lastUpdatedMillis = 0L).decayedTo(3 * 24 * hourMs)
        assertEquals(PetMood.SICK, neglected.mood)
        val treated = neglected.medicined()
        assertEquals(PetMood.HAPPY, treated.mood)
    }
}
