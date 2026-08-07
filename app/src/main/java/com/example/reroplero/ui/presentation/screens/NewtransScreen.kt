package com.example.reroplero.ui.presentation.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.reroplero.R
import com.example.reroplero.data.local.models.Payment
import com.example.reroplero.domain.CurrencyRepository
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

@Composable
fun NewtransScreen(viewModel: MainPageViewModel, state: MainUiState, editing: Payment? = null, onLeave: () -> Unit) {
    DisposableEffect(Unit) {
        onDispose { onLeave() }
    }
    LaunchedEffect(Unit) {
        viewModel.onIntent(MainIntent.RefreshCurrencies)
    }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures( onTap = {
                    focusManager.clearFocus()
                }
                )
            }
    ) {
        item {
            key(editing, state.formVersion) {
                PaymentForm(
                    editing = editing,
                    onSave = { category, cost, timeMillis, selectedCurrency ->
                          viewModel.onIntent(
                              MainIntent.SavePayment(
                              category = category,
                              cost = cost,
                              timeMillis = timeMillis,
                              currency = selectedCurrency
                            )
                          )
                    },
                    currencies = state.currencies
                )
            }
        }
    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentForm(editing: Payment? = null, onSave: (category: String, cost: String, timeMillis: Long, selectedCurrency: String) -> Unit, currencies: List<String>) {
    val categories = listOf(stringResource(R.string.food), stringResource(R.string.transport), stringResource(R.string.rent), stringResource(R.string.fun_))

    var selectedCurrency by remember(editing) {
        mutableStateOf(CurrencyRepository.BASE_CURRENCY)
    }

    var currencyExpanded by remember {mutableStateOf(false)}
    val focusManager = LocalFocusManager.current
    var selectedCategory by remember(editing) { mutableStateOf(editing?.category ?: categories.first()) }
    var cost by remember(editing) {  mutableStateOf(editing?.cost?.toString() ?: "") }

    // Date & time default to "now" and can be changed with the pickers below
    val now = remember(editing){
        Instant.ofEpochMilli(editing?.timestamp ?: System.currentTimeMillis())
            .atZone(ZoneId.systemDefault())
    }

    var dateMillis by remember(editing) { mutableLongStateOf(now.toInstant().toEpochMilli()) }
    var hour by remember { mutableIntStateOf(now.hour) }
    var minute by remember { mutableIntStateOf(now.minute) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateformat = stringResource(R.string.dateFormat)
    val dateLabel = remember(dateMillis) {
        SimpleDateFormat(dateformat, Locale.getDefault()).format(Date(dateMillis))
    }
    val timeLabel = String.format(Locale.getDefault(), stringResource(R.string.timeFormat), hour, minute)

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

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            OutlinedTextField(
                value = cost,
                onValueChange = { input -> if (input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) cost = input },
                label = { Text("Cost") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )

            ExposedDropdownMenuBox(
                expanded = currencyExpanded,
                onExpandedChange = { currencyExpanded = it },
                modifier = Modifier.width(110.dp)
            ) {
                OutlinedTextField(
                    value = selectedCurrency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Currency") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded)
                    },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = currencyExpanded,
                    onDismissRequest = { currencyExpanded = false }
                ){
                    currencies.forEach { currency ->
                        DropdownMenuItem(
                            text = { Text(currency) },
                            onClick = {
                                selectedCurrency = currency
                                currencyExpanded = false
                            }
                        )
                    }
                }
            }

        }

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
                val chosenDate = Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
                val chosenMillis = chosenDate.atTime(hour, minute)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                onSave(selectedCategory, cost, chosenMillis, selectedCurrency)
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
