# Shared Kerker art, 3D-look shading, and touch reactions

Date: 2026-08-11
Spec 3 of 4. Build order: karaoke drag → hydration → **this** → emotions & care overhaul.

## Goal

Kerker is currently drawn six separate times, each a hand-rolled copy with its own duplicated skin
palette and geometry:

| Scene | Function |
|---|---|
| Home | `pet/PetCanvas.kt` — full pet |
| Catch game | `minigames/CatchGameScreen.kt` — compact, mouth always open |
| Karaoke | `minigames/KaraokeView.kt` — mini dancer |
| Shower | `care/ShowerScreen.kt` — wet Kerker |
| Feed | `care/FeedScreen.kt` — eating Kerker |
| Brush | `care/BrushScreen.kt` — giant zoomed head |

Unify them behind one drawing module, raise the art to a convincing 3D look, and add Talking
Tom-style tap reactions on four body zones. Every improvement must land in all six scenes at once.

## Reference and originality

Talking Tom is the reference for *production quality* — a big expressive character, soft 3D
shading, squash-and-stretch, reactions everywhere you touch. It is explicitly **not** a reference
for character design. Kerker stays original, the same call already made about Pou: no borrowing of
Tom's species, proportions, face, colors, or trade dress.

"3D" here means a 3D **look** rendered with 2D Compose Canvas. No 3D engine, no models, no new
dependencies, no app-size cost. This was chosen over Filament/SceneView and over a game engine
because a rigged model plus an animation clip per emotion is an art pipeline, not a coding task.

## 1. `pet/KerkerArt.kt` (new)

One module owning the character. Everything keys off a center and a radius so the same code draws
the mini karaoke dancer and the giant brush-screen head.

**Palette** — the constants currently copy-pasted across six files, defined once: skin base,
highlight, and shade; hair dark and hair highlight; tank-top white and its shade; mouth interior;
tooth white and tooth yellow. Per `game-art`, the character stays at 2–3 body colors with one
accent reserved for state changes.

**Building blocks**, each a `DrawScope` extension:

- `kerkerShadow(...)` — soft contact shadow that tracks height and squash
- `kerkerHead(...)` — one Bezier body path, not forty detail paths, with layered shading
- `kerkerHair(...)` — curls and wisps
- `kerkerEyes(...)` — glossy pupils with catchlights, blink amount, gaze direction, lid droop
- `kerkerMouth(...)` — variants: smile, open, wail, gulp, chew, laugh
- `kerkerBody(...)` — tank top, stubby arms and feet
- `kerkerTeeth(...)` — individual crowns under a gum line, per-tooth cleanliness (BrushScreen)

Draw order is fixed and documented: shadow → body → head → markings → eyes → mouth → overlays
(dirt, bubbles, water, tears). Every scene calls these and adds only its own extras — bubbles and
droplets, the toothbrush zoom, falling tiles, the karaoke bop and tumble.

**Expression parameters** are grouped in one `KerkerLook` data class (mouth variant, brow angle,
eye state, tint, squash, rotation, overlays) so a new expression is one value away from appearing
in every scene.

## 2. The 3D look

Applied inside `KerkerArt` so all six scenes inherit it:

- **Key light and rim light** — a warm radial key from the upper left, and a cool rim along the
  opposite edge of the head and body that separates him from the background
- **Ambient occlusion** — soft darkening under the chin, inside the arm joins, and where the hair
  meets the scalp; this is what sells volume more than any highlight
- **Cast shadow** — the contact shadow scales and softens with height rather than staying fixed
- **Specular** — glossy catchlights on the eyes, a soft sheen on the hair and the tank top
- **Perspective** — slight scaling toward the viewer on lean and hop, so motion reads in depth
- **Parallax** — scene backgrounds shift slightly against Kerker's position
- **Volume-preserving squash-and-stretch** — width and height scale inversely, never uniformly

All geometry stays proportional to `min(width, height)`, so it is resolution independent.

## 3. Idle life

A character that freezes reads as dead. Driven from a few animated floats:

- breathing bob on a 1.5s sine cycle
- blink every 3–5s, occasionally a double blink
- occasional glance to the side, then back
- micro-sway of the curls trailing the head motion (follow-through)

## 4. Touch reactions

Four hit zones derived from the existing anchors (`headC()`, `headR()`, `mouthAnchor()`,
`feetY()`), each with anticipation (50–150ms wind-up), a reaction, follow-through overshoot, and
its own rotating TTS line so repeat taps stay fresh:

| Zone | Reaction |
|---|---|
| Head / hair | leans into the pat, curls compress and spring back, happy hum |
| Cheeks | that side of the face squishes, the eye on that side squints, muffled squeak |
| Belly | doubles over, wobbles, "Oof! Hehe!" |
| Feet | hops with legs tucked, laughing |

Plus a **tickle** gesture: a rub with several direction reversals inside a short window makes him
laugh and squirm. Detected from direction changes over time, not event counts.

Tap response begins under 100ms, reactions run 300–900ms, and none of them block further input —
a kid mashing the screen gets a reaction every time. Zones are generous and overlapping-safe; a
tap that hits no zone still gets the plain squish, hearts, and giggle that exist today.

`PetCanvas` exposes these through one `onTouchZone(zone: KerkerZone)` callback, and
`PetHomeScreen` wires the voice lines, mirroring how `onPoke` works today.

## 5. Scene migration

Each of the six scenes is converted to call `KerkerArt`, keeping its current silhouette and
behaviour but gaining the shading, idle life, and expressions. The karaoke dancer additionally
keeps its physics from Spec 1; the brush screen keeps its zoom by passing a large radius.

## Testing

The drawing code has no pure logic worth unit-testing, consistent with the rest of the app's
Canvas work. What *is* testable is extracted and tested: the tickle detector (direction reversals
within a window) and the zone hit-testing (`KerkerZone.at(offset, center, radius)`) go in
`KerkerArt`'s companion logic with a `KerkerTouchTest`. Visual results are verified by building and
running each of the six scenes.

## Out of scope

- Any 3D engine, model format, or new rendering dependency
- Emotional states and crying — that is Spec 4
- New scenes; this spec only unifies and upgrades existing ones
