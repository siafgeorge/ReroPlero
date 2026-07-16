package com.example.reroplero.ui.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.reroplero.data.local.models.Payment
import com.example.reroplero.ui.theme.ReroPleroTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainPage : ComponentActivity() {
    private val viewModel by lazy { MainPageViewModel(applicationContext) }
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContent() {
          ReroPleroTheme {
            val scope = rememberCoroutineScope()
            var total by remember { mutableDoubleStateOf(0.0) }
            var username by remember { mutableStateOf<String>("")}
            LaunchedEffect(Unit) {
                val user = viewModel.getCurrentUser() //TODO move this to viewmodel
                if (user == null) {
                    finish()
                    return@LaunchedEffect
                }
                username = user
                total = viewModel.getCurrentMoney() //TODO move this to viewmodel
            }

              var tab by remember { mutableStateOf(Tab.HOME) }
              var editing by remember { mutableStateOf<Payment?>(null)}

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = tab == t,
                                onClick = { tab = t },
                                icon = { Icon(t.icon, contentDescription = t.label) },
                                label = { Text(t.label) },
                            )
                        }
                    }
                }
            ) {
                innerPadding ->
                Box(Modifier.padding(innerPadding)) {
                    when (tab) {
                        Tab.HOME -> Homescreen(
                            username = username,
                            total = total,
                            onPayment = { scope.launch { total = viewModel.getCurrentMoney() } },
                            viewModel = viewModel,
                        )
                        Tab.ANALYTICS -> AnalyticsScreen()
                        Tab.TRANSACTION -> NewtransScreen(
                            viewModel,
                            editing = editing,
                            onLeave = {editing = null},
                            onSaved = {
                                scope.launch { total = viewModel.getCurrentMoney() }
                                tab = Tab.TRANSLIST
                            },
                        )

                        Tab.TRANSLIST -> TransList(viewModel, onEdit = { payment ->
                            editing = payment
                            tab = Tab.TRANSACTION
                           },
                            onChanged = {
                                scope.launch {
                                    total = viewModel.getCurrentMoney()
                                }
                            }
                        )

                        Tab.CRYPTO -> CryptoScreen()
                    }
                }
            }
          }

        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentForm(editing: Payment? = null, onSave: (category: String, cost: String, timeMillis: Long) -> Unit) {
    val categories = listOf("Food", "Transport", "Rent", "Fun")
    var selectedCategory by remember { mutableStateOf(editing?.category ?: categories.first()) }
    var cost by remember {  mutableStateOf(editing?.cost?.toString() ?: "") }

    // Date & time default to "now" and can be changed with the pickers below.vo
    val now = remember { Calendar.getInstance().apply { if (editing != null) timeInMillis = editing.timestamp } }

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

@Composable
fun Homescreen(
    username: String,
    total: Double,
    onPayment: () -> Unit,
    viewModel: MainPageViewModel
){
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){
        Text("HELLO $username !")
        Spacer(Modifier.height(12.dp))
        Text("Your money are minus $total")
    }
}

@Composable fun AnalyticsScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        Text("2BA nalytics")
    }
}
@Composable fun NewtransScreen(viewModel: MainPageViewModel, editing: Payment? = null, onLeave: () -> Unit, onSaved: () -> Unit) {
    DisposableEffect(Unit) {
        onDispose { onLeave() }
    }
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf<String?> (null)}

    LaunchedEffect(Unit) { username = viewModel.getCurrentUser() }
    val user : String = username ?: return
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

fun checkDouble(num: Double?): Double?{
    if (num != null && num > 0.0) return num
    return null
}

@Composable fun TransList(viewModel: MainPageViewModel, onEdit: (Payment) -> Unit, onChanged: () -> Job) {
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
                        viewModel.delPay(payment)
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

@OptIn(ExperimentalMaterial3Api::class) //TODO check if you can remove this line
@Composable
fun SwipeablePaymentCard(payment: Payment, onDelete: () -> Unit, onEdit: () -> Unit) { //TODO add here onedit and edit the transaction
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            }else if (value == SwipeToDismissBoxValue.StartToEnd){
                onEdit()
                false
            }else{
                false
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

@Composable fun CryptoScreen() { Text("Crypto") }

enum class Tab(val label: String, val icon: ImageVector){
    HOME("Home", Icons.Default.Home),
    ANALYTICS(label = "Analytics", Icons.Default.Check),
    TRANSACTION(label = "New", Icons.Default.AddCircle),
    TRANSLIST(label = "List", Icons.AutoMirrored.Filled.List),
    CRYPTO(label = "Crypto", Icons.Filled.Lock)

}