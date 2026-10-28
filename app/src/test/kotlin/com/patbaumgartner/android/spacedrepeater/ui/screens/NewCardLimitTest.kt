package com.patbaumgartner.android.spacedrepeater.ui.screens

import com.patbaumgartner.android.spacedrepeater.domain.model.Card
import com.patbaumgartner.android.spacedrepeater.domain.model.CardPhase
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the daily new-card limit (UC-08) enforced by
 * [applyNewCardLimit] in the study session: cards in the NEW phase are
 * first-time introductions and are capped; learning/review/relearning cards
 * are never held back (Anki semantics).
 */
class NewCardLimitTest {
    private fun card(id: Long) = Card(id = id, deckId = 1L, front = "q$id", back = "a$id", createdAt = java.time.Instant.EPOCH)

    private fun phasesOf(vararg newIds: Long) =
        listOf(1L, 2L, 3L, 4L).associateWith { if (it in newIds) CardPhase.NEW else CardPhase.REVIEW }

    private fun ids(state: StudyUiState) = state.cards.map { it.id }

    @Test
    fun `unlimited budget keeps every due card`() {
        val due = listOf(card(1), card(2), card(3))
        val state = applyNewCardLimit(due, phasesOf(2), Int.MAX_VALUE)

        assertEquals(0, state.newCardsHeldBack)
        assertEquals(setOf(1L, 2L, 3L), ids(state).toSet())
        assertEquals(3, state.total)
    }

    @Test
    fun `new cards beyond the remaining budget are held back`() {
        val due = listOf(card(1), card(2), card(3), card(4))
        val state = applyNewCardLimit(due, phasesOf(2, 3, 4), newRemaining = 2)

        assertEquals(1, state.newCardsHeldBack)
        // Review card 1 plus the first two NEW cards (2, 3) in DAO order.
        assertEquals(listOf(1L, 2L, 3L), ids(state))
    }

    @Test
    fun `review and learning cards are never held back`() {
        val due = listOf(card(1), card(2))
        val phases = mapOf(1L to CardPhase.LEARNING, 2L to CardPhase.REVIEW)
        val state = applyNewCardLimit(due, phases, newRemaining = 0)

        assertEquals(0, state.newCardsHeldBack)
        assertEquals(listOf(1L, 2L), ids(state))
    }

    @Test
    fun `zero remaining budget holds back every new card`() {
        val due = listOf(card(1), card(2), card(3))
        val state = applyNewCardLimit(due, phasesOf(1, 3), newRemaining = 0)

        assertEquals(2, state.newCardsHeldBack)
        assertEquals(listOf(2L), ids(state))
    }

    @Test
    fun `held-back count is zero when budget exceeds due new cards`() {
        val state = applyNewCardLimit(listOf(card(1)), phasesOf(1), newRemaining = 5)

        assertEquals(0, state.newCardsHeldBack)
        assertEquals(listOf(1L), ids(state))
    }

    @Test
    fun `relearning cards are never held back`() {
        val due = listOf(card(1))
        val phases = mapOf(1L to CardPhase.RELEARNING)
        val state = applyNewCardLimit(due, phases, newRemaining = 0)

        assertEquals(0, state.newCardsHeldBack)
        assertEquals(listOf(1L), ids(state))
    }

    @Test
    fun `held-back cards are the ones beyond the budget in due order`() {
        // New cards 2 and 4 are due; one slot left → card 4 waits until tomorrow.
        val due = listOf(card(1), card(2), card(3), card(4))
        val state = applyNewCardLimit(due, phasesOf(2, 4), newRemaining = 1)

        assertEquals(1, state.newCardsHeldBack)
        assertEquals(listOf(1L, 3L, 2L), ids(state))
    }

    @Test
    fun `empty due list yields empty session`() {
        val state = applyNewCardLimit(emptyList(), emptyMap(), newRemaining = 0)

        assertEquals(0, state.newCardsHeldBack)
        assertEquals(emptyList<Long>(), ids(state))
    }
}
