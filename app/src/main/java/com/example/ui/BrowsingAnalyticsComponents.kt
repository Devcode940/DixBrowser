package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.History
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

// Data Models for Analytics
data class DailyVisitStat(
    val dayName: String,
    val count: Int,
    val isToday: Boolean = false
)

data class DomainVisitStat(
    val domain: String,
    val count: Int,
    val category: String,
    val color: Color
)

data class CategoryStat(
    val category: String,
    val percentage: Float, // 0.0 to 1.0
    val count: Int,
    val color: Color
)

data class HourlyPatternStat(
    val label: String,
    val level: Float // 0.0 to 1.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowsingAnalyticsSheet(
    historyList: List<History>,
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTimeframe by remember { mutableStateOf("This Week") }
    var useSampleDataIfEmpty by remember { mutableStateOf(true) }

    // Process real history data into analytics structure or fallback to sample
    val dailyStats = remember(historyList, selectedTimeframe, useSampleDataIfEmpty) {
        if (historyList.isNotEmpty() && !useSampleDataIfEmpty) {
            processRealDailyStats(historyList)
        } else {
            getSampleDailyStats(selectedTimeframe)
        }
    }

    val domainStats = remember(historyList, selectedTimeframe, useSampleDataIfEmpty) {
        if (historyList.isNotEmpty() && !useSampleDataIfEmpty) {
            processRealDomainStats(historyList)
        } else {
            getSampleDomainStats()
        }
    }

    val categoryStats = remember(domainStats) {
        calculateCategoryStats(domainStats)
    }

    val hourlyStats = remember(selectedTimeframe) {
        getSampleHourlyStats()
    }

    val totalVisits = dailyStats.sumOf { it.count }
    val avgVisitsPerDay = if (dailyStats.isNotEmpty()) totalVisits / dailyStats.size else 0
    val topDomain = domainStats.firstOrNull()?.domain ?: "N/A"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF181825), // Catppuccin Mocha surface theme
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF89B4FA), Color(0xFFCBA6F7)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Analytics",
                            tint = Color(0xFF11111B)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Browsing Analytics",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Weekly habits & site frequency dashboard",
                            fontSize = 12.sp,
                            color = Color(0xFFA6ADC8)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF313244), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Timeframe Selector & Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Timeframe Chips
                SingleChoiceSegmentedButtonRow {
                    listOf("This Week", "Last Week", "All Time").forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = selectedTimeframe == option,
                            onClick = { selectedTimeframe = option },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = Color(0xFF89B4FA),
                                activeContentColor = Color(0xFF11111B),
                                inactiveContainerColor = Color(0xFF313244),
                                inactiveContentColor = Color.White
                            )
                        ) {
                            Text(option, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Sample Data Toggle Badge
                FilterChip(
                    selected = useSampleDataIfEmpty,
                    onClick = { useSampleDataIfEmpty = !useSampleDataIfEmpty },
                    label = {
                        Text(
                            if (useSampleDataIfEmpty) "Demo Mode" else "Real History",
                            fontSize = 11.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (useSampleDataIfEmpty) Icons.Default.AutoAwesome else Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFF9E2AF),
                        selectedLabelColor = Color(0xFF11111B),
                        selectedLeadingIconColor = Color(0xFF11111B),
                        containerColor = Color(0xFF313244),
                        labelColor = Color.White
                    )
                )
            }

            HorizontalDivider(color = Color(0xFF313244))

            // Main Scrollable Analytics Dashboard Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Summary Overview Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnalyticsMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Total Visits",
                            value = "$totalVisits",
                            subtitle = "Pages viewed",
                            icon = Icons.Default.Language,
                            accentColor = Color(0xFF89B4FA)
                        )
                        AnalyticsMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Daily Average",
                            value = "$avgVisitsPerDay",
                            subtitle = "Visits / day",
                            icon = Icons.Default.Speed,
                            accentColor = Color(0xFFA6E3A1)
                        )
                        AnalyticsMetricCard(
                            modifier = Modifier.weight(1f),
                            title = "Top Site",
                            value = topDomain.take(12),
                            subtitle = "${domainStats.firstOrNull()?.count ?: 0} visits",
                            icon = Icons.Default.Star,
                            accentColor = Color(0xFFF9E2AF)
                        )
                    }
                }

                // 1. Weekly Browsing Activity Bar Chart
                item {
                    DashboardSectionCard(
                        title = "Weekly Browsing Activity",
                        subtitle = "Frequency of site visits per day",
                        icon = Icons.Default.BarChart
                    ) {
                        WeeklyActivityBarChart(
                            dailyStats = dailyStats,
                            avgBenchmark = avgVisitsPerDay
                        )
                    }
                }

                // 2. Frequency of Site Visits (Top Visited Domains)
                item {
                    DashboardSectionCard(
                        title = "Top Visited Domains",
                        subtitle = "Site visit distribution & frequency ranking",
                        icon = Icons.Default.Public
                    ) {
                        SiteVisitFrequencyList(
                            domains = domainStats,
                            totalVisits = totalVisits,
                            onOpenUrl = onOpenUrl
                        )
                    }
                }

                // 3. Category Breakdown (Donut Chart)
                item {
                    DashboardSectionCard(
                        title = "Browsing Category Breakdown",
                        subtitle = "Distribution across web domains",
                        icon = Icons.Default.PieChart
                    ) {
                        BrowsingCategoryDonutChart(
                            categories = categoryStats,
                            totalVisits = totalVisits
                        )
                    }
                }

                // 4. Hourly Activity Trend Line Chart
                item {
                    DashboardSectionCard(
                        title = "Peak Activity Hours",
                        subtitle = "Hourly visit pattern across the day",
                        icon = Icons.Default.Schedule
                    ) {
                        HourlyActivityTrendChart(hourlyStats = hourlyStats)
                    }
                }

                // Insight Summary Footer
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                        shape = RoundedCornerShape(16.dp),
                        border = borderStroke()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = Color(0xFFF9E2AF),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Habit Insight",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Your peak browsing occurs between 2 PM - 6 PM. Most visited: $topDomain (${
                                        if (totalVisits > 0) ((domainStats.firstOrNull()?.count ?: 0) * 100 / totalVisits) else 0
                                    }% of total traffic).",
                                    fontSize = 12.sp,
                                    color = Color(0xFFA6ADC8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun borderStroke() = BorderStroke(1.dp, Color(0xFF313244))

@Composable
fun DashboardSectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(16.dp),
        border = borderStroke()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF89B4FA),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFFA6ADC8)
                    )
                }
            }

            content()
        }
    }
}

@Composable
fun AnalyticsMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(12.dp),
        border = borderStroke()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = Color(0xFFA6ADC8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = accentColor
            )
        }
    }
}

// -------------------------------------------------------------
// 1. WEEKLY ACTIVITY BAR CHART (CANVAS IMPLEMENTATION)
// -------------------------------------------------------------
@Composable
fun WeeklyActivityBarChart(
    dailyStats: List<DailyVisitStat>,
    avgBenchmark: Int
) {
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }
    val maxCount = remember(dailyStats) { (dailyStats.maxOfOrNull { it.count } ?: 100).coerceAtLeast(10) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Top Selected Bar Info Tooltip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val selected = selectedDayIndex?.let { dailyStats.getOrNull(it) }
            Text(
                text = if (selected != null) "${selected.dayName}: ${selected.count} site visits" else "Tap a bar to inspect daily total",
                fontSize = 12.sp,
                color = if (selected != null) Color(0xFF89B4FA) else Color(0xFFA6ADC8),
                fontWeight = if (selected != null) FontWeight.Bold else FontWeight.Normal
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFFAB387), CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Avg ($avgBenchmark)", fontSize = 10.sp, color = Color(0xFFA6ADC8))
            }
        }

        // Canvas Bar Chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFF181825), RoundedCornerShape(12.dp))
                .padding(top = 16.dp, bottom = 28.dp, start = 12.dp, end = 12.dp)
                .clickable {
                    // Reset selection on tap outside
                    selectedDayIndex = null
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val itemCount = dailyStats.size
            if (itemCount == 0) return@Canvas

            val barSpacing = 12.dp.toPx()
            val totalSpacing = barSpacing * (itemCount + 1)
            val barWidth = ((canvasWidth - totalSpacing) / itemCount).coerceAtLeast(10.0f)

            // Draw Horizontal Gridlines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = canvasHeight * (1f - i / gridLines.toFloat())
                drawLine(
                    color = Color(0xFF313244),
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw Average Benchmark Dotted Line
            if (maxCount > 0) {
                val avgY = canvasHeight * (1f - (avgBenchmark.toFloat() / maxCount).coerceIn(0f, 1f))
                drawLine(
                    color = Color(0xFFFAB387),
                    start = Offset(0f, avgY),
                    end = Offset(canvasWidth, avgY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            // Draw Bars
            dailyStats.forEachIndexed { index, stat ->
                val x = barSpacing + index * (barWidth + barSpacing)
                val barHeightFraction = (stat.count.toFloat() / maxCount).coerceIn(0.05f, 1f)
                val barHeight = canvasHeight * barHeightFraction
                val y = canvasHeight - barHeight

                val isSelected = selectedDayIndex == index
                val barColor = when {
                    isSelected -> Color(0xFFF9E2AF)
                    stat.isToday -> Color(0xFFA6E3A1)
                    else -> Color(0xFF89B4FA)
                }

                // Bar Column with Rounded Corners
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(barColor, barColor.copy(alpha = 0.6f))
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                // Selection Highlight Outline
                if (isSelected) {
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(x - 2.dp.toPx(), y - 2.dp.toPx()),
                        size = Size(barWidth + 4.dp.toPx(), barHeight + 4.dp.toPx()),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // X-Axis Day Labels
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = if (stat.isToday) android.graphics.Color.parseColor("#A6E3A1") else android.graphics.Color.parseColor("#A6ADC8")
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        isFakeBoldText = stat.isToday || isSelected
                    }
                    drawText(
                        stat.dayName,
                        x + barWidth / 2,
                        canvasHeight + 22.dp.toPx(),
                        paint
                    )
                }
            }
        }

        // Tap interactions via row of invisible overlay touch targets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dailyStats.forEachIndexed { index, stat ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { selectedDayIndex = if (selectedDayIndex == index) null else index }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 2. TOP VISITED DOMAINS & FREQUENCY LIST
// -------------------------------------------------------------
@Composable
fun SiteVisitFrequencyList(
    domains: List<DomainVisitStat>,
    totalVisits: Int,
    onOpenUrl: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        domains.take(5).forEachIndexed { index, domainStat ->
            val percentage = if (totalVisits > 0) (domainStat.count.toFloat() / totalVisits) else 0f

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenUrl("https://${domainStat.domain}") },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF181825)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Rank Badge
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (index) {
                                            0 -> Color(0xFFF9E2AF)
                                            1 -> Color(0xFFBAC2DE)
                                            2 -> Color(0xFFFAB387)
                                            else -> Color(0xFF313244)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (index < 3) Color(0xFF11111B) else Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = domainStat.domain,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = domainStat.category,
                                    fontSize = 10.sp,
                                    color = domainStat.color
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${domainStat.count} visits",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "${(percentage * 100).toInt()}% of traffic",
                                fontSize = 10.sp,
                                color = Color(0xFFA6ADC8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress Fill Bar
                    LinearProgressIndicator(
                        progress = { percentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = domainStat.color,
                        trackColor = Color(0xFF313244)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. CATEGORY BREAKDOWN DONUT CHART
// -------------------------------------------------------------
@Composable
fun BrowsingCategoryDonutChart(
    categories: List<CategoryStat>,
    totalVisits: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Donut Canvas
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                val strokeWidth = 24.dp.toPx()

                categories.forEach { category ->
                    val sweepAngle = category.percentage * 360f
                    drawArc(
                        color = category.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                    startAngle += sweepAngle
                }
            }

            // Center Label
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$totalVisits",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Visits",
                    fontSize = 10.sp,
                    color = Color(0xFFA6ADC8)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Legend List
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { category ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(category.color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = category.category,
                            fontSize = 11.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "${(category.percentage * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA6ADC8)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. HOURLY ACTIVITY AREA LINE CHART
// -------------------------------------------------------------
@Composable
fun HourlyActivityTrendChart(hourlyStats: List<HourlyPatternStat>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(Color(0xFF181825), RoundedCornerShape(12.dp))
                .padding(top = 16.dp, bottom = 24.dp, start = 12.dp, end = 12.dp)
        ) {
            val width = size.width
            val height = size.height
            val count = hourlyStats.size
            if (count < 2) return@Canvas

            val stepX = width / (count - 1)
            val path = Path()
            val fillPath = Path()

            hourlyStats.forEachIndexed { i, stat ->
                val x = i * stepX
                val y = height * (1f - stat.level.coerceIn(0.1f, 1f))

                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    val prevX = (i - 1) * stepX
                    val prevY = height * (1f - hourlyStats[i - 1].level.coerceIn(0.1f, 1f))
                    val controlX1 = prevX + stepX / 2
                    val controlX2 = x - stepX / 2
                    path.cubicTo(controlX1, prevY, controlX2, y, x, y)
                    fillPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                }

                if (i == count - 1) {
                    fillPath.lineTo(x, height)
                    fillPath.close()
                }

                // Draw X-axis label
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#A6ADC8")
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    drawText(stat.label, x, height + 18.dp.toPx(), paint)
                }
            }

            // Fill Gradient Path under curve
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF89B4FA).copy(alpha = 0.4f), Color.Transparent)
                )
            )

            // Draw Curve Line
            drawPath(
                path = path,
                color = Color(0xFF89B4FA),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

// -------------------------------------------------------------
// HELPER DATA CALCULATIONS & SAMPLE DATA GENERATION
// -------------------------------------------------------------
private fun processRealDailyStats(historyList: List<History>): List<DailyVisitStat> {
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val cal = Calendar.getInstance()
    val todayDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Convert Sun=1 to Mon=0

    val counts = MutableList(7) { 0 }
    historyList.forEach { item ->
        cal.timeInMillis = item.timestamp
        val dayIndex = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        counts[dayIndex]++
    }

    return daysOfWeek.mapIndexed { index, name ->
        DailyVisitStat(
            dayName = name,
            count = counts[index],
            isToday = index == todayDayOfWeek
        )
    }
}

private fun processRealDomainStats(historyList: List<History>): List<DomainVisitStat> {
    val domainCounts = mutableMapOf<String, Int>()
    historyList.forEach { item ->
        val domain = extractDomain(item.url)
        if (domain.isNotBlank()) {
            domainCounts[domain] = (domainCounts[domain] ?: 0) + 1
        }
    }

    return domainCounts.entries
        .sortedByDescending { it.value }
        .take(10)
        .map { entry ->
            val catAndColor = categorizeDomain(entry.key)
            DomainVisitStat(
                domain = entry.key,
                count = entry.value,
                category = catAndColor.first,
                color = catAndColor.second
            )
        }
}

private fun extractDomain(url: String): String {
    return try {
        val uri = URI(url)
        var host = uri.host ?: url
        if (host.startsWith("www.")) {
            host = host.substring(4)
        }
        host
    } catch (e: Exception) {
        url.removePrefix("https://").removePrefix("http://").split("/").firstOrNull() ?: url
    }
}

private fun categorizeDomain(domain: String): Pair<String, Color> {
    val d = domain.lowercase()
    return when {
        d.contains("google") || d.contains("bing") || d.contains("duckduckgo") -> "Search & Portals" to Color(0xFF89B4FA)
        d.contains("github") || d.contains("stackoverflow") || d.contains("dev") -> "Developer & Tech" to Color(0xFFA6E3A1)
        d.contains("youtube") || d.contains("netflix") || d.contains("twitch") -> "Video & Streaming" to Color(0xFFF38BA8)
        d.contains("reddit") || d.contains("twitter") || d.contains("x.com") || d.contains("instagram") -> "Social Media" to Color(0xFFCBA6F7)
        d.contains("wikipedia") || d.contains("news") || d.contains("medium") -> "News & Articles" to Color(0xFFFAB387)
        d.contains("amazon") || d.contains("ebay") || d.contains("shopping") -> "Shopping & E-Com" to Color(0xFFF9E2AF)
        else -> "General Browsing" to Color(0xFFBAC2DE)
    }
}

private fun calculateCategoryStats(domainStats: List<DomainVisitStat>): List<CategoryStat> {
    val categoryTotals = mutableMapOf<String, Pair<Int, Color>>()
    var grandTotal = 0

    domainStats.forEach { stat ->
        val existing = categoryTotals[stat.category] ?: (0 to stat.color)
        categoryTotals[stat.category] = (existing.first + stat.count) to stat.color
        grandTotal += stat.count
    }

    if (grandTotal == 0) return emptyList()

    return categoryTotals.map { (cat, pair) ->
        CategoryStat(
            category = cat,
            percentage = pair.first.toFloat() / grandTotal,
            count = pair.first,
            color = pair.second
        )
    }.sortedByDescending { it.percentage }
}

private fun getSampleDailyStats(timeframe: String): List<DailyVisitStat> {
    return when (timeframe) {
        "Last Week" -> listOf(
            DailyVisitStat("Mon", 42),
            DailyVisitStat("Tue", 68),
            DailyVisitStat("Wed", 55),
            DailyVisitStat("Thu", 80),
            DailyVisitStat("Fri", 95),
            DailyVisitStat("Sat", 34),
            DailyVisitStat("Sun", 28)
        )
        "All Time" -> listOf(
            DailyVisitStat("Mon", 145),
            DailyVisitStat("Tue", 182),
            DailyVisitStat("Wed", 160),
            DailyVisitStat("Thu", 210),
            DailyVisitStat("Fri", 240),
            DailyVisitStat("Sat", 110),
            DailyVisitStat("Sun", 95)
        )
        else -> listOf( // This Week
            DailyVisitStat("Mon", 35),
            DailyVisitStat("Tue", 52),
            DailyVisitStat("Wed", 78, isToday = false),
            DailyVisitStat("Thu", 94, isToday = true),
            DailyVisitStat("Fri", 61),
            DailyVisitStat("Sat", 40),
            DailyVisitStat("Sun", 25)
        )
    }
}

private fun getSampleDomainStats(): List<DomainVisitStat> {
    return listOf(
        DomainVisitStat("google.com", 124, "Search & Portals", Color(0xFF89B4FA)),
        DomainVisitStat("github.com", 86, "Developer & Tech", Color(0xFFA6E3A1)),
        DomainVisitStat("youtube.com", 62, "Video & Streaming", Color(0xFFF38BA8)),
        DomainVisitStat("reddit.com", 45, "Social Media", Color(0xFFCBA6F7)),
        DomainVisitStat("wikipedia.org", 38, "News & Articles", Color(0xFFFAB387)),
        DomainVisitStat("amazon.com", 24, "Shopping & E-Com", Color(0xFFF9E2AF))
    )
}

private fun getSampleHourlyStats(): List<HourlyPatternStat> {
    return listOf(
        HourlyPatternStat("6 AM", 0.15f),
        HourlyPatternStat("9 AM", 0.45f),
        HourlyPatternStat("12 PM", 0.80f),
        HourlyPatternStat("3 PM", 0.95f),
        HourlyPatternStat("6 PM", 0.70f),
        HourlyPatternStat("9 PM", 0.50f),
        HourlyPatternStat("12 AM", 0.20f)
    )
}
