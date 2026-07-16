package com.example.reroplero.data

import android.content.Context
import com.example.reroplero.data.local.AppDatabase
import com.example.reroplero.data.local.models.Payment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PaymentRepoImpl(private val context: Context) : PaymentRepository {
    private val dao = AppDatabase.getInstance(context).dao()

    override suspend fun addPayment(payment: Payment) : Boolean = withContext(Dispatchers.IO) {
        return@withContext dao.insertPayment(payment) != -1L
    }

    override suspend fun getPayments(username: String): List<Payment> = withContext(Dispatchers.IO){
        return@withContext dao.getPayments(username)
    }



    override suspend fun deletePayment(payment: Payment) = dao.deletePayment(payment)
    override suspend fun deletePaymentID(id: String) = dao.deletePaymentById(id)
    override suspend fun deleteAllPaymentsUser(user: String) = dao.deleteAllPaymentsForUser(user)
}
