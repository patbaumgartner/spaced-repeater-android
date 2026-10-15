package com.patbaumgartner.android.spacedrepeater.data.repository

import androidx.room.withTransaction
import com.patbaumgartner.android.spacedrepeater.data.local.SpacedRepeaterDatabase
import com.patbaumgartner.android.spacedrepeater.data.local.card.CardDao
import com.patbaumgartner.android.spacedrepeater.data.local.card.CardStateDao
import com.patbaumgartner.android.spacedrepeater.data.local.card.ReviewLogDao
import com.patbaumgartner.android.spacedrepeater.data.local.model.CardEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.CardStateEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.ReviewLogEntity
import com.patbaumgartner.android.spacedrepeater.data.local.model.toDomain
import com.patbaumgartner.android.spacedrepeater.data.scheduler.SM2Scheduler
import com.patbaumgartner.android.spacedrepeater.domain.model.Card
import com.patbaumgartner.android.spacedrepeater.domain.model.CardState
import com.patbaumgartner.android.spacedrepeater.domain.model.ReviewRating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

data class ReviewResult(
    val updatedState: CardState,
    val logId: Long,
    /** Pre-review snapshot so the single-level undo can restore it. */
    val previousState: CardStateEntity?,
)

class CardRepository(
    private val db: SpacedRepeaterDatabase,
    private val cardDao: CardDao,
    private val cardStateDao: CardStateDao,
    private val reviewLogDao: ReviewLogDao,
    private val scheduler: SM2Scheduler,
) {
    fun observeDeckCards(deckId: Long): Flow<List<Card>> =
        cardDao.observeDeckCards(deckId).map { it.map(CardEntity::toDomain) }

    fun observeDueCards(deckId: Long, now: Long = System.currentTimeMillis()): Flow<List<Card>> =
        cardDao.observeDueCards(deckId, now).map { it.map(CardEntity::toDomain) }

    /** Inserts [cards] together with a fresh NEW schedule bound to each generated card id. */
    suspend fun insertCards(cards: List<CardEntity>, dueDate: Long) {
        db.withTransaction {
            val ids = cardDao.insertAll(cards)
            cardStateDao.insertAll(ids.map { CardStateEntity(cardId = it, dueDate = dueDate) })
        }
    }

    suspend fun stateFor(cardId: Long): CardState? = cardStateDao.getById(cardId)?.toDomain()

    suspend fun rate(cardId: Long, rating: ReviewRating, timeTakenMs: Long = 0L): ReviewResult {
        val previous = cardStateDao.getById(cardId)
        val state = previous?.toDomain() ?: CardState(cardId = cardId)
        val schedule = scheduler.calculateNextReview(state, rating)
        val now = Instant.now()
        val newState = CardStateEntity(
            cardId = cardId,
            repetitions = when (rating) {
                ReviewRating.AGAIN -> 0
                ReviewRating.HARD -> state.repetitions + 1
                ReviewRating.GOOD -> state.repetitions + 1
                ReviewRating.EASY -> state.repetitions + 1
            },
            easeFactor = schedule.easeFactor,
            interval = schedule.interval,
            dueDate = schedule.dueAt.toEpochMilli(),
            lastReviewedAt = now.toEpochMilli(),
            phase = schedule.nextPhase.name,
        )
        var logId = 0L
        db.withTransaction {
            cardStateDao.upsert(newState)
            logId = reviewLogDao.insert(
                ReviewLogEntity(
                    cardId = cardId,
                    reviewedAt = now.toEpochMilli(),
                    rating = rating.value,
                    interval = schedule.interval,
                    easeFactor = schedule.easeFactor,
                    timeTakenMs = timeTakenMs,
                ),
            )
        }
        return ReviewResult(updatedState = newState.toDomain(), logId = logId, previousState = previous)
    }

    /** Reverts the most recent rating: restores the snapshot and deletes the review-log entry. */
    suspend fun undo(cardId: Long, previousState: CardStateEntity?, logId: Long) {
        db.withTransaction {
            if (previousState != null) {
                cardStateDao.upsert(previousState)
            } else {
                // The card never had a state before this review; drop it back to NEW.
                cardStateDao.upsert(
                    CardStateEntity(
                        cardId = cardId,
                        dueDate = System.currentTimeMillis(),
                        phase = "NEW",
                    ),
                )
            }
            reviewLogDao.delete(logId)
        }
    }

    /** Next future due timestamp across all cards, or null when nothing is due later. */
    suspend fun nextDueAfter(now: Long): Long? = cardStateDao.nextDue(now)

    /** Count of cards due right now across all decks (for the reminder). */
    suspend fun dueCountNow(now: Long = System.currentTimeMillis()): Int =
        cardStateDao.dueStates(now).size
}
