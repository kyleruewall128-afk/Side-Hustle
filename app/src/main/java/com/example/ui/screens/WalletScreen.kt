package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Formatters
import com.example.data.PayoutMethod
import com.example.data.PayoutRecord
import com.example.ui.components.DailyGoalDialog
import com.example.ui.theme.NumericDisplayStyle
import com.example.viewmodel.DashboardData
import kotlin.math.roundToLong

@Composable
fun WalletScreen(
    data: DashboardData,
    isPayoutSubmitting: Boolean,
    onRequestPayout: (Long, PayoutMethod, String, () -> Unit) -> Unit,
    onUpdateDailyGoal: (Long, String) -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenSettings: () -> Unit,
    onSignOut: () -> Unit,
) {
    val linked = data.linkedPaymentMethod
    var selectedMethod by remember(linked?.method) {
        mutableStateOf(linked?.parsedMethod() ?: PayoutMethod.PAYPAL)
    }
    var destinationText by remember(linked?.maskedDestination, data.profile.email) {
        mutableStateOf(
            linked?.maskedDestination?.takeIf { it.isNotBlank() } ?: data.profile.email
        )
    }
    var amountDollarsInput by remember { mutableStateOf("1.00") }
    var showGoalDialog by remember { mutableStateOf(false) }

    val availableCents = data.profile.availableCents
    val parsedAmountCents = remember(amountDollarsInput) {
        val parsed = amountDollarsInput.toDoubleOrNull() ?: 0.0
        (parsed * 100.0).roundToLong().coerceAtLeast(0L)
    }
    val canCashOut = parsedAmountCents >= selectedMethod.minCents &&
        parsedAmountCents <= availableCents &&
        destinationText.trim().length >= 3 &&
        !isPayoutSubmitting

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "CASH & SETTINGS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Your Wallet",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("wallet_settings_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Payment settings",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("sign_out_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Sign out",
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out")
                    }
                }
            }
        }

        // Metallic Emerald Balance Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF064E3B),
                                Color(0xFF059669),
                                Color(0xFF0F766E),
                            )
                        )
                    )
                    .padding(22.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "WITHDRAWABLE CASH",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA7F3D0),
                            )
                            Text(
                                text = Formatters.formatCents(availableCents),
                                style = NumericDisplayStyle.copy(
                                    fontSize = MaterialTheme.typography.displayLarge.fontSize
                                ),
                                color = Color.White,
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color(0xFFA7F3D0),
                            modifier = Modifier.size(34.dp),
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Lifetime Earned",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA7F3D0),
                            )
                            Text(
                                text = Formatters.formatCents(data.profile.lifetimeEarnedCents),
                                style = NumericDisplayStyle,
                                color = Color.White,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Withdrawn",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA7F3D0),
                            )
                            Text(
                                text = Formatters.formatCents(data.profile.totalWithdrawnCents),
                                style = NumericDisplayStyle,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }

        // Cashout Builder
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
                    Text(
                        text = "Cash Out Earnings",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PayoutMethod.entries.take(2).forEach { method ->
                            PayoutMethodChip(
                                method = method,
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PayoutMethod.entries.drop(2).forEach { method ->
                            PayoutMethodChip(
                                method = method,
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    Text(
                        text = "${selectedMethod.label}: ${selectedMethod.subtitle} (Min ${Formatters.formatCents(selectedMethod.minCents)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Quick Preset Amounts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(100L to "$1", 500L to "$5", 1000L to "$10").forEach { (cents, label) ->
                            FilterChip(
                                selected = parsedAmountCents == cents,
                                onClick = {
                                    amountDollarsInput = String.format(java.util.Locale.US, "%.2f", cents / 100.0)
                                },
                                label = { Text(label, style = NumericDisplayStyle) },
                                enabled = availableCents >= cents,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        }
                        FilterChip(
                            selected = availableCents > 0L && parsedAmountCents == availableCents,
                            onClick = {
                                amountDollarsInput = String.format(
                                    java.util.Locale.US,
                                    "%.2f",
                                    availableCents / 100.0,
                                )
                            },
                            label = { Text("Max", style = MaterialTheme.typography.labelLarge) },
                            enabled = availableCents >= selectedMethod.minCents,
                        )
                    }

                    OutlinedTextField(
                        value = amountDollarsInput,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d{0,4}(\\.\\d{0,2})?$"))) {
                                amountDollarsInput = input
                            }
                        },
                        label = { Text("Amount (USD)") },
                        prefix = { Text("$") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = destinationText,
                        onValueChange = { destinationText = it },
                        label = { Text("Destination (${selectedMethod.destinationHint})") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Button(
                        onClick = {
                            onRequestPayout(parsedAmountCents, selectedMethod, destinationText) {
                                amountDollarsInput = "1.00"
                            }
                        },
                        enabled = canCashOut,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("cash_out_button"),
                    ) {
                        if (isPayoutSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Submitting cashout...")
                        } else {
                            Text(
                                text = if (availableCents < selectedMethod.minCents) {
                                    "Earn ${Formatters.formatCents(selectedMethod.minCents - availableCents)} more to cash out"
                                } else {
                                    "Cash Out ${Formatters.formatCents(parsedAmountCents)}"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }
        }

        // Ad Preferences & Daily Goal Settings
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
                    Text(
                        text = "Personalization & Goals",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Preferred Ad Categories",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = if (data.profile.preferredCategories.isEmpty()) {
                                    "All categories shown equally"
                                } else {
                                    data.profile.preferredCategories.joinToString(", ")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenPreferences,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("wallet_edit_preferences_button"),
                        ) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Linked Withdrawal Account",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = if (linked != null) {
                                    "${linked.parsedMethod().label} • ${linked.maskedDestination}"
                                } else {
                                    "None linked yet — set PayPal or Bank ACH"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (linked != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenSettings,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("manage_payment_method_button"),
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (linked != null) "Manage" else "Link")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Earnings Goal",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "${Formatters.formatCents(data.profile.dailyGoalCents)} / day (unlocks +$0.25 bonus)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(
                            onClick = { showGoalDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("edit_goal_button"),
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Goal")
                        }
                    }
                }
            }
        }

        // Payout History
        item {
            Text(
                text = "Payout History",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (data.payouts.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "No cashouts requested yet. Reach $1.00 to submit your first payout.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp),
                    )
                }
            }
        } else {
            items(data.payouts, key = { it.id }) { payout ->
                PayoutHistoryCard(payout)
            }
        }
    }

    if (showGoalDialog) {
        DailyGoalDialog(
            currentGoalCents = data.profile.dailyGoalCents,
            currentDisplayName = data.profile.displayName,
            showDisplayNameField = true,
            onDismiss = { showGoalDialog = false },
            onConfirm = { newGoalCents, newName ->
                showGoalDialog = false
                onUpdateDailyGoal(newGoalCents, newName)
            },
        )
    }
}

@Composable
private fun PayoutMethodChip(
    method: PayoutMethod,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        ),
        modifier = modifier.height(54.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = method.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PayoutHistoryCard(payout: PayoutRecord) {
    val method = payout.parsedMethod()
    val status = payout.parsedStatus()
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "${method.label} • ${payout.referenceCode}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${payout.destinationLabel} • ${Formatters.friendlyDateLabel(payout.dateKey)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatCents(payout.amountCents),
                    style = NumericDisplayStyle.copy(
                        fontSize = MaterialTheme.typography.titleMedium.fontSize
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = status.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
