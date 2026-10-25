package com.patbaumgartner.android.spacedrepeater.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patbaumgartner.android.spacedrepeater.data.repository.Stats
import com.patbaumgartner.android.spacedrepeater.data.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
) : ViewModel() {
    private val _stats = MutableStateFlow<Stats?>(null)
    val stats: StateFlow<Stats?> = _stats.asStateFlow()

    init {
        refresh()
    }

    /** Recomputes stats so the screen reflects reviews made since the last visit. */
    fun refresh() {
        viewModelScope.launch {
            _stats.value = statsRepository.computeStats()
        }
    }
}
