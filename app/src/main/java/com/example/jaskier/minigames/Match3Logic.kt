package com.example.jaskier.minigames

import kotlin.random.Random

/**
 * Pure match-3 board logic (Candy-Crush mechanics): swap adjacent tiles,
 * matches of 3+ in a row/column clear, tiles fall, new ones refill from the
 * top, and cascades keep resolving. No move limit and no fail state.
 */
class Match3Logic(
    val columns: Int = 6,
    val rows: Int = 7,
    val typeCount: Int = 6,
    private val random: Random = Random.Default,
) {
    val size get() = columns * rows

    fun index(col: Int, row: Int) = row * columns + col

    /** A starting board with no pre-made matches. */
    fun newBoard(): IntArray {
        val board = IntArray(size)
        for (i in 0 until size) {
            var type: Int
            do {
                type = random.nextInt(typeCount)
                board[i] = type
            } while (createsImmediateMatch(board, i))
        }
        return board
    }

    private fun createsImmediateMatch(board: IntArray, i: Int): Boolean {
        val col = i % columns
        val row = i / columns
        val type = board[i]
        if (col >= 2 && board[i - 1] == type && board[i - 2] == type) return true
        if (row >= 2 && board[i - columns] == type && board[i - 2 * columns] == type) return true
        return false
    }

    fun areAdjacent(a: Int, b: Int): Boolean {
        val colA = a % columns
        val colB = b % columns
        val rowA = a / columns
        val rowB = b / columns
        return (colA == colB && kotlin.math.abs(rowA - rowB) == 1) ||
            (rowA == rowB && kotlin.math.abs(colA - colB) == 1)
    }

    /** All indices that are part of a horizontal or vertical run of 3+. */
    fun findMatches(board: IntArray): Set<Int> {
        val matched = mutableSetOf<Int>()
        // horizontal runs
        for (row in 0 until rows) {
            var runStart = 0
            for (col in 1..columns) {
                val prev = index(runStart, row)
                val same = col < columns && board[index(col, row)] == board[prev]
                if (!same) {
                    if (col - runStart >= 3) {
                        for (c in runStart until col) matched += index(c, row)
                    }
                    runStart = col
                }
            }
        }
        // vertical runs
        for (col in 0 until columns) {
            var runStart = 0
            for (row in 1..rows) {
                val prev = index(col, runStart)
                val same = row < rows && board[index(col, row)] == board[prev]
                if (!same) {
                    if (row - runStart >= 3) {
                        for (r in runStart until row) matched += index(col, r)
                    }
                    runStart = row
                }
            }
        }
        return matched
    }

    /** Swap in place; returns true when the swap produces a match (else it's undone). */
    fun trySwap(board: IntArray, a: Int, b: Int): Boolean {
        if (!areAdjacent(a, b)) return false
        board.swap(a, b)
        if (findMatches(board).isEmpty()) {
            board.swap(a, b)
            return false
        }
        return true
    }

    /** Remove matches, drop tiles down, refill the gaps from the top. */
    fun clearAndFall(board: IntArray, matches: Set<Int>): IntArray {
        val next = board.copyOf()
        for (col in 0 until columns) {
            var writeRow = rows - 1
            for (row in rows - 1 downTo 0) {
                val i = index(col, row)
                if (i !in matches) {
                    next[index(col, writeRow)] = board[i]
                    writeRow--
                }
            }
            while (writeRow >= 0) {
                next[index(col, writeRow)] = random.nextInt(typeCount)
                writeRow--
            }
        }
        return next
    }

    private fun IntArray.swap(a: Int, b: Int) {
        val tmp = this[a]
        this[a] = this[b]
        this[b] = tmp
    }
}
