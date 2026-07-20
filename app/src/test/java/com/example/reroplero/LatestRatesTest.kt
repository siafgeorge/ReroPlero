package com.example.reroplero

import com.example.reroplero.data.remote.LatestRates
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test


class LatestRatesTest {
    @Test
    fun `parses a Frankfurter latest response`(){
        val json = """
            {"amount" : 1.0, "base":"USD", "date":"2026-07-17", "rates":{"EUR":0.8571}}
        """.trimIndent()
        val result = Gson().fromJson(json, LatestRates::class.java)

        assertEquals("USD", result.base)
        assertEquals(1.0, result.amount, 0.0001)
        assertEquals("2026-07-17", result.date)
        assertEquals(0.8571, result.rates["EUR"]!!, 0.0001)
    }
}