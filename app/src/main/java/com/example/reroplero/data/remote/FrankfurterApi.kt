package com.example.reroplero.data.remote

import com.example.reroplero.data.remote.model.LatestRates
import retrofit2.http.GET
import retrofit2.http.Query

// GET https://api.frankfurter.dev/v1/latest?base=USD&symbols=EUR
// -> {"amount":1.0,"base":"USD","date":"2026-07-17","rates":{"EUR":0.8571}}

interface FrankfurterApi {
    companion object {
        const val BASE_URL= "https://api.frankfurter.dev/"
    }
    @GET("v1/latest")
    suspend fun getLatest(
        @Query("base") base: String,
        @Query("symbols") symbols: String
    ): LatestRates

    @GET("v1/currencies")
    suspend fun getCurrencies(): Map<String, String>

}