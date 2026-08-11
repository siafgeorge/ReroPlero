package com.example.reroplero.data

import com.example.reroplero.data.local.AppDao
import com.example.reroplero.data.local.models.Payment
import com.example.reroplero.domain.PaymentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PaymentRepoImpl @Inject constructor(
    private val dao: AppDao
    ) : PaymentRepository {

    override suspend fun addPayment(payment: Payment) : Boolean = withContext(Dispatchers.IO) {
        return@withContext dao.insertPayment(payment) != -1L
    }

    override suspend fun addPayments(payments: List<Payment>) = withContext(Dispatchers.IO){
        dao.insertPayments(payments)
    }

    override suspend fun getPayments(username: String): List<Payment> = withContext(Dispatchers.IO){
        return@withContext dao.getPayments(username)
    }

    override suspend fun findByReceipt(username: String, receiptUid: String): Payment? =
        withContext(Dispatchers.IO){
            return@withContext dao.paymentByReceipt(username, receiptUid)
        }



    override suspend fun deletePayment(payment: Payment) = dao.deletePayment(payment)
    override suspend fun deletePaymentID(id: String) = dao.deletePaymentById(id)
    override suspend fun deleteAllPaymentsUser(user: String) = dao.deleteAllPaymentsForUser(user)
}
