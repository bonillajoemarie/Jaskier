package com.example.jaskier.pet

// What Kerker is feeling right now, and how hard he is feeling it.
//
// Crying is not a new mood: it is the severe end of an existing need. Kids-UX
// rule that governs all of this — the character must never appear to *suffer*,
// only to be recoverable. Crying is a cartoon waah that stops the instant the
// kid helps, and no line ever blames the kid for being away.

/** A mood plus its severity. */
data class Feeling(val mood: PetMood, val crying: Boolean)

fun feelingOf(hunger: Float, hydration: Float, cleanliness: Float, teeth: Float): Feeling {
    val mood = moodOf(hunger, hydration, cleanliness, teeth)
    val driver = when (mood) {
        PetMood.HUNGRY -> hunger
        PetMood.THIRSTY -> hydration
        PetMood.DIRTY -> cleanliness
        PetMood.YUCKY_TEETH -> teeth
        PetMood.SICK -> 0f // being ill always warrants tears
        PetMood.HAPPY -> STAT_MAX
    }
    return Feeling(mood, crying = driver < SICK_THRESHOLD)
}

val PetStats.feeling: Feeling get() = feelingOf(hunger, hydration, cleanliness, teeth)

/**
 * Short-lived feelings that are not persisted needs. They live in the UI layer
 * but the priority rule is pure, so it can be tested.
 */
enum class Emotion { CRYING, NEEDY, EXCITED, LAUGHING, SLEEPY, BORED, HAPPY }

/**
 * Resolves what Kerker shows. Distress always wins: a hungry pet must never
 * look bored or sleepy, or the kid gets no signal about what to fix.
 */
fun emotionOf(
    feeling: Feeling,
    excited: Boolean = false,
    laughing: Boolean = false,
    sleepy: Boolean = false,
    bored: Boolean = false,
): Emotion = when {
    feeling.crying -> Emotion.CRYING
    feeling.mood != PetMood.HAPPY -> Emotion.NEEDY
    excited -> Emotion.EXCITED
    laughing -> Emotion.LAUGHING
    sleepy -> Emotion.SLEEPY
    bored -> Emotion.BORED
    else -> Emotion.HAPPY
}

/** Evening by the device clock, when a small kid is winding down anyway. */
fun isSleepyHour(hourOfDay: Int): Boolean = hourOfDay >= 19 || hourOfDay < 6
