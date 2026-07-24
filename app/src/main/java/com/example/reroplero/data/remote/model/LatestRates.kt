package com.example.reroplero.data.remote.model

import com.google.gson.annotations.SerializedName

data class LatestRates(
    @SerializedName("amount")  val amount: Double,
    @SerializedName ("base")   val base: String,
    @SerializedName ("date")   val date: String,
    @SerializedName("rates")   val rates: Map<String, Double>
)
