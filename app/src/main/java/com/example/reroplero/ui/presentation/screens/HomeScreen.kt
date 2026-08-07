package com.example.reroplero.ui.presentation.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.reroplero.R

@Composable
fun Homescreen(
    state: MainUiState,
    username: String,
    total: Double,
    onLogout: () -> Unit,
    onSetLogoutDialog: (Boolean) -> Unit,
    onOpenSettings: () -> Unit
){
    Box(modifier = Modifier.fillMaxSize()){
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){
            Text(stringResource(R.string.hello_user, username))
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.your_balance, stringResource(R.string.euro), total))
        }

        val bitmap = remember(state.profilePicturePath, state.profilePictureVersion){
            state.profilePicturePath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
        }
        Box(
            modifier = Modifier.align(Alignment.TopStart)
                .padding(top = 10.dp, start = 25.dp)
                .size(70.dp)
                .clickable(onClick = onOpenSettings)
        ){
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ){
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = stringResource(R.string.profile_picture_desc),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = stringResource(R.string.profile_picture_desc), modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
