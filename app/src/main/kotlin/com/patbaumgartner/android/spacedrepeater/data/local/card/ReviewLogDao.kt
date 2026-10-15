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

    @Query("SELECT * FROM review_log ORDER BY reviewedAt DESC LIMIT 1")
    suspend fun latest(): ReviewLogEntity?

    @Query("SELECT COUNT(*) FROM review_log WHERE reviewedAt >= :startOfToday AND reviewedAt < :startOfTomorrow")
    suspend fun countToday(startOfToday: Long, startOfTomorrow: Long): Int

    @Query("SELECT * FROM review_log WHERE reviewedAt >= :startOfToday AND reviewedAt < :startOfTomorrow")
    suspend fun today(startOfToday: Long, startOfTomorrow: Long): List<ReviewLogEntity>

    @Query("SELECT reviewedAt FROM review_log")
    suspend fun allReviewedAt(): List<Long>
}
