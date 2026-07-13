package com.example.reroplero.data

import android.content.Context
import com.example.reroplero.data.local.AppDatabase
import com.example.reroplero.data.local.models.Payment
import com.example.reroplero.data.local.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepositoryImpl(private val context: Context) : UserRepository {
    private val dao = AppDatabase.getInstance(context).dao()
    override suspend fun register(username: String, password: String): Boolean {
        if (username.isBlank() || password.isBlank()) return false
        if (userExists(username)) return false
        dao.insertUser(User(username, password))
        return true
    }

    override suspend fun checkCredentials(username: String, password: String) = dao.checkCreds(username, password)

    override suspend fun addPayment(payment: Payment) : Boolean = withContext(Dispatchers.IO) {
        return@withContext dao.insertPayment(payment) != -1L
    }

    override suspend fun currentMoney(username: String) : Double = withContext(Dispatchers.IO){
        return@withContext dao.totalFor(username)
    }

    override suspend fun getPayments(username: String): List<Payment> = withContext(Dispatchers.IO){
        return@withContext dao.getPayments(username)
    }


    override suspend fun deleteUser(user: User) = dao.delUser(user)
    override suspend fun getUser(user: String) : User = dao.getUser(user) ?: throw NoSuchElementException("User not found")
    override suspend fun userExists(username: String) = dao.userExists(username)



    override suspend fun deletePayment(payment: Payment) = dao.deletePayment(payment)
    override suspend fun deletePaymentID(id: String) = dao.deletePaymentById(id)
    override suspend fun deleteAllPaymentsUser(user: String) = dao.deleteAllPaymentsForUser(user)

}