package com.patbaumgartner.android.spacedrepeater.data.local.model

import com.patbaumgartner.android.spacedrepeater.domain.model.Card
import com.patbaumgartner.android.spacedrepeater.domain.model.CardPhase
import com.patbaumgartner.android.spacedrepeater.domain.model.CardState
import com.patbaumgartner.android.spacedrepeater.domain.model.Deck
import com.patbaumgartner.android.spacedrepeater.domain.model.ReviewLog
import java.time.Instant

fun DeckEntity.toDomain(): Deck = Deck(
    id = id,
    name = name,
    description = description,
    createdAt = Instant.ofEpochMilli(createdAt),
    cardCount = cardCount,
    lastStudiedAt = lastStudiedAt?.let(Instant::ofEpochMilli),
)

fun Deck.toEntity(): DeckEntity = DeckEntity(
    id = id,
    name = name,
    description = description,
    createdAt = createdAt.toEpochMilli(),
    cardCount = cardCount,
    lastStudiedAt = lastStudiedAt?.toEpochMilli(),
)

fun CardEntity.toDomain(): Card = Card(
    id = id,
    deckId = deckId,
    front = front,
    back = back,
    tags = tags,
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun CardStateEntity.toDomain(): CardState = CardState(
    cardId = cardId,
    repetitions = repetitions,
    easeFactor = easeFactor,
    interval = interval,
    dueAt = Instant.ofEpochMilli(dueDate),
    lastReviewedAt = lastReviewedAt?.let(Instant::ofEpochMilli),
    phase = CardPhase.valueOf(phase),
)

fun ReviewLogEntity.toDomain(): ReviewLog = ReviewLog(
    id = id,
    cardId = cardId,
    reviewedAt = Instant.ofEpochMilli(reviewedAt),
    rating = rating,
    interval = interval,
    easeFactor = easeFactor,
    timeTakenMs = timeTakenMs,
)
