package com.example.reroplero.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CurrencyRepoImpl @Inject constructor(
        private val api: FrankfurterApi
    ) : CurrencyRepository {

    override suspend fun availableCurrencies(): List<String> = withContext(Dispatchers.IO){
        return@withContext try{
            api.getCurrencies().keys.sorted()
        }catch (e : Exception){
            listOf(CurrencyRepository.BASE_CURRENCY)
        }
    }

    override suspend fun toEur(amount: Double, fromCurrency: String) : Double {
        if (fromCurrency == CurrencyRepository.BASE_CURRENCY) {
            return amount
        }

        val response = api.getLatest(base = fromCurrency, symbols = CurrencyRepository.BASE_CURRENCY)
        val rate = response.rates[CurrencyRepository.BASE_CURRENCY] ?: throw IllegalStateException("No EUR rate for $fromCurrency")
        return amount * rate
    }
}