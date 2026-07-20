package com.example.reroplero.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


// GET https://api.frankfurter.dev/v1/latest?base=USD&symbols=EUR
// -> {"amount":1.0,"base":"USD","date":"2026-07-17","rates":{"EUR":0.8571}}

data class LatestRates(
    val amount: Double,
    val base: String,
    val date: String,
    val rates: Map<String, Double>
)

interface FrankfurterApi {
    @GET("v1/latest")
    suspend fun getLatest(
        @Query("base") base: String,
        @Query("symbols") symbols: String
    ): LatestRates

    @GET("v1/currencies")
    suspend fun getCurrencies(): Map<String, String>

}

object FrankfurterClient {
    val api: FrankfurterApi = Retrofit.Builder()
        .baseUrl("https://api.frankfurter.dev/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FrankfurterApi::class.java)
}