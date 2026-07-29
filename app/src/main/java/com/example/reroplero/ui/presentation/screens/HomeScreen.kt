package com.example.reroplero.ui.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Homescreen(
    state: MainUiState,
    username: String,
    total: Double,
    onLogout: () -> Unit,
    onSetLogoutDialog: (Boolean) -> Unit
){
    Box(modifier = Modifier.fillMaxSize()){

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){
            Text("HELLO $username !")
            Spacer(Modifier.height(12.dp))
            Text("Your balance is: -$${String.format("%.2f", total)}")
        }
        Button(
            onClick = { onSetLogoutDialog(true) },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red,
                contentColor = Color.White
            ),
            modifier = Modifier.align(Alignment.TopEnd)
                .padding(16.dp)
        )
            {
                Text("Logout")
            }
        if (state.showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { onSetLogoutDialog(false) },
                title = { Text("Log out") },
                text = { Text("Are you sure you want to log out ?") },
                confirmButton = {
                    TextButton(onClick = {
                        onSetLogoutDialog(false)
                        onLogout()
                    }) {Text("Yes")}
                },
                dismissButton = {
                    TextButton(onClick = {
                        onSetLogoutDialog(false)
                    }) { Text("No") }
                }
            )
        }
    }

}
