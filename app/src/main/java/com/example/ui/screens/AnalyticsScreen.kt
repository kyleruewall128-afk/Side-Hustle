package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AdViewRecord
import com.example.data.Formatters
import com.example.ui.components.categoryIcon
import com.example.ui.theme.NumericDisplayStyle
import com.example.viewmodel.AnalyticsRange
import com.example.viewmodel.DailyAnalyticsSummary
import com.example.viewmodel.DailyChartPoint
import com.example.viewmodel.DashboardData

@Composable
fun AnalyticsScreen(
    data: DashboardData,
    analytics: DailyAnalyticsSummary,
    selectedRange: AnalyticsRange,
    selectedDateKey: String,
    onSelectRange: (AnalyticsRange) -> Unit,
    onSelectDateKey: (String) -> Unit,
) {
    val selectedDayPoint = remember(analytics.chartPoints, selectedDateKey) {
        analytics.chartPoints.firstOrNull { it.dateKey == selectedDateKey }
            ?: analytics.chartPoints.lastOrNull()
    }
    val activeDayKey = selectedDayPoint?.dateKey ?: analytics.todayDateKey
    val viewsForSelectedDay = remember(data.adViews, activeDayKey) {
        data.adViews.filter { it.dateKey == activeDayKey }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header + Range Switcher
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PERFORMANCE LEDGER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Daily Analytics",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AnalyticsRange.entries.forEach { range ->
                        val active = range == selectedRange
                        FilterChip(
                            selected = active,
                            onClick = { onSelectRange(range) },
                            label = {
                                Text(
                                    text = range.label,
                                    style = NumericDisplayStyle.copy(
                                        fontSize = MaterialTheme.typography.labelMedium.fontSize
                                    ),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier.testTag("range_chip_${range.label}"),
                        )
                    }
                }
            }
        }

        // 2x2 KPI Summary Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    KpiStatCard(
                        title = "${selectedRange.label} Total",
                        value = Formatters.formatCents(analytics.rangeTotalCents),
                        subtitle = "Daily avg ${Formatters.formatCents(analytics.rangeDailyAvgCents)}",
                        icon = Icons.Rounded.MonetizationOn,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    KpiStatCard(
                        title = "Effective Rate",
                        value = "${Formatters.formatCents(analytics.effectiveHourlyCents)}/hr",
                        subtitle = "Avg ${Formatters.formatCents(analytics.avgRewardPerAdCents)} / ad",
                        icon = Icons.Rounded.Speed,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    KpiStatCard(
                        title = "Today's Revenue",
                        value = Formatters.formatCents(analytics.todayEarnedCents),
                        subtitle = "${analytics.todayAdsWatched} ads • ${Formatters.formatDuration(analytics.todayWatchSeconds)}",
                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                    )
                    KpiStatCard(
                        title = "Best Day (${selectedRange.label})",
                        value = analytics.bestDay?.let { Formatters.formatCents(it.earnedCents) } ?: "$0.00",
                        subtitle = analytics.bestDay?.friendlyLabel ?: "No views yet",
                        icon = Icons.Rounded.AutoGraph,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Interactive Daily Earnings Bar Chart
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Daily Earnings Trend",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Tap any bar to inspect that day's views",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = "Goal ${Formatters.formatCents(analytics.dailyGoalCents)}",
                                style = NumericDisplayStyle.copy(
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize
                                ),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }

                    if (analytics.rangeTotalCents == 0L) {
                        EmptyAnalyticsChartPlaceholder()
                    } else {
                        DailyEarningsBarChart(
                            points = analytics.chartPoints,
                            selectedDateKey = activeDayKey,
                            goalCents = analytics.dailyGoalCents,
                            onSelectPoint = { onSelectDateKey(it.dateKey) },
                        )
                    }

                    // Selectable Day Pills for quick date switching
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(analytics.chartPoints.reversed(), key = { it.dateKey }) { pt ->
                            val isSelected = pt.dateKey == activeDayKey
                            Surface(
                                onClick = { onSelectDateKey(pt.dateKey) },
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = if (isSelected) {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    null
                                },
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = pt.friendlyLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                    Text(
                                        text = Formatters.formatCents(pt.earnedCents),
                                        style = NumericDisplayStyle.copy(
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize
                                        ),
                                        color = if (pt.earnedCents > 0L) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Deep-Dive Breakdown
        if (selectedDayPoint != null) {
            item {
                SelectedDayDetailCard(
                    point = selectedDayPoint,
                    views = viewsForSelectedDay,
                )
            }
        }

        // Category Revenue Breakdown
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Earnings by Ad Category (${selectedRange.label})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (analytics.categoryBreakdown.isEmpty()) {
                        Text(
                            text = "Watch adverts to unlock category-level earnings insights.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        analytics.categoryBreakdown.forEach { cat ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = categoryIcon(cat.category),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            text = cat.category,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = "(${cat.adsWatched} ads)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "${(cat.shareFraction * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = Formatters.formatCents(cat.earnedCents),
                                            style = NumericDisplayStyle.copy(
                                                fontSize = MaterialTheme.typography.labelLarge.fontSize
                                            ),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                                LinearProgressIndicator(
                                    progress = { cat.shareFraction.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Peak Time-of-Day Windows
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Peak Earning Windows (${selectedRange.label})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        analytics.timeWindows.forEach { window ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.weight(1f),
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = window.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = Formatters.formatCents(window.earnedCents),
                                        style = NumericDisplayStyle.copy(
                                            fontSize = MaterialTheme.typography.titleSmall.fontSize
                                        ),
                                        color = if (window.earnedCents > 0L) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                    Text(
                                        text = "${window.adsWatched} ads",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Publisher Yield & App Revenue Economics Card
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Platform Economics & Yield (${selectedRange.label})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = "You earn 40% of verified sponsor ad spend while the platform retains 60% net margin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Gross Sponsor Spend", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = Formatters.formatCents(analytics.grossSponsorCents),
                                    style = NumericDisplayStyle.copy(
                                        fontSize = MaterialTheme.typography.titleMedium.fontSize
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Net App Margin",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                                Text(
                                    text = Formatters.formatCents(analytics.netPlatformMarginCents),
                                    style = NumericDisplayStyle.copy(
                                        fontSize = MaterialTheme.typography.titleMedium.fontSize
                                    ),
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }
                    Text(
                        text = "Interest match rate: ${analytics.targetedMatchRatePercent}% of watched ads matched preferred categories.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = value,
                style = NumericDisplayStyle.copy(
                    fontSize = MaterialTheme.typography.titleLarge.fontSize
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DailyEarningsBarChart(
    points: List<DailyChartPoint>,
    selectedDateKey: String,
    goalCents: Long,
    onSelectPoint: (DailyChartPoint) -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val goldColor = MaterialTheme.colorScheme.tertiary
    val mutedBarColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val goalLineColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f)
    val selectedOutline = MaterialTheme.colorScheme.onSurface

    val maxEarned = remember(points, goalCents) {
        maxOf(points.maxOfOrNull { it.earnedCents } ?: 0L, goalCents, 100L)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        if (points.isNotEmpty() && size.width > 0) {
                            val slotWidth = size.width.toFloat() / points.size.toFloat()
                            val index = (offset.x / slotWidth).toInt().coerceIn(0, points.lastIndex)
                            onSelectPoint(points[index])
                        }
                    }
                },
        ) {
            if (points.isEmpty()) return@Canvas
            val chartHeight = size.height
            val slotWidth = size.width / points.size.toFloat()
            val barWidth = (slotWidth * 0.56f).coerceIn(8f, 44f)

            // Draw dashed daily goal target line
            val goalRatio = (goalCents.toFloat() / maxEarned.toFloat()).coerceIn(0.05f, 1f)
            val goalY = chartHeight * (1f - goalRatio)
            drawLine(
                color = goalLineColor,
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
            )

            points.forEachIndexed { idx, pt ->
                val centerX = slotWidth * idx + slotWidth / 2f
                val left = centerX - barWidth / 2f
                val ratio = (pt.earnedCents.toFloat() / maxEarned.toFloat()).coerceIn(0f, 1f)
                val barHeight = (chartHeight * ratio).coerceAtLeast(if (pt.earnedCents > 0) 14f else 6f)
                val top = chartHeight - barHeight

                val isSelected = pt.dateKey == selectedDateKey
                val barColor = when {
                    pt.earnedCents == 0L -> mutedBarColor
                    pt.earnedCents >= goalCents -> goldColor
                    else -> primaryColor
                }

                drawRoundRect(
                    color = if (isSelected) barColor else barColor.copy(alpha = 0.72f),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(10f, 10f),
                )

                if (isSelected) {
                    drawCircle(
                        color = selectedOutline,
                        radius = 5f,
                        center = Offset(centerX, (top - 10f).coerceAtLeast(6f)),
                    )
                }
            }
        }

        if (points.size <= 14) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                points.forEach { pt ->
                    Text(
                        text = if (points.size <= 7) pt.shortLabel else pt.dateKey.takeLast(2),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pt.dateKey == selectedDateKey) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAnalyticsChartPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.BarChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp),
            )
            Text(
                text = "Your daily earnings chart appears as soon as you watch your first advert.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SelectedDayDetailCard(
    point: DailyChartPoint,
    views: List<AdViewRecord>,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "${point.friendlyLabel} (${point.dateKey})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${point.adsWatched} ads • ${Formatters.formatDuration(point.watchSeconds)} watch time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatCents(point.earnedCents),
                        style = NumericDisplayStyle.copy(
                            fontSize = MaterialTheme.typography.headlineMedium.fontSize
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (point.bonusCents > 0L) {
                        Text(
                            text = "incl. +${Formatters.formatCents(point.bonusCents)} bonus",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }

            if (views.isEmpty()) {
                Text(
                    text = "No completed advert views logged on this day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                views.forEach { view ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = categoryIcon(view.category),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = view.campaignTitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${view.sponsorName} • ${view.category} • ${Formatters.formatTimestampTime(view.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+${Formatters.formatCents(view.rewardCents)}",
                            style = NumericDisplayStyle.copy(
                                fontSize = MaterialTheme.typography.labelLarge.fontSize
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
