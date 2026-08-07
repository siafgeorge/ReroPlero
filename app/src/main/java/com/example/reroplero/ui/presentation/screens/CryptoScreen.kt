package com.example.reroplero.ui.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.reroplero.R
import com.example.reroplero.data.remote.model.CoinMarket


@Composable
fun CryptoScreen(
    state: CryptoUiState,
    onRefresh: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()){
        when {
            state.isLoading && state.coins.isEmpty() ->
                CircularProgressIndicator(Modifier.align(Alignment.Center))

            state.error != null && state.coins.isEmpty() ->
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.error)
                    TextButton(onClick = onRefresh) {
                        Text(stringResource(R.string.retry))
                    }
                }
            else -> LazyColumn(Modifier.fillMaxSize()){
                items(state.coins, key = {it.id}) {
                    coin -> CoinRow(coin)
                }
            }
        }
    }
}


@Composable
fun CoinRow(coin: CoinMarket) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(coin.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                coin.symbol.uppercase(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("%.2f €".format(coin.price), fontWeight = FontWeight.Bold)

            val change = coin.change24h
            Text(
                text = change?.let { "%+.2f%%".format(it) } ?: "—",
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    change == null ->
                        MaterialTheme.colorScheme.onSurfaceVariant
                    change < 0 -> MaterialTheme.colorScheme.error
                    else -> Color(0xFF2E7D32)
                }
            )
        }
    }
}