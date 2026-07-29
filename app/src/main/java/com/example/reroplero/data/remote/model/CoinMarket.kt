package com.example.reroplero.data.remote.model

import com.google.gson.annotations.SerializedName

data class CoinMarket (
    @SerializedName("id") val id: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("name") val name: String,
    @SerializedName("current_price") val price: Double,
    @SerializedName("market_cap_rank") val rank: Int,
    @SerializedName("price_change_percentage_24h") val change24h: Double?
)