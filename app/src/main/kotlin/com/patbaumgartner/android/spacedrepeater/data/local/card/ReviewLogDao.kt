package com.patbaumgartner.android.spacedrepeater.data.local.card

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.patbaumgartner.android.spacedrepeater.data.local.model.ReviewLogEntity

@Dao
interface ReviewLogDao {
    @Insert
    suspend fun insert(entry: ReviewLogEntity): Long

    @Query("DELETE FROM review_log WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM review_log WHERE id = :id")
    suspend fun getById(id: Long): ReviewLogEntity?

    @Query("UPDATE review_log SET reviewedAt = :ts WHERE id = :id")
    suspend fun updateReviewedAt(id: Long, ts: Long)

    @Query("SELECT * FROM review_log ORDER BY reviewedAt DESC LIMIT 1")
    suspend fun latest(): ReviewLogEntity?

    @Query("SELECT COUNT(*) FROM review_log WHERE reviewedAt >= :startOfToday AND reviewedAt < :startOfTomorrow")
    suspend fun countToday(startOfToday: Long, startOfTomorrow: Long): Int

    @Query("SELECT * FROM review_log WHERE reviewedAt >= :startOfToday AND reviewedAt < :startOfTomorrow")
    suspend fun today(startOfToday: Long, startOfTomorrow: Long): List<ReviewLogEntity>

    @Query("SELECT reviewedAt FROM review_log")
    suspend fun allReviewedAt(): List<Long>

    /**
     * Number of distinct cards introduced today, i.e. cards whose first review
     * happened inside the [startOfToday, startOfTomorrow) window. Counts each
     * card once regardless of how many NEW-phase reviews it received that day
     * (a 1-minute learning step must not consume the budget twice).
     */
    @Query(
        """
        SELECT COUNT(*) FROM (
            SELECT DISTINCT cardId FROM review_log
            WHERE reviewedAt >= :startOfToday AND reviewedAt < :startOfTomorrow
              AND previousPhase = 'NEW'
        )
        """,
    )
    suspend fun countNewIntroducedToday(startOfToday: Long, startOfTomorrow: Long): Int
}
