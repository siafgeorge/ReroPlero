package com.example.reroplero.ui.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reroplero.data.remote.model.CoinMarket
import com.example.reroplero.domain.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class CryptoUiState(
    val coins: List<CoinMarket> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)


@HiltViewModel
class CryptoViewModel @Inject constructor(private val repo: CryptoRepository) : ViewModel() {
    private val _state = MutableStateFlow(CryptoUiState())
    val state: StateFlow<CryptoUiState> = _state.asStateFlow()

    init {
        refresh()
    }
    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        try{
            val coins = repo.topCoins()
            _state.update { it.copy(coins = coins, isLoading = false) }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = "Couldn't load prices") }
        }
    }
}