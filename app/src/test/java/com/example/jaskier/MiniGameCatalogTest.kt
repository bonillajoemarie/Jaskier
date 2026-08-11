package com.example.jaskier

import com.example.jaskier.minigames.AnnounceGame
import com.example.jaskier.minigames.MiniGames
import com.example.jaskier.minigames.TileArt
import com.example.jaskier.minigames.miniGameById
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniGameCatalogTest {

    private fun announceGame(id: String) = miniGameById(id) as AnnounceGame

    @Test
    fun `every game id is unique`() {
        val ids = MiniGames.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every game is resolvable by id`() {
        for (game in MiniGames) assertEquals(game, miniGameById(game.id))
    }

    @Test
    fun `the colors game covers the eleven main colors`() {
        assertEquals(11, announceGame("colors").items.size)
    }

    @Test
    fun `the shapes game covers the ten main shapes`() {
        assertEquals(10, announceGame("shapes").items.size)
    }

    @Test
    fun `every announce item says something`() {
        for (game in MiniGames.filterIsInstance<AnnounceGame>()) {
            for (item in game.items) {
                assertTrue("${game.id} has a blank utterance", item.utterance.isNotBlank())
            }
        }
    }

    @Test
    fun `colors are swatches and shapes are shapes`() {
        for (item in announceGame("colors").items) {
            assertTrue("${item.display} should be a swatch", item.art is TileArt.Swatch)
        }
        for (item in announceGame("shapes").items) {
            assertTrue("${item.display} should be a shape", item.art is TileArt.Shape)
        }
    }

    @Test
    fun `letters and numbers still render as plain glyphs`() {
        for (id in listOf("abc", "numbers")) {
            for (item in announceGame(id).items) {
                assertEquals(TileArt.Glyph, item.art)
            }
        }
    }
}
