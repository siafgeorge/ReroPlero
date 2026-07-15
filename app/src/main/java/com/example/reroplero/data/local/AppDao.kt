package com.example.reroplero.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.reroplero.data.local.models.Payment
import com.example.reroplero.data.local.models.User

@Dao
interface AppDao {
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE username = :username)")
    suspend fun userExists(username: String): Boolean

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUser(username: String): User?

    @Insert
    suspend fun insertUser(user: User): Long

    @Delete
    suspend fun delUser(user: User)

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE username = :username AND password = :password)")
    suspend fun checkCreds(username: String, password: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE, entity = Payment::class)
    suspend fun insertPayment(payment: Payment) : Long

    @Query("SELECT * FROM payments WHERE username = :username ORDER BY timestamp DESC")
    suspend fun getPayments(username: String): List<Payment>

    @Query("SELECT SUM(cost) FROM payments WHERE username = :username")
    suspend fun totalFor(username: String): Double

    @Delete
    suspend fun deletePayment(payment: Payment)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePaymentById(id: String)

    @Query("DELETE FROM payments WHERE username = :username")
    suspend fun deleteAllPaymentsForUser(username: String)
}
