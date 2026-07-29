package com.example.reroplero.domain

import com.example.reroplero.data.remote.model.CoinMarket

interface CryptoRepository {
    suspend fun topCoins(): List<CoinMarket>
}