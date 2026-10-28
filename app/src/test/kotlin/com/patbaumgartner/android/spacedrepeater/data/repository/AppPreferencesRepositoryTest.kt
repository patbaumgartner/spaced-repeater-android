package com.patbaumgartner.android.spacedrepeater.data.repository

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AppPreferencesRepository] — persistence of theme and
 * reminder preferences backing the Settings screen.
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
    fun `darkTheme returns null when unset`() {
        every { prefs.getString(AppPreferencesRepository.KEY_DARK_THEME, null) } returns null

        assertNull(repo.darkTheme())
    }

    @Test
    fun `darkTheme returns stored true and false values`() {
        every { prefs.getString(AppPreferencesRepository.KEY_DARK_THEME, null) } returns "true"
        assertEquals(true, repo.darkTheme())

        every { prefs.getString(AppPreferencesRepository.KEY_DARK_THEME, null) } returns "false"
        assertEquals(false, repo.darkTheme())
    }

    @Test
    fun `updateDarkTheme true persists true`() {
        repo.updateDarkTheme(true)

        verify { prefsEditor.putString(AppPreferencesRepository.KEY_DARK_THEME, "true") }
    }

    @Test
    fun `updateDarkTheme null removes the override`() {
        repo.updateDarkTheme(null)

        verify { prefsEditor.remove(AppPreferencesRepository.KEY_DARK_THEME) }
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
