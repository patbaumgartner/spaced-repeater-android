package com.patbaumgartner.android.spacedrepeater.data.local.model

import androidx.room.Embedded

data class DeckWithCounts(
    @Embedded
    val deck: DeckEntity,
    val totalCards: Int,
    val dueCards: Int,
    val studiedCards: Int,
)
