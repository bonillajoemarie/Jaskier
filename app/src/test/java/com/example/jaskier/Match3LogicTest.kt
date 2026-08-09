package com.example.jaskier

import com.example.jaskier.minigames.Match3Logic
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Match3LogicTest {

    private val logic = Match3Logic(columns = 4, rows = 4, typeCount = 4, random = Random(7))

    @Test
    fun `new board has no pre-made matches`() {
        repeat(20) {
            val fresh = Match3Logic(random = Random(it))
            assertTrue(fresh.findMatches(fresh.newBoard()).isEmpty())
        }
    }

    @Test
    fun `finds horizontal and vertical runs`() {
        // 4x4: row 0 = [1,1,1,2]; col 3 = [2,2,2,0]
        val board = intArrayOf(
            1, 1, 1, 2,
            0, 2, 3, 2,
            3, 0, 1, 2,
            2, 3, 0, 0,
        )
        val matches = logic.findMatches(board)
        assertEquals(setOf(0, 1, 2, 3, 7, 11), matches)
    }

    @Test
    fun `swap that makes no match is undone`() {
        val board = intArrayOf(
            0, 1, 0, 1,
            2, 3, 2, 3,
            0, 1, 0, 1,
            2, 3, 2, 3,
        )
        val copy = board.copyOf()
        assertFalse(logic.trySwap(board, 0, 1))
        assertTrue(board.contentEquals(copy))
    }

    @Test
    fun `swap that completes a run sticks`() {
        // Swapping (1,1)=0 with (1,0)=3 completes row 0 as 3,3,3.
        val board = intArrayOf(
            3, 0, 3, 1,
            2, 3, 1, 2,
            0, 1, 2, 0,
            1, 2, 0, 1,
        )
        assertTrue(logic.trySwap(board, 1, 5))
        assertEquals(3, board[1])
        assertTrue(logic.findMatches(board).isNotEmpty())
    }

    @Test
    fun `clearAndFall drops tiles and refills the top`() {
        val board = intArrayOf(
            0, 1, 2, 3,
            1, 2, 3, 0,
            2, 2, 2, 1, // this row matches horizontally? 2,2,2 = indices 8,9,10
            3, 0, 1, 2,
        )
        val matches = setOf(8, 9, 10)
        val next = logic.clearAndFall(board, matches)
        // Bottom row unchanged
        assertEquals(3, next[logic.index(0, 3)])
        assertEquals(0, next[logic.index(1, 3)])
        assertEquals(1, next[logic.index(2, 3)])
        // Column 0 kept (top→bottom) 0,1,3 → after the fall: row1=0, row2=1, row3=3
        assertEquals(0, next[logic.index(0, 1)])
        assertEquals(1, next[logic.index(0, 2)])
        // Column 3 was untouched by the match
        assertEquals(3, next[logic.index(3, 0)])
        assertEquals(0, next[logic.index(3, 1)])
        assertEquals(1, next[logic.index(3, 2)])
        assertEquals(2, next[logic.index(3, 3)])
    }
}
