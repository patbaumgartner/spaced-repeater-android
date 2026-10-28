package com.patbaumgartner.android.spacedrepeater.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** User-adjustable app preferences: theme, reminders, and the daily new-card limit. */
data class AppPreferences(
    /** 0 means unlimited (see [StudyLimitsRepository]). */
    val newPerDay: Int,
    val remindersEnabled: Boolean,
    /** Null follows the system setting; true/false force dark/light. */
    val darkTheme: Boolean?,
)

/**
 * Central access point for persisted user preferences. Plain
 * [SharedPreferences] — no DataStore dependency, matching [StudyLimitsRepository].
 */
class AppPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val prefs: SharedPreferences
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun darkTheme(): Boolean? =
        when (prefs.getString(KEY_DARK_THEME, null)) {
            VALUE_TRUE -> true
            VALUE_FALSE -> false
            else -> null
        }

    fun updateDarkTheme(value: Boolean?) {
        prefs.edit().apply {
            if (value == null) remove(KEY_DARK_THEME) else putString(KEY_DARK_THEME, value.toString())
            apply()
        }
    }

    fun remindersEnabled(default: Boolean = true): Boolean =
        prefs.getBoolean(KEY_REMINDERS_ENABLED, default)

    fun updateRemindersEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, value).apply()
    }

    companion object {
        const val PREFS_NAME = "app_preferences"
        const val KEY_DARK_THEME = "dark_theme"
        const val KEY_REMINDERS_ENABLED = "reminders_enabled"

        private const val VALUE_TRUE = "true"
        private const val VALUE_FALSE = "false"
    }
}
