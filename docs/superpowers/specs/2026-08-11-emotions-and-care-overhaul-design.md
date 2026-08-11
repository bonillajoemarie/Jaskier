# Emotions and care-routine overhaul

Date: 2026-08-11
Spec 4 of 4. Build order: karaoke drag → hydration → shared Kerker art → **this**.

## Goal

Two halves, both about making Kerker feel alive and easy for a small child:

1. **Emotions** — he cries when a need gets bad, and shows excited, laughing, sleepy, and bored
   states the rest of the time.
2. **Care overhaul** — the Brush and Shower routines are too hard for a toddler. Rebuild them (and
   Feed) around the tap-first interaction contract from Spec 2.

Both depend on Spec 3's `KerkerArt`, so every expression lands in all six scenes at once.

## Part 1 — Emotions

### Crying as a severity of need

Crying is not a new mood; it is the severe end of an existing one. In `pet/PetStats.kt`:

```kotlin
data class Feeling(val mood: PetMood, val crying: Boolean)
fun feelingOf(hunger: Float, hydration: Float, cleanliness: Float, teeth: Float): Feeling
```

`crying` is true when the stat driving the mood is below `SICK_THRESHOLD` (30), and always when
the mood is `SICK`. Between 30 and 45 he keeps today's sad expression. `PetStats.mood` stays for
existing callers; `feeling` is the richer accessor. All of this stays pure and unit-tested.

**Emotional-safety constraint** (from `kids-ux`: the character must never appear to suffer, only
to be recoverable):

- Crying is a *cartoon* waah — big comic tears, a wobbly lip, over in a moment. Never sobbing,
  never sustained distress.
- It stops the instant the kid helps, replaced by the excited celebration.
- Kerker never guilt-trips. Voice lines are needs ("I'm so thirsty!"), never blame ("you left me
  all alone"). The same rule governs the bored state — charming, never nagging.
- The `AWAY_FLOOR` guarantee is unchanged: he is always recoverable, and a returning kid gets a
  warm welcome regardless of how long they were away.

### Transient emotions

These are not persisted needs, so they live in the UI layer (`PetViewModel` / `PetHomeScreen`), not
in `PetStats`:

| Emotion | Trigger | Expression |
|---|---|---|
| Excited | ~3s after any care action or `PetEvent` | jump with volume-preserving squash, sparkles, "Yay!" |
| Laughing | tickle or a body-zone tap (Spec 3) | eyes squeezed shut, shaking, laugh lines |
| Sleepy | evening by device clock, only when no need is unmet | droopy lids, yawn, floating Zzz, slower breathing |
| Bored | ~25s with no touch, only when otherwise happy | sighs, glances around, taps a foot |

**Priority**, highest first: crying/sick → unmet-need moods → excited → laughing → sleepy → bored →
happy. A distressed Kerker therefore never looks bored or sleepy, and a celebration always wins over
a need for its brief duration.

Each state maps to a `KerkerLook` from Spec 3, so it renders identically in every scene, and each
has a rotating set of TTS lines so repetition stays fresh.

## Part 2 — Care-routine overhaul

### Why it is hard today

Measured from the code: brushing requires six separate teeth, each held for 450ms inside a
0.13×`minDimension` radius, after squeezing paste for 900ms inside 0.22. Showering requires three
dirt spots, each held 1300ms inside 0.17. Every step is a sustained drag — the hardest gesture for a
2-year-old — and a tap that misses a target does nothing at all.

### Fixes, applied to Brush, Shower, and Feed

**Tap-first** (the main fix). Every step completable with taps alone:

- Brush: tap a tooth → the brush flies over, scrubs, the tooth goes white. Tap the tube → paste
  lands on the brush.
- Shower: tap the knob (already a tap) → water. Tap a dirt spot → the soap flies to it and bubbles
  it away.
- Feed: tap a food → it lands on the plate. Tap Kerker's mouth → the spoon delivers a bite.

Dragging keeps working exactly as it does now for kids who prefer it. Neither path is required.

**Tuning for the drag path**, for kids who do drag:

| Knob | Now | After |
|---|---|---|
| Brush hit radius | 0.13 | 0.20 |
| Soap hit radius | 0.17 | 0.26 |
| Per-tooth fill | 450ms | 300ms |
| Per-dirt-spot fill | 1300ms | 800ms |
| Paste fill | 900ms | 600ms |

Plus: **neighbour bleed** — scrubbing near a tooth credits its neighbours at 40% rate, so dragging
across the mouth makes visible progress everywhere; **snap-to-clean** at 85%, removing the
last-few-percent hunt; and a **motion trickle** so a tool held and moving anywhere on the body always
makes some progress.

**No silent taps.** Any tap anywhere on any care screen gives a sparkle and a soft pop; near a
target it also speaks a nudge.

**Ghost-hand demo.** After ~3s without progress on a step, a translucent hand animates the gesture
on a loop until the kid acts. After ~6s the spoken instruction repeats and the remaining target
pulses with an arrow.

**Picture progress.** A filling row of stars along the top of each routine replaces any implicit
sense of progress. No numbers.

**Targets and timing.** All controls audited to ≥64dp with ≥12dp spacing. Tap response under
100ms, celebrations 300–900ms, never blocking input.

## Testing

`PetStatsTest` extends to cover `feelingOf`: crying below 30 for each of the four stats, not crying
between 30 and 45, always crying when sick, and mood ordering unchanged.

New `EmotionPriorityTest` covers the pure priority resolver (a function from feeling + transient
flags to the displayed emotion), including that distress beats sleepy and bored.

New `CareProgressTest` covers the extracted scrub-progress helper: neighbour bleed, snap-to-clean at
85%, the motion trickle, and monotonicity (progress never decreases).

Compose-side behaviour is verified by building and running each routine.

## Out of scope

- New care routines beyond the Drink screen already specified in Spec 2
- Persisting transient emotions across app restarts
- Any change to the mini-games beyond the expressions they inherit from `KerkerArt`
