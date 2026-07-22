package com.example.reroplero.ui.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reroplero.data.local.models.Payment
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransListScreen(viewModel: MainPageViewModel, onEdit: (Payment) -> Unit, onChanged: () -> Job) {
    var payments by remember { mutableStateOf<List<Payment>>(emptyList()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { payments = viewModel.getPay() }

    if (payments.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No transactions yet")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp), //TODO move this value to the constants.
        verticalArrangement = Arrangement.spacedBy(12.dp) //TODO same here.
    ) {
        items(payments, key = {it.id}) { payment ->
            SwipeablePaymentCard(
                payment = payment,
                onEdit = { onEdit(payment) },
                onDelete = {
                    payments = payments.filterNot { it.id == payment.id }
                    scope.launch {
                        viewModel.onIntent(MainIntent.DeletePayment(payment))
                        onChanged()
                    }
                })
        }
    }
}


@Composable
fun PaymentCard(payment: Payment){
    val dateLabel = remember(payment.timestamp){
        SimpleDateFormat("dd MMM yyyy . HH:mm", Locale.getDefault())
            .format(Date(payment.timestamp))
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(payment.category, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "%.2f".format(payment.cost),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }

}

@Composable
fun SwipeablePaymentCard(payment: Payment, onDelete: () -> Unit, onEdit: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit()
                    false
                }
                else -> {
                    false
                }
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val isDelete = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CardDefaults.shape)
                    .background(if (isDelete) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primary
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = if (isDelete) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Icon(
                    if (isDelete) Icons.Filled.Delete else Icons.Filled.Edit,
                    contentDescription = if (isDelete) "Delete" else "Edit",
                    tint = if (isDelete) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) {
        PaymentCard(payment)
    }

}


//fun checkDouble(num: Double?): Double?{
//    if (num != null && num > 0.0) return num
//    return null
//}
