package com.example.reroplero.ui.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.reroplero.data.local.models.Payment
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
fun AnalyticsScreen(payments: List<Payment>, analytics: AnalyticsStats) {

    val currentMonth = remember { YearMonth.now() }
    val thisMonth = remember(payments, currentMonth) {
        payments.filter {
            YearMonth.from(it.timestamp.toLocalDate()) == currentMonth
        }
    }

    val dailyCosts: Map<Int, Double> = remember(thisMonth) {
        thisMonth.groupBy { it.timestamp.toLocalDate().dayOfMonth }
            .mapValues { (_, list) -> list.sumOf { it.cost } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Monthly Spending", style = MaterialTheme.typography.titleLarge)
        analytics.projectedTotal?.let{
            Text("Projected this month: €${"%.2f".format(it)}")
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (dailyCosts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No data available")
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                colors = CardDefaults.cardColors()
            ) {
                val modelProducer = remember { CartesianChartModelProducer() }
                val today = remember { LocalDate.now().dayOfMonth }
                val xValues = (1..today).toList()
                val yValues = remember(dailyCosts){
                    var running = 0.0
                    xValues.map { day ->
                        running += dailyCosts[day] ?: 0.0
                        running.toFloat()
                    }
                }
                val daysInMonth = remember { YearMonth.now().lengthOfMonth() }
                val projectionX = remember(today, daysInMonth) { listOf(today, daysInMonth) }
                val projectionY = remember (yValues, analytics.projectedTotal){
                    analytics.projectedTotal?.let { listOf(yValues.last(), it.toFloat()) }
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
                                _, value, _ -> "€${"%.0f".format(value)}"
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
    }
}

