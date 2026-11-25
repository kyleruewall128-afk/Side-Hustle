package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AdCampaign
import com.example.data.AdCampaignCatalog
import com.example.data.Formatters
import com.example.data.ReferralRecord
import com.example.ui.components.DailyGoalDialog
import com.example.ui.components.ReferralSectionCard
import com.example.ui.components.WeeklyEarningsLineChartCard
import com.example.ui.components.categoryIcon
import com.example.ui.theme.NumericDisplayStyle
import com.example.viewmodel.DailyAnalyticsSummary
import com.example.viewmodel.DashboardData

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EarnScreen(
    data: DashboardData,
    analytics: DailyAnalyticsSummary,
    prioritizedCampaigns: List<AdCampaign>,
    selectedFilterCategory: String,
    onSelectFilterCategory: (String) -> Unit,
    onOpenPreferences: () -> Unit,
    onQuickSavePreferences: (List<String>) -> Unit,
    onWatchCampaign: (AdCampaign, Int) -> Unit,
    onUpdateDailyGoal: (Long, String) -> Unit,
    isApplyingReferralCode: Boolean,
    onApplyReferralCode: (String, String, () -> Unit) -> Unit,
    onClaimPendingReferral: (ReferralRecord) -> Unit,
    onNotifyMessage: (String) -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val preferredSet = remember(data.profile.preferredCategories) {
        data.profile.preferredCategories.toSet()
    }
    var quickPickSet by remember { mutableStateOf(setOf("Finance", "Technology")) }
    var showGoalDialog by remember { mutableStateOf(false) }
    val animatedGoalProgress by animateFloatAsState(
        targetValue = analytics.dailyGoalProgress,
        label = "goalProgress",
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Top Header with Live Balance & Preferences Trigger
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Welcome back, ${data.profile.displayName.substringBefore(" ")}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Earn Cash Rewards",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        onClick = onOpenPreferences,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                        modifier = Modifier.testTag("open_preferences_button"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Ad preferences",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = if (preferredSet.isEmpty()) "Interests" else "${preferredSet.size} Picks",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    Surface(
                        onClick = onNavigateToWallet,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = Formatters.formatCents(data.profile.availableCents),
                            style = NumericDisplayStyle.copy(
                                fontSize = MaterialTheme.typography.titleSmall.fontSize
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("open_settings_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Payment settings",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // First-time / Empty Preferred Categories Onboarding Prompt
        item {
            AnimatedVisibility(visible = preferredSet.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "Select Your Preferred Ad Categories",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            text = "Pick topics you enjoy so we prioritize matching higher-paying campaigns in your feed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AdCampaignCatalog.preferenceCategories.forEach { category ->
                                val active = category in quickPickSet
                                FilterChip(
                                    selected = active,
                                    onClick = {
                                        quickPickSet = if (active) quickPickSet - category else quickPickSet + category
                                    },
                                    label = { Text(category) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (active) Icons.Rounded.Check else categoryIcon(category),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    },
                                )
                            }
                        }
                        Button(
                            onClick = { onQuickSavePreferences(quickPickSet.toList()) },
                            enabled = quickPickSet.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("quick_save_preferences_button"),
                        ) {
                            Text("Prioritize Selected Categories")
                        }
                    }
                }
            }
        }

        // Today's Earnings & Daily Earning Goal Ring + Progress Card
        item {
            val goalReached = analytics.dailyGoalProgress >= 1f
            val remainingCents = (analytics.dailyGoalCents - analytics.todayEarnedCents).coerceAtLeast(0L)
            val progressPercent = (analytics.dailyGoalProgress * 100f).toInt().coerceIn(0, 100)
            val ringTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            val ringFillColor = if (goalReached) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.primary
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_goal_card"),
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "DAILY EARNING GOAL",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(50),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocalFireDepartment,
                                        contentDescription = "Streak",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = "${data.profile.currentStreakDays}d streak",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = { showGoalDialog = true },
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("set_daily_goal_button"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Flag,
                                    contentDescription = "Set daily earning goal",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    text = "Set Goal",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Circular Daily Goal Progress Ring
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .testTag("daily_goal_progress_ring"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokePx = 9.dp.toPx()
                                drawArc(
                                    color = ringTrackColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                                )
                                if (animatedGoalProgress > 0f) {
                                    drawArc(
                                        color = ringFillColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f * animatedGoalProgress,
                                        useCenter = false,
                                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$progressPercent%",
                                    style = NumericDisplayStyle.copy(
                                        fontSize = MaterialTheme.typography.titleMedium.fontSize
                                    ),
                                    color = ringFillColor,
                                )
                                Text(
                                    text = if (goalReached) "Done" else "Goal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = Formatters.formatCents(analytics.todayEarnedCents),
                                    style = NumericDisplayStyle.copy(
                                        fontSize = MaterialTheme.typography.headlineLarge.fontSize
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "/ ${Formatters.formatCents(analytics.dailyGoalCents)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 3.dp),
                                )
                            }
                            Text(
                                text = if (goalReached) {
                                    "Daily target achieved!"
                                } else {
                                    "${Formatters.formatCents(remainingCents)} remaining today"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (goalReached) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { animatedGoalProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                            .testTag("daily_goal_progress_bar"),
                        color = ringFillColor,
                        trackColor = ringTrackColor,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${analytics.todayAdsWatched} ads watched today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = if (analytics.todayBonusCents > 0L) {
                                "+${Formatters.formatCents(analytics.todayBonusCents)} goal bonus unlocked"
                            } else {
                                "Reach goal for +$0.25 bonus"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (analytics.todayBonusCents > 0L) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                }
            }
        }

        // Past 7 Days Earnings Line Chart
        item {
            val pastWeekPoints = remember(analytics.chartPoints) {
                analytics.chartPoints.takeLast(7)
            }
            WeeklyEarningsLineChartCard(
                weeklyPoints = pastWeekPoints,
                dailyGoalCents = analytics.dailyGoalCents,
            )
        }

        // Invite Friends & Referral Bonus Card
        item {
            ReferralSectionCard(
                myReferralCode = data.myReferralCode,
                incomingReferral = data.incomingReferral,
                outgoingReferrals = data.outgoingReferrals,
                isApplyingCode = isApplyingReferralCode,
                onApplyCode = { rawCode, onSuccess ->
                    onApplyReferralCode(rawCode, data.profile.displayName, onSuccess)
                },
                onClaimPendingReferral = onClaimPendingReferral,
                onCodeCopied = { code ->
                    onNotifyMessage("Copied invite code $code to clipboard.")
                },
            )
        }

        // Active Preferred Categories Strip (when already configured)
        if (preferredSet.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Prioritizing: ${preferredSet.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onOpenPreferences) {
                        Text("Update")
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(AdCampaignCatalog.filterCategories) { category ->
                    val isSelected = selectedFilterCategory == category
                    val isPreferred = category in preferredSet
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectFilterCategory(category) },
                        label = {
                            Text(
                                text = if (isPreferred) "$category ★" else category,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
        }

        // Sponsored Campaigns Feed
        items(prioritizedCampaigns, key = { it.id }) { campaign ->
            val todayCount = data.todayViewsByCampaign[campaign.id] ?: 0
            val isMatchedPreference = campaign.category in preferredSet
            AdCampaignCard(
                campaign = campaign,
                todayCount = todayCount,
                isMatchedPreference = isMatchedPreference,
                onWatchClick = { onWatchCampaign(campaign, todayCount) },
            )
        }
    }

    if (showGoalDialog) {
        DailyGoalDialog(
            currentGoalCents = data.profile.dailyGoalCents,
            currentDisplayName = data.profile.displayName,
            showDisplayNameField = false,
            onDismiss = { showGoalDialog = false },
            onConfirm = { newGoalCents, preservedName ->
                showGoalDialog = false
                onUpdateDailyGoal(newGoalCents, preservedName)
            },
        )
    }
}

@Composable
private fun AdCampaignCard(
    campaign: AdCampaign,
    todayCount: Int,
    isMatchedPreference: Boolean,
    onWatchClick: () -> Unit,
) {
    val remainingToday = (campaign.dailyCap - todayCount).coerceAtLeast(0)
    val isCapped = remainingToday == 0

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            width = if (isMatchedPreference && !isCapped) 1.5.dp else 1.dp,
            color = if (isMatchedPreference && !isCapped) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp),
            ) {
                Image(
                    painter = painterResource(id = campaign.imageRes),
                    contentDescription = campaign.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0x22000000), Color(0xCC06130E))
                            )
                        ),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = Color(0xCC0A1F18),
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                text = campaign.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                        if (isMatchedPreference) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(50),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(12.dp),
                                    )
                                    Text(
                                        text = "FOR YOU",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.tertiary,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = "+${Formatters.formatCents(campaign.rewardCents)}",
                            style = NumericDisplayStyle.copy(
                                fontSize = MaterialTheme.typography.titleSmall.fontSize
                            ),
                            color = Color(0xFF1A1000),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = campaign.sponsorName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA7F3D0),
                    )
                    Text(
                        text = campaign.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = campaign.headline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = "Duration",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "${campaign.durationSeconds}s",
                                style = NumericDisplayStyle.copy(
                                    fontSize = MaterialTheme.typography.labelMedium.fontSize
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Text(
                            text = "• $remainingToday/${campaign.dailyCap} left today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (isCapped) {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Cap Reached")
                        }
                    } else {
                        Button(
                            onClick = onWatchClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("watch_ad_${campaign.id}"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Watch",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }
        }
    }
}
