package com.example.jaskier.pet

const val STAT_MAX = 100f

// While the kid is away, stats never drop below this floor: the pet gets
// hungry, grubby, and yucky-toothed but never miserable or dead.
const val AWAY_FLOOR = 20f

const val HUNGER_FULL_TO_FLOOR_HOURS = 16f
const val CLEAN_FULL_TO_FLOOR_HOURS = 24f
const val TEETH_FULL_TO_FLOOR_HOURS = 12f // brush roughly twice a day, like real teeth

const val FEED_AMOUNT = 30f

// Below this a stat starts affecting the pet's mood.
const val MOOD_THRESHOLD = 45f

// Tamagotchi-style sickness: neglect at least two needs badly (below this)
// and the pet falls ill. Medicine lifts every low stat back up.
const val SICK_THRESHOLD = 30f
const val MEDICINE_RECOVERY = 55f

enum class PetMood { HAPPY, HUNGRY, DIRTY, YUCKY_TEETH, SICK }

data class PetStats(
    val hunger: Float = STAT_MAX,
    val cleanliness: Float = STAT_MAX,
    val teeth: Float = STAT_MAX,
    val lastUpdatedMillis: Long = 0L,
) {
    val mood: PetMood get() = moodOf(hunger, cleanliness, teeth)
}

fun applyDecay(value: Float, elapsedMillis: Long, fullToFloorHours: Float): Float {
    // Guards against clock skew: never decay backwards in time.
    if (elapsedMillis <= 0) return value
    val ratePerMs = (STAT_MAX - AWAY_FLOOR) / (fullToFloorHours * 3_600_000f)
    return (value - elapsedMillis * ratePerMs).coerceIn(AWAY_FLOOR, STAT_MAX)
}

fun PetStats.decayedTo(nowMillis: Long): PetStats {
    val elapsed = nowMillis - lastUpdatedMillis
    return copy(
        hunger = applyDecay(hunger, elapsed, HUNGER_FULL_TO_FLOOR_HOURS),
        cleanliness = applyDecay(cleanliness, elapsed, CLEAN_FULL_TO_FLOOR_HOURS),
        teeth = applyDecay(teeth, elapsed, TEETH_FULL_TO_FLOOR_HOURS),
        lastUpdatedMillis = maxOf(nowMillis, lastUpdatedMillis),
    )
}

fun PetStats.fed(): PetStats = copy(hunger = (hunger + FEED_AMOUNT).coerceAtMost(STAT_MAX))

fun PetStats.showered(): PetStats = copy(cleanliness = STAT_MAX)

fun PetStats.brushed(): PetStats = copy(teeth = STAT_MAX)

fun PetStats.medicined(): PetStats = copy(
    hunger = hunger.coerceAtLeast(MEDICINE_RECOVERY),
    cleanliness = cleanliness.coerceAtLeast(MEDICINE_RECOVERY),
    teeth = teeth.coerceAtLeast(MEDICINE_RECOVERY),
)

fun moodOf(hunger: Float, cleanliness: Float, teeth: Float): PetMood {
    val neglected = listOf(hunger, cleanliness, teeth).count { it < SICK_THRESHOLD }
    return when {
        neglected >= 2 -> PetMood.SICK
        // Hunger wins ties: feeding is the clearer call to action for a kid.
        hunger < MOOD_THRESHOLD -> PetMood.HUNGRY
        cleanliness < MOOD_THRESHOLD -> PetMood.DIRTY
        teeth < MOOD_THRESHOLD -> PetMood.YUCKY_TEETH
        else -> PetMood.HAPPY
    }
}
