package com.example.reroplero.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private const val USERS_FILE = "users.json"

class UserStore(context: Context) {
    private val file = File(context.filesDir, USERS_FILE)

    // Private helper — always called from inside a withContext(IO) block below,
    // so the actual disk read happens off the main thread.
    private fun readAll(): JSONArray =
        if (file.exists()) JSONArray(file.readText()) else JSONArray()

    suspend fun userExists(username: String): Boolean = withContext(Dispatchers.IO) {
        val users = readAll()
        for (i in 0 until users.length()) {
            if (users.getJSONObject(i).getString("username") == username) {
                return@withContext true
            }
        }
        false
    }

    /** Returns true if registered, false if input is blank or the user already exists. */
    suspend fun register(username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        if (username.isBlank() || password.isBlank()) return@withContext false
        if (userExists(username)) return@withContext false

        val users = readAll().put(
            JSONObject()
                .put("username", username)
                .put("password", password)
        )
        file.writeText(users.toString())
        true
    }

    /** Returns true if a stored user matches the given credentials. **/
    suspend fun checkCredentials(username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val users = readAll()
        for (i in 0 until users.length()) {
            val user = users.getJSONObject(i)
            if (user.getString("username") == username &&
                user.getString("password") == password
            ) {
                return@withContext true
            }
        }
        false
    }
}