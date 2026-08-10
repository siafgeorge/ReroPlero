package com.example.reroplero.ui.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reroplero.R
import com.example.reroplero.data.local.models.Payment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun TransListScreen(viewModel: MainPageViewModel, onEdit: (Payment) -> Unit, onFlingNext: () -> Unit, onFlingPrev: () -> Unit) {
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.payments.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.notransyet))
        }
        return
    }

    var shouldDeleteDialog by remember{ mutableStateOf(false) }
    var curPayment by remember{mutableStateOf<Payment?>(null)}
    var selectedCategory by rememberSaveable {mutableStateOf<String?>(null)}
    val categories = remember(state.payments) { state.payments.map { it.category }.distinct().sorted() }
    var costCeiling = remember(state.payments) {
        ceil(state.payments.maxOf { it.cost }).toFloat().coerceAtLeast(1f)
    }
    var minCost by rememberSaveable(costCeiling) { mutableStateOf(0f) }
    var maxCost by rememberSaveable(costCeiling) { mutableStateOf(costCeiling) }

    val filteredPayments = remember(state.payments, selectedCategory, minCost, maxCost) {
        state.payments.filter { payment ->
            (selectedCategory == null || payment.category == selectedCategory) && payment.cost >= minCost && payment.cost <= maxCost
        }
    }

    Column(modifier = Modifier.fillMaxSize()){
        LazyRow(
            modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text(stringResource(R.string.all) ) }
                )
            }
            items(categories) { category ->
                FilterChip(
                    selected = category == selectedCategory,
                    onClick = {selectedCategory = category},
                    label = {Text(category)}
                )
            }
        }
        Text(
            text = "${stringResource(R.string.euro)}${"%.0f".format(minCost)} - " +
                    "${stringResource(R.string.euro)}${"%.0f".format(maxCost)}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        RangeSlider(
            value = minCost..maxCost,
            onValueChange = { range ->
                minCost = range.start
                maxCost = range.endInclusive
            },
            valueRange = 0f..costCeiling,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState {}
                )
        )
        Spacer(Modifier.height(12.dp))

        if (filteredPayments.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                Text(stringResource(R.string.no_data))
            }
            return
        }


        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredPayments, key = {it.id}) { payment ->
                SwipeablePaymentCard(
                    payment = payment,
                    onEdit = { onEdit(payment) },
                    onDelete = {
                        shouldDeleteDialog = true
                        curPayment = payment
                    },
                    onFlingNext = onFlingNext,
                    onFlingPrev = onFlingPrev
                )
            }
        }
    }

    if (shouldDeleteDialog){
        AlertDialog(
            onDismissRequest = { shouldDeleteDialog = false},
            title = { Text(stringResource(R.string.delpay)) },
            text = { Text(stringResource(R.string.suredelpay)) },
            confirmButton = {
                TextButton(onClick = {
                    shouldDeleteDialog = false
                    curPayment?.let {
                        scope.launch {
                            viewModel.onIntent(MainIntent.DeletePayment(curPayment!!))
                        }
                    }
                }) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    shouldDeleteDialog = false
                }) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }
}

@Composable
fun PaymentCard(payment: Payment){
    val payDayformat = stringResource(R.string.payDayformat)
    val dateLabel = remember(payment.timestamp){
        SimpleDateFormat(payDayformat, Locale.getDefault())
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
                text = "${stringResource(R.string.euro)}%.2f".format(payment.cost),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }

}

@Composable
fun SwipeablePaymentCard(payment: Payment, onDelete: () -> Unit, onEdit: () -> Unit, onFlingNext: () -> Unit, onFlingPrev: () -> Unit ) {
    val scope = rememberCoroutineScope()
    val offsetX = remember {Animatable(0f)}

    val fastFling = 2000f
    val actionDistance = with(LocalDensity.current) {80.dp.toPx()}

    Box(modifier = Modifier.fillMaxWidth()){
        val slidingLeft = offsetX.value < 0
        Box(
            modifier = Modifier.matchParentSize()
                .clip(CardDefaults.shape)
                .background(
                    if (slidingLeft) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.primary
                )
                .padding(horizontal = 20.dp),
                contentAlignment = if (slidingLeft) Alignment.CenterEnd else Alignment.CenterStart
        ){
            Icon(
                if (slidingLeft) Icons.Filled.Delete else Icons.Filled.Edit,
                contentDescription = if (slidingLeft) stringResource(R.string.delete) else stringResource(R.string.edit),
                tint = if (slidingLeft) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary
            )
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(payment.id) {
                    val tracker = VelocityTracker()
                    detectHorizontalDragGestures(
                        onDragStart = { tracker.resetTracking() },
                        onDragEnd = {
                            val velocity = tracker.calculateVelocity().x
                            val distance = offsetX.value
                            when {
                                velocity <= -fastFling -> onFlingNext()
                                velocity >= fastFling  -> onFlingPrev()
                                distance <= -actionDistance -> onDelete()
                                distance >= actionDistance  -> onEdit()
                            }
                            scope.launch { offsetX.animateTo(0f) }
                        },
                        onDragCancel = { scope.launch { offsetX.animateTo(0f) } }
                    ) { change, dragAmount ->
                        tracker.addPointerInputChange(change)
                        scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                    }
                }
        ){
            PaymentCard(payment)
        }
    }
}