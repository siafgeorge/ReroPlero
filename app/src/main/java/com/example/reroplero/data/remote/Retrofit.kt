package com.example.reroplero.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object FrankfurterClient {
    val api: FrankfurterApi = Retrofit.Builder()
        .baseUrl("https://api.frankfurter.dev/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(FrankfurterApi::class.java)
}