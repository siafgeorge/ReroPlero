package com.example.reroplero.ui.presentation.screens

import android.net.Uri
import com.example.reroplero.data.local.models.Payment
import com.example.reroplero.domain.CurrencyRepository
import java.time.YearMonth


data class AnalyticsStats(
    val spentThisMonth: Double = 0.0,
    val fixedThisMonth: Double = 0.0,
    val variablePerDay: Double = 0.0,
    val projectedTotal: Double? = null
)

data class MainUiState(
    val username: String = "",
    val total: Double = 0.0,
    val payments: List<Payment> = emptyList(),
    val currencies: List<String> = listOf(CurrencyRepository.BASE_CURRENCY),
    var editing: Payment? = null,
    val isLoading: Boolean = false,
    val notLoggedIn: Boolean = false,
    val formVersion: Int = 0,
    val showLogoutDialog: Boolean = false,
    val analytics: AnalyticsStats = AnalyticsStats(),
    val analyticsMonth: YearMonth = YearMonth.now(),
    val canGoToPreviousMonth: Boolean = false,
    val canGoToNextMonth: Boolean = false,
    val analyticsYear: Int = YearMonth.now().year,
    val canGoToPreviousYear: Boolean = false,
    val canGoToNextYear: Boolean = false,
    val profilePicturePath: String? = null,
    val profilePictureVersion : Int = 0
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
    data object Logout : MainIntent
    data object RefreshCurrencies : MainIntent
    data class SetProfilePicture(val uri: Uri) : MainIntent
    data class SetLogoutDialog(val visible: Boolean) : MainIntent
    data object PreviousAnalyticsMonth : MainIntent
    data object NextAnalyticsMonth : MainIntent
    data object PreviousAnalyticsYear : MainIntent
    data object NextAnalyticsYear : MainIntent
}

sealed interface MainEffect {
    data object GoToList : MainEffect
    data class ShowError(val message : String) : MainEffect

    data object GoToLogin : MainEffect
}
