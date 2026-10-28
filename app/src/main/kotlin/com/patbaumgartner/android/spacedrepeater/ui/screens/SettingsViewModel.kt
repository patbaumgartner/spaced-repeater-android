package com.patbaumgartner.android.spacedrepeater.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patbaumgartner.android.spacedrepeater.data.repository.AppPreferences
import com.patbaumgartner.android.spacedrepeater.data.repository.AppPreferencesRepository
import com.patbaumgartner.android.spacedrepeater.data.repository.StudyLimits
import com.patbaumgartner.android.spacedrepeater.data.repository.StudyLimitsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    /** Maximum NEW cards introduced per day; 0 = unlimited. */
    val newPerDay: Int = StudyLimitsRepository.DEFAULT_NEW_PER_DAY,
    val remindersEnabled: Boolean = true,
    /** Null follows the system dark-mode setting. */
    val darkTheme: Boolean? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val studyLimitsRepository: StudyLimitsRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            newPerDay = studyLimitsRepository.limits.newPerDay,
            remindersEnabled = appPreferencesRepository.remindersEnabled(),
            darkTheme = appPreferencesRepository.darkTheme(),
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /** Persists the daily new-card limit; 0 disables the cap. */
    fun setNewPerDay(value: Int) {
        val coerced = value.coerceAtLeast(0)
        studyLimitsRepository.updateLimits(StudyLimits(newPerDay = coerced))
        _uiState.value = _uiState.value.copy(newPerDay = coerced)
    }

    fun setRemindersEnabled(enabled: Boolean) {
        appPreferencesRepository.updateRemindersEnabled(enabled)
        _uiState.value = _uiState.value.copy(remindersEnabled = enabled)
    }

    /** Null means "follow system". */
    fun setDarkTheme(value: Boolean?) {
        appPreferencesRepository.updateDarkTheme(value)
        _uiState.value = _uiState.value.copy(darkTheme = value)
    }
}
