package com.example.jaskier.care

// Scrub progress, tuned for a two-year-old.
//
// The old numbers demanded six teeth each held 450ms inside a 0.13 radius, or
// three dirt spots each held 1300ms. That is a lot of precision for a toddler.
// These helpers forgive imprecision without removing the routine: targets are
// bigger, fills are faster, scrubbing near a target credits its neighbours, and
// nothing ever goes backwards.

/** Progress at or above this snaps clean, so no one hunts the last few percent. */
const val SNAP_TO_CLEAN = 0.85f

/** A neighbouring target earns this share of the rate. */
const val NEIGHBOUR_BLEED = 0.4f

/** Progress earned per millisecond just for moving the tool anywhere on the body. */
const val MOTION_TRICKLE_PER_MS = 1f / 6_000f

/**
 * New progress for one target.
 *
 * @param current progress so far, 0..1
 * @param dtMillis time the tool spent in this sample
 * @param fillMillis time-in-zone needed to clean a target outright
 * @param onTarget the tool is on this target
 * @param nearTarget the tool is on a neighbouring target
 * @param toolMoving the tool is held and moving anywhere on the body
 */
fun scrubProgress(
    current: Float,
    dtMillis: Float,
    fillMillis: Float,
    onTarget: Boolean,
    nearTarget: Boolean = false,
    toolMoving: Boolean = false,
): Float {
    if (current >= 1f) return 1f

    var gained = 0f
    if (onTarget) gained += dtMillis / fillMillis
    else if (nearTarget) gained += dtMillis / fillMillis * NEIGHBOUR_BLEED
    if (toolMoving) gained += dtMillis * MOTION_TRICKLE_PER_MS

    val next = (current + gained).coerceIn(0f, 1f)
    // Snap home rather than leaving a sliver that needs precision to finish.
    return if (next >= SNAP_TO_CLEAN) 1f else next
}
