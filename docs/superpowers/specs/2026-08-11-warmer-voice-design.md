# A warmer, more human voice

Date: 2026-08-11
Spec 6. Stage 1 is independent of Specs 1–5. Stage 2 depends on Spec 4's emotion states existing.

## Goal

Everything in Jaskier speaks, and right now it all sounds mechanical: `TtsManager` takes the
device's default voice at a flat rate of 0.8 with no pitch control and no variation. Make Kerker
sound like a small, warm, excitable character a kid wants to listen to.

## Stage 1 — Tune the built-in TTS

Free, immediate, and works for any text. All changes are in `speech/TtsManager.kt`.

### Pick the best available voice

Android ships a default voice that is rarely the best one installed. On init, enumerate
`tts.voices` and score candidates:

1. Locale matches the app language.
2. Prefer `quality >= Voice.QUALITY_HIGH`.
3. Prefer voices **without** `Voice.LATENCY_VERY_HIGH` and without the
   `FEATURE_NOT_INSTALLED` feature — a network-only voice that stalls or fails is worse than a
   plain local one for a kid tapping fast.
4. Among the remainder, prefer names containing a female/higher register marker where the engine
   exposes one, since it reads as warmer for this character.

Fall back silently to the current behaviour if nothing scores — the app must never be mute.

### Child-friendly baseline

- Pitch **1.15** (`setPitch`) — noticeably younger without turning into a chipmunk.
- Rate stays **0.8**, the existing kid-friendly pace.

### Per-emotion delivery

`speak()` gains an optional tone:

```kotlin
enum class VoiceTone(val pitch: Float, val rate: Float) {
    NORMAL(1.15f, 0.80f),
    EXCITED(1.30f, 0.95f),   // fed, washed, celebrating
    SAD(1.00f, 0.70f),       // hungry, thirsty, dirty
    CRYING(1.22f, 0.65f),    // wobbly and slow
    SLEEPY(0.95f, 0.60f),    // yawning, winding down
    GIGGLY(1.35f, 1.00f),    // pokes and tickles
}

fun speak(text: String, tone: VoiceTone = VoiceTone.NORMAL)
```

`speak` applies the tone's pitch and rate before speaking. Since `QUEUE_FLUSH` is already used,
rapid taps still always announce the latest thing.

### Natural phrasing

Mechanical delivery is as much about the script as the engine. Lines get commas and ellipses where
a person would breathe ("Yum... yum!", "Ooh, that tickles!"), and every repeated line becomes a
small rotating set so the kid never hears the same reading twice in a row — the pattern
`PetHomeScreen` already uses for giggles, applied everywhere.

## Stage 2 — Bundled recorded clips

TTS, however well tuned, cannot laugh, yawn, cry, or sing a word. The ~200 lines that never change
get real recordings; TTS remains the fallback for anything dynamic.

### Module

Clips live in the existing `:songs` module, which already owns all bundled audio, its players, and
`ATTRIBUTIONS.md`. A new `VoiceClips` catalog maps a stable line id to a raw resource, and a
`VoiceClipPlayer` (SoundPool, mirroring `AnimalSoundPlayer`) plays them.

### Routing

`TtsManager.speak(lineId, fallbackText, tone)` plays the clip when one exists for `lineId` and
speaks the text otherwise. Call sites move from raw strings to line ids over time; anything not yet
recorded keeps working through TTS, so this can ship incrementally.

### What gets recorded

Greetings and welcome-backs; all four care routines' step instructions; the mood and need lines;
crying, laughing, yawning, and celebration sounds; the four poke-zone reactions; every mini-game
intro; the 26 letters, 20 numbers, 11 colors, 10 shapes, 10 foods, and the animal names.

### Licensing

If recorded by a person, no attribution is needed but the file must note who recorded them and
that they consented to bundling. If generated with a neural voice, the license must permit
bundling in a shipped app, and both the tool and the license go in `songs/ATTRIBUTIONS.md`
alongside the existing song and animal-clip provenance. No voice recordings of children are
collected by the app — this is bundled content only, nothing is recorded on device, and no
microphone permission is added. That constraint is deliberate and was decided when the Talking Tom
voice-repeat feature was rejected.

## Testing

`VoiceToneTest`: every `VoiceTone` has a pitch and rate inside the ranges Android accepts (0.5–2.0),
`NORMAL` keeps the existing 0.8 rate so nothing regresses, and the emotion-to-tone mapping is
exhaustive over the emotion states from Spec 4.

`VoiceClipsTest` (Stage 2): every catalog line id is unique, every id maps to a resource that
exists, and `speak` falls back to TTS for an unknown id rather than going silent.

Voice selection is verified by hand on a device — it depends on which engines are installed and
cannot be asserted in a JVM unit test.

## Out of scope

- Microphone recording or voice repeat, in any form.
- Any network TTS service at runtime — latency, cost, and sending a child's session to a third
  party all rule it out.
- Localisation into other languages.
