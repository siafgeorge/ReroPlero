package com.example.reroplero.ui.presentation.screens


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reroplero.data.local.models.Payment
import com.example.reroplero.domain.CurrencyRepository
import com.example.reroplero.domain.PaymentRepository
import com.example.reroplero.domain.SessionRepository
import com.example.reroplero.domain.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject


@HiltViewModel
class MainPageViewModel @Inject constructor(
    private val userRepo: UserRepository,
    private val session: SessionRepository,
    private val globStore: PaymentRepository,
    private val apiRepo: CurrencyRepository
    ): ViewModel() {

    private val MIN_DAYS_FOR_PROJECTION = 4
    private val FIXED_CATEGORIES = setOf<String>("Rent")

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
            is MainIntent.StopEditing -> _state.update { it.copy(editing = null) }
            is MainIntent.Logout -> logout()
            is MainIntent.RefreshCurrencies -> refreshCurrencies()
            is MainIntent.SetLogoutDialog -> _state.update { it.copy(showLogoutDialog = intent.visible) }
            is MainIntent.PreviousAnalyticsMonth -> previousAnalyticsMonth()
            is MainIntent.NextAnalyticsMonth -> nextAnalyticsMonth()
        }
    }

    private fun earliestMonthWithData(payments: List<Payment>): YearMonth? {
        val zone = ZoneId.systemDefault()
        return payments.minOfOrNull {
            YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate())
        } ?: YearMonth.now()
    }
    private fun MainUiState.withPayments(newPayments: List<Payment>) : MainUiState = copy(
        payments = newPayments,
        analytics = analyticsFrom(newPayments, analyticsMonth),
        canGoToPreviousMonth = analyticsMonth > earliestMonthWithData(newPayments),
        canGoToNextMonth = analyticsMonth < YearMonth.now()
    )
    private fun MainUiState.withMonth(month: YearMonth) : MainUiState = copy(
        analyticsMonth = month,
        analytics = analyticsFrom(payments, month),
        canGoToPreviousMonth = month > earliestMonthWithData(payments),
        canGoToNextMonth = month < YearMonth.now()
    )
    private fun previousAnalyticsMonth() {
        val current = _state.value
        val target = current.analyticsMonth.minusMonths(1)
        if (target >= earliestMonthWithData(current.payments)) {
            _state.update { it.withMonth(target) }
        }
    }
    private fun nextAnalyticsMonth() {
        val current = _state.value
        val target = current.analyticsMonth.plusMonths(1)
        if (target >= earliestMonthWithData(current.payments)){
            _state.update { it.withMonth(target) }
        }
    }
    private fun refreshCurrencies() = viewModelScope.launch {
        if (state.value.currencies.size > 1) {
            return@launch
        }
        val currencies = apiRepo.availableCurrencies()
        if (currencies.size > 1) {
            _state.update{it.copy(currencies=currencies)}
        }
    }

    private fun load() = viewModelScope.launch {
        val user = session.currentUser()
        if (user == null) { _effects.send(MainEffect.GoToLogin); return@launch }
        _state.update { it.copy(isLoading = true, username = user) }
        val payments = globStore.getPayments(user)
        val total = userRepo.currentMoney(user) ?: 0.0
        _state.update { it.withPayments(payments).copy(total = total, isLoading = false) }
        refreshCurrencies()
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
        _state.update { it.withPayments(payments).copy(total = total, editing = null, formVersion = it.formVersion + 1) }
        _effects.send(MainEffect.GoToList)
    }

    private fun delete(payment: Payment){
        val previous = _state.value.payments
        _state.update { s -> s.withPayments(s.payments.filterNot{it.id == payment.id}) }
        viewModelScope.launch {
            try {
                globStore.deletePayment(payment)
                val total = userRepo.currentMoney(_state.value.username) ?: 0.0
                println("the user total is $total on delete")
                _state.update {it.copy(total = total)}
            } catch (_: Exception){
                _state.update { it.withPayments(previous) }
                _effects.send(MainEffect.ShowError("Couldn't delete"))
            }
        }
    }

    private fun logout() = viewModelScope.launch{
        _state.update { it.copy(showLogoutDialog = false) }
        session.clear()
        _effects.send(MainEffect.GoToLogin)
    }


    private fun analyticsFrom(payments: List<Payment>, month: YearMonth) : AnalyticsStats {
        val zone = ZoneId.systemDefault()
        val daysElapsed = if (month == YearMonth.now()) LocalDate.now().dayOfMonth else month.lengthOfMonth()
        val thisMonth = payments.filter {
            YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()) == month
        }
        val spent = thisMonth.sumOf { it.cost }
        val fixed = thisMonth.filter { it.category in FIXED_CATEGORIES }.sumOf { it.cost }
        val variable = spent - fixed
        val variablePerDay = if (daysElapsed > 0) variable / daysElapsed else 0.0

        return AnalyticsStats(
            spentThisMonth = spent,
            fixedThisMonth = fixed,
            variablePerDay = variablePerDay,
            projectedTotal = if (daysElapsed < MIN_DAYS_FOR_PROJECTION) null else fixed + (variablePerDay) * month.lengthOfMonth()
        )
    }
}

fun checkDouble(num: Double?): Double?{
    if (num != null && num > 0.0) return num
    return null
}

//TODO check what is composable navigation.