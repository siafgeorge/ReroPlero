package com.example.reroplero.ui.presentation.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reroplero.data.SessionStore
import com.example.reroplero.data.UserRepoImpl
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginVewModel(private val context: Context) : ViewModel() {
    private val userRepo = UserRepoImpl(context)
    private val session = SessionStore(context)

    private val _state = MutableStateFlow(LoginContracts.LoginState())

    val state: StateFlow<LoginContracts.LoginState> = _state.asStateFlow()

    private val _effects = Channel<LoginContracts.LoginEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onAction(action: LoginContracts.LoginActions){
        when(action){
            is LoginContracts.LoginActions.OnUsernameChange -> {
                _state.update { LoginContracts.Mutation(it).onUsernameChange(action.username).state }
            }
            is LoginContracts.LoginActions.OnPasswordChange -> {
                _state.update { LoginContracts.Mutation(it).onPasswordChange(action.password).state }
            }
            is LoginContracts.LoginActions.Login -> {
                login()
            }
            is LoginContracts.LoginActions.OnRegister -> {
                register()
            }
            is LoginContracts.LoginActions.Logout -> {
                logout()
            }
        }
    }

    private fun login() = viewModelScope.launch {
        val s = _state.value
        val usernameError = if (s.username.isBlank()) "Username Required" else null
        val passwordError = if (s.password.isBlank()) "Password Required" else null

        if (usernameError != null || passwordError != null){
            _state.update {
                LoginContracts.Mutation(it).onValidateInput(usernameError, passwordError).state
            }
            return@launch
        }

        _state.update { LoginContracts.Mutation(it).onLoading(true).state }
        val ok  = userRepo.checkCredentials(s.username, s.password)
        _state.update { LoginContracts.Mutation(it).onLoading(false).state }
        if (ok) {
            session.setCurrentUser(s.username)
            _state.update{
                LoginContracts.Mutation(it).onLoggedIn().state
            }
        } else {
            _state.update {
                LoginContracts.Mutation(it).onValidateInput(null, "Wrong username or password").state
            }
        }
    }

    private fun register() = viewModelScope.launch {
        val s = _state.value
        val usernameError = if (s.username.isBlank()) "Username Required" else null
        val passwordError = if (s.password.isBlank()) "Password Required" else null

        if (usernameError != null || passwordError != null){
            _state.update {
                LoginContracts.Mutation(it).onValidateInput(usernameError, passwordError).state
            }
            return@launch
        }

        val ok = userRepo.register(s.username, s.password)
        if (ok) {
            _effects.send(LoginContracts.LoginEffect.ShowMessage("Register Successful"))
        }else{
            _effects.send(LoginContracts.LoginEffect.ShowMessage("User already exists"))
        }
    }

    private fun logout() = viewModelScope.launch {
        session.setCurrentUser(null)
        _state.update {
            LoginContracts.Mutation(it).onLoggedOut().state
        }
    }
}