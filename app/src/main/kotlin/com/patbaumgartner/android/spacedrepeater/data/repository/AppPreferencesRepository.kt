package com.patbaumgartner.android.spacedrepeater.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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

    /**
     * Emits the dark-theme preference whenever it changes, so the UI can react
     * without an activity restart. Replays the current value on collection.
     */
    fun darkThemeFlow(): Flow<Boolean?> =
        callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
                if (key == KEY_DARK_THEME) {
                    when (sp.getString(KEY_DARK_THEME, null)) {
                        VALUE_TRUE -> trySend(true)
                        VALUE_FALSE -> trySend(false)
                        else -> trySend(null)
                    }
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            trySend(darkTheme())
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }

    fun updateDarkTheme(value: Boolean?) {
        prefs.edit().apply {
            if (value == null) remove(KEY_DARK_THEME) else putString(KEY_DARK_THEME, value.toString())
            apply()
        }
    }

    fun remindersEnabled(default: Boolean = true): Boolean =
        prefs.getBoolean(KEY_REMINDERS_ENABLED, default)

    /**
     * Emits the reminders toggle whenever it changes, so observers (e.g. the
     * notification-permission request in MainActivity) react without a restart.
     * Replays the current value on collection.
     */
    fun remindersEnabledFlow(): Flow<Boolean> =
        callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
                if (key == KEY_REMINDERS_ENABLED) {
                    trySend(sp.getBoolean(KEY_REMINDERS_ENABLED, true))
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            trySend(remindersEnabled())
            awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }

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
