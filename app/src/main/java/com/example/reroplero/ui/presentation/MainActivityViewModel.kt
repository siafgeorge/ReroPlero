package com.example.reroplero.ui.presentation

import androidx.lifecycle.ViewModel
import com.example.reroplero.data.SessionStore
import com.example.reroplero.ui.presentation.login.LoginPage
import com.example.reroplero.ui.presentation.screens.MainPage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(private val session: SessionStore) : ViewModel() {
    suspend fun checkLogin(): Class<*> {
        val user = session.currentUser()
        val target = if (user.isNullOrBlank()) LoginPage::class.java else MainPage::class.java
        return target
    }
}