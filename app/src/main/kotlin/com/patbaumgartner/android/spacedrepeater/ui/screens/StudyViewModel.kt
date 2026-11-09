package com.patbaumgartner.android.spacedrepeater.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patbaumgartner.android.spacedrepeater.data.repository.CardRepository
import com.patbaumgartner.android.spacedrepeater.data.repository.DeckRepository
import com.patbaumgartner.android.spacedrepeater.data.repository.StudyLimitsRepository
import com.patbaumgartner.android.spacedrepeater.data.scheduler.SM2Scheduler
import com.patbaumgartner.android.spacedrepeater.domain.model.Card
import com.patbaumgartner.android.spacedrepeater.domain.model.CardPhase
import com.patbaumgartner.android.spacedrepeater.domain.model.CardState
import com.patbaumgartner.android.spacedrepeater.domain.model.ReviewRating
import com.patbaumgartner.android.spacedrepeater.service.ReviewReminderService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudyUiState(
    val deckId: Long = 0L,
    val cards: List<Card> = emptyList(),
    val index: Int = 0,
    val answered: Boolean = false,
    val done: Boolean = false,
    /** Due NEW cards held back by the daily limit; they become available tomorrow. */
    val newCardsHeldBack: Int = 0,
) {
    val current: Card? get() = cards.getOrNull(index)
    val total: Int get() = cards.size
    val remaining: Int get() = (total - index).coerceAtLeast(0)
}

/**
 * Applies the daily new-card limit (UC-08) to a due-card list. Pure function.
 *
 * Cards currently in the NEW phase are first-time introductions: only
 * [newRemaining] of them are allowed into the session (Anki semantics).
 * Learning/review/relearning cards are never capped. The DAO's due ordering
 * (learning/relearning first, then by due date) is preserved.
 */
internal fun applyNewCardLimit(
    due: List<Card>,
    phases: Map<Long, CardPhase>,
    newRemaining: Int,
): StudyUiState {
    val (newCards, otherCards) = due.partition { phases[it.id] == CardPhase.NEW }
    val allowedNew = if (newRemaining == Int.MAX_VALUE) newCards else newCards.take(newRemaining)
    return StudyUiState(
        cards = otherCards + allowedNew,
        newCardsHeldBack = (newCards.size - allowedNew.size).coerceAtLeast(0),
    )
}

/** Single-level undo history: what was rated and the snapshot needed to revert it. */
private data class RatedReview(
    val cardId: Long,
    val logId: Long,
    val previousState: com.patbaumgartner.android.spacedrepeater.data.local.model.CardStateEntity?,
)

/** What to do with the pending review reminder after a session (UC-07). */
internal enum class ReminderDecision { SCHEDULE, CANCEL }

/**
 * UC-07 reminder decision — pure and unit-testable.
 *
 * - Reminders disabled in Settings → cancel any pending reminder.
 * - **3b. No card due later ([nextDue] == null) → no reminder is scheduled**
 *   (and any stale pending one is cancelled).
 * - Otherwise schedule at the next real due timestamp (never a placeholder).
 */
internal fun reminderDecision(remindersEnabled: Boolean, nextDue: Long?): ReminderDecision =
    if (remindersEnabled && nextDue != null) ReminderDecision.SCHEDULE else ReminderDecision.CANCEL

@HiltViewModel
class StudyViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val deckRepository: DeckRepository,
    private val reminderService: ReviewReminderService,
    private val studyLimitsRepository: StudyLimitsRepository,
    private val appPreferencesRepository: com.patbaumgartner.android.spacedrepeater.data.repository.AppPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private var lastReview: RatedReview? = null

    fun startSession(deckId: Long) {
        if (_uiState.value.deckId == deckId && _uiState.value.cards.isNotEmpty()) return
        lastReview = null
        viewModelScope.launch {
            deckRepository.markStudied(deckId)
            val due = cardRepository.observeDueCards(deckId).first()
            val newRemaining = studyLimitsRepository.newRemainingToday()
            val phases = cardRepository.phasesForDeck(deckId)
            _uiState.value = applyNewCardLimit(due, phases, newRemaining)
                .copy(deckId = deckId)
        }
    }

    fun reveal() {
        val s = _uiState.value
        if (s.current != null && !s.answered) {
            _uiState.value = s.copy(answered = true)
        }
    }

    fun rate(rating: ReviewRating) {
        val s = _uiState.value
        val card = s.current ?: return
        if (!s.answered) return
        viewModelScope.launch {
            val result = cardRepository.rate(card.id, rating)
            lastReview = RatedReview(card.id, result.logId, result.previousState)
            if (s.index + 1 >= s.total) {
                _uiState.value = s.copy(answered = false, done = true)
                scheduleReminder()
            } else {
                _uiState.value = s.copy(index = s.index + 1, answered = false)
            }
        }
    }

    fun undo() {
        val review = lastReview ?: return
        val s = _uiState.value
        if (s.index == 0 || s.done) return
        viewModelScope.launch {
            cardRepository.undo(review.cardId, review.previousState, review.logId)
            lastReview = null
            _uiState.value = s.copy(index = s.index - 1, answered = true)
        }
    }

    fun canUndo(): Boolean = lastReview != null && !_uiState.value.done && _uiState.value.index > 0

    internal fun scheduleReminder() {
        viewModelScope.launch {
            val nextDue = cardRepository.nextDueAfter(now = System.currentTimeMillis())
            when (reminderDecision(appPreferencesRepository.remindersEnabled(), nextDue)) {
                // Schedule at the next real due timestamp; the worker re-checks the
                // due count and notification permission again at execution time.
                ReminderDecision.SCHEDULE -> reminderService.scheduleNext(nextDue!!)
                // UC-07 3b / opt-out: nothing to remind about — drop any pending work.
                ReminderDecision.CANCEL -> reminderService.cancel()
            }
        }
    }
}
