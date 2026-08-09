---
name: game-art
description: Game visual design principles. Style, color, character design, animation feel, code-drawn art.
allowed-tools: Read, Write, Edit, Glob, Grep
---

# Game Art & Visual Design

> Principles for readable, charming game visuals.

---

## 1. Style Selection

| Style | Cost | Best For |
|-------|------|----------|
| **Code-drawn (Canvas/vector)** | Low | Simple mascots, UI-heavy games, tiny teams |
| **Flat 2D sprites** | Medium | Most 2D games |
| **Pixel art** | Medium | Retro identity; hides low detail |
| **3D** | High | Only when gameplay demands it |

**Decision rule:** Pick the cheapest style that expresses the game's
personality. Consistency beats fidelity — one coherent cheap style
outperforms mixed-quality assets.

---

## 2. Character/Mascot Design

- **Silhouette first**: recognizable as a black shape or it fails
- **One body, big eyes**: eye size ≈ emotion bandwidth; kids' characters run 2–4× realistic eye scale
- **2–3 body colors max**; accent color for mood/state changes
- **Expressions over anatomy**: mouth + eyebrows carry 90% of emotion
- Derive proportions from one dimension so the character scales anywhere

### Emotion States

| State | Cheapest Signal |
|-------|-----------------|
| Happy | Wide smile, raised cheeks |
| Sad/hungry | Drooped mouth arc + angled brows |
| Dirty/sick | Desaturate/tint body + overlay marks |
| Excited | Squash-stretch pop + particles |

---

## 3. Color

| Rule | Why |
|------|-----|
| 60/30/10 (ground/support/accent) | Prevents rainbow soup |
| Saturate interactables, mute scenery | Guides the eye to what's tappable |
| Reserve one hue per meaning (orange=food) | Color becomes language |
| Test on worst screen, brightest sun | Mobile reality |

Kids' palettes: high saturation is fine, but keep *values* distinct —
squint-test the screen; if it blurs to one tone, restructure.

---

## 4. Animation Feel (the 12 principles, game edition)

| Principle | Game Application |
|-----------|------------------|
| **Squash & stretch** | Impacts, bounces, jumps — never scale uniformly |
| **Anticipation** | Wind-up before action (50–150ms) |
| **Follow-through** | Overshoot then settle |
| **Secondary action** | Particles, ears, blinks layered on primary motion |
| **Ease in/out** | Nothing moves linearly except projectiles |

### Idle Life

Characters must never freeze: breathing bob (1–2s cycle), blink every
3–5s, occasional glance. Cheap sine waves read as "alive."

### Feedback Animation Budget

- Tap response: < 100ms or it feels dead
- Reward animation: 300–900ms — long enough to savor, short enough to spam
- Never block input on cosmetic animation

---

## 5. Code-Drawn Art (Canvas/vector)

- Build from few primitives: one Bezier body path beats 40 detail paths
- All geometry proportional to `min(width, height)` — resolution independent
- Gradients: one vertical two-stop gradient adds depth for free
- Layer order: body → markings → eyes → mouth → overlays (dirt, bubbles)
- Drive everything from a handful of animated floats (time, mood, stat)

---

## 6. Anti-Patterns

| ❌ Don't | ✅ Do |
|----------|-------|
| Mix asset styles | One style, strictly |
| Detail over silhouette | Shape first, detail last |
| Uniform-scale "animation" | Squash-stretch with volume |
| Frozen idle characters | Always-on micro-motion |
| Decorate every pixel | Leave rest areas for the eye |

---

> **Remember:** Readability is the art direction. Charm is a bonus you earn after clarity.
