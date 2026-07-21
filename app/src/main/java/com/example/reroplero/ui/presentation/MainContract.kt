package com.example.reroplero.ui.presentation

import com.example.reroplero.data.local.models.Payment

data class MainUiState(
    val username: String = "",
    val total: Double = 0.0,
    val payments: List<Payment> = emptyList(),
    val currencies: List<String> = listOf("EUR"),
    val editing: Payment? = null,
    val isLoading: Boolean = false,
    val notLoggedIn: Boolean = false
)

sealed interface MainIntent {
    data object Load: MainIntent
    data class SavePayment(
        val category: String,
        val cost: String,
        val timeMillis: Long,
        val currency: String
    ): MainIntent
    data class DeletePayment(val payment: Payment) : MainIntent
    data class StartEditing(val payment: Payment) : MainIntent
    data object StopEditing : MainIntent
}

sealed interface MainEffect {
    data object GoToList : MainEffect
    data object Finish : MainEffect
    data class ShowError(val message : String) : MainEffect
}
