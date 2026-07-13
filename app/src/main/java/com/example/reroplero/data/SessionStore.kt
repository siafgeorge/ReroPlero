package com.example.reroplero.data

import android.content.Context
import com.example.reroplero.data.UserStore

private const val PREFS = "session"
private const val KEY_USER = "current_username"
class SessionStore(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun setCurrentUser(username: String){
        prefs.edit().putString(KEY_USER, username).apply()
    }
    fun currentUser(): String? = prefs.getString(KEY_USER, null)

    fun clear(){
        prefs.edit().remove(KEY_USER).apply()
    }


    suspend fun addPay(payment: Payment): Boolean {
        val username = currentUser() ?: return false
        val globStore = UserStore(context)
        return globStore.addPayment(username, payment)
    }

    suspend fun getPay(): List<Payment> {
        val globStore = UserStore(context)
        val user = currentUser() ?: return emptyList()
        return globStore.getPayments(user)
    }

    suspend fun curMon(): Double{
        return UserStore(context).currentMoney(currentUser() ?: return -1.0)
    }
}