package com.example.reroplero.data

import com.example.reroplero.data.local.models.Payment
import com.example.reroplero.data.local.models.User

interface UserRepository {
    suspend fun register(username: String, password: String): Boolean

    suspend fun checkCredentials(username: String, password: String) : Boolean

    public suspend fun addPayment(payment: Payment) : Boolean

    suspend fun currentMoney(username: String) : Double

    suspend fun getPayments(username: String): List<Payment>

    suspend fun deleteUser(user: User)
    suspend fun getUser(user: String) : User
    suspend fun userExists(username: String): Boolean

    suspend fun deletePayment(payment: Payment)
    suspend fun deletePaymentID(id: String)
    suspend fun deleteAllPaymentsUser(user: String)

}