package com.example.reroplero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

import com.example.reroplero.data.local.models.Payment
import com.example.reroplero.data.SessionStore
import kotlinx.coroutines.launch

lateinit var globalSession: SessionStore
class MainPage : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        globalSession = SessionStore(this)
        super.onCreate(savedInstanceState)
        val username = globalSession.currentUser() ?: return
        setContent() {
            var showSheet by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            var total by remember {mutableStateOf(0.0)}

            LaunchedEffect(Unit) {
                total = globalSession.curMon()
            }

            Column(
                modifier = Modifier.fillMaxSize().background(Color(getColor(R.color.background))),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Total: $total", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.padding(20.dp))
            Box(
                modifier = Modifier.fillMaxSize().background(Color(getColor(R.color.background)))
            ){
                val topgap = 70
                Text(
                    text = "%.2f".format(total),
                    color = Color(getColor(R.color.appGreen)),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = topgap.dp, start = 16.dp)
                )

                FloatingActionButton(
                    onClick = { showSheet = true },
                    containerColor = Color(getColor(R.color.appGreen)),
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = topgap.dp, end = 16.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add"
                    )
                }

                if (showSheet){
                    ModalBottomSheet(onDismissRequest = { showSheet = false }) {
                        PaymentForm(
                            onSave = { category, cost, timeMillis ->
                                val payment = Payment(
                                    id = UUID.randomUUID().toString(),
                                    username = username,
                                    category = category,
                                    cost = cost.toDoubleOrNull() ?: 0.0,
                                    timestamp = timeMillis
                                )

                                scope.launch {
                                    globalSession.addPay(payment)
                                    total = globalSession.curMon()
                                }
                                showSheet = false
                            }
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentForm(onSave: (category: String, cost: String, timeMillis: Long) -> Unit) {
    val categories = listOf("Food", "Transport", "Rent", "Fun")
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var cost by remember { mutableStateOf("") }

    // Date & time default to "now" and can be changed with the pickers below.vo
    val now = remember { Calendar.getInstance() }
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
            onValueChange = { cost = it },
            label = { Text("Cost") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
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