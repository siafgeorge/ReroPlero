package com.example.reroplero.ui.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.reroplero.R
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
fun AnalyticsScreen(viewModel: MainPageViewModel, state: MainUiState) {

    val thisYear = remember(state.payments, state.analyticsYear) {
        state.payments.filter { it.timestamp.toLocalDate().year == state.analyticsYear }
    }

    val monthlyCosts: Map<Int, Double> = remember(thisYear) {
        thisYear.groupBy { it.timestamp.toLocalDate().monthValue }
            .mapValues { (_, list) -> list.sumOf { it.cost } }
    }

    val thisMonth = remember(state.payments, state.analyticsMonth) {
        state.payments.filter {
            YearMonth.from(it.timestamp.toLocalDate()) == state.analyticsMonth
        }
    }

    val dailyCosts: Map<Int, Double> = remember(thisMonth) {
        thisMonth.groupBy { it.timestamp.toLocalDate().dayOfMonth }
            .mapValues { (_, list) -> list.sumOf { it.cost } }
    }
    val euroSymbol = stringResource(R.string.euro)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.monthly), style = MaterialTheme.typography.titleLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { viewModel.onIntent(MainIntent.PreviousAnalyticsMonth) },
                enabled = state.canGoToPreviousMonth
            ) { Text(stringResource(R.string.prev)) }
            Text(state.analyticsMonth.toString(), style = MaterialTheme.typography.titleLarge)
            OutlinedButton(
                onClick = { viewModel.onIntent(MainIntent.NextAnalyticsMonth) },
                enabled = state.canGoToNextMonth
            ) { Text(stringResource(R.string.next)) }
        }

        state.analytics.projectedTotal?.let {
            Text(stringResource(R.string.projected_month, stringResource(R.string.euro), it))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (dailyCosts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_data))
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                colors = CardDefaults.cardColors()
            ) {
                val modelProducer = remember { CartesianChartModelProducer() }
                val isCurrentMonth = state.analyticsMonth == YearMonth.now()
                val daysInMonth = remember(state.analyticsMonth) { state.analyticsMonth.lengthOfMonth() }
                val today = if (isCurrentMonth) LocalDate.now().dayOfMonth else daysInMonth
                val xValues = (1..today).toList()
                val yValues = remember(dailyCosts){
                    var running = 0.0
                    xValues.map { day ->
                        running += dailyCosts[day] ?: 0.0
                        running.toFloat()
                    }
                }
                val projectionX = remember(today, daysInMonth) { listOf(today, daysInMonth) }
                val projectionY = remember(yValues, state.analytics.projectedTotal, isCurrentMonth){
                    if (isCurrentMonth) state.analytics.projectedTotal?.let { listOf(yValues.last(), it.toFloat()) } else null
                }

                LaunchedEffect(yValues, projectionY) {
                    modelProducer.runTransaction {
                        lineSeries {
                            series(xValues ,yValues)
                            if (projectionY != null) series(projectionX, projectionY)
                        }
                    }
                }

                val primary = MaterialTheme.colorScheme.primary
                val areaFill = remember(primary) {
                    LineCartesianLayer.AreaFill.single(
                        Fill(
                            Brush.verticalGradient(
                                listOf(
                                    primary.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                    )
                }

                CartesianChartHost(
                    modelProducer = modelProducer,
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(
                            rangeProvider = remember(today, daysInMonth, projectionY) {
                                CartesianLayerRangeProvider.fixed(minX = 1.0, maxX = if (projectionY != null) daysInMonth.toDouble() else today.toDouble())
                            },
                            lineProvider = LineCartesianLayer.LineProvider.series(
                                LineCartesianLayer.rememberLine(
                                    fill = LineCartesianLayer.LineFill.single(Fill(primary)),
                                    areaFill = areaFill
                                ),
                                LineCartesianLayer.rememberLine(
                                    fill = LineCartesianLayer.LineFill.single(Fill(primary.copy(alpha = 0.4f))),
                                    stroke = LineCartesianLayer.LineStroke.Dashed()
                                )
                            )
                        ),
                        startAxis = VerticalAxis.rememberStart(
                            valueFormatter = CartesianValueFormatter{
                                    _, value, _ -> "${euroSymbol}${"%.0f".format(value)}"
                            }
                        ),
                        bottomAxis = HorizontalAxis.rememberBottom()
                    ),

                    scrollState = rememberVicoScrollState(scrollEnabled = false),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.yearly), style = MaterialTheme.typography.titleLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { viewModel.onIntent(MainIntent.PreviousAnalyticsYear) },
                enabled = state.canGoToPreviousYear
            ) {
                Text(stringResource(R.string.prev))
            }
            Text(state.analyticsYear.toString(), style = MaterialTheme.typography.titleLarge)
            OutlinedButton(
                onClick = { viewModel.onIntent(MainIntent.NextAnalyticsYear) },
                enabled = state.canGoToNextYear
            ){
                Text(stringResource(R.string.next))
            }
        }
        Spacer( modifier = Modifier.height(16.dp) )
        if (monthlyCosts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                Text(stringResource(R.string.no_data))
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth()
                    .height(300.dp),
                colors = CardDefaults.cardColors()
            ) {
                val yearModelProducer = remember { CartesianChartModelProducer() }
                val isCurrentYear = state.analyticsYear == YearMonth.now().year
                val currentMonthIndex = if (isCurrentYear) YearMonth.now().monthValue else 12
                val monthValues = (1..currentMonthIndex).toList()
                val yearYValues = remember(monthlyCosts){
                    var running = 0.0
                    monthValues.map { month ->
                        running += monthlyCosts[month] ?: 0.0
                        running.toFloat()
                    }
                }
                LaunchedEffect(yearYValues) {
                    yearModelProducer.runTransaction {
                        lineSeries {
                            series(monthValues, yearYValues)
                        }
                    }
                }

                val yearPrimary = MaterialTheme.colorScheme.primary
                val yearAreaFill = remember(yearPrimary) {
                    LineCartesianLayer.AreaFill.single(
                        Fill(
                            Brush.verticalGradient(
                                listOf(
                                    yearPrimary.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                    )
                }
                CartesianChartHost(
                    modelProducer = yearModelProducer,
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(
                            rangeProvider = remember(currentMonthIndex) {
                                CartesianLayerRangeProvider.fixed(minX = 1.0, maxX = currentMonthIndex.toDouble())
                            },
                            lineProvider = LineCartesianLayer.LineProvider.series(
                                LineCartesianLayer.rememberLine(
                                    fill = LineCartesianLayer.LineFill.single(Fill(yearPrimary)),
                                    areaFill = yearAreaFill
                                )
                            )
                        ),
                        startAxis = VerticalAxis.rememberStart(
                            valueFormatter = CartesianValueFormatter{
                                _, value, _ -> "${euroSymbol}${"%.0f".format(value)}"
                            }
                        ),
                        bottomAxis = HorizontalAxis.rememberBottom()
                    ),
                    scrollState = rememberVicoScrollState(scrollEnabled = false),
                    modifier = Modifier.fillMaxWidth()
                        .height(250.dp)
                        .padding(8.dp)
                )
            }
        }
    }
}
@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors()){
        Column(modifier = Modifier.padding(12.dp)){
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}