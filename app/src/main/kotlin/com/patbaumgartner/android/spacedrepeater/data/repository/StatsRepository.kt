package com.patbaumgartner.android.spacedrepeater.data.repository

import com.patbaumgartner.android.spacedrepeater.data.local.card.ReviewLogDao
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Stats(
    val studiedToday: Int,
    val retention: Float?,      // share of non-Again answers today; null when no reviews
    val streakDays: Int,
)

class StatsRepository(
    private val reviewLogDao: ReviewLogDao,
) {
    suspend fun computeStats(zoneId: ZoneId = ZoneId.systemDefault()): Stats {
        val now = Instant.now()
        val startOfToday = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val startOfTomorrow = LocalDate.now(zoneId).plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val today = reviewLogDao.today(startOfToday, startOfTomorrow)
        val studiedToday = today.size
        val retention = if (today.isEmpty()) null else {
            val nonAgain = today.count { it.rating != 0 }
            nonAgain.toFloat() / today.size
        }

        val days = reviewLogDao.allReviewedAt()
            .map { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate().toEpochDay() }
            .toSet()
        val streak = longestStreak(days, zoneId)
        return Stats(studiedToday, retention, streak)
    }

    /**
     * The consecutive-day run ending at the most recent studied day (today or
     * yesterday). Returning the last achieved streak instead of 0 keeps the
     * number stable on a day the learner has not yet studied; an empty history
     * still yields 0.
     */
    private fun longestStreak(dayBuckets: Set<Long>, zoneId: ZoneId): Int {
        if (dayBuckets.isEmpty()) return 0
        val today = LocalDate.now(zoneId).toEpochDay()
        val yesterday = today - 1
        val anchor = when {
            today in dayBuckets -> today
            yesterday in dayBuckets -> yesterday
            else -> return 0
        }
        var streak = 0
        var day = anchor
        while (day in dayBuckets) {
            streak += 1
            day -= 1
        }
        return streak
    }
}
