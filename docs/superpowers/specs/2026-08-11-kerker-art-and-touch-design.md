# Shared Kerker art, soft-flat visual restyle, and touch reactions

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

Unify them behind one drawing module, restyle the whole app to a soft-flat vector look, and add
tap reactions on four body zones. Every improvement must land in all six scenes at once.

## References and originality

Two references, each for a different thing:

- **Toca Boca — the visual style and the interaction philosophy.** Flat chunky vector art, and the
  principle that every object on screen does something when touched.
- **Talking Tom — reaction ideas only.** Poke the belly and he doubles over; touch different body
  parts and get different responses.

Neither is a reference for character design. Kerker stays original — the same call already made
about Pou: no borrowing of any character's species, proportions, face, colors, or trade dress.
Visual *style* is not protected; specific characters and trade dress are, and we copy neither.

Rejected alternatives, recorded so they are not relitigated: a real 3D engine (Godot/Unity) or a
3D character renderer (Filament/SceneView) both require a rigged model with facial blend shapes —
an art pipeline, not a coding task, and free asset libraries do not ship faces. The earlier
"3D-look shading" direction (rim light, ambient occlusion, catchlights, specular) is **superseded**
by the soft-flat style below; the two cannot coexist.

## 1. The soft-flat style

The visual language for the entire app — Kerker, backgrounds, props, and UI:

- **Flat fills.** No gradients inside shapes, no glossy highlights, no rim light, no specular. Form
  reads from silhouette and value contrast alone.
- **One soft shadow per object.** A single blurred drop shadow offset down-right keeps a character
  readable against a busy background. This is the "soft" in soft-flat and the only depth cue.
- **Chunky rounded shapes.** Generous corner radii, thick forms, no thin details that vanish on a
  small screen.
- **Hand-drawn wobble.** Outlines and edges carry a slight deterministic irregularity (a fixed
  seed per shape, never random per frame — a jittering outline is nauseating) so shapes look drawn
  rather than computed.
- **Paper grain.** A subtle static noise overlay across flat areas, drawn once into a cached layer.
- **Warm saturated palette** with strong value separation. Per `game-art`: 60/30/10
  ground/support/accent, saturated interactables against muted scenery, and one hue reserved per
  meaning. Squint-test every scene; if it blurs to one tone, restructure.
- **Puppet-joint animation.** Limbs and the head pivot at joints with bouncy overshoot easing
  rather than smooth mesh-style deformation. Squash-and-stretch still preserves volume — width and
  height scale inversely, never uniformly.

All geometry stays proportional to `min(width, height)`, so it is resolution independent.

`CLAUDE.md`'s description of Kerker as "code-drawn, depth-shaded" is updated as part of this work.

## 2. `pet/KerkerArt.kt` (new)

One module owning the character. Everything keys off a center and a radius so the same code draws
the mini karaoke dancer and the giant brush-screen head.

**Palette** — the constants currently copy-pasted across six files, defined once: skin, skin shade,
hair, tank-top white and its shade, mouth interior, tooth white and tooth yellow. Per `game-art`
the character holds to 2–3 body colors with one accent reserved for state changes.

**Building blocks**, each a `DrawScope` extension:

- `kerkerShadow(...)` — the single soft drop shadow
- `kerkerHead(...)` — one Bezier body path, not forty detail paths
- `kerkerHair(...)` — chunky curl shapes
- `kerkerEyes(...)` — flat pupils with a single small catchlight dot, blink amount, gaze, lid droop
- `kerkerMouth(...)` — variants: smile, open, wail, gulp, chew, laugh
- `kerkerBody(...)` — tank top, stubby arms and feet, pivoting at joints
- `kerkerTeeth(...)` — individual crowns under a gum line, per-tooth cleanliness (BrushScreen)

Draw order is fixed and documented: shadow → body → head → markings → eyes → mouth → overlays
(dirt, bubbles, water, tears) → grain.

Mouth and brows carry ~90% of the emotion, per `game-art`, which is exactly why the flat style
costs nothing here: expression lives in shape, not shading. Eyes stay 2–4× realistic scale.

**Expression parameters** are grouped in one `KerkerLook` data class (mouth variant, brow angle,
eye state, tint, squash, joint angles, overlays) so a new expression is one value away from
appearing in every scene.

## 3. Idle life

A character that freezes reads as dead. Driven from a few animated floats:

- breathing bob on a 1.5s sine cycle
- blink every 3–5s, occasionally a double blink
- occasional glance to the side, then back
- curls trailing the head motion by a frame or two (follow-through)

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
a kid mashing the screen gets a reaction every time. Zones are generous and overlapping-safe; a tap
that hits no zone still gets the plain squish, hearts, and giggle that exist today.

`PetCanvas` exposes these through one `onTouchZone(zone: KerkerZone)` callback, and
`PetHomeScreen` wires the voice lines, mirroring how `onPoke` works today.

## 5. Everything reacts

Toca's defining property, and the direct answer to "it's not interactive enough": scenery is not
wallpaper. Each scene registers its props as tappable with a small reaction and a sound —
the shower knob and towel, the plate and cup, the toothbrush cup and mirror, the karaoke scenery
(barn, moon, clock, sheep), the sun and clouds on the home backdrop. Reactions are cosmetic only:
they never alter stats, never gate progress, and never block input.

Implemented as a lightweight `SceneProp` list per screen (hit shape, reaction animation, optional
sound or TTS name), so adding a reactive prop is a single entry.

## 6. Scene migration

Each of the six scenes is converted to call `KerkerArt` and restyled to soft-flat, keeping its
current layout and behaviour. The karaoke dancer additionally keeps its physics from Spec 1; the
brush screen keeps its zoom by passing a large radius. Backgrounds and props are restyled in the
same pass so no scene is left in the old shaded style — mixed styles are worse than either style.

## Testing

Drawing code has no pure logic worth unit-testing, consistent with the rest of the app's Canvas
work. What *is* testable is extracted and tested in `KerkerTouchTest`: the tickle detector
(direction reversals within a window), zone hit-testing (`KerkerZone.at(offset, center, radius)`),
and `SceneProp` hit-testing. The wobble seed is asserted deterministic — the same shape must
produce the same outline every frame. Visual results are verified by building and running all six
scenes.

## Out of scope

- Any 3D engine, model format, or new rendering dependency
- Emotional states and crying — that is Spec 4
- New scenes; this spec only unifies, restyles, and upgrades existing ones
