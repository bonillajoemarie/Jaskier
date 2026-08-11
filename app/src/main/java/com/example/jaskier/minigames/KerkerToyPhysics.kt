package com.example.jaskier.minigames

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs

// Kerker as a physical toy: the kid flings him and he tumbles until he settles.
//
// Tunables are fractions of the playground height per second, so the toy behaves
// identically on every screen size. Pure Kotlin plus Compose geometry only — no
// Compose runtime, no Android types — so it runs on the JVM under unit test.

/** Downward acceleration, in playground-heights per second squared. */
const val TOY_GRAVITY = 2.2f

/** Fraction of speed kept after hitting the floor. */
const val TOY_FLOOR_BOUNCE = 0.55f

/** Fraction of speed kept after hitting a wall or the ceiling. */
const val TOY_WALL_BOUNCE = 0.6f

/** Fraction of horizontal speed kept per floor contact — scuffing along the ground. */
const val TOY_FLOOR_FRICTION = 0.85f

/** Fraction of spin kept per bounce. */
const val TOY_SPIN_BOUNCE_DAMPING = 0.7f

/** Air resistance applied per second to both travel and spin. */
const val TOY_AIR_DRAG = 0.35f

/** Below this speed (playground-heights per second) while touching the floor, he settles. */
const val TOY_REST_SPEED = 0.35f

/** Below this spin (degrees per second) while touching the floor, he settles. */
const val TOY_REST_SPIN = 60f

data class ToyState(
    val pos: Offset = Offset.Zero,
    /** Pixels per second. */
    val vel: Offset = Offset.Zero,
    /** Degrees. */
    val angle: Float = 0f,
    /** Degrees per second. */
    val spin: Float = 0f,
    /** True once he has come to rest; a resting toy is left alone until flung again. */
    val resting: Boolean = true,
)

/**
 * Advances the toy by [dt] seconds inside [bounds], which constrains the toy's
 * **center** and must already be inset by the caller for the character's draw size.
 */
fun step(state: ToyState, dt: Float, bounds: Rect): ToyState {
    if (state.resting || dt <= 0f || bounds.width <= 0f || bounds.height <= 0f) return state

    val height = bounds.height
    val drag = (1f - TOY_AIR_DRAG * dt).coerceIn(0f, 1f)

    var vel = Offset(state.vel.x, state.vel.y + TOY_GRAVITY * height * dt) * drag
    var spin = state.spin * drag
    var pos = state.pos + vel * dt
    var angle = state.angle + spin * dt

    if (pos.x < bounds.left) {
        pos = Offset(bounds.left, pos.y)
        vel = Offset(-vel.x * TOY_WALL_BOUNCE, vel.y)
        spin *= TOY_SPIN_BOUNCE_DAMPING
    } else if (pos.x > bounds.right) {
        pos = Offset(bounds.right, pos.y)
        vel = Offset(-vel.x * TOY_WALL_BOUNCE, vel.y)
        spin *= TOY_SPIN_BOUNCE_DAMPING
    }

    if (pos.y < bounds.top) {
        pos = Offset(pos.x, bounds.top)
        vel = Offset(vel.x, -vel.y * TOY_WALL_BOUNCE)
        spin *= TOY_SPIN_BOUNCE_DAMPING
    }

    var onFloor = false
    if (pos.y > bounds.bottom) {
        pos = Offset(pos.x, bounds.bottom)
        vel = Offset(vel.x * TOY_FLOOR_FRICTION, -vel.y * TOY_FLOOR_BOUNCE)
        spin *= TOY_SPIN_BOUNCE_DAMPING
        onFloor = true
    }

    // Settle: once he is barely moving on the ground, stand him back up and stop
    // simulating. Without this he jitters on the floor forever.
    if (onFloor && vel.getDistance() < TOY_REST_SPEED * height && abs(spin) < TOY_REST_SPIN) {
        return ToyState(pos = pos, vel = Offset.Zero, angle = 0f, spin = 0f, resting = true)
    }

    return ToyState(pos = pos, vel = vel, angle = angle, spin = spin, resting = false)
}
