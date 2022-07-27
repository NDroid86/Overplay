package com.overplay.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Created by Nishant Rajput on 27/07/22.
 *
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "overplay_references")

class OverplayPreferences(context: Context) {

    private val appContext = context.applicationContext

    val location: Flow<String?>
        get() = appContext.dataStore.data.map { preferences ->
            preferences[KEY_LOCATION]
        }

    suspend fun saveLocation(location: String) {
        appContext.dataStore.edit { preferences ->
            preferences[KEY_LOCATION] = location
        }
    }

    companion object {
        private val KEY_LOCATION = stringPreferencesKey("key_location")
    }
}