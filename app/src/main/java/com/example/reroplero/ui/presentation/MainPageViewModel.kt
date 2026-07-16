package com.example.reroplero.ui.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.reroplero.data.PaymentRepoImpl
import com.example.reroplero.data.SessionStore
import com.example.reroplero.data.UserRepoImpl
import com.example.reroplero.data.local.models.Payment

class MainPageViewModel(private val context: Context): ViewModel() {

    private val session: SessionStore = SessionStore(context)

    suspend fun addPay(payment: Payment): Boolean {
        val globStore = PaymentRepoImpl(context)
        val user = getCurrentUser() ?: return false
        return globStore.addPayment(payment)
    }

    suspend fun getPay(): List<Payment> {
        val globStore = PaymentRepoImpl(context)
        val user = getCurrentUser() ?: return emptyList()
        return globStore.getPayments(user)
    }

    suspend fun getCurrentMoney(): Double{
        return UserRepoImpl(context).currentMoney(getCurrentUser() ?: return -1.0)
    }

    suspend fun delPay(payment: Payment) {
        PaymentRepoImpl(context).deletePayment(payment)
    }

    suspend fun getCurrentUser(): String? {
        return session.currentUser()
    }
}