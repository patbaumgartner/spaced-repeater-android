package com.patbaumgartner.android.spacedrepeater.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.patbaumgartner.android.spacedrepeater.data.local.card.ReviewLogDao
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId

/**
 * Unit tests for [StudyLimitsRepository] — UC-08 daily new-card limit.
 * Tests: limit persistence, budget counting for first-time introductions,
 * remaining budget math including the unlimited (0 = no cap) case.
 */
class StudyLimitsRepositoryTest {
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var prefsEditor: SharedPreferences.Editor
    private lateinit var logDao: ReviewLogDao
    private lateinit var repo: StudyLimitsRepository
    private lateinit var zone: ZoneId

    @Before
    fun setup() {
        context = mockk()
        prefs = mockk()
        prefsEditor = mockk(relaxed = true)
        every { context.getSharedPreferences(StudyLimitsRepository.PREFS_NAME, Context.MODE_PRIVATE) } returns prefs
        every { prefs.getInt(StudyLimitsRepository.KEY_NEW_PER_DAY, StudyLimitsRepository.DEFAULT_NEW_PER_DAY) } returns
            StudyLimitsRepository.DEFAULT_NEW_PER_DAY
        every { prefs.edit() } returns prefsEditor
        logDao = mockk(relaxed = true)
        repo = StudyLimitsRepository(context, logDao)
        zone = ZoneId.of("UTC")
    }

    @Test
    fun `default limit is 20 new cards per day`() {
        assertEquals(20, repo.limits.newPerDay)
    }

    @Test
    fun `updateLimits persists the configured limit`() {
        every { prefsEditor.putInt(any(), any()) } returns prefsEditor

        repo.updateLimits(StudyLimits(newPerDay = 5))

        verify { prefsEditor.putInt(StudyLimitsRepository.KEY_NEW_PER_DAY, 5) }
        verify { prefsEditor.apply() }
    }

    @Test
    fun `updateLimits coerces negative limits to zero`() {
        every { prefsEditor.putInt(any(), any()) } returns prefsEditor

        repo.updateLimits(StudyLimits(newPerDay = -3))

        verify { prefsEditor.putInt(StudyLimitsRepository.KEY_NEW_PER_DAY, 0) }
    }

    @Test
    fun `newIntroducedToday delegates to DAO with day window`() = runBlocking {
        val (start, end) = repo.dayWindow(zone)
        coEvery { logDao.countNewIntroducedToday(start, end) } returns 7

        assertEquals(7, repo.newIntroducedToday(zone))
    }

    @Test
    fun `newRemainingToday subtracts introduced cards from limit`() = runBlocking {
        every { prefs.getInt(any(), any()) } returns 20
        coEvery { logDao.countNewIntroducedToday(any(), any()) } returns 8

        assertEquals(12, repo.newRemainingToday(zone))
    }

    @Test
    fun `newRemainingToday never goes below zero`() = runBlocking {
        every { prefs.getInt(any(), any()) } returns 20
        coEvery { logDao.countNewIntroducedToday(any(), any()) } returns 25

        assertEquals(0, repo.newRemainingToday(zone))
    }

    @Test
    fun `newRemainingToday is unlimited when limit is zero`() = runBlocking {
        every { prefs.getInt(any(), any()) } returns 0

        assertEquals(Int.MAX_VALUE, repo.newRemainingToday(zone))
    }

    @Test
    fun `newRemainingToday is unlimited when limit is negative`() = runBlocking {
        every { prefs.getInt(any(), any()) } returns -1

        assertEquals(Int.MAX_VALUE, repo.newRemainingToday(zone))
    }

    @Test
    fun `dayWindow starts at local midnight and ends at next midnight`() {
        val (start, end) = repo.dayWindow(zone)

        val today = java.time.LocalDate.now(zone)
        assertEquals(today.atStartOfDay(zone).toInstant().toEpochMilli(), start)
        assertEquals(today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), end)
        assertTrue(end > start)
    }
}
