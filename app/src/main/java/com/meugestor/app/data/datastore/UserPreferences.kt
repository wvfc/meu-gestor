package com.meugestor.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    private object Keys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val USER_NAME = stringPreferencesKey("user_name")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val PIN_CODE = stringPreferencesKey("pin_code")
        val LAST_BACKUP_DATE = stringPreferencesKey("last_backup_date")
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[Keys.IS_DARK_MODE] ?: false }

    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[Keys.HAS_COMPLETED_ONBOARDING] ?: false }

    val userName: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[Keys.USER_NAME] ?: "" }

    val defaultCurrency: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[Keys.DEFAULT_CURRENCY] ?: "BRL" }

    val pinCode: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[Keys.PIN_CODE] ?: "" }

    val lastBackupDate: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[Keys.LAST_BACKUP_DATE] ?: "" }

    suspend fun setDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.IS_DARK_MODE] = isDarkMode
        }
    }

    suspend fun setHasCompletedOnboarding(hasCompleted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.HAS_COMPLETED_ONBOARDING] = hasCompleted
        }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.USER_NAME] = name
        }
    }

    suspend fun setDefaultCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DEFAULT_CURRENCY] = currency
        }
    }

    suspend fun setPinCode(pin: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.PIN_CODE] = pin
        }
    }

    suspend fun setLastBackupDate(date: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LAST_BACKUP_DATE] = date
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
