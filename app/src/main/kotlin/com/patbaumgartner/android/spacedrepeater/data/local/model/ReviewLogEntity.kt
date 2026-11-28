package com.patbaumgartner.android.spacedrepeater.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.patbaumgartner.android.spacedrepeater.domain.model.CardPhase

@Entity(
    tableName = "review_log",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("cardId"), Index("reviewedAt")],
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val cardId: Long,
    val reviewedAt: Long,
    val rating: Int,
    val interval: Int,
    val easeFactor: Float,
    val timeTakenMs: Long = 0L,
    /**
     * Card phase before this review for newly recorded rows; NEW marks an introduction.
     * Migrated v1 history is lossy: only the earliest row per card is NEW, while
     * later REVIEW values are non-NEW budget sentinels, not exact historical
     * LEARNING/RELEARNING phases or replay state.
     */
    val previousPhase: String = CardPhase.NEW.name,
)
