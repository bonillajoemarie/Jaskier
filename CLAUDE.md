# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Jaskier is a two-module Android app (`:app` + `:songs` library, package `com.example.jaskier`):
a kids' educational virtual-pet game built with Jetpack Compose. The code-drawn, depth-shaded pet
"Kerker" (cartoon of the owner's kid: tan skin, dark curly hair, white tank top) has three stats —
hunger, cleanliness, teeth — that decay in real time (timestamped, recomputed on app open; clamped
to a floor so the pet never "dies"). Kids feed, shower, and brush its teeth; neglect two or more
needs and it gets sick Tamagotchi-style until given medicine. Seven mini-games: ABC grid, Numbers
grid, Colors grid (11 main colors as swatch tiles), Shapes grid (10 main shapes, code-drawn via
`minigames/ShapeArt.kt`) — all four TTS-voiced tap-to-announce — Catch (falling letters/numbers eaten by a draggable Kerker, scored), Sing
(13 public-domain sung recordings with karaoke view: synced lyrics, per-song themed animated
backgrounds, tap-for-stars), and Zoo (random fruits/animals board — animals play real recordings,
fruits squish with juice splatter + TTS name). The pet design is original — inspired by the
virtual-pet genre, deliberately not copying Pou's character or trade dress.

- minSdk 33, targetSdk/compileSdk 37, Java 11 compatibility
- AGP 9.3.1 with **built-in Kotlin** (2.2.10). No `org.jetbrains.kotlin.android` plugin — it is
  incompatible with built-in Kotlin. Compose is enabled by `org.jetbrains.kotlin.plugin.compose`
  (version **must match** the built-in Kotlin version, currently 2.2.10) plus
  `buildFeatures { compose = true }`. There is no `composeOptions` block (obsolete since Kotlin 2.0).
- Dependencies are declared through the version catalog at `gradle/libs.versions.toml` — add new libraries there and reference them as `libs.*` in `app/build.gradle.kts`. Compose libraries are pinned by `androidx.compose:compose-bom`.
- Gradle configuration cache is enabled (`gradle.properties`)

## Architecture

Single activity (`MainActivity`), no DI framework, no navigation library.

- `JaskierApp.kt` — sealed-interface `Screen` navigation (`PetHome`, `MiniGamePlay(gameId)`) held in
  `rememberSaveable` with a string Saver; `BackHandler` returns home; paints the edge-to-edge
  `SkyGradient` and dispatches `MiniGamePlay` by catalog type (`AnnounceGame` → grid,
  `CatchGame` → catch screen). New tap-to-announce mini-games need only a new entry in
  `minigames/MiniGame.kt`'s `MiniGames` list; structurally new games add a `MiniGameDef` variant.
  An `AnnounceItem` carries a `TileArt` — `Glyph` (default, draws the display string), `Swatch`
  (the tile becomes that color; the label flips to dark ink on pale swatches), or `Shape` (a white
  silhouette from `ShapeKind`) — so a new grid game rarely needs screen changes.
- `pet/PetStats.kt` — pure decay/mood math (unit-tested in `app/src/test/.../PetStatsTest.kt`).
  Three stats decay from 100 toward `AWAY_FLOOR` (20) at 16h/24h/12h full-to-floor; `moodOf` gives
  HUNGRY/DIRTY/YUCKY_TEETH below 45 (hunger wins ties) and SICK when ≥2 stats are below 30;
  `medicined()` lifts low stats to 55.
- `pet/PetRepository.kt` — DataStore Preferences (`pet_state`); every mutation is decay-then-transform
  inside one atomic `edit {}` keyed on a caller-supplied `now` timestamp.
- `pet/PetViewModel.kt` — StateFlow UI state + 30s refresh ticker + one-shot `PetEvent` SharedFlow
  (FED/SHOWERED/BRUSHED/HEALED) that drives Canvas animations.
- `pet/PetCanvas.kt` — Kerker is drawn entirely with Compose Canvas in a chibi style (big round
  head, huge sparkly brown pupils, curls + wisps, tiny tank-top body with stubby arms/feet) with
  3D-look shading and a contact shadow, over a sun/clouds backdrop. Geometry hangs off shared
  anchors (`headC()`, `headR()`, `mouthAnchor()`, `feetY()`). Interactive: pupils track the finger,
  tap = squish + hearts + `onPoke` callback (home speaks a giggle). Mood visuals: sad brows
  (hungry), dirt/stink (dirty), yellow teeth (yucky), pale green + droopy lids + sweat drop (sick).
  Teeth render as individual crowns under a gum line (see also BrushScreen).
- **Narration**: everything speaks. Home greets and voices Kerker's needs on mood change; every
  `MiniGameDef` has an `intro` spoken on entry; care steps speak their instructions; activities
  have voice lines ("Yum yum!", "Scrub scrub!", giggles on poke). All via `TtsManager`.
- `minigames/CatchGameScreen.kt` — frame-loop game (`withFrameNanos`): letter/number tiles fall,
  Kerker follows the finger and eats what touches his mouth zone; each catch is spoken and scored.
  No fail state; spawn rate/speed scale gently with score.
- `minigames/Match3Logic.kt` + `Match3Screen.kt` — Candy-Crush mechanics with the zoo animals:
  pure board logic (swap/match/fall/refill, unit-tested in `Match3LogicTest`) + a Canvas board of
  6×7 pastel tiles with mini animal faces. Drag past 40% of a tile to swap; matches pop with stars,
  play the matched animal's SoundPool clip, and cascade. No move limit, no fail state.
- `speech/TtsManager.kt` — TextToSpeech wrapper owned by MainActivity (created onCreate, shutdown
  onDestroy), QUEUE_FLUSH. On init it picks the warmest usable installed voice via the pure,
  unit-tested `speech/VoiceSelection.kt` (skipping network-only voices, which stall when a kid taps
  fast) and falls back to the engine default if none qualify. Delivery is a `VoiceTone`
  (`speech/VoiceTone.kt`) per utterance — NORMAL is pitch 1.15 at the long-standing 0.8 rate, with
  EXCITED/SAD/CRYING/SLEEPY/GIGGLY variants. Repeated lines are drawn from rotating sets so nothing
  is ever heard the same way twice in a row.
- Theme is always-light and bright (kids' app) regardless of system dark mode; colors in
  `ui/theme/Color.kt`; shared gradient/press-bounce helpers in `ui/theme/Effects.kt`.
- **`:songs` module** — all bundled audio + catalogs + players. `Catalog.kt` (13 `Song`s with
  public-domain lyric lines, 6 `AnimalSound`s), `SongPlayer` (MediaPlayer; one song at a time,
  exposes `playingSongId`/position for karaoke sync), `AnimalSoundPlayer` (SoundPool). ALL audio
  is verified public-domain or CC BY-SA — sources and required attribution in
  `songs/ATTRIBUTIONS.md`; keep that file updated when adding audio. Songs: U.S. State Dept
  "Sing Out Loud" collection (PD, sung); animal clips: Wikimedia Commons.
- `minigames/SongsScreen.kt` + `KaraokeView.kt` — song list → full-screen karaoke: line highlight
  synced to `player.positionMs()` (lines weighted by length), themed animated background chosen by
  song id (night sky / floating glyphs / farm / clock / meadow / confetti / steam / snow / sun),
  tap-anywhere star bursts, and a draggable Kerker: kids fling him and he tumbles around the scene
  — spinning, falling, and bouncing off the walls and floor — until he settles and dances where he
  landed. Physics live in `minigames/KerkerToyPhysics.kt` as a pure `step()` function (unit-tested
  in `KerkerToyPhysicsTest`); tapping him squishes him and speaks a giggle.
- `minigames/ZooScreen.kt` — shuffled fruits/animals grid, all faces/fruits code-drawn with the
  shared shading style; animal tap → SoundPool clip + wiggle, fruit tap → squish scale + juice
  splatter particles + TTS name.
- **`care/` package** — the Feed/Shower/Brush home buttons open interactive routine screens
  (`Screen.Care(careId)`) instead of instant actions, teaching kids the real steps. Every step
  instruction is spoken via TTS. Progress on drag interactions is **time-in-zone** (accumulate
  `change.uptimeMillis` deltas), never event-count — event density varies wildly between real
  fingers and synthetic input. During an action step, tools snap to any drag (toddler fingers
  aren't precise). `ShowerScreen`: tap knob → wet → drag soap over 3 dirt spots (bubbles mark
  soaped spots) → tap knob to rinse. `BrushScreen`: zoomed face, drag toothpaste onto brush, scrub
  6 yellow teeth white. `FeedScreen`: tap foods onto a plate, drag spoon to scoop and deliver to
  the mouth. All complete → ViewModel action + auto-return home.

## Commands

```bash
./gradlew assembleDebug                 # build debug APK
./gradlew test                          # JVM unit tests (app/src/test)
./gradlew connectedAndroidTest          # instrumented tests (app/src/androidTest; needs a device/emulator)
./gradlew lint                          # Android Lint
./gradlew :app:testDebugUnitTest --tests "com.example.jaskier.PetStatsTest"   # single unit test class
```

## Notes

- R8 keep rules use the AGP 9 convention: files under `app/src/main/keepRules/` (e.g. `rules.keep`) are combined automatically — there is no `proguard-rules.pro`.
- The release build type currently has `optimization { enable = false }` (no minification).
- Headless emulator on this machine: use `-gpu swangle_indirect` — the default
  `swiftshader_indirect` segfaults. AVD `jaskier_test` (Pixel 7, android-35 google_apis) exists.
