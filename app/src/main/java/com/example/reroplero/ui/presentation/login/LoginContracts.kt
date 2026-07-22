package com.example.reroplero.ui.presentation.login

class LoginContracts {
    data class LoginState(
        val username: String = "",
        val password: String = "",
        val usernameErrorText: String? = null,
        val passwordErrorText: String? = null,
        val isLoading: Boolean = false
    )

    sealed interface LoginActions {
        data class OnUsernameChange(val username: String) : LoginActions
        data class OnPasswordChange(val password: String) : LoginActions
        data object Login : LoginActions
    }

    sealed interface LoginEffect{
        data object GoToMain : LoginEffect
    }

    class Mutation(val state: LoginState) {
        fun onUsernameChange(value: String) : Mutation{
            return Mutation(state.copy(username = value, usernameErrorText = null))
        }
        fun onPasswordChange(value: String): Mutation {
            return Mutation(state.copy(password = value, passwordErrorText = null))
        }
        fun onValidateInput(usernameError: String?, passwordError: String?): Mutation {
            return Mutation(state.copy(usernameErrorText = usernameError, passwordErrorText = passwordError))
        }
        fun onLoading(loading: Boolean) : Mutation {
            return Mutation(state.copy(isLoading = loading))
        }

    }
}