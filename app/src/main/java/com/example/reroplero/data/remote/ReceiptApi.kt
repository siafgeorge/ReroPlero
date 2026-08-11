package com.example.reroplero.data.remote

import retrofit2.http.GET
import retrofit2.http.Url

interface ReceiptApi {
    /**
     * The host isn't known until a QR is scanned, so the call passes a full
     * absolute URL. Retrofit's @Url overrides baseUrl when it's absolute.
     */
    @GET
    suspend fun fetchMyDataXml(@Url url: String): String

    companion object {
        // Only used to satisfy Retrofit's builder; every real call carries its own URL.
        const val BASE_URL = "https://epsilondigital-3rdpartb.epsilonnet.gr/"
    }
}
