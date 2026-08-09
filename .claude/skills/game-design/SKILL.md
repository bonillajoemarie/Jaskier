---
name: game-design
description: Game design principles. Core loops, balancing, progression, player psychology.
allowed-tools: Read, Write, Edit, Glob, Grep
---

# Game Design

> Principles for designing games players want to return to.

---

## 1. The Core Loop

Every game is a loop the player repeats willingly:

```
ACTION → REWARD → UPGRADE/PROGRESS → new ACTION
```

**Design rule:** State your core loop in one sentence before building anything.
If you can't, the game has no spine.

| Loop length | Example | Purpose |
|-------------|---------|---------|
| Seconds | Tap letter → hear it | Moment-to-moment fun |
| Minutes | Care for pet → mood improves | Session goal |
| Days | Return → pet needs you again | Retention |

---

## 2. Motivation Models

| Driver | Mechanic | Use When |
|--------|----------|----------|
| **Competence** | Levels, mastery, streaks | Skill-based play |
| **Autonomy** | Choices, customization | Sandbox, dress-up |
| **Relatedness** | Pet/character bonds | Care games |
| **Curiosity** | Unlockables, surprises | Exploration |

**Care games** (pet sims) run on relatedness + light guilt: the pet
*needs* the player. Tune the guilt gently — motivating for adults,
distressing for kids.

---

## 3. Balancing

### Decay/Resource Tuning

| Question | Heuristic |
|----------|-----------|
| How fast to drain? | Player sees change between sessions, not within minutes |
| Punish absence? | Never make the player return to a lose state |
| Reward size? | One action = visible progress (≥25% of a bar) |

### Difficulty Curves

- Ramp challenge slower than you think (players are worse than devs)
- Failure should teach, not gate — retry instantly
- For kids: no fail states at all; only degrees of success

---

## 4. Progression Design

| Pattern | Description | Risk |
|---------|-------------|------|
| **Linear unlock** | Content gates by milestone | Boredom if too slow |
| **Collection** | Sets to complete | Grind if drop rates poor |
| **Mastery** | Same content, deeper skill | Needs great core loop |

**Decision rule:** Ship the core loop polished with tiny progression;
add systems only when players ask "what's next?"

---

## 5. GDD (Game Design Doc) — Minimum Viable

1. One-sentence pitch
2. Core loop diagram
3. Player verbs (feed, bathe, tap, ...)
4. Screens list + navigation map
5. Tuning table (all magic numbers in one place)

Keep it to one page. A GDD nobody updates is worse than none.

---

## 6. Anti-Patterns

| ❌ Don't | ✅ Do |
|----------|-------|
| Mechanics before loop | Loop first, mechanics serve it |
| Punish lapsed players | Welcome them back warmly |
| Tune by feel in code | Centralize tuning constants |
| Add systems to fix boredom | Fix the core loop |

---

> **Remember:** Players don't remember features. They remember how the loop felt.
