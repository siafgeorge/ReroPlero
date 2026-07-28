package com.example.reroplero.ui.presentation.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.reroplero.ui.presentation.screens.MainPage
import com.example.reroplero.ui.theme.ReroPleroTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginPage : ComponentActivity() {
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReroPleroTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val focusManager = LocalFocusManager.current
                    Column (
                        modifier = Modifier.fillMaxSize()
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
    }
}


@Composable
fun LoginFields(viewModel: LoginViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LoginContracts.LoginEffect.ShowMessage ->
                    Toast.makeText(context, effect.text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn){
            context.startActivity(Intent(context, MainPage::class.java))
            (context as? Activity)?.finish()
        }
    }

    TextField(
        value = state.username,
        onValueChange = { viewModel.onAction(LoginContracts.LoginActions.OnUsernameChange(it)) },
        label = { Text(state.usernameHint) },
        isError = state.usernameErrorText != null,
        supportingText = {state.usernameErrorText?.let { Text(it) } }
    )

    TextField(
        value = state.password,
        onValueChange = { viewModel.onAction(LoginContracts.LoginActions.OnPasswordChange(it)) },
        label = { Text(state.passwordHint) },
        visualTransformation = PasswordVisualTransformation(),
        isError = state.passwordErrorText != null,
        supportingText = {state.passwordErrorText?.let { Text(it) } }
    )

    Text(
        text = state.registerUser,
        color = Color.White,
        fontSize = 12.sp,
        modifier = Modifier.clickable {
            viewModel.onAction(LoginContracts.LoginActions.OnRegister(state.username, state.password))
        }
    )

    Button(onClick = { viewModel.onAction(LoginContracts.LoginActions.Login) } ) {
        Text(stringResource(R.string.login_text))
    }
}