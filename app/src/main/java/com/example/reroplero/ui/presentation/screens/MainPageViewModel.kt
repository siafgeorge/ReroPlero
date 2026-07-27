package com.example.reroplero.ui.presentation.screens


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reroplero.data.PaymentRepository
import com.example.reroplero.data.SessionStore
import com.example.reroplero.data.UserRepository
import com.example.reroplero.data.local.models.Payment
import com.example.reroplero.data.remote.CurrencyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainPageViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val session: SessionStore,
    private val globStore: PaymentRepository,
    private val apiRepo: CurrencyRepository
    ): ViewModel() {


    private val _effects = Channel<MainEffect>(Channel.BUFFERED)
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
            MainIntent.Logout -> logout()
        }
    }

    private fun load() = viewModelScope.launch {
        val user = session.currentUser()
        if (user == null) { _effects.send(MainEffect.GoToLogin); return@launch }
        _state.update { it.copy(isLoading = true, username = user) }
        val payments = globStore.getPayments(user)
        val total = userRepo.currentMoney(user) ?: 0.0
        _state.update { it.copy(payments = payments, total = total, isLoading = false) }
        val currencies = apiRepo.availableCurrencies()
        _state.update { it.copy(currencies = currencies) }
    }

    private fun save(intent: MainIntent.SavePayment) = viewModelScope.launch {
        val amount = intent.cost.toDoubleOrNull()
        if (amount == null || amount <= 0.0){
            _effects.send(MainEffect.ShowError("Enter a valid amount"))
            return@launch
        }
        val user = _state.value.username.ifBlank { return@launch }
        val eur = try {
            apiRepo.toEur(amount, intent.currency)
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
        println("the user total is $total")
        _state.update { it.copy(payments = payments, total = total, editing = null, formVersion = it.formVersion + 1) }
        _effects.send(MainEffect.GoToList)
    }

    private fun delete(payment: Payment){
        val previous = _state.value.payments
        _state.update { s -> s.copy(payments = s.payments.filterNot{it.id == payment.id}) }
        viewModelScope.launch {
            try {
                globStore.deletePayment(payment)
                val total = userRepo.currentMoney(_state.value.username) ?: 0.0
                println("the user total is $total on delete")
                _state.update {it.copy(total = total)}
            } catch (_: Exception){
                _state.update { it.copy(payments = previous) }
                _effects.send(MainEffect.ShowError("Couldn't delete"))
            }
        }
    }

    private fun logout() = viewModelScope.launch{
        session.clear()
        _effects.send(MainEffect.GoToLogin)
    }

}

fun checkDouble(num: Double?): Double?{
    if (num != null && num > 0.0) return num
    return null
}

