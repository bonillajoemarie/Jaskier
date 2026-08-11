# Colors and Shapes mini-games

Date: 2026-08-11
Spec 5. Independent of Specs 1–4; can be built at any point.

## Goal

Two new tap-to-announce mini-games teaching the main colors and the main shapes. Tapping a tile
speaks its name, exactly like the existing ABC and 123 games.

## Approach

`minigames/MiniGame.kt` already makes a tap-to-announce game a single list entry, and
`AnnounceGridScreen` already handles the grid, the speaking, and the press feedback. The only gap
is that `AnnounceItem` renders its `display` string as text — a color swatch and a drawn shape are
not glyphs.

`AnnounceItem` therefore grows a `TileArt`:

```kotlin
sealed interface TileArt {
    data object Glyph : TileArt                       // today's behaviour: draw `display` as text
    data class Swatch(val color: Color) : TileArt     // a filled rounded square of that color
    data class Shape(val kind: ShapeKind) : TileArt   // a code-drawn shape
}

data class AnnounceItem(
    val display: String,
    val utterance: String,
    val art: TileArt = TileArt.Glyph,
)
```

The default keeps ABC and 123 working untouched. The grid screen switches on `art` when drawing a
tile; everything else — layout, tap handling, speech, press bounce — is unchanged.

## Colors game

`AnnounceGame("colors", "Colors", "🎨", …)`, intro: "Tap a color to hear its name!"

Eleven main colors, each a `TileArt.Swatch`:

| Name | Hex |
|---|---|
| Red | `#E53935` |
| Orange | `#FB8C00` |
| Yellow | `#FDD835` |
| Green | `#43A047` |
| Blue | `#1E88E5` |
| Purple | `#8E24AA` |
| Pink | `#EC407A` |
| Brown | `#795548` |
| Black | `#212121` |
| White | `#FFFFFF` |
| Gray | `#9E9E9E` |

Per `game-art`, tiles are saturated against a muted background so the eye goes to what is tappable.
White and black swatches carry a thin neutral outline so they stay visible against the tile
background and against each other — the one place a swatch needs help reading as a shape.

The utterance is just the color name. The tile shows the swatch only; the written name appears
beneath it in small text for parents, per `kids-ux` (words are decoration for parents; the audio is
the interface).

## Shapes game

`AnnounceGame("shapes", "Shapes", "🔷", …)`, intro: "Tap a shape to hear its name!"

Ten main shapes, each a `TileArt.Shape` drawn on Canvas in the soft-flat style (flat fill, one soft
shadow, chunky proportions — matching Spec 3):

circle, square, triangle, rectangle, oval, star, heart, diamond, pentagon, hexagon.

`ShapeKind` is an enum; a single `drawShape(kind, center, size, color)` function in a new
`minigames/ShapeArt.kt` renders each from primitives and Bezier paths, proportional to the tile
size. Each shape gets a distinct color from the app palette so the two games do not look identical,
and so a kid can refer to "the blue triangle".

## Registration

Both games are appended to `MiniGames` in `MiniGame.kt`, placed after `numbers` so the learning
games sit together on the menu. No changes to `JaskierApp.kt` are required — `AnnounceGame` already
dispatches to the grid screen.

## Testing

`MiniGameCatalogTest` (new): every game id is unique, `miniGameById` resolves each one, the colors
game has 11 items and the shapes game has 10, every `AnnounceItem` has a non-blank utterance, and
every colors/shapes item carries non-`Glyph` art. `ShapeArtTest` asserts `ShapeKind` has an entry
for each of the ten shapes and that `drawShape` handles them all exhaustively (a `when` over the
enum with no `else`, so a new shape is a compile error rather than a blank tile).

Visual results verified by building and running.

## Out of scope

- Matching or quiz mechanics ("find the red one") — these are pure tap-to-hear, self-paced, no
  scoring and no fail state, consistent with ABC and 123.
- Color mixing, shape sorting, or drag interactions.
