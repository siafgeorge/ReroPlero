package com.example.reroplero.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.reroplero.data.local.models.Payment
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")
private val KEY_USER = stringPreferencesKey("current_username")

class SessionStore(private val context: Context) {
    suspend fun setCurrentUser(username: String){
        context.dataStore.edit{it[KEY_USER] = username}
    }

    suspend fun currentUser() : String? {
        return context.dataStore.data.first()[KEY_USER]
    }

    suspend fun clear() {
        context.dataStore.edit{ it.remove(KEY_USER) }
    }

    suspend fun addPay(payment: Payment): Boolean {
        val username = currentUser() ?: return false
        val globStore = UserRepositoryImpl(context)
        return globStore.addPayment(payment)
    }

    suspend fun getPay(): List<Payment> {
        val globStore = UserRepositoryImpl(context)
        val user = currentUser() ?: return emptyList()
        return globStore.getPayments(user)
    }

    suspend fun curMon(): Double{
        return UserRepositoryImpl(context).currentMoney(currentUser() ?: return -1.0)
    }
}