package com.example.reroplero.data.remote

import com.example.reroplero.data.remote.model.CoinMarket
import com.example.reroplero.domain.CryptoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CryptoRepoImpl @Inject constructor(
    private val api: CoinGeckoApi
) : CryptoRepository {
    override suspend fun topCoins(): List<CoinMarket> = withContext(Dispatchers.IO){
        return@withContext api.getMarkets()
    }
}