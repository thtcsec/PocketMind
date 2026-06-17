package com.tuhoang.pocketmind.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
private val ChartColors = listOf(
    Color(0xFF4CAF50),
    Color(0xFF2196F3),
    Color(0xFFFF9800),
    Color(0xFFE91E63),
    Color(0xFF9C27B0),
    Color(0xFF00BCD4),
    Color(0xFFFFEB3B),
    Color(0xFF795548)
)

@Composable
fun PieChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier,
    centerText: String = ""
) {
    if (data.isEmpty()) {
        Box(modifier = modifier.height(200.dp), contentAlignment = Alignment.Center) {
            Text("No data", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val total = data.values.sum()
    val entries = data.entries.toList()

    Box(modifier = modifier.height(220.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            var startAngle = -90f

            entries.forEachIndexed { index, entry ->
                val sweep = (entry.value / total * 360f).toFloat()
                drawArc(
                    color = ChartColors[index % ChartColors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
                startAngle += sweep
            }
        }
        if (centerText.isNotEmpty()) {
            Text(
                text = centerText,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BarChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier = modifier.height(200.dp), contentAlignment = Alignment.Center) {
            Text("No data", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val maxValue = data.values.maxOrNull() ?: 1.0
    val entries = data.entries.toList()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val barWidth = size.width / (entries.size * 2f)
        val chartHeight = size.height

        entries.forEachIndexed { index, entry ->
            val barHeight = (entry.value / maxValue * chartHeight * 0.85f).toFloat()
            val x = barWidth + index * barWidth * 2f
            val y = chartHeight - barHeight

            drawRect(
                color = ChartColors[index % ChartColors.size],
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

@Composable
fun CategoryLegend(data: Map<String, Double>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return

    val entries = data.entries.toList()
    entries.forEachIndexed { index, entry ->
        androidx.compose.foundation.layout.Row(
            modifier = modifier.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(12.dp)) {
                drawCircle(
                    color = ChartColors[index % ChartColors.size],
                    radius = 6.dp.toPx()
                )
            }
            Text(
                text = "${entry.key}: $${String.format("%.2f", entry.value)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
