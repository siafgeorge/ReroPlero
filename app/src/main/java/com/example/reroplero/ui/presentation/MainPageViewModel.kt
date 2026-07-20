package com.example.reroplero.ui.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.reroplero.data.PaymentRepoImpl
import com.example.reroplero.data.SessionStore
import com.example.reroplero.data.UserRepoImpl
import com.example.reroplero.data.local.models.Payment
import com.example.reroplero.data.remote.FrankfurterClient
import kotlinx.coroutines.CoroutineScope
import java.util.UUID

class MainPageViewModel(private val context: Context): ViewModel() {

    private val session: SessionStore = SessionStore(context)
    private val globStore = PaymentRepoImpl(context)
    private val userRepo = UserRepoImpl(context)


    suspend fun addPay(payment: Payment): Boolean {
        return globStore.addPayment(payment)
    }

    suspend fun getPay(): List<Payment> {
        val user = getCurrentUser() ?: return emptyList()
        return globStore.getPayments(user)
    }

    suspend fun getCurrentMoney(): Double? {
        return userRepo.currentMoney(getCurrentUser() ?: return -1.0)
    }

    suspend fun delPay(payment: Payment) {
        globStore.deletePayment(payment)
    }

    suspend fun getCurrentUser(): String? {
        return session.currentUser()
    }

    suspend fun newTranSave(category: String, cost: String, timeMillis: Long, editing: Payment?, scope: CoroutineScope, onSaved: () -> Unit, selectedCurrency: String) {

        val updatedcost = convertCurrencyToEur(cost.toDoubleOrNull() ?: return, selectedCurrency)
        val payment = Payment(
            id = editing?.id ?: UUID.randomUUID().toString(),
            username = getCurrentUser() ?: return ,
            category = category,
            cost = updatedcost,
            timestamp = timeMillis
        )

        addPay(payment)
        onSaved()
    }

    suspend fun getCurrencies(): List<String> {
        return try{
            FrankfurterClient.api.getCurrencies().keys.sorted()
        }catch (e : Exception){
            listOf("EUR")
        }
    }
}


suspend fun convertCurrencyToEur(cost: Double, selectedCurrency: String ) : Double {

    if (selectedCurrency == "EUR") {
        return cost
    }

    val response = FrankfurterClient.api.getLatest(base = selectedCurrency, symbols = "EUR")
    val rate = response.rates["EUR"] ?: return 0.0
    return cost * rate
}

fun checkDouble(num: Double?): Double?{
    if (num != null && num > 0.0) return num
    return null
}

