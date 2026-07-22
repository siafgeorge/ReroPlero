package com.example.reroplero.data.remote

class CurrencyRepoImpl(
        private val api: FrankfurterApi = FrankfurterClient.api
    ) : CurrencyRepository {

    override suspend fun availableCurrencies(): List<String> {
        return try{
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