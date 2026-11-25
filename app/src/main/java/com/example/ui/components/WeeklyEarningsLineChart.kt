package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.Formatters
import com.example.ui.theme.NumericDisplayStyle
import com.example.viewmodel.DailyChartPoint
import kotlin.math.roundToInt

@Composable
fun WeeklyEarningsLineChartCard(
    weeklyPoints: List<DailyChartPoint>,
    dailyGoalCents: Long,
    modifier: Modifier = Modifier,
) {
    if (weeklyPoints.isEmpty()) return

    var selectedIndex by remember(weeklyPoints.size) {
        mutableIntStateOf(weeklyPoints.lastIndex.coerceAtLeast(0))
    }
    val clampedIndex = selectedIndex.coerceIn(0, weeklyPoints.lastIndex)
    val selectedPoint = weeklyPoints[clampedIndex]
    val weeklyTotalCents = remember(weeklyPoints) { weeklyPoints.sumOf { it.earnedCents } }

    // Round chart ceiling cleanly above the max point (and daily goal when helpful)
    val maxDataCents = remember(weeklyPoints, dailyGoalCents) {
        val peak = weeklyPoints.maxOfOrNull { it.earnedCents } ?: 0L
        val rawCeiling = maxOf(peak, if (peak > 0L) dailyGoalCents else 100L, 100L)
        // Round up to nearest 50 cents for crisp Y-axis mid-point ticks
        ((rawCeiling + 49L) / 50L) * 50L
    }

    val lineColor = MaterialTheme.colorScheme.primary
    val goalColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    val nodeBorderColor = MaterialTheme.colorScheme.surfaceContainer

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_line_chart_card"),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "PAST 7 DAYS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = "Weekly Earnings History",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = Formatters.formatCents(weeklyTotalCents),
                            style = NumericDisplayStyle.copy(
                                fontSize = MaterialTheme.typography.titleSmall.fontSize
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "7D total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            // Interactive Selected-Day Inspection Pill (Tooltip bar)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${selectedPoint.friendlyLabel} (${selectedPoint.shortLabel})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${Formatters.formatCents(selectedPoint.earnedCents)} • ${selectedPoint.adsWatched} ${if (selectedPoint.adsWatched == 1L) "ad" else "ads"}",
                        style = NumericDisplayStyle.copy(
                            fontSize = MaterialTheme.typography.labelMedium.fontSize
                        ),
                        color = if (selectedPoint.earnedCents > 0L) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            // Y-Axis Labels + Line Chart Canvas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .width(46.dp)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = Formatters.formatCents(maxDataCents),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = Formatters.formatCents(maxDataCents / 2L),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "$0.00",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("weekly_line_chart_canvas")
                        .pointerInput(weeklyPoints) {
                            detectTapGestures { offset ->
                                if (weeklyPoints.size > 1 && size.width > 0) {
                                    val horizontalPad = 12.dp.toPx()
                                    val usableWidth = (size.width - horizontalPad * 2f).coerceAtLeast(1f)
                                    val normalized = ((offset.x - horizontalPad) / usableWidth).coerceIn(0f, 1f)
                                    selectedIndex = (normalized * (weeklyPoints.lastIndex)).roundToInt()
                                        .coerceIn(0, weeklyPoints.lastIndex)
                                }
                            }
                        },
                ) {
                    val horizontalPad = 12.dp.toPx()
                    val verticalPad = 12.dp.toPx()
                    val plotWidth = (size.width - horizontalPad * 2f).coerceAtLeast(1f)
                    val plotHeight = (size.height - verticalPad * 2f).coerceAtLeast(1f)
                    val baselineY = verticalPad + plotHeight

                    // 1. Horizontal subtle dashed gridlines (100%, 50%, 0%)
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    listOf(0f, 0.5f, 1f).forEach { fraction ->
                        val y = verticalPad + plotHeight * fraction
                        drawLine(
                            color = gridColor,
                            start = Offset(horizontalPad, y),
                            end = Offset(horizontalPad + plotWidth, y),
                            strokeWidth = if (fraction == 1f) 2f else 1.5f,
                            pathEffect = if (fraction == 1f) null else dashEffect,
                        )
                    }

                    // 2. Daily Goal horizontal dashed reference line
                    if (dailyGoalCents in 1..maxDataCents) {
                        val goalFraction = dailyGoalCents.toFloat() / maxDataCents.toFloat()
                        val goalY = baselineY - (plotHeight * goalFraction)
                        drawLine(
                            color = goalColor.copy(alpha = 0.65f),
                            start = Offset(horizontalPad, goalY),
                            end = Offset(horizontalPad + plotWidth, goalY),
                            strokeWidth = 2.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                        )
                    }

                    // 3. Map the 7 points to Canvas coordinates
                    val count = weeklyPoints.size
                    val coords = weeklyPoints.mapIndexed { idx, point ->
                        val x = if (count <= 1) {
                            horizontalPad + plotWidth / 2f
                        } else {
                            horizontalPad + (plotWidth * idx.toFloat() / (count - 1).toFloat())
                        }
                        val ratio = (point.earnedCents.toFloat() / maxDataCents.toFloat()).coerceIn(0f, 1f)
                        val y = baselineY - (plotHeight * ratio)
                        Offset(x, y)
                    }

                    // 4. Vertical crosshair guide at the selected point
                    val activeCoord = coords[clampedIndex]
                    drawLine(
                        color = lineColor.copy(alpha = 0.45f),
                        start = Offset(activeCoord.x, verticalPad),
                        end = Offset(activeCoord.x, baselineY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                    )

                    // 5. Smooth cubic Bezier line & gradient area fill
                    if (coords.size >= 2) {
                        val linePath = Path().apply {
                            moveTo(coords.first().x, coords.first().y)
                            for (i in 1 until coords.size) {
                                val prev = coords[i - 1]
                                val curr = coords[i]
                                val midX = (prev.x + curr.x) / 2f
                                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                            }
                        }

                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(coords.last().x, baselineY)
                            lineTo(coords.first().x, baselineY)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    lineColor.copy(alpha = 0.28f),
                                    lineColor.copy(alpha = 0.02f),
                                ),
                                startY = verticalPad,
                                endY = baselineY,
                            ),
                        )

                        drawPath(
                            path = linePath,
                            color = lineColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }

                    // 6. Data nodes + selected outer halo
                    coords.forEachIndexed { idx, coord ->
                        val isSelected = idx == clampedIndex
                        val hasEarnings = weeklyPoints[idx].earnedCents > 0L
                        if (isSelected) {
                            drawCircle(
                                color = lineColor.copy(alpha = 0.24f),
                                radius = 11.dp.toPx(),
                                center = coord,
                            )
                        }
                        drawCircle(
                            color = nodeBorderColor,
                            radius = if (isSelected) 6.5.dp.toPx() else 5.dp.toPx(),
                            center = coord,
                        )
                        drawCircle(
                            color = if (hasEarnings || isSelected) lineColor else lineColor.copy(alpha = 0.45f),
                            radius = if (isSelected) 4.5.dp.toPx() else 3.2.dp.toPx(),
                            center = coord,
                        )
                    }
                }
            }

            // X-Axis 7-day Labels aligned with Canvas plot area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 46.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                weeklyPoints.forEachIndexed { idx, pt ->
                    val isSelected = idx == clampedIndex
                    val label = if (idx == weeklyPoints.lastIndex) "Today" else pt.shortLabel
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        ),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (weeklyTotalCents == 0L) {
                Text(
                    text = "Watch an advert below to plot your first point on this week's curve. Tap any day to inspect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
