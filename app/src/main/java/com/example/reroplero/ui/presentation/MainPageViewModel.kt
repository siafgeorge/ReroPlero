package com.example.reroplero.ui.presentation


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reroplero.data.PaymentRepoImpl
import com.example.reroplero.data.SessionStore
import com.example.reroplero.data.UserRepoImpl
import com.example.reroplero.data.local.models.Payment
import com.example.reroplero.data.remote.FrankfurterClient
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class MainPageViewModel(private val context: Context): ViewModel() {

    private val session: SessionStore = SessionStore(context)
    private val globStore = PaymentRepoImpl(context)
    private val userRepo = UserRepoImpl(context)

    private val _effects = Channel<MainEffect>()
    val effects = _effects.receiveAsFlow()

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init { onIntent(MainIntent.Load) }

    fun onIntent(intent: MainIntent) {
        when (intent) {
            MainIntent.Load -> load()
            is MainIntent.SavePayment -> save(intent)
            is MainIntent.DeletePayment -> delete(intent.payment)
            is MainIntent.StartEditing -> _state.update { it.copy(editing = intent.payment) }
            MainIntent.StopEditing -> _state.update { it.copy(editing = null) }
        }
    }

    private fun load() = viewModelScope.launch {
        val user = session.currentUser()
        if (user == null) { _effects.send(MainEffect.Finish); return@launch }
        _state.update { it.copy(isLoading = true, username = user) }
        val payments = globStore.getPayments(user)
        val total = userRepo.currentMoney(user) ?: 0.0
        val currencies = getCurrencies()
        _state.update { it.copy(payments = payments, total = total, currencies = currencies, isLoading = false) }
    }

    private fun save(intent: MainIntent.SavePayment) = viewModelScope.launch {
        val amount = intent.cost.toDoubleOrNull()
        if (amount == null || amount <= 0.0){
            _effects.send(MainEffect.ShowError("Enter a valid amount"))
            return@launch
        }
        val user = _state.value.username.ifBlank { return@launch }
        val eur = try {
            convertCurrencyToEur(amount, intent.currency)
        }catch (_: Exception) {
            _effects.send(MainEffect.ShowError("Couldn't fetch exchange rate"))
            return@launch
        }
        val payment = Payment(
            id = _state.value.editing?.id ?: UUID.randomUUID().toString(),
            username = user,
            category = intent.category,
            cost = eur,
            timestamp = intent.timeMillis
        )
        globStore.addPayment(payment)
        val payments = globStore.getPayments(user)
        val total = userRepo.currentMoney(user) ?: 0.0
        _state.update { it.copy(payments = payments, total = total, editing = null) }
        _effects.send(MainEffect.GoToList)
    }

    private fun delete(payment: Payment){
        val previous = _state.value.payments
        _state.update { s -> s.copy(payments = s.payments.filterNot{it.id == payment.id}) }
        viewModelScope.launch {
            try {
                globStore.deletePayment(payment)
                val total = userRepo.currentMoney(_state.value.username) ?: 0.0
                _state.update {it.copy(total = total)}
            } catch (_: Exception){
                _state.update { it.copy(payments = previous) }
                _effects.send(MainEffect.ShowError("Couldn't delete"))
            }
        }
    }

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

    suspend fun newTranSave(category: String, cost: String, timeMillis: Long, editing: Payment?, onSaved: () -> Unit, selectedCurrency: String) {

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

