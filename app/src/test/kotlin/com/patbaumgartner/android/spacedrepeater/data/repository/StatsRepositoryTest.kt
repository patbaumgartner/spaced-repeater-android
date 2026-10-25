package com.patbaumgartner.android.spacedrepeater.data.repository

import com.patbaumgartner.android.spacedrepeater.data.local.card.ReviewLogDao
import com.patbaumgartner.android.spacedrepeater.data.local.model.ReviewLogEntity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId

/**
 * Unit tests for [StatsRepository] — UC-06 View Progress.
 * Tests: cards studied today, retention rate, study streak.
 */
class StatsRepositoryTest {
    private lateinit var logDao: ReviewLogDao
    private lateinit var repo: StatsRepository
    private lateinit var zone: ZoneId

    @Before
    fun setup() {
        logDao = mockk(relaxed = true)
        repo = StatsRepository(logDao)
        zone = ZoneId.of("UTC")
    }

    private fun instant(daysAgo: Int, hour: Int = 12): Long =
        java.time.LocalDate.now(zone)
            .minusDays(daysAgo.toLong())
            .atTime(hour, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    private fun todayRange(): Pair<Long, Long> {
        val today = java.time.LocalDate.now(zone)
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    @Test
    fun `studied today counts reviews within today window`() = runBlocking {
        val (start, end) = todayRange()
        coEvery { logDao.today(start, end) } returns listOf(
            log(instant(0, 8)),
            log(instant(0, 14)),
            log(instant(0, 20)),
        )

        val stats = repo.computeStats(zone)

        assertEquals(3, stats.studiedToday)
    }

    @Test
    fun `studied today is zero when no reviews`() = runBlocking {
        val (start, end) = todayRange()
        coEvery { logDao.today(start, end) } returns emptyList()
        coEvery { logDao.allReviewedAt() } returns emptyList()

        val stats = repo.computeStats(zone)

        assertEquals(0, stats.studiedToday)
        assertNull(stats.retention)
    }

    @Test
    fun `retention is null when no reviews today`() = runBlocking {
        val (start, end) = todayRange()
        coEvery { logDao.today(start, end) } returns emptyList()
        coEvery { logDao.allReviewedAt() } returns emptyList()

        val stats = repo.computeStats(zone)

        assertNull(stats.retention)
    }

    @Test
    fun `retention is share of non-Again answers`() = runBlocking {
        val (start, end) = todayRange()
        coEvery { logDao.today(start, end) } returns listOf(
            log(instant(0), rating = 0), // Again
            log(instant(0), rating = 2), // Good
            log(instant(0), rating = 3), // Easy
            log(instant(0), rating = 1), // Hard
        )

        val stats = repo.computeStats(zone)

        assertNotNull(stats.retention)
        assertEquals(0.75f, stats.retention!!, 0.001f)
    }

    @Test
    fun `streak counts consecutive days ending today`() = runBlocking {
        coEvery { logDao.today(any(), any()) } returns emptyList()
        coEvery { logDao.allReviewedAt() } returns listOf(
            instant(0),  // today
            instant(1),  // yesterday
            instant(2),  // 2 days ago
            // gap on day 3
            instant(4),  // 4 days ago
        )

        val stats = repo.computeStats(zone)

        assertEquals(3, stats.streakDays)
    }

    @Test
    fun `streak is zero when no review history`() = runBlocking {
        coEvery { logDao.today(any(), any()) } returns emptyList()
        coEvery { logDao.allReviewedAt() } returns emptyList()

        val stats = repo.computeStats(zone)

        assertEquals(0, stats.streakDays)
    }

    @Test
    fun `streak resets after a gap day`() = runBlocking {
        coEvery { logDao.today(any(), any()) } returns emptyList()
        coEvery { logDao.allReviewedAt() } returns listOf(
            instant(0),  // today
            instant(1),  // yesterday
            instant(2),  // 2 days ago
            // gap
            instant(5),  // 5 days ago
        )

        val stats = repo.computeStats(zone)

        assertEquals(3, stats.streakDays)
    }

    @Test
    fun `streak is zero when no reviews today even if yesterday studied`() = runBlocking {
        coEvery { logDao.today(any(), any()) } returns emptyList()
        coEvery { logDao.allReviewedAt() } returns listOf(
            instant(1),  // yesterday
            instant(2),  // 2 days ago
            instant(3),  // 3 days ago
        )

        val stats = repo.computeStats(zone)

        assertEquals(0, stats.streakDays)
    }

    private fun log(reviewedAt: Long, rating: Int = 2) = ReviewLogEntity(
        id = 1L,
        cardId = 1L,
        reviewedAt = reviewedAt,
        rating = rating,
        interval = 1,
        easeFactor = 2.5f,
        timeTakenMs = 0L,
    )
}
