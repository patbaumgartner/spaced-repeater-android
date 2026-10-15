package com.patbaumgartner.android.spacedrepeater.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patbaumgartner.android.spacedrepeater.data.repository.DeckRepository
import com.patbaumgartner.android.spacedrepeater.domain.model.Deck
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeDeckUi(
    val deck: Deck,
    val dueCount: Int,
    val studiedCount: Int,
)

data class HomeUiState(
    val decks: List<HomeDeckUi> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deckRepository: DeckRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = deckRepository.observeDecks()
        .map { rows -> HomeUiState(decks = rows.map { HomeDeckUi(it.deck, it.dueCount, it.studiedCount) }, loading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun deleteDeck(deck: Deck) {
        viewModelScope.launch { deckRepository.delete(deck.id) }
    }
}
