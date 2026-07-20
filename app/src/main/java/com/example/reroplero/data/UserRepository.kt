package com.example.reroplero.data

import com.example.reroplero.data.local.models.User

interface UserRepository {
    suspend fun register(username: String, password: String): Boolean

    suspend fun checkCredentials(username: String, password: String) : Boolean


    suspend fun currentMoney(username: String) : Double?

    suspend fun deleteUser(user: User)
    suspend fun getUser(user: String) : User
    suspend fun userExists(username: String): Boolean


}