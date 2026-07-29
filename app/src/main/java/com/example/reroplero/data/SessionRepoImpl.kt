package com.example.reroplero.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.reroplero.domain.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton


//private val Context.dataStore: DataStore<androidx.datastore.preferences.core.Preferences> by preferencesDataStore(name = "session")
private val KEY_USER = stringPreferencesKey("current_username")


@Singleton
class SessionRepoImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SessionRepository {

    override suspend fun setCurrentUser(username: String?){
        dataStore.edit {
                prefs -> if (username.isNullOrBlank()) prefs.remove(KEY_USER)
        else prefs[KEY_USER] = username
        }
    }

    override suspend fun currentUser() : String? {
        return dataStore.data.first()[KEY_USER]
    }

    override suspend fun clear() {
        dataStore.edit{ it.remove(KEY_USER) }
    }
}