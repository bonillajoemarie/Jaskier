package com.example.jaskier.speech

/**
 * How a line is delivered. A flat synthesiser is what makes an app sound
 * mechanical; varying pitch and pace with the moment is most of the fix.
 *
 * Android accepts 0.5..2.0 for both pitch and rate.
 */
enum class VoiceTone(val pitch: Float, val rate: Float) {
    /** Everyday narration: a small character, at the established kid-friendly pace. */
    NORMAL(1.15f, 0.80f),

    /** Fed, washed, brushed, celebrating. */
    EXCITED(1.30f, 0.95f),

    /** Hungry, thirsty, grubby — droopy, not miserable. */
    SAD(1.00f, 0.70f),

    /** Wobbly and slow. Cartoon upset, never distress. */
    CRYING(1.22f, 0.65f),

    /** Yawning, winding down. */
    SLEEPY(0.95f, 0.60f),

    /** Pokes and tickles. */
    GIGGLY(1.35f, 1.00f),
}
