package com.example.reroplero.data

import com.example.reroplero.data.local.models.Payment


interface PaymentRepository {

    suspend fun addPayment(payment: Payment) : Boolean

    suspend fun getPayments(username: String): List<Payment>

    suspend fun deletePayment(payment: Payment)

    suspend fun deletePaymentID(id: String)

    suspend fun deleteAllPaymentsUser(user: String)
}