package com.example.reroplero

import com.example.reroplero.data.local.models.Payment
import com.example.reroplero.ui.presentation.screens.checkDouble
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class PaymentTest {
    private fun payment(cost: Double) = Payment(
        id = "1",
        username = "john",
        category = "food",
        cost = cost,
        timestamp = 0L
    )

    @Test
    fun `plus adds the costs of two payments`(){
        val result = payment(10.0) + payment(5.5)
        assertEquals(15.5, result, 0.0001)
    }

    @Test
    fun `checkDouble returns null for zero and negatives`(){
        assertNull(checkDouble(0.0))
        assertNull(checkDouble(-3.0))
        assertNull(checkDouble(null))
    }

    @Test
    fun `checkDouble returns the number when positive`(){
        assertEquals(4.0, checkDouble(4.0)!!, 0.0001)
    }

}