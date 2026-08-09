package com.example.jaskier.minigames

import androidx.compose.ui.graphics.Color
import com.example.jaskier.ui.theme.BubblePink
import com.example.jaskier.ui.theme.CleanTeal
import com.example.jaskier.ui.theme.GrapePurple
import com.example.jaskier.ui.theme.LeafGreen
import com.example.jaskier.ui.theme.SunYellow
import com.example.jaskier.ui.theme.WarmOrange

data class AnnounceItem(val display: String, val utterance: String)

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
    CatchGame,
    SingAlong,
    ZooGame,
    MatchGame,
)

fun miniGameById(id: String): MiniGameDef = MiniGames.first { it.id == id }

/** Item pool for the catch game: single-glyph letters and numbers. */
val CatchPool: List<AnnounceItem> =
    ('A'..'Z').map { AnnounceItem("$it", "$it") } + (1..9).map { AnnounceItem("$it", "$it") }
