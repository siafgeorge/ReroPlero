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
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import java.util.Calendar


@Composable
fun AnalyticsScreen(payments: List<Payment>) {


    val dailyCosts = payments.groupBy { payment ->
        val cal = Calendar.getInstance().apply { timeInMillis = payment.timestamp }
        cal.get(Calendar.DAY_OF_MONTH)
    }.mapValues { (_, paymentList) ->
        paymentList.sumOf { it.cost }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Monthly Spending", style = MaterialTheme.typography.titleLarge)
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
                val daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
                val xValues = (1..daysInMonth).toList()
                val yValues = remember(dailyCosts){
                    var running = 0.0
                    xValues.map { day ->
                        running += dailyCosts[day] ?: 0.0
                        running.toFloat()
                    }
                }

                LaunchedEffect(yValues) {
                    modelProducer.runTransaction {
                        lineSeries {
                            series(xValues ,yValues)
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
                        rememberLineCartesianLayer(),
                        startAxis = VerticalAxis.rememberStart(
                            valueFormatter = CartesianValueFormatter{
                                _, value, _ -> "${value.toInt()}"
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

