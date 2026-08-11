# Hydration stat, drink routine, and food variety

Date: 2026-08-11
Spec 2 of 4. Build order: karaoke drag → **this** → shared Kerker art → emotions & care overhaul.

## Goal

Kerker has three needs today (hunger, cleanliness, teeth). Add a fourth — **hydration** — with its
own care routine, including a baby-bottle routine where the kid mixes water and formula powder,
shakes the bottle, and feeds it to Kerker. Also grow the Feed screen from 4 foods to 10 distinct
ones, including meat and milk.

## Interaction contract for care screens

This contract is established here and applied retroactively to Feed/Shower/Brush in Spec 4. It
comes from the `kids-ux` skill: a 2-year-old cannot reliably sustain a drag, and silence reads as
a broken app.

1. **Tap-first.** Every step must be completable with taps alone. Tapping a target makes the tool
   fly to it and perform the action. Dragging remains supported for kids who prefer it, but is
   never required, and no step may depend on a gesture more complex than a tap or a drag.
2. **No silent taps.** A tap anywhere produces a sparkle and a soft pop. A tap near a target also
   speaks a nudge.
3. **Demonstrate, don't tell.** After ~3s without progress on a step, a translucent ghost hand
   animates the gesture on a loop until the kid acts. Written instructions stay for parents, but
   the routine must be completable with the screen muted and unread.
4. **Targets ≥64dp with ≥12dp spacing.**
5. **Celebrations 300–900ms, spammable, never blocking input.** Tap response under 100ms.
6. **No fail state, no timer, no precision requirement.** Progress only ever increases.
7. **Progress is a picture** — a filling row of stars — never a number.

## Hydration stat

`pet/PetStats.kt`:

- `HYDRATION_FULL_TO_FLOOR_HOURS = 10f` — thirst outpaces hunger (16h), matching real need and
  giving the kid a reason to visit the new screen.
- `hydration: Float = STAT_MAX` on `PetStats`, included in `decayedTo`, `medicined()`, and the
  sickness count, which becomes "2 or more of 4 stats below `SICK_THRESHOLD`".
- `PetMood.THIRSTY`, evaluated directly after `HUNGRY` in `moodOf` — hunger keeps its tie-break
  priority, thirst takes the next.
- `fun PetStats.drank(): PetStats` sets hydration to `STAT_MAX`.
- `fun PetStats.bottleFed(): PetStats` sets hydration to `STAT_MAX` and adds `BOTTLE_FEED_AMOUNT`
  (40f) to hunger — milk is both drink and food.

`pet/PetRepository.kt` gets a `hydration` preferences key. A missing key reads as `STAT_MAX`, so
existing saved pets migrate without appearing parched. `PetViewModel` gains `drink()` and
`bottleFeed()` actions and `PetEvent.DRANK`.

`pet/PetHomeScreen.kt` gains a fourth care button (💧 Water) routing to `Screen.Care("drink")`,
with the four buttons re-laid-out to keep ≥64dp targets and ≥12dp spacing.

## `care/DrinkScreen.kt` (new)

Opens on a two-choice picker — a **cup of water** and a **baby bottle** — both large, saturated,
and spoken aloud on entry. Back returns home; picking one enters that routine.

### Cup of water

1. Tap the faucet → water pours, the cup fills with a rising water level and a pouring sound.
2. Tap Kerker's mouth (or drag the cup there) → he tips it back and gulps in stages, the level
   dropping with each gulp and a spoken "Glug glug!".
3. Empty → celebration, `drink()`, auto-return home.

### Baby bottle

1. Tap the faucet → the bottle fills with water.
2. Tap the formula tin → a scoop of powder drops in, the water clouding slightly. Three scoops,
   each with its own sound; tapping the tin again is always safe.
3. Tap the cap → it screws on with a satisfying click.
4. **Shake**: tapping the bottle repeatedly shakes it, and the device accelerometer also counts if
   the kid physically shakes the phone — but tapping alone always suffices, per the contract. Each
   shake swirls the liquid; after enough shaking the contents turn opaque creamy white.
5. Tap Kerker's mouth → he cradles the bottle and drinks in rhythmic gulps with contented eyes, the
   level dropping.
6. Empty → burp, sparkles, `bottleFeed()`, auto-return home.

Hold-style progress anywhere in this screen accumulates `change.uptimeMillis` deltas (time-in-zone),
never event counts, per the repo rule.

## Food variety

`care/FeedScreen.kt`'s `FoodChoice` grows a `shape` so foods are distinguishable by silhouette, not
just color — a colored blob cannot tell meat from berry. Ten foods, each with its own code-drawn
shape, spoken name, and chew sound:

| Food | Shape | Hunger |
|---|---|---|
| Apple | round with stem and leaf | 30 |
| Banana | crescent | 30 |
| Berry | small cluster of spheres | 25 |
| Broccoli | stalk with bumpy crown | 25 |
| Carrot | tapered cone with fronds | 25 |
| Bread | rounded loaf with a scored top | 30 |
| Egg | oval with a cracked-shell cap | 30 |
| Meat | drumstick — bone plus rounded meat | 40 |
| Milk | glass with a white fill and highlight | 30, plus 25 hydration |
| Cookie | disc with chocolate chips | 20 |

`FEED_AMOUNT` becomes per-food rather than a single constant: `FoodChoice` carries `hunger` and
`hydration` amounts, and `fed()` takes both (`fed(hunger: Float, hydration: Float = 0f)`), each
clamped at `STAT_MAX`. Milk is the only food with a non-zero hydration amount.
Milk is the one food that also touches hydration, which is why it lives on the Feed screen rather
than the Drink screen. The food tray scrolls horizontally with ≥64dp targets, and per the `game-art`
palette rule the foods are saturated against a muted kitchen background so the eye goes to what is
tappable.

## Testing

`PetStatsTest` extends to cover: hydration decay rate and floor, thirst ordering behind hunger in
`moodOf`, the 4-stat sickness threshold, `drank()`, `bottleFed()` raising both stats with hunger
clamped at `STAT_MAX`, and `medicined()` lifting hydration. Repository migration (missing key reads
as full) is covered by a test on the default value.

Compose-side interaction is verified by building and running, consistent with the rest of the app.

## Out of scope

- Rebuilding the existing Feed/Shower/Brush interactions to the tap-first contract — that is Spec 4.
- Emotional reactions to thirst — that is Spec 4.
- Any change to Kerker's rendering — that is Spec 3.
