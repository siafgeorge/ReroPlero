package com.example.reroplero.data.remote

import com.example.reroplero.data.remote.model.CoinMarket
import retrofit2.http.GET
import retrofit2.http.Query


interface CoinGeckoApi {
    @GET("api/v3/coins/markets")
    suspend fun getMarkets(
        @Query("vs_currency") vsCurrency: String = "eur",
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1
    ): List<CoinMarket>
}