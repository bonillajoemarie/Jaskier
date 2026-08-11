package com.example.jaskier.pet

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

// Where a kid touched Kerker, and whether they are tickling him.
//
// Talking Tom's lesson is that touching different parts should do different
// things. The zones are deliberately generous and ordered head-first: a
// toddler aiming for the face should never be told they hit the belly.

enum class KerkerZone { HEAD, CHEEK_LEFT, CHEEK_RIGHT, BELLY, FEET, NONE }

/**
 * Which part of Kerker [at] landed on, given his head [center] and [headRadius].
 * The body hangs below the head, as it is drawn.
 */
fun zoneAt(at: Offset, center: Offset, headRadius: Float): KerkerZone {
    val dx = at.x - center.x
    val dy = at.y - center.y
    val r = headRadius

    // Hair and the top of the head.
    if (dy < -r * 0.35f && abs(dx) < r * 1.15f && dy > -r * 1.9f) return KerkerZone.HEAD

    // Cheeks: either side of the lower face.
    if (dy in (-r * 0.35f)..(r * 0.75f) && abs(dx) > r * 0.3f && abs(dx) < r * 1.15f) {
        return if (dx < 0) KerkerZone.CHEEK_LEFT else KerkerZone.CHEEK_RIGHT
    }

    // Tummy, just below the head.
    if (dy in (r * 0.75f)..(r * 2.1f) && abs(dx) < r * 1.0f) return KerkerZone.BELLY

    // Feet at the bottom.
    if (dy in (r * 2.1f)..(r * 3.0f) && abs(dx) < r * 1.0f) return KerkerZone.FEET

    // Anything else on the face still counts as a poke on the head.
    if ((at - center).getDistance() < r * 1.15f) return KerkerZone.HEAD

    return KerkerZone.NONE
}

/** Voice lines per zone, several each so repeat pokes stay fresh. */
fun linesFor(zone: KerkerZone): List<String> = when (zone) {
    KerkerZone.HEAD -> listOf("Mmm, pat pat!", "That feels nice!", "My curls are bouncy!")
    KerkerZone.CHEEK_LEFT, KerkerZone.CHEEK_RIGHT ->
        listOf("Squishy cheeks!", "Mmmph!", "Boop!")
    KerkerZone.BELLY -> listOf("Oof! Hehe!", "Not my tummy!", "That tickles my tummy!")
    KerkerZone.FEET -> listOf("Wheee! My feet!", "Hop hop!", "Eee, my toes!")
    KerkerZone.NONE -> listOf("Hehe!", "Hi hi!", "Hehehe!")
}

/**
 * Tickling is a rub, not a swipe: the finger has to change direction several
 * times without a long pause. Tracked by direction reversals rather than event
 * count, because event density varies wildly between real fingers and
 * synthetic input.
 */
class TickleDetector(
    private val reversalsNeeded: Int = 3,
    private val windowMillis: Long = 1_200L,
) {
    private var lastX: Float? = null
    private var lastDirection = 0
    private val reversalTimes = ArrayDeque<Long>()

    /** Feeds one drag sample. Returns true on the sample that completes a tickle. */
    fun onMove(x: Float, timeMillis: Long): Boolean {
        val previous = lastX
        lastX = x
        if (previous == null) return false

        val delta = x - previous
        // Ignore jitter: a real rub covers ground.
        if (abs(delta) < 2f) return false

        val direction = if (delta > 0) 1 else -1
        if (lastDirection != 0 && direction != lastDirection) {
            reversalTimes.addLast(timeMillis)
        }
        lastDirection = direction

        while (reversalTimes.isNotEmpty() && timeMillis - reversalTimes.first() > windowMillis) {
            reversalTimes.removeFirst()
        }

        if (reversalTimes.size >= reversalsNeeded) {
            reset()
            return true
        }
        return false
    }

    fun reset() {
        lastX = null
        lastDirection = 0
        reversalTimes.clear()
    }
}
