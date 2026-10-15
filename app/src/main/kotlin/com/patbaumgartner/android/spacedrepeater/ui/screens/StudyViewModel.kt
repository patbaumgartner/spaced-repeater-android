package com.patbaumgartner.android.spacedrepeater.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patbaumgartner.android.spacedrepeater.data.repository.CardRepository
import com.patbaumgartner.android.spacedrepeater.data.repository.DeckRepository
import com.patbaumgartner.android.spacedrepeater.data.scheduler.SM2Scheduler
import com.patbaumgartner.android.spacedrepeater.domain.model.Card
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
) {
    val current: Card? get() = cards.getOrNull(index)
    val total: Int get() = cards.size
    val remaining: Int get() = (total - index).coerceAtLeast(0)
}

/** Single-level undo history: what was rated and the snapshot needed to revert it. */
private data class RatedReview(
    val cardId: Long,
    val logId: Long,
    val previousState: com.patbaumgartner.android.spacedrepeater.data.local.model.CardStateEntity?,
)

@HiltViewModel
class StudyViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val deckRepository: DeckRepository,
    private val reminderService: ReviewReminderService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private var lastReview: RatedReview? = null

    fun startSession(deckId: Long) {
        if (_uiState.value.deckId == deckId && _uiState.value.cards.isNotEmpty()) return
        lastReview = null
        viewModelScope.launch {
            deckRepository.markStudied(deckId)
            val cards = cardRepository.observeDueCards(deckId).first()
            _uiState.value = StudyUiState(deckId = deckId, cards = cards)
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

    private fun scheduleReminder() {
        viewModelScope.launch {
            val nextDue = cardRepository.nextDueAfter(now = System.currentTimeMillis())
            if (nextDue != null) {
                val count = cardRepository.dueCountNow()
                reminderService.scheduleNext(nextDue, count)
            }
        }
    }
}
