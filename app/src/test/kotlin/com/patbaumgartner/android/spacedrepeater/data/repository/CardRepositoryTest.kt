package com.patbaumgartner.android.spacedrepeater.data.repository

import com.patbaumgartner.android.spacedrepeater.data.local.DbTransaction
import com.patbaumgartner.android.spacedrepeater.data.local.SyncDbTransaction
import com.patbaumgartner.android.spacedrepeater.data.local.SpacedRepeaterDatabase
import com.patbaumgartner.android.spacedrepeater.data.local.card.CardDao
import com.patbaumgartner.android.spacedrepeater.data.local.card.CardStateDao
import com.patbaumgartner.android.spacedrepeater.data.local.card.ReviewLogDao
import com.patbaumgartner.android.spacedrepeater.data.local.model.CardStateEntity
import com.patbaumgartner.android.spacedrepeater.data.scheduler.SM2Scheduler
import com.patbaumgartner.android.spacedrepeater.domain.model.CardPhase
import com.patbaumgartner.android.spacedrepeater.domain.model.ReviewRating
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CardRepository] covering UC-04 Rate Recall and UC-05 Undo Review.
 * Uses [DbTransaction.sync] so no Room extension-function mocking is needed.
 */
class CardRepositoryTest {
    private lateinit var cardDao: CardDao
    private lateinit var stateDao: CardStateDao
    private lateinit var logDao: ReviewLogDao
    private lateinit var db: SpacedRepeaterDatabase
    private lateinit var repo: CardRepository

    @Before
    fun setup() {
        cardDao = mockk(relaxed = true)
        stateDao = mockk(relaxed = true)
        logDao = mockk(relaxed = true)
        db = mockk(relaxed = true)
        repo = CardRepository(db, cardDao, stateDao, logDao, SM2Scheduler(), SyncDbTransaction)
    }

    // ─── UC-04: Rate Recall ────────────────────────────────────────────────────

    @Test
    fun `rate Good on new card graduates to REVIEW with interval 1`() = runBlocking {
        val cardId = 1L
        coEvery { stateDao.getById(cardId) } returns CardStateEntity(
            cardId = cardId, dueDate = 0L, phase = CardPhase.NEW.name,
        )
        coEvery { logDao.insert(any()) } returns 42L

        val result = repo.rate(cardId, ReviewRating.GOOD)

        assertEquals(CardPhase.REVIEW, result.updatedState.phase)
        assertEquals(1, result.updatedState.interval)
        assertTrue(result.updatedState.easeFactor >= 2.5f)
        assertEquals(42L, result.logId)
        assertNotNull(result.previousState)
    }

    @Test
    fun `rate Again on new card stays in LEARNING with reduced ease factor`() = runBlocking {
        val cardId = 2L
        coEvery { stateDao.getById(cardId) } returns CardStateEntity(
            cardId = cardId, dueDate = 0L, phase = CardPhase.NEW.name, easeFactor = 2.5f,
        )
        coEvery { logDao.insert(any()) } returns 1L

        val result = repo.rate(cardId, ReviewRating.AGAIN)

        assertEquals(CardPhase.LEARNING, result.updatedState.phase)
        assertTrue(result.updatedState.easeFactor < 2.5f)
    }

    @Test
    fun `rate Again on review card moves to RELEARNING phase`() = runBlocking {
        val cardId = 3L
        coEvery { stateDao.getById(cardId) } returns CardStateEntity(
            cardId = cardId,
            dueDate = System.currentTimeMillis(),
            phase = CardPhase.REVIEW.name,
            easeFactor = 2.5f,
            interval = 10,
            repetitions = 3,
        )
        coEvery { logDao.insert(any()) } returns 1L

        val result = repo.rate(cardId, ReviewRating.AGAIN)

        assertEquals(CardPhase.RELEARNING, result.updatedState.phase)
        assertEquals(1, result.updatedState.interval)
    }

    @Test
    fun `rate Easy on review card increases ease factor`() = runBlocking {
        val cardId = 4L
        val startingEF = 2.5f
        coEvery { stateDao.getById(cardId) } returns CardStateEntity(
            cardId = cardId,
            dueDate = System.currentTimeMillis(),
            phase = CardPhase.REVIEW.name,
            easeFactor = startingEF,
            interval = 10,
            repetitions = 5,
        )
        coEvery { logDao.insert(any()) } returns 1L

        val result = repo.rate(cardId, ReviewRating.EASY)

        assertTrue(result.updatedState.easeFactor > startingEF)
    }

    @Test
    fun `rate logs the review with correct rating value`() = runBlocking {
        val cardId = 5L
        coEvery { stateDao.getById(cardId) } returns CardStateEntity(
            cardId = cardId, dueDate = 0L, phase = CardPhase.NEW.name,
        )
        val logSlot = slot<com.patbaumgartner.android.spacedrepeater.data.local.model.ReviewLogEntity>()
        coEvery { logDao.insert(capture(logSlot)) } returns 99L

        repo.rate(cardId, ReviewRating.HARD)

        assertEquals(1, logSlot.captured.rating)
    }

    // ─── UC-05: Undo Review ────────────────────────────────────────────────────

    @Test
    fun `undo restores previous state and deletes log entry`() = runBlocking {
        val cardId = 6L
        val logId = 55L
        val previousState = CardStateEntity(
            cardId = cardId,
            dueDate = 1000L,
            phase = CardPhase.REVIEW.name,
            interval = 5,
            easeFactor = 2.5f,
        )

        repo.undo(cardId, previousState, logId)

        coVerify { stateDao.upsert(previousState) }
        coVerify { logDao.delete(logId) }
    }

    @Test
    fun `undo of card with no previous state resets to NEW phase`() = runBlocking {
        val cardId = 7L
        val logId = 56L
        val capturedState = slot<CardStateEntity>()
        coEvery { stateDao.upsert(capture(capturedState)) } returns Unit

        repo.undo(cardId, null, logId)

        assertEquals("NEW", capturedState.captured.phase)
    }

    @Test
    fun `stateFor returns null for non-existent card`() = runBlocking {
        coEvery { stateDao.getById(9999L) } returns null

        val state = repo.stateFor(9999L)

        assertNull(state)
    }

    @Test
    fun `stateFor returns domain CardState for existing card`() = runBlocking {
        val cardId = 8L
        val entity = CardStateEntity(
            cardId = cardId,
            dueDate = 1000L,
            phase = CardPhase.REVIEW.name,
            interval = 7,
            easeFactor = 2.6f,
            repetitions = 3,
        )
        coEvery { stateDao.getById(cardId) } returns entity

        val state = repo.stateFor(cardId)

        assertNotNull(state)
        assertEquals(cardId, state!!.cardId)
        assertEquals(CardPhase.REVIEW, state.phase)
        assertEquals(7, state.interval)
    }
}
