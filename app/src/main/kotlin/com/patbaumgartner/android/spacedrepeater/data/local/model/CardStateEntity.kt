package com.patbaumgartner.android.spacedrepeater.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "card_states",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [androidx.room.Index("dueDate")],
)
data class CardStateEntity(
    @PrimaryKey
    val cardId: Long,
    val repetitions: Int = 0,
    val easeFactor: Float = 2.5f,
    val interval: Int = 0,
    val dueDate: Long,
    val lastReviewedAt: Long? = null,
    val phase: String = "NEW",
)
