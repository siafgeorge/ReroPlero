package com.example.reroplero.ui.presentation.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reroplero.data.remote.model.CoinMarket
import com.example.reroplero.domain.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class CryptoUiState(
    val coins: List<CoinMarket> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)


@HiltViewModel
class CryptoViewModel @Inject constructor(
    private val repo: CryptoRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow(CryptoUiState())
    val state: StateFlow<CryptoUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            observeConnectivity().collect{
                isOnline -> if (isOnline){
                    refresh()
                }
            }
        }
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

    private fun observeConnectivity() = callbackFlow<Boolean> {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }
            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
}