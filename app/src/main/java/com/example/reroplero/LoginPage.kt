package com.example.reroplero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

class LoginPage : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column (
                modifier = Modifier.fillMaxSize()
                    .background(Color(0xFF1E1E2E))
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
                    UserName()
                    Password()
                    Spacer(modifier = Modifier.height(20.dp))
                    LoginButton()
                }

                Spacer(modifier = Modifier.weight(2f))

            }


        }
    }
}

@Composable
fun UserName() {
    var text by remember { mutableStateOf("") }
    TextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Username") }
    )
}

@Composable
fun Password() {
    var text by remember { mutableStateOf("") }
    TextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Password") },
        visualTransformation = PasswordVisualTransformation()
    )
}

@Composable
fun LoginButton(){
    Button(onClick = { /* TODO: Handle login */ }) {
        Text("Login")
    }
}