package com.patbaumgartner.android.spacedrepeater.domain.model

import java.time.Instant

data class Card(
    val id: Long = 0L,
    val deckId: Long,
    val front: String,
    val back: String,
    val tags: String = "",
    val createdAt: Instant = Instant.now(),
)

enum class CardPhase {
    NEW,
    LEARNING,
    REVIEW,
    RELARNING,
}

data class CardState(
    val cardId: Long,
    val repetitions: Int = 0,
    val easeFactor: Float = 2.5f,
    val interval: Int = 0,
    val dueAt: Instant = Instant.now(),
    val lastReviewedAt: Instant? = null,
    val phase: CardPhase = CardPhase.NEW,
)

enum class ReviewRating(val value: Int) {
    AGAIN(0),
    HARD(1),
    GOOD(2),
    EASY(3),
}

data class ReviewLog(
    val id: Long = 0L,
    val cardId: Long,
    val reviewedAt: Instant = Instant.now(),
    val rating: Int,
    val interval: Int,
    val easeFactor: Float,
    val timeTakenMs: Long = 0L,
)

data class CardSchedule(
    val interval: Int,
    val easeFactor: Float,
    val dueAt: Instant,
    val nextPhase: CardPhase,
)
