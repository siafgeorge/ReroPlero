package com.example.reroplero.data

import com.example.reroplero.data.local.AppDao
import com.example.reroplero.data.local.models.User
import com.example.reroplero.domain.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserRepoImpl @Inject constructor(
    private val dao: AppDao
    ) : UserRepository {
    override suspend fun register(username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        if (username.isBlank() || password.isBlank()) return@withContext false
        if (userExists(username)) return@withContext false
        dao.insertUser(User(username, password))
        return@withContext true
    }

    override suspend fun checkCredentials(username: String, password: String): Boolean = dao.checkCreds(username, password)

    override suspend fun currentMoney(username: String) : Double? = withContext(Dispatchers.IO){
        return@withContext dao.totalFor(username)
    }

    override suspend fun deleteUser(user: User) = dao.delUser(user)
    override suspend fun getUser(user: String) : User = dao.getUser(user) ?: throw NoSuchElementException("User not found")
    override suspend fun userExists(username: String) = dao.userExists(username)


}