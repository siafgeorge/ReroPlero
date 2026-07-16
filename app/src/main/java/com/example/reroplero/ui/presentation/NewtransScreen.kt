package com.example.reroplero.ui.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.reroplero.data.local.models.Payment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun NewtransScreen(viewModel: MainPageViewModel, editing: Payment? = null, onLeave: () -> Unit, onSaved: () -> Unit) {
    DisposableEffect(Unit) {
        onDispose { onLeave() }
    }
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf<String?> (null)}

    LaunchedEffect(Unit) { username = viewModel.getCurrentUser() }
    val user : String = username ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            key(editing) {
                PaymentForm(
                    editing = editing,
                    onSave = { category, cost, timeMillis ->
                        val payment = Payment(
                            id = editing?.id ?: UUID.randomUUID().toString(),
                            username = user,
                            category = category,
                            cost = checkDouble(cost.toDoubleOrNull()) ?: return@PaymentForm,
                            timestamp = timeMillis
                        )
                        scope.launch {
                            viewModel.addPay(payment)
                            onSaved()
                        }
                    }
                )
            }
        }
    }

}


@OptIn(ExperimentalMaterial3Api::class) // used for calendar dialog
@Composable
fun PaymentForm(editing: Payment? = null, onSave: (category: String, cost: String, timeMillis: Long) -> Unit) {
    val categories = listOf("Food", "Transport", "Rent", "Fun")
    var selectedCategory by remember(editing) { mutableStateOf(editing?.category ?: categories.first()) }
    var cost by remember(editing) {  mutableStateOf(editing?.cost?.toString() ?: "") }

    // Date & time default to "now" and can be changed with the pickers below.vo
    val now = remember (editing) { Calendar.getInstance().apply { if (editing != null) timeInMillis = editing.timestamp } }

    var dateMillis by remember { mutableLongStateOf(now.timeInMillis) }
    var hour by remember { mutableIntStateOf(now.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(now.get(Calendar.MINUTE)) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateLabel = remember(dateMillis) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dateMillis))
    }
    val timeLabel = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("New payment", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Text("Category")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { category ->
                FilterChip(
                    selected = category == selectedCategory,
                    onClick = { selectedCategory = category },
                    label = { Text(category) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = cost,
            onValueChange = { input -> if (input.matches(Regex("^\\d*\\.?\\d{0,2}\$"))) cost = input },
            label = { Text("Cost") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        Text("When")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showDatePicker = true }) {
                Text(dateLabel)
            }
            OutlinedButton(onClick = { showTimePicker = true }) {
                Text(timeLabel)
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                // combine the chosen date with the chosen hour/minute into one timestamp
                val chosen = Calendar.getInstance().apply {
                    timeInMillis = dateMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                }
                onSave(selectedCategory, cost, chosen.timeInMillis)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(initialHour = hour, initialMinute = minute)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    hour = timeState.hour
                    minute = timeState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timeState) }
        )
    }
}
