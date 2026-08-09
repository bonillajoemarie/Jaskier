---
name: kids-ux
description: UX design principles for young children's apps. Pre-reader interaction, touch, audio feedback, safety.
allowed-tools: Read, Write, Edit, Glob, Grep
---

# Kids' UX Design

> Principles for apps used by children ~2–7 years old (pre-readers and early readers).

---

## 1. Pre-Reader First

Assume the user cannot read. Words are decoration for parents.

| Instead of text... | Use |
|--------------------|-----|
| Labels | Icons + color coding |
| Instructions | Audio prompts, demonstration |
| Menus | Pictures of the destination |
| Confirmation dialogs | Just undo-ability |

Every interactive element should announce itself or its result with
sound. Audio *is* the UI.

---

## 2. Touch for Small Hands

| Rule | Value |
|------|-------|
| Minimum target | 64dp+ (adult minimum is 48dp) |
| Spacing between targets | ≥ 12dp — fat fingers drift |
| Gestures | Tap only; no long-press, double-tap, or multi-finger |
| Drag | Only with huge tolerance and snap assist |
| Timing | Never require speed or precision timing |

Kids rest fingers on screens: ignore edge touches where feasible and
never make accidental taps destructive.

---

## 3. Feedback & Reward

- **Every tap does something** — silence teaches kids the app is broken
- Reward action, not correctness: wrong answers get a gentle sound, never a buzzer
- Animations: instant response (<100ms), celebration ≤1s, spammable
- Progress must be visible as *pictures* (filling bar, growing plant), not numbers

### Emotional Safety

| ❌ Never | ✅ Instead |
|----------|-----------|
| Fail states, game over | Degrees of success |
| Character dies/suffers | Character gets sleepy/grubby, always recoverable |
| Time pressure | Self-paced everything |
| Dark patterns (streak guilt) | Warm welcome-back regardless of absence |

---

## 4. Learning Design

- One concept per interaction (tap letter → hear letter; nothing else)
- Repetition is a feature: kids replay the same content dozens of times
- Announce with a friendly, slightly slow voice (~0.8× speech rate)
- Pair every symbol with sound and motion — multi-sensory encoding
- No scores or comparisons; mastery shows as fluency, not points

---

## 5. Parents & Safety

| Concern | Practice |
|---------|----------|
| Purchases/links | None reachable by kids, or behind a parent gate (math/hold puzzle) |
| Ads | Avoid entirely in kids' apps |
| Data | Collect nothing; COPPA/GDPR-K applies under 13 |
| Exit/settings | Small, corner-placed, low-contrast — boring to kids |
| Sessions | Natural stopping points; no infinite hooks |

App-store note: "Designed for Families" (Play) and "Kids" (App Store)
categories audit all of the above.

---

## 6. Anti-Patterns

| ❌ Don't | ✅ Do |
|----------|-------|
| Text instructions | Voice + demonstration |
| Small/dense controls | Few, huge, spaced controls |
| Punish mistakes | Make every outcome pleasant |
| Reward only success | Reward participation |
| Adult navigation depth | ≤ 2 taps to anything |

---

> **Remember:** A confused kid doesn't complain — they just leave. Watch a real child use it.
