package com.patbaumgartner.android.spacedrepeater.domain.model

import java.time.Instant

data class Deck(
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val createdAt: Instant = Instant.now(),
    val cardCount: Int = 0,
    val lastStudiedAt: Instant? = null,
)
