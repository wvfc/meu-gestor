package com.meugestor.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.meugestor.app.ui.theme.IncomeGreen
import com.meugestor.app.ui.theme.ExpenseRed
import com.meugestor.app.util.CurrencyUtils

data class BarChartItem(
    val label: String,
    val value: Double,
    val color: Color
)

data class PieChartItem(
    val label: String,
    val value: Double,
    val color: Color
)

@Composable
fun SimpleBarChart(
    items: List<BarChartItem>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val maxValue = items.maxOf { it.value }.coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth()) {
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(100.dp),
                    maxLines = 1
                )
                Box(modifier = Modifier.weight(1f).height(20.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barWidth = (size.width * (item.value / maxValue)).toFloat()
                        drawRoundRect(
                            color = item.color,
                            size = Size(barWidth, size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = CurrencyUtils.formatBRL(item.value),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun SimplePieChart(
    items: List<PieChartItem>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val total = items.sumOf { it.value }.coerceAtLeast(1.0)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(140.dp)) {
            var startAngle = -90f
            items.forEach { item ->
                val sweep = (item.value / total * 360f).toFloat()
                drawArc(
                    color = item.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    size = Size(size.width, size.height)
                )
                startAngle += sweep
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            items.take(6).forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(color = item.color)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${item.label} (${CurrencyUtils.formatPercent(item.value / total * 100)})",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyComparisonChart(
    incomeByMonth: List<Pair<String, Double>>,
    expenseByMonth: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    if (incomeByMonth.isEmpty()) return
    val maxValue = (incomeByMonth.maxOfOrNull { it.second } ?: 0.0)
        .coerceAtLeast(expenseByMonth.maxOfOrNull { it.second } ?: 0.0)
        .coerceAtLeast(1.0)

    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val barGroupWidth = size.width / incomeByMonth.size
        val barWidth = barGroupWidth * 0.35f
        val bottomPadding = 30f

        incomeByMonth.forEachIndexed { index, (month, income) ->
            val expense = expenseByMonth.getOrNull(index)?.second ?: 0.0
            val x = index * barGroupWidth

            val incomeHeight = ((income / maxValue) * (size.height - bottomPadding)).toFloat()
            drawRect(
                color = IncomeGreen,
                topLeft = Offset(x + barGroupWidth * 0.1f, size.height - bottomPadding - incomeHeight),
                size = Size(barWidth, incomeHeight)
            )

            val expenseHeight = ((expense / maxValue) * (size.height - bottomPadding)).toFloat()
            drawRect(
                color = ExpenseRed,
                topLeft = Offset(x + barGroupWidth * 0.1f + barWidth + 4, size.height - bottomPadding - expenseHeight),
                size = Size(barWidth, expenseHeight)
            )

            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = textColor
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                canvas.nativeCanvas.drawText(
                    month.take(3),
                    x + barGroupWidth / 2,
                    size.height - 5f,
                    paint
                )
            }
        }
    }
}
