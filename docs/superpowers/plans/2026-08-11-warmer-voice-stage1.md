# Warmer Voice (Stage 1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Kerker sound like a warm, excitable little character instead of the device's flat default synthesiser — by picking the best installed voice, raising the pitch, and varying delivery per emotion.

**Architecture:** Voice *selection* is extracted into a pure, Android-free scoring function so it can be unit-tested on the JVM; `TtsManager` adapts Android's `Voice` objects into it. Delivery is a `VoiceTone` enum of pitch/rate pairs applied per utterance, defaulting to the current behaviour so no existing call site changes meaning.

**Tech Stack:** Kotlin, `android.speech.tts.TextToSpeech`, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-08-11-warmer-voice-design.md` (Stage 1 only; Stage 2, bundled recordings, is deliberately out of scope here.)

## Global Constraints

- Kotlin 2.2.10 built-in via AGP 9.3.1. Do **not** add the `org.jetbrains.kotlin.android` plugin.
- No new dependencies.
- The app must never go mute: every failure path falls back to the current default-voice behaviour.
- `QUEUE_FLUSH` stays, so rapid taps always announce the latest thing.
- Speech rate for normal narration stays **0.8** — the existing kid-friendly pace.
- Pitch and rate passed to Android must stay within 0.5–2.0.
- No microphone, no network TTS, no recording — decided when Talking Tom's voice-repeat was rejected.
- Tests live in `app/src/test/java/com/example/jaskier/` in package `com.example.jaskier`, JUnit 4, matching `PetStatsTest.kt`.

## File Structure

| File | Responsibility |
|---|---|
| `app/src/main/java/com/example/jaskier/speech/VoiceSelection.kt` | **Create.** Pure voice scoring/picking. No Android imports. |
| `app/src/test/java/com/example/jaskier/VoiceSelectionTest.kt` | **Create.** Unit tests for the scorer. |
| `app/src/main/java/com/example/jaskier/speech/VoiceTone.kt` | **Create.** Pitch/rate presets per emotional delivery. |
| `app/src/test/java/com/example/jaskier/VoiceToneTest.kt` | **Create.** Range guards for the presets. |
| `app/src/main/java/com/example/jaskier/speech/TtsManager.kt` | **Modify.** Apply voice selection, pitch, and per-utterance tone. |
| `app/src/main/java/com/example/jaskier/pet/PetHomeScreen.kt` | **Modify.** Giggles giggly, mood lines sad. |
| `app/src/main/java/com/example/jaskier/care/*.kt` | **Modify.** Completion lines excited. |
| `CLAUDE.md` | **Modify.** Update the `TtsManager` bullet. |

---

### Task 1: Pure voice selection

Android hands us a list of installed voices; the default is rarely the nicest one. Scoring lives
apart from Android so it can be tested.

**Files:**
- Create: `app/src/main/java/com/example/jaskier/speech/VoiceSelection.kt`
- Test: `app/src/test/java/com/example/jaskier/VoiceSelectionTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `data class VoiceOption(name: String, language: String, quality: Int, latency: Int, isNetworkOnly: Boolean)`, `fun scoreVoice(option: VoiceOption, language: String): Int` (negative means unusable), and `fun pickWarmestVoice(options: List<VoiceOption>, language: String): VoiceOption?`.

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/example/jaskier/VoiceSelectionTest.kt`:

```kotlin
package com.example.jaskier

import com.example.jaskier.speech.VoiceOption
import com.example.jaskier.speech.pickWarmestVoice
import com.example.jaskier.speech.scoreVoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceSelectionTest {

    private fun voice(
        name: String,
        language: String = "en",
        quality: Int = 300,
        latency: Int = 300,
        networkOnly: Boolean = false,
    ) = VoiceOption(name, language, quality, latency, networkOnly)

    @Test
    fun `a different language is never usable`() {
        assertTrue(scoreVoice(voice("de-DE-x-nfh", language = "de"), "en") < 0)
    }

    @Test
    fun `higher quality scores higher`() {
        val good = scoreVoice(voice("en-us-a", quality = 500), "en")
        val poor = scoreVoice(voice("en-us-b", quality = 100), "en")
        assertTrue("expected $good > $poor", good > poor)
    }

    @Test
    fun `lower latency scores higher`() {
        val quick = scoreVoice(voice("en-us-a", latency = 100), "en")
        val slow = scoreVoice(voice("en-us-b", latency = 500), "en")
        assertTrue("expected $quick > $slow", quick > slow)
    }

    @Test
    fun `a network-only voice loses to any local voice`() {
        // A kid tapping fast must never wait on the network, however nice it sounds.
        val network = scoreVoice(voice("en-us-x", quality = 500, networkOnly = true), "en")
        val local = scoreVoice(voice("en-us-y", quality = 100, networkOnly = false), "en")
        assertTrue("expected local $local to beat network $network", local > network)
    }

    @Test
    fun `a higher-register voice wins among otherwise equal voices`() {
        val warm = scoreVoice(voice("en-us-x-sfg#female_1"), "en")
        val plain = scoreVoice(voice("en-us-x-sfg#male_1"), "en")
        assertTrue("expected $warm > $plain", warm > plain)
    }

    @Test
    fun `picks the best usable voice`() {
        val best = voice("en-us-best#female_2", quality = 500, latency = 100)
        val options = listOf(
            voice("de-DE-x", language = "de", quality = 500, latency = 100),
            voice("en-us-meh", quality = 100, latency = 500),
            best,
            voice("en-us-network", quality = 500, latency = 100, networkOnly = true),
        )
        assertEquals(best, pickWarmestVoice(options, "en"))
    }

    @Test
    fun `no usable voice yields null so the caller keeps the default`() {
        assertNull(pickWarmestVoice(listOf(voice("de-DE-x", language = "de")), "en"))
        assertNull(pickWarmestVoice(emptyList(), "en"))
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.jaskier.VoiceSelectionTest"`
Expected: FAIL — unresolved references `VoiceOption`, `scoreVoice`, `pickWarmestVoice`.

- [ ] **Step 3: Write the implementation**

Create `app/src/main/java/com/example/jaskier/speech/VoiceSelection.kt`:

```kotlin
package com.example.jaskier.speech

// Picking a voice, kept free of Android types so it can be unit-tested.
//
// Android's default voice is rarely the best one installed. We score every voice
// the engine offers and take the warmest usable one, falling back to the default
// when nothing qualifies — the app must never go mute.

/** Mirrors android.speech.tts.Voice.QUALITY_* — higher is better. */
const val VOICE_QUALITY_HIGH = 400

/** Network-only voices stall when a kid taps fast, so they lose to any local voice. */
private const val NETWORK_PENALTY = 10_000

/** Nudge toward a higher register, which reads as warmer for this character. */
private const val HIGHER_REGISTER_BONUS = 150

private val HIGHER_REGISTER_MARKERS = listOf("female", "#f", "-f-", "_f_")

data class VoiceOption(
    val name: String,
    /** Language tag only, e.g. "en" — country variants are not a preference. */
    val language: String,
    /** android.speech.tts.Voice.getQuality(), 100..500. */
    val quality: Int,
    /** android.speech.tts.Voice.getLatency(), 100..500; lower is better. */
    val latency: Int,
    val isNetworkOnly: Boolean,
)

/** Higher is better. A negative score means the voice is unusable. */
fun scoreVoice(option: VoiceOption, language: String): Int {
    if (!option.language.equals(language, ignoreCase = true)) return -1

    var score = option.quality - option.latency
    if (option.isNetworkOnly) score -= NETWORK_PENALTY
    val name = option.name.lowercase()
    if (HIGHER_REGISTER_MARKERS.any { name.contains(it) }) score += HIGHER_REGISTER_BONUS
    if (option.quality >= VOICE_QUALITY_HIGH) score += 100
    return score
}

/** The warmest usable voice, or null to keep the engine's default. */
fun pickWarmestVoice(options: List<VoiceOption>, language: String): VoiceOption? =
    options.map { it to scoreVoice(it, language) }
        .filter { (_, score) -> score >= 0 }
        .maxByOrNull { (_, score) -> score }
        ?.first
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.jaskier.VoiceSelectionTest"`
Expected: PASS, 7 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/jaskier/speech/VoiceSelection.kt app/src/test/java/com/example/jaskier/VoiceSelectionTest.kt
git commit -m "feat: pick the warmest installed TTS voice instead of the default"
```

---

### Task 2: Voice tones

**Files:**
- Create: `app/src/main/java/com/example/jaskier/speech/VoiceTone.kt`
- Test: `app/src/test/java/com/example/jaskier/VoiceToneTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `enum class VoiceTone(val pitch: Float, val rate: Float)` with entries `NORMAL, EXCITED, SAD, CRYING, SLEEPY, GIGGLY`.

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/example/jaskier/VoiceToneTest.kt`:

```kotlin
package com.example.jaskier

import com.example.jaskier.speech.VoiceTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceToneTest {

    @Test
    fun `every tone is within the range Android accepts`() {
        for (tone in VoiceTone.entries) {
            assertTrue("${tone.name} pitch ${tone.pitch}", tone.pitch in 0.5f..2.0f)
            assertTrue("${tone.name} rate ${tone.rate}", tone.rate in 0.5f..2.0f)
        }
    }

    @Test
    fun `normal narration keeps the established kid-friendly pace`() {
        assertEquals(0.8f, VoiceTone.NORMAL.rate, 0.0001f)
    }

    @Test
    fun `normal sits above default pitch so he sounds like a small character`() {
        assertTrue(VoiceTone.NORMAL.pitch > 1.0f)
    }

    @Test
    fun `excitement is higher and faster than normal`() {
        assertTrue(VoiceTone.EXCITED.pitch > VoiceTone.NORMAL.pitch)
        assertTrue(VoiceTone.EXCITED.rate > VoiceTone.NORMAL.rate)
    }

    @Test
    fun `sleepy and sad are slower than normal`() {
        assertTrue(VoiceTone.SLEEPY.rate < VoiceTone.NORMAL.rate)
        assertTrue(VoiceTone.SAD.rate < VoiceTone.NORMAL.rate)
    }

    @Test
    fun `giggling is the highest and fastest tone`() {
        assertEquals(VoiceTone.GIGGLY, VoiceTone.entries.maxBy { it.pitch })
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.jaskier.VoiceToneTest"`
Expected: FAIL — unresolved reference `VoiceTone`.

- [ ] **Step 3: Write the implementation**

Create `app/src/main/java/com/example/jaskier/speech/VoiceTone.kt`:

```kotlin
package com.example.jaskier.speech

/**
 * How a line is delivered. A flat synthesiser is what makes an app sound
 * mechanical; varying pitch and pace with the moment is most of the fix.
 *
 * Android accepts 0.5..2.0 for both pitch and rate.
 */
enum class VoiceTone(val pitch: Float, val rate: Float) {
    /** Everyday narration: a small character, at the established kid-friendly pace. */
    NORMAL(1.15f, 0.80f),

    /** Fed, washed, brushed, celebrating. */
    EXCITED(1.30f, 0.95f),

    /** Hungry, thirsty, grubby — droopy, not miserable. */
    SAD(1.00f, 0.70f),

    /** Wobbly and slow. Cartoon upset, never distress. */
    CRYING(1.22f, 0.65f),

    /** Yawning, winding down. */
    SLEEPY(0.95f, 0.60f),

    /** Pokes and tickles. */
    GIGGLY(1.35f, 1.00f),
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.jaskier.VoiceToneTest"`
Expected: PASS, 6 tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/jaskier/speech/VoiceTone.kt app/src/test/java/com/example/jaskier/VoiceToneTest.kt
git commit -m "feat: add per-emotion voice tones"
```

---

### Task 3: Apply selection and tone in TtsManager

**Files:**
- Modify: `app/src/main/java/com/example/jaskier/speech/TtsManager.kt` (whole file)

**Interfaces:**
- Consumes: `pickWarmestVoice`, `VoiceOption` (Task 1); `VoiceTone` (Task 2).
- Produces: `fun speak(text: String, tone: VoiceTone = VoiceTone.NORMAL)` — the default keeps all 17 existing call sites compiling and behaving as before, apart from the warmer baseline.

- [ ] **Step 1: Rewrite TtsManager**

Replace the whole of `app/src/main/java/com/example/jaskier/speech/TtsManager.kt`:

```kotlin
package com.example.jaskier.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context.applicationContext, this)

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    // Setting pitch and rate on every utterance is wasteful; only change on switch.
    private var appliedTone: VoiceTone? = null

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return

        val locale = if (tts.setLanguage(Locale.US) >= TextToSpeech.LANG_AVAILABLE) {
            Locale.US
        } else {
            tts.language = Locale.getDefault()
            Locale.getDefault()
        }

        selectWarmestVoice(locale.language)
        applyTone(VoiceTone.NORMAL)
        _isReady.value = true
    }

    /**
     * The engine default is rarely the nicest voice installed. Anything unexpected
     * here is swallowed: a plain default voice beats a silent app.
     */
    private fun selectWarmestVoice(language: String) {
        val available = runCatching { tts.voices }.getOrNull() ?: return
        val options = available.mapNotNull { voice ->
            val tag = voice.locale?.language ?: return@mapNotNull null
            VoiceOption(
                name = voice.name.orEmpty(),
                language = tag,
                quality = voice.quality,
                latency = voice.latency,
                isNetworkOnly = voice.isNetworkConnectionRequired ||
                    voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true,
            )
        }
        val best = pickWarmestVoice(options, language) ?: return
        available.firstOrNull { it.name == best.name }?.let { chosen: Voice ->
            runCatching { tts.voice = chosen }
        }
    }

    fun speak(text: String, tone: VoiceTone = VoiceTone.NORMAL) {
        if (!_isReady.value) return
        applyTone(tone)
        // QUEUE_FLUSH so rapid taps always announce the latest item.
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    private fun applyTone(tone: VoiceTone) {
        if (tone == appliedTone) return
        tts.setPitch(tone.pitch)
        tts.setSpeechRate(tone.rate)
        appliedTone = tone
    }

    fun shutdown() {
        _isReady.value = false
        tts.shutdown()
    }

    private companion object {
        const val UTTERANCE_ID = "jaskier"
    }
}
```

Note `KID_SPEECH_RATE` is gone — `VoiceTone.NORMAL.rate` now owns that 0.8, and `VoiceToneTest` guards it.

- [ ] **Step 2: Build and run the full test suite**

Run: `./gradlew assembleDebug && ./gradlew test`
Expected: BUILD SUCCESSFUL; all tests pass. All 17 existing `tts.speak(...)` call sites still compile untouched, thanks to the default parameter.

- [ ] **Step 3: Verify by ear on a device**

Install and open the app. Kerker's greeting should sound noticeably younger and warmer than before, at the same unhurried pace. If the device has multiple TTS engines installed, the app must not stall before speaking — if it does, the network-only filter is not catching that voice.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/jaskier/speech/TtsManager.kt
git commit -m "feat: warmer TTS voice selection, pitch, and per-utterance tone"
```

---

### Task 4: Use the tones, and vary the lines

Tones exist but nothing uses them yet, and repeated identical lines are half of what makes an app
sound robotic.

**Files:**
- Modify: `app/src/main/java/com/example/jaskier/pet/PetHomeScreen.kt:85` and `:120`
- Modify: `app/src/main/java/com/example/jaskier/care/FeedScreen.kt:168`
- Modify: `app/src/main/java/com/example/jaskier/care/ShowerScreen.kt:173`
- Modify: `CLAUDE.md`

**Interfaces:**
- Consumes: `VoiceTone` (Task 2), `speak(text, tone)` (Task 3).
- Produces: nothing for later tasks.

- [ ] **Step 1: Mood lines get the sad tone**

In `PetHomeScreen.kt`, the mood-change narration at line 85 becomes:

```kotlin
        tts.speak(line, if (mood == PetMood.HAPPY) VoiceTone.NORMAL else VoiceTone.SAD)
```

Add the imports `com.example.jaskier.speech.VoiceTone` and, if not already present,
`com.example.jaskier.pet.PetMood`.

- [ ] **Step 2: Giggles get the giggly tone and more variety**

In `PetHomeScreen.kt`, replace the giggles list at line 112 and the speak at line 120:

```kotlin
            val giggles = remember {
                listOf(
                    "Hehe! That tickles!",
                    "Hi hi!",
                    "Hehehe!",
                    "Ooh, that tickles!",
                    "Wheee!",
                    "Do it again!",
                )
            }
```

```kotlin
                    tts.speak(giggles[giggleIndex % giggles.size], VoiceTone.GIGGLY)
```

- [ ] **Step 3: Care completions get the excited tone, with breath punctuation**

In `FeedScreen.kt` line 168:

```kotlin
                                tts.speak(
                                    if (plate.isEmpty()) "Yum... yum!" else "Mmm, yummy!",
                                    VoiceTone.EXCITED,
                                )
```

In `ShowerScreen.kt` line 173:

```kotlin
                                        if (before < 1f && scrubbed[index] >= 1f) {
                                            tts.speak("Scrub, scrub!", VoiceTone.EXCITED)
                                        }
```

Add `import com.example.jaskier.speech.VoiceTone` to both files.

- [ ] **Step 4: Build, test, and listen**

Run: `./gradlew assembleDebug && ./gradlew test && ./gradlew lint`
Expected: all green.

On a device: poke Kerker several times — the giggle line varies and is delivered high and fast;
let a need drop and hear the mood line come out slower and lower; finish a feed and hear the
excited celebration. Rapid pokes must never queue up or lag.

- [ ] **Step 5: Update CLAUDE.md**

Replace the `speech/TtsManager.kt` bullet with:

```markdown
- `speech/TtsManager.kt` — TextToSpeech wrapper owned by MainActivity (created onCreate, shutdown
  onDestroy), QUEUE_FLUSH. On init it picks the warmest usable installed voice via the pure,
  unit-tested `speech/VoiceSelection.kt` (skipping network-only voices, which stall when a kid taps
  fast) and falls back to the engine default if none qualify. Delivery is a `VoiceTone`
  (`speech/VoiceTone.kt`) per utterance — NORMAL is pitch 1.15 at the long-standing 0.8 rate, with
  EXCITED/SAD/CRYING/SLEEPY/GIGGLY variants. Repeated lines are drawn from rotating sets so nothing
  is ever heard the same way twice in a row.
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/jaskier/pet/PetHomeScreen.kt app/src/main/java/com/example/jaskier/care/FeedScreen.kt app/src/main/java/com/example/jaskier/care/ShowerScreen.kt CLAUDE.md
git commit -m "feat: deliver giggles, moods, and celebrations in matching tones"
```

---

## Verification

```bash
./gradlew test          # VoiceSelectionTest, VoiceToneTest, PetStatsTest, Match3LogicTest
./gradlew lint
./gradlew assembleDebug
```

Then listen on a real device — this is a change you cannot verify by reading test output.
