package com.example.jaskier.minigames

import androidx.compose.ui.graphics.Color
import com.example.jaskier.ui.theme.BubblePink
import com.example.jaskier.ui.theme.CleanTeal
import com.example.jaskier.ui.theme.GrapePurple
import com.example.jaskier.ui.theme.LeafGreen
import com.example.jaskier.ui.theme.SkyBlue
import com.example.jaskier.ui.theme.SunYellow
import com.example.jaskier.ui.theme.WarmOrange

/**
 * How an announce tile is drawn. Letters and numbers are glyphs; colors and
 * shapes cannot be — a colored square is not a character.
 */
sealed interface TileArt {
    /** Draw [AnnounceItem.display] as text, the original behaviour. */
    data object Glyph : TileArt

    /** Fill the tile with this color; the name goes underneath for parents. */
    data class Swatch(val color: Color) : TileArt

    /** Draw this shape on the tile; the name goes underneath for parents. */
    data class Shape(val kind: ShapeKind) : TileArt
}

data class AnnounceItem(
    val display: String,
    val utterance: String,
    val art: TileArt = TileArt.Glyph,
)

sealed interface MiniGameDef {
    val id: String
    val title: String
    val cardEmoji: String
    val color: Color

    /** Spoken when the game opens, so pre-readers know what to do. */
    val intro: String
}

/** Tap-a-tile-to-hear-it games (ABC, 123, ...). */
data class AnnounceGame(
    override val id: String,
    override val title: String,
    override val cardEmoji: String,
    override val color: Color,
    override val intro: String,
    val items: List<AnnounceItem>,
) : MiniGameDef

/** Falling letters/numbers that Kerker catches and eats. */
data object CatchGame : MiniGameDef {
    override val id = "catch"
    override val title = "Catch"
    override val cardEmoji = "🍎"
    override val color = WarmOrange
    override val intro = "Move Kerker to catch the falling letters and numbers!"
}

/** Sing-along player for the bundled public-domain kids songs. */
data object SingAlong : MiniGameDef {
    override val id = "sing"
    override val title = "Sing"
    override val cardEmoji = "🎵"
    override val color = BubblePink
    override val intro = "Pick a song and sing along!"
}

/** Candy-Crush-style match-3 with the zoo animals. */
data object MatchGame : MiniGameDef {
    override val id = "match"
    override val title = "Match"
    override val cardEmoji = "🐾"
    override val color = SunYellow
    override val intro = "Swap two animals to line up three of the same!"
}

/** Random fruits & animals board: animals make sounds, fruits squish. */
data object ZooGame : MiniGameDef {
    override val id = "zoo"
    override val title = "Zoo"
    override val cardEmoji = "🐮"
    override val color = LeafGreen
    override val intro = "Tap the animals to hear them, and squish the fruits!"
}

/** The main colors, in the order a kid meets them. */
private val MainColors: List<Pair<String, Color>> = listOf(
    "Red" to Color(0xFFE53935),
    "Orange" to Color(0xFFFB8C00),
    "Yellow" to Color(0xFFFDD835),
    "Green" to Color(0xFF43A047),
    "Blue" to Color(0xFF1E88E5),
    "Purple" to Color(0xFF8E24AA),
    "Pink" to Color(0xFFEC407A),
    "Brown" to Color(0xFF795548),
    "Black" to Color(0xFF212121),
    "White" to Color(0xFFFFFFFF),
    "Gray" to Color(0xFF9E9E9E),
)

/** The main shapes, simplest first. */
private val MainShapes: List<Pair<String, ShapeKind>> = listOf(
    "Circle" to ShapeKind.CIRCLE,
    "Square" to ShapeKind.SQUARE,
    "Triangle" to ShapeKind.TRIANGLE,
    "Rectangle" to ShapeKind.RECTANGLE,
    "Oval" to ShapeKind.OVAL,
    "Star" to ShapeKind.STAR,
    "Heart" to ShapeKind.HEART,
    "Diamond" to ShapeKind.DIAMOND,
    "Pentagon" to ShapeKind.PENTAGON,
    "Hexagon" to ShapeKind.HEXAGON,
)

// Adding a tap-to-announce mini-game = adding an entry here; the menu card
// and play screen pick it up automatically.
val MiniGames: List<MiniGameDef> = listOf(
    AnnounceGame(
        "abc", "ABC", "🔤", CleanTeal,
        "Tap a letter to hear its name!",
        ('A'..'Z').map { AnnounceItem("$it", "$it") },
    ),
    AnnounceGame(
        "numbers", "123", "🔢", GrapePurple,
        "Tap a number to hear it!",
        (1..20).map { AnnounceItem("$it", "$it") },
    ),
    AnnounceGame(
        "colors", "Colors", "🎨", BubblePink,
        "Tap a color to hear its name!",
        MainColors.map { (name, color) -> AnnounceItem(name, name, TileArt.Swatch(color)) },
    ),
    AnnounceGame(
        "shapes", "Shapes", "🔷", SkyBlue,
        "Tap a shape to hear its name!",
        MainShapes.map { (name, kind) -> AnnounceItem(name, name, TileArt.Shape(kind)) },
    ),
    CatchGame,
    SingAlong,
    ZooGame,
    MatchGame,
)

fun miniGameById(id: String): MiniGameDef = MiniGames.first { it.id == id }

/** Item pool for the catch game: single-glyph letters and numbers. */
val CatchPool: List<AnnounceItem> =
    ('A'..'Z').map { AnnounceItem("$it", "$it") } + (1..9).map { AnnounceItem("$it", "$it") }
