package com.patbaumgartner.android.spacedrepeater.data.repository

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AppPreferencesRepository] — persistence of the
 * reminder preference backing the Settings screen.
 */
class AppPreferencesRepositoryTest {
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var prefsEditor: SharedPreferences.Editor
    private lateinit var repo: AppPreferencesRepository

    @Before
    fun setup() {
        context = mockk()
        prefs = mockk(relaxed = true)
        prefsEditor = mockk(relaxed = true)
        every { context.getSharedPreferences(AppPreferencesRepository.PREFS_NAME, Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns prefsEditor
        repo = AppPreferencesRepository(context)
    }

    @Test
    fun `remindersEnabled defaults to true`() {
        every { prefs.getBoolean(AppPreferencesRepository.KEY_REMINDERS_ENABLED, true) } returns true

        assertTrue(repo.remindersEnabled())
    }

    @Test
    fun `updateRemindersEnabled persists the toggle`() {
        repo.updateRemindersEnabled(false)

        verify { prefsEditor.putBoolean(AppPreferencesRepository.KEY_REMINDERS_ENABLED, false) }
    }
}
