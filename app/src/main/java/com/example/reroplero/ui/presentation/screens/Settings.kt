package com.example.reroplero.ui.presentation.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    viewModel: MainPageViewModel,
    state: MainUiState,
    onBack: () -> Unit,
    onSetLogoutDialog: (Boolean) -> Unit,
    onLogout: () -> Unit
){
    TopAppBar(
        title = {Text("Settings")},
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            Button(
                onClick = { viewModel.onIntent(MainIntent.SetLogoutDialog(true))},
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                )
            ){
                Text("Logout")
            }
        }
    )
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        uri -> if (uri != null) viewModel.onIntent(MainIntent.SetProfilePicture(uri))
    }

    val focusManager = LocalFocusManager.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.onIntent(MainIntent.SetLogoutDialog(true)) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.padding(16.dp)
                    ){
                        Text("Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .pointerInput(Unit){
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ){
            Text("hello ${state.username}")
            Box(
                modifier = Modifier.padding(top = 24.dp)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ){
                val bitmap = remember(state.profilePicturePath, state.profilePictureVersion){
                    state.profilePicturePath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Text(
                "Tap to change",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(20.dp))
            Text(
                "Change password",
                modifier = Modifier.padding(end = 140.dp, bottom = 5.dp),
                style = MaterialTheme.typography.titleMedium
            )

            var currentPassword by remember { mutableStateOf("") }
            var newPassword by remember { mutableStateOf("") }
            val submitPasswordChange = {
                if (currentPassword.isNotBlank() && newPassword.isNotBlank()) {
                    viewModel.onIntent(MainIntent.ChangePassword(currentPassword, newPassword))
                    currentPassword = ""
                    newPassword = ""
                    focusManager.clearFocus()
                }
            }
            TextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text("Current Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) } )
            )
            Spacer(Modifier.height(20.dp))
            TextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {submitPasswordChange()})
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    viewModel.onIntent(MainIntent.ChangePassword(currentPassword, newPassword))
                    currentPassword = ""
                    newPassword = ""
                },
                enabled = currentPassword.isNotBlank() && newPassword.isNotBlank()
            ){
                Text("Save new password")
            }
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