package com.example.reroplero.domain

import com.example.reroplero.data.local.models.Payment


interface PaymentRepository {

    suspend fun addPayment(payment: Payment) : Boolean

    suspend fun addPayments(payments: List<Payment>)

    suspend fun getPayments(username: String): List<Payment>

    /** The payment already recorded from this receipt, or null if it's new. */
    suspend fun findByReceipt(username: String, receiptUid: String): Payment?

    suspend fun deletePayment(payment: Payment)

    suspend fun deletePaymentID(id: String)

    suspend fun deleteAllPaymentsUser(user: String)

    suspend fun updateNote(id: String, note: String?)
}