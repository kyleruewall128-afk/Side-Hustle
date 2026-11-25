package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AdWatchRewardOutcome
import com.example.data.Formatters
import com.example.ui.theme.NumericDisplayStyle
import com.example.viewmodel.ActiveWatchSession

@Composable
fun WatchAdOverlay(
    session: ActiveWatchSession,
    onCancelConfirmed: () -> Unit,
    onVerifyAndClaim: () -> Unit,
) {
    var showExitConfirm by remember { mutableStateOf(false) }

    BackHandler {
        if (session.isReadyToClaim && !session.isSubmitting) {
            showExitConfirm = true
        } else if (!session.isSubmitting) {
            showExitConfirm = true
        }
    }

    val progress = 1f - (session.remainingSeconds.toFloat() / session.totalSeconds.coerceAtLeast(1).toFloat())
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "adWatchProgress")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF060C0A))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 580.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                // Top Player Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = Color(0xFF14241E),
                        shape = RoundedCornerShape(50),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (session.isReadyToClaim) Color(0xFF34D399) else Color(0xFFFBBF24)
                                    ),
                            )
                            Text(
                                text = if (session.isReadyToClaim) {
                                    "VERIFICATION READY"
                                } else {
                                    "EARNING IN ${session.remainingSeconds}s"
                                },
                                style = NumericDisplayStyle.copy(
                                    fontSize = MaterialTheme.typography.labelMedium.fontSize
                                ),
                                color = Color.White,
                            )
                        }
                    }

                    IconButton(
                        onClick = { showExitConfirm = true },
                        enabled = !session.isSubmitting,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close advert",
                            tint = Color(0xFFCBD5E1),
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = Color(0xFF34D399),
                    trackColor = Color(0xFF1A2E27),
                )

                // Sponsored Creative Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(MaterialTheme.shapes.extraLarge),
                ) {
                    Image(
                        painter = painterResource(id = session.campaign.imageRes),
                        contentDescription = session.campaign.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xDF050E0B))
                                )
                            ),
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "${session.campaign.sponsorName} • ${session.campaign.category}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF34D399),
                        )
                        Text(
                            text = session.campaign.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                        )
                    }
                }

                // Sponsor Copy & Highlights
                Surface(
                    color = Color(0xFF0F1B17),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = session.campaign.headline,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                        session.campaign.highlights.forEach { bullet ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(top = 2.dp),
                                )
                                Text(
                                    text = bullet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFB5C7C0),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Verification & Claim Footer
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AnimatedVisibility(visible = session.isReadyToClaim) {
                    Button(
                        onClick = onVerifyAndClaim,
                        enabled = !session.isSubmitting,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color(0xFF022C22),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("claim_reward_button"),
                    ) {
                        if (session.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color(0xFF022C22),
                                strokeWidth = 2.5.dp,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Crediting wallet...")
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Verify & Claim +${Formatters.formatCents(session.campaign.rewardCents)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }

                if (!session.isReadyToClaim) {
                    Surface(
                        color = Color(0xFF13221D),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Keep watching (${session.remainingSeconds}s) to unlock +${Formatters.formatCents(session.campaign.rewardCents)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF9BB3AA),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("Leave advert early?") },
            text = {
                Text(
                    "Closing before verifying forfeits the ${Formatters.formatCents(session.campaign.rewardCents)} cash reward for this view."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirm = false
                        onCancelConfirmed()
                    }
                ) {
                    Text("Leave & Forfeit", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text("Keep Watching")
                }
            },
        )
    }
}

@Composable
fun RewardEarnedDialog(
    outcome: AdWatchRewardOutcome,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Verified,
                contentDescription = "Reward credited",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
        },
        title = {
            Text(
                text = "Cash Reward Credited!",
                style = MaterialTheme.typography.headlineMedium,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Verified view of ${outcome.campaignTitle}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Advert Payout", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "+${Formatters.formatCents(outcome.rewardCents)}",
                                style = NumericDisplayStyle,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (outcome.goalReachedNow) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Daily Goal Bonus", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "+${Formatters.formatCents(outcome.bonusCents)}",
                                    style = NumericDisplayStyle,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        }
                        if (outcome.referralBonusCents > 0L) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Referral Bonus Unlocked", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "+${Formatters.formatCents(outcome.referralBonusCents)}",
                                    style = NumericDisplayStyle,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Updated Balance", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = Formatters.formatCents(outcome.newAvailableCents),
                                style = NumericDisplayStyle,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Awesome")
            }
        },
    )
}
