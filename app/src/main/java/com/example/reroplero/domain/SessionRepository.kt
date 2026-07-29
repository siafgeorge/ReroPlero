package com.example.reroplero.domain

//private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")
//private val KEY_USER = stringPreferencesKey("current_username")

interface SessionRepository {
    suspend fun setCurrentUser(username: String?) : Unit

    suspend fun currentUser() : String?

    suspend fun clear() : Unit

}