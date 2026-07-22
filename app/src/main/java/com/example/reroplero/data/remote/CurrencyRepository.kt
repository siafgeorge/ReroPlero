package com.example.reroplero.data.remote

interface CurrencyRepository {
    companion object{
        const val BASE_CURRENCY = "EUR"
    }
    suspend fun availableCurrencies(): List<String>
    suspend fun toEur(amount: Double, fromCurrency: String) : Double
}