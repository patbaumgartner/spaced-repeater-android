package com.patbaumgartner.android.spacedrepeater.data.scheduler

import com.patbaumgartner.android.spacedrepeater.domain.model.CardPhase
import com.patbaumgartner.android.spacedrepeater.domain.model.CardState
import com.patbaumgartner.android.spacedrepeater.domain.model.ReviewRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class SM2SchedulerTest {
    private val scheduler = SM2Scheduler()
    private val now = Instant.now()

    private fun reviewState(interval: Int, ef: Float, phase: CardPhase = CardPhase.REVIEW) = CardState(
        cardId = 1L,
        repetitions = 5,
        easeFactor = ef,
        interval = interval,
        dueAt = now,
        phase = phase,
    )

    @Test
    fun `new card Good graduates to 1 day interval`() {
        val schedule = scheduler.calculateNextReview(reviewState(0, 2.5f, CardPhase.NEW), ReviewRating.GOOD, now)
        assertEquals(1, schedule.interval)
        assertEquals(CardPhase.REVIEW, schedule.nextPhase)
        assertEquals(now.plus(Duration.ofDays(1)), schedule.dueAt)
    }

    @Test
    fun `new card Easy graduates to 6 day interval`() {
        val schedule = scheduler.calculateNextReview(reviewState(0, 2.5f, CardPhase.NEW), ReviewRating.EASY, now)
        assertEquals(6, schedule.interval)
        assertEquals(CardPhase.REVIEW, schedule.nextPhase)
        assertEquals(now.plus(Duration.ofDays(6)), schedule.dueAt)
    }

    @Test
    fun `learning Again stays in learning with 1 minute step`() {
        val schedule = scheduler.calculateNextReview(reviewState(0, 2.5f, CardPhase.LEARNING), ReviewRating.AGAIN, now)
        assertEquals(CardPhase.LEARNING, schedule.nextPhase)
        assertEquals(now.plus(Duration.ofMinutes(1)), schedule.dueAt)
    }

    @Test
    fun `learning Good graduates to review`() {
        val schedule = scheduler.calculateNextReview(reviewState(0, 2.5f, CardPhase.LEARNING), ReviewRating.GOOD, now)
        assertEquals(CardPhase.REVIEW, schedule.nextPhase)
        assertEquals(now.plus(Duration.ofDays(1)), schedule.dueAt)
    }

    @Test
    fun `ease factor never drops below 13`() {
        val schedule = scheduler.calculateNextReview(reviewState(100, 1.3f), ReviewRating.AGAIN, now)
        assertTrue("ease factor must be >= 1.3 but was ${schedule.easeFactor}", schedule.easeFactor >= 1.3f)
    }

    @Test
    fun `good with interval 10 and ef 25 yields interval 25`() {
        val schedule = scheduler.calculateNextReview(reviewState(10, 2.5f), ReviewRating.GOOD, now)
        assertEquals(25, schedule.interval)
        assertEquals(2.5f, schedule.easeFactor, 0.001f)
    }

    @Test
    fun `easy adds bonus to interval`() {
        val schedule = scheduler.calculateNextReview(reviewState(10, 2.5f), ReviewRating.EASY, now)
        val expected = Math.round(Math.round(10 * 2.65f) * 1.3f).toInt()
        assertEquals(expected, schedule.interval)
        assertEquals(2.65f, schedule.easeFactor, 0.001f)
    }

    @Test
    fun `lapse resets to relearning phase with 1 minute step`() {
        val schedule = scheduler.calculateNextReview(reviewState(50, 2.5f), ReviewRating.AGAIN, now)
        assertEquals(CardPhase.RELARNING, schedule.nextPhase)
        assertEquals(1, schedule.interval)
        assertEquals(now.plus(Duration.ofMinutes(1)), schedule.dueAt)
    }

    @Test
    fun `relearning Good graduates back to review`() {
        val schedule = scheduler.calculateNextReview(reviewState(1, 2.5f, CardPhase.RELARNING), ReviewRating.GOOD, now)
        assertEquals(CardPhase.REVIEW, schedule.nextPhase)
        assertEquals(now.plus(Duration.ofDays(1)), schedule.dueAt)
    }
}
