package com.example.reroplero.ui.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.reroplero.data.SessionStore
import com.example.reroplero.ui.presentation.login.LoginPage
import com.example.reroplero.ui.presentation.screens.MainPage
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val user = SessionStore(applicationContext).currentUser()
            val target = if (user.isNullOrBlank()) LoginPage::class.java else MainPage::class.java
            startActivity(Intent(this@MainActivity, target))
            finish()
        }
    }
}