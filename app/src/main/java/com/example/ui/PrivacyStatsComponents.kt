package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun PrivacyStatsBarChart(modifier: Modifier = Modifier) {
    // Mock data for the last 30 days
    val dataPoints = List(30) { Random.nextInt(10, 150) }
    val maxPoint = dataPoints.maxOrNull() ?: 150

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Trackers & Cookies Blocked (Last 30 Days)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            val width = size.width
            val height = size.height
            val barWidth = width / dataPoints.size
            val barSpacing = 2.dp.toPx()

            dataPoints.forEachIndexed { index, value ->
                val barHeight = (value.toFloat() / maxPoint.toFloat()) * height
                val x = index * barWidth + barSpacing
                val y = height - barHeight
                
                drawRect(
                    color = Color(0xFFF9E2AF),
                    topLeft = Offset(x, y),
                    size = Size(barWidth - (barSpacing * 2), barHeight)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("30 days ago", fontSize = 12.sp, color = Color.Gray)
            Text("Today", fontSize = 12.sp, color = Color.Gray)
        }
    }
}
