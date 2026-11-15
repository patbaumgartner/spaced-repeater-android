package com.patbaumgartner.android.spacedrepeater.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** User-adjustable app preferences: reminders and the daily new-card limit. */
data class AppPreferences(
    /** 0 means unlimited (see [StudyLimitsRepository]). */
    val newPerDay: Int,
    val remindersEnabled: Boolean,
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
        const val KEY_REMINDERS_ENABLED = "reminders_enabled"
    }
}
