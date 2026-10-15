package com.patbaumgartner.android.spacedrepeater.data.local.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "decks",
    indices = [Index(value = ["lastStudiedAt"])],
)
data class DeckEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val createdAt: Long,
    val cardCount: Int = 0,
    val lastStudiedAt: Long? = null,
)
