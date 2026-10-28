package com.patbaumgartner.android.spacedrepeater.ui.screens

import android.content.Context
import android.content.SharedPreferences
import com.patbaumgartner.android.spacedrepeater.data.repository.AppPreferencesRepository
import com.patbaumgartner.android.spacedrepeater.data.repository.StudyLimitsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SettingsViewModel] — Settings screen state and persistence
 * of the daily new-card limit, reminders toggle, and theme preference.
 * Real repositories over mocked SharedPreferences (mirrors the DAO-mocking
 * pattern used by the repository tests).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var limitPrefs: SharedPreferences
    private lateinit var limitEditor: SharedPreferences.Editor
    private lateinit var appPrefs: SharedPreferences
    private lateinit var appPrefsEditor: SharedPreferences.Editor
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        limitEditor = mockk(relaxed = true)
        limitPrefs = mockk {
            every { getInt(StudyLimitsRepository.KEY_NEW_PER_DAY, any()) } returns 20
            every { edit() } returns limitEditor
        }
        appPrefsEditor = mockk(relaxed = true)
        appPrefs = mockk {
            every { edit() } returns appPrefsEditor
            every { getBoolean(AppPreferencesRepository.KEY_REMINDERS_ENABLED, true) } returns true
            every { getString(AppPreferencesRepository.KEY_DARK_THEME, null) } returns null
        }
        val context = mockk<Context> {
            every { getSharedPreferences(StudyLimitsRepository.PREFS_NAME, any()) } returns limitPrefs
            every { getSharedPreferences(AppPreferencesRepository.PREFS_NAME, any()) } returns appPrefs
        }
        val studyLimitsRepository = StudyLimitsRepository(context, mockk(relaxed = true))
        val appPreferencesRepository = AppPreferencesRepository(context)
        viewModel = SettingsViewModel(studyLimitsRepository, appPreferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state reflects stored preferences`() {
        val state = viewModel.uiState.value

        assertEquals(20, state.newPerDay)
        assertEquals(true, state.remindersEnabled)
        assertEquals(null, state.darkTheme)
    }

    @Test
    fun `setNewPerDay persists and updates state`() {
        every { limitEditor.putInt(any(), any()) } returns limitEditor

        viewModel.setNewPerDay(10)

        verify { limitEditor.putInt(StudyLimitsRepository.KEY_NEW_PER_DAY, 10) }
        assertEquals(10, viewModel.uiState.value.newPerDay)
    }

    @Test
    fun `setNewPerDay coerces negative values to zero`() {
        every { limitEditor.putInt(any(), any()) } returns limitEditor

        viewModel.setNewPerDay(-3)

        verify { limitEditor.putInt(StudyLimitsRepository.KEY_NEW_PER_DAY, 0) }
        assertEquals(0, viewModel.uiState.value.newPerDay)
    }

    @Test
    fun `setRemindersEnabled persists and updates state`() {
        viewModel.setRemindersEnabled(false)

        verify { appPrefsEditor.putBoolean(AppPreferencesRepository.KEY_REMINDERS_ENABLED, false) }
        assertEquals(false, viewModel.uiState.value.remindersEnabled)
    }

    @Test
    fun `setDarkTheme persists and updates state`() {
        every { appPrefsEditor.putString(any(), any()) } returns appPrefsEditor

        viewModel.setDarkTheme(true)

        verify { appPrefsEditor.putString(AppPreferencesRepository.KEY_DARK_THEME, "true") }
        assertEquals(true, viewModel.uiState.value.darkTheme)
    }

    @Test
    fun `setDarkTheme null clears the override`() {
        viewModel.setDarkTheme(null)

        verify { appPrefsEditor.remove(AppPreferencesRepository.KEY_DARK_THEME) }
        assertEquals(null, viewModel.uiState.value.darkTheme)
    }
}
