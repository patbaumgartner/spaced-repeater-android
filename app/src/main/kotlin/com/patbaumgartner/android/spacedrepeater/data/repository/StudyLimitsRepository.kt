package com.patbaumgartner.android.spacedrepeater.data.repository

import android.content.Context
import com.patbaumgartner.android.spacedrepeater.data.local.card.ReviewLogDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Per-day study limits, Anki-style. Zero means unlimited. */
data class StudyLimits(
    /** Maximum NEW cards introduced per calendar day; 0 disables the cap. */
    val newPerDay: Int,
)

/**
 * Reads the daily study limits and tracks how much of today's NEW budget has
 * been consumed. A card consumes budget exactly once per day: on the review
 * that moves it out of the NEW phase (its first-ever review).
 */
class StudyLimitsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reviewLogDao: ReviewLogDao,
) {
    val limits: StudyLimits
        get() = StudyLimits(
            newPerDay = prefs.getInt(KEY_NEW_PER_DAY, DEFAULT_NEW_PER_DAY),
        )

    fun updateLimits(limits: StudyLimits) {
        prefs.edit()
            .putInt(KEY_NEW_PER_DAY, limits.newPerDay.coerceAtLeast(0))
            .apply()
    }

    /** Distinct cards whose first-ever review happened today. */
    suspend fun newIntroducedToday(zoneId: ZoneId = ZoneId.systemDefault()): Int {
        val window = dayWindow(zoneId)
        return reviewLogDao.countNewIntroducedToday(window.first, window.second)
    }

    /**
     * How many NEW cards may still be introduced today given the configured
     * limit. Returns [Int.MAX_VALUE] when the limit is unlimited.
     */
    suspend fun newRemainingToday(zoneId: ZoneId = ZoneId.systemDefault()): Int {
        val limit = limits.newPerDay
        if (limit <= 0) return Int.MAX_VALUE
        val introduced = newIntroducedToday(zoneId)
        return (limit - introduced).coerceAtLeast(0)
    }

    /** Day window as (startOfTodayInclusive, startOfTomorrowExclusive) epoch millis. */
    fun dayWindow(zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val today = LocalDate.now(zoneId)
        val start = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return start to end
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        const val PREFS_NAME = "study_limits"
        const val KEY_NEW_PER_DAY = "new_per_day"
        const val DEFAULT_NEW_PER_DAY = 20

        /** Sentinel "no limit" value used by [newRemainingToday]. */
        const val UNLIMITED = Int.MAX_VALUE
    }
}
