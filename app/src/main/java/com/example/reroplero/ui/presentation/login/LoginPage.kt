package com.example.reroplero.ui.presentation.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reroplero.R
import com.example.reroplero.data.SessionStore
import com.example.reroplero.data.UserRepoImpl
import com.example.reroplero.data.UserRepository
import com.example.reroplero.ui.presentation.screens.MainPage
import kotlinx.coroutines.launch

class LoginPage : ComponentActivity() {

    private val viewModel by lazy {
        LoginVewModel(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val focusManager = LocalFocusManager.current
            Column (
                modifier = Modifier.fillMaxSize()
                    .background(Color(getColor(R.color.mybackground)))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    }
                    .padding(43.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(120.dp)
                        .padding(top = 32.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                ){
                    LoginFields(viewModel)
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Spacer(modifier = Modifier.weight(2f))

            }


        }
    }
}


@Composable
fun LoginFields(viewModel: LoginVewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val userRepository = remember { UserRepoImpl(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LoginContracts.LoginEffect.GoToMain ->
                    context.startActivity(Intent(context, MainPage::class.java))
            }
        }
    }

    TextField(
        value = state.username,
        onValueChange = { viewModel.onAction(LoginContracts.LoginActions.OnUsernameChange(it)) },
        label = { Text("Username") },
        isError = state.usernameErrorText != null,
        supportingText = {state.usernameErrorText?.let { Text(it) } }
    )

    TextField(
        value = state.password,
        onValueChange = { viewModel.onAction(LoginContracts.LoginActions.OnPasswordChange(it)) },
        label = { Text("Password") },
        visualTransformation = PasswordVisualTransformation(),
        isError = state.passwordErrorText != null,
        supportingText = {state.passwordErrorText?.let { Text(it) } }
    )

    Text(
        text = "Register user",
        color = Color.White,
        fontSize = 12.sp,
        modifier = Modifier.clickable {
            scope.launch {
                val ok = userRepository.register(state.username, state.password)
                println(if (ok) "registered ${state.username}" else "registration failed (blank or exists)")
            }
        }
    )

    Button(onClick = { viewModel.onAction(LoginContracts.LoginActions.Login) } ) {
        Text(stringResource(R.string.login_text))
    }
}

suspend fun loginAction(context: Context, username: String, password: String, userRepository: UserRepository) {
    //here the login will be handled
    println("loginAction called with $username and $password")
    val success = userRepository.checkCredentials(username, password)
    if (success) {
        println("login successful")
        SessionStore(context).setCurrentUser(username)
        context.startActivity(Intent(context, MainPage::class.java))
    } else {
        println("login not successful")
    }
}