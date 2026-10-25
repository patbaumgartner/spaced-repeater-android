package com.patbaumgartner.android.spacedrepeater.data.scheduler

import com.patbaumgartner.android.spacedrepeater.domain.model.CardPhase
import com.patbaumgartner.android.spacedrepeater.domain.model.CardSchedule
import com.patbaumgartner.android.spacedrepeater.domain.model.CardState
import com.patbaumgartner.android.spacedrepeater.domain.model.ReviewRating
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

/**
 * Pure SM-2 (Anki variant) scheduler. No Android or Room dependencies.
 * Ratings: Again(0), Hard(1), Good(2), Easy(3).
 *
 * Learning steps for new/lapsed cards: 1 min -> 10 min -> graduate (1 day).
 */
class SM2Scheduler {
    private val easeFloor = 1.3f
    private val easyBonus = 1.3f

    fun calculateNextReview(state: CardState, rating: ReviewRating, now: Instant = Instant.now()): CardSchedule {
        return when (state.phase) {
            CardPhase.NEW, CardPhase.LEARNING -> learningStep(state, rating, now)
            CardPhase.REVIEW -> reviewStep(state, rating, now)
            CardPhase.RELEARNING -> relearningStep(state, rating, now)
        }
    }

    private fun learningStep(state: CardState, rating: ReviewRating, now: Instant): CardSchedule =
        when (rating) {
            ReviewRating.AGAIN -> CardSchedule(
                interval = 1,
                easeFactor = (state.easeFactor - 0.2f).coerceAtLeast(easeFloor),
                dueAt = now.plus(Duration.ofMinutes(1)),
                nextPhase = CardPhase.LEARNING,
            )
            ReviewRating.HARD -> CardSchedule(
                interval = 10,
                easeFactor = (state.easeFactor - 0.15f).coerceAtLeast(easeFloor),
                dueAt = now.plus(Duration.ofMinutes(10)),
                nextPhase = CardPhase.LEARNING,
            )
            ReviewRating.GOOD -> CardSchedule(
                interval = 1,
                easeFactor = state.easeFactor.coerceAtLeast(easeFloor),
                dueAt = now.plus(Duration.ofDays(1)),
                nextPhase = CardPhase.REVIEW,
            )
            ReviewRating.EASY -> CardSchedule(
                interval = 6,
                easeFactor = (state.easeFactor + 0.15f).coerceAtLeast(easeFloor),
                dueAt = now.plus(Duration.ofDays(6)),
                nextPhase = CardPhase.REVIEW,
            )
        }

    private fun relearningStep(state: CardState, rating: ReviewRating, now: Instant): CardSchedule =
        when (rating) {
            ReviewRating.AGAIN -> CardSchedule(
                interval = 1,
                easeFactor = (state.easeFactor - 0.2f).coerceAtLeast(easeFloor),
                dueAt = now.plus(Duration.ofMinutes(1)),
                nextPhase = CardPhase.RELEARNING,
            )
            ReviewRating.HARD -> CardSchedule(
                interval = 10,
                easeFactor = (state.easeFactor - 0.15f).coerceAtLeast(easeFloor),
                dueAt = now.plus(Duration.ofMinutes(10)),
                nextPhase = CardPhase.RELEARNING,
            )
            ReviewRating.GOOD, ReviewRating.EASY -> CardSchedule(
                interval = 1,
                easeFactor = if (rating == ReviewRating.EASY) {
                    (state.easeFactor + 0.15f).coerceAtLeast(easeFloor)
                } else {
                    state.easeFactor.coerceAtLeast(easeFloor)
                },
                dueAt = now.plus(Duration.ofDays(1)),
                nextPhase = CardPhase.REVIEW,
            )
        }

    private fun reviewStep(state: CardState, rating: ReviewRating, now: Instant): CardSchedule =
        when (rating) {
            ReviewRating.AGAIN -> CardSchedule(
                interval = 1,
                easeFactor = (state.easeFactor - 0.2f).coerceAtLeast(easeFloor),
                dueAt = now.plus(Duration.ofMinutes(1)),
                nextPhase = CardPhase.RELEARNING,
            )
            ReviewRating.HARD -> {
                val ef = (state.easeFactor - 0.15f).coerceAtLeast(easeFloor)
                val interval = if (state.interval == 0) 6 else (state.interval * ef).roundToInt()
                CardSchedule(
                    interval = interval,
                    easeFactor = ef,
                    dueAt = now.plus(Duration.ofDays(interval.toLong())),
                    nextPhase = CardPhase.REVIEW,
                )
            }
            ReviewRating.GOOD -> {
                val ef = state.easeFactor.coerceAtLeast(easeFloor)
                val interval = if (state.interval == 0) 6 else (state.interval * ef).roundToInt()
                CardSchedule(
                    interval = interval,
                    easeFactor = ef,
                    dueAt = now.plus(Duration.ofDays(interval.toLong())),
                    nextPhase = CardPhase.REVIEW,
                )
            }
            ReviewRating.EASY -> {
                val ef = (state.easeFactor + 0.15f).coerceAtLeast(easeFloor)
                val base = if (state.interval == 0) 6 else (state.interval * ef).roundToInt()
                val interval = (base * easyBonus).roundToInt()
                CardSchedule(
                    interval = interval,
                    easeFactor = ef,
                    dueAt = now.plus(Duration.ofDays(interval.toLong())),
                    nextPhase = CardPhase.REVIEW,
                )
            }
        }
}
