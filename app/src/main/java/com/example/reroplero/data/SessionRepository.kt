package com.example.reroplero.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")
private val KEY_USER = stringPreferencesKey("current_username")

@Singleton
class SessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
    ) {
    suspend fun setCurrentUser(username: String?){
        dataStore.edit {
            prefs -> if (username.isNullOrBlank()) prefs.remove(KEY_USER)
                     else prefs[KEY_USER] = username
        }
    }

    suspend fun currentUser() : String? {
        return dataStore.data.first()[KEY_USER]
    }

    suspend fun clear() {
        dataStore.edit{ it.remove(KEY_USER) }
    }

}