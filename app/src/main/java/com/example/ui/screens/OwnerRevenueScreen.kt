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
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.example.data.APP_OWNER_EMAIL
import com.example.data.Formatters
import com.example.data.OwnerPayoutRecord
import com.example.data.OwnerRevenueEntry
import com.example.data.PayoutMethod
import com.example.data.UiState
import com.example.ui.theme.Emerald15
import com.example.ui.theme.Emerald30
import com.example.ui.theme.Emerald90
import com.example.ui.theme.Gold20
import com.example.ui.theme.Gold40
import com.example.ui.theme.NumericDisplayStyle
import com.example.viewmodel.OwnerConsoleData
import java.util.Locale
import kotlin.math.roundToLong

private const val OWNER_MIN_WITHDRAW_CENTS = 100L

@Composable
fun OwnerRevenueScreen(
    ownerState: UiState<OwnerConsoleData>?,
    isOwnerConfigSaving: Boolean,
    isOwnerWithdrawing: Boolean,
    onSaveOwnerConfig: (PayoutMethod, String, Boolean) -> Unit,
    onWithdrawOwnerIncome: (Long, PayoutMethod, String, () -> Unit) -> Unit,
) {
    when (ownerState) {
        null, is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("owner_console_loading"),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is UiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Unable to load Owner Income Console",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = ownerState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        is UiState.Success -> {
            OwnerConsoleContent(
                data = ownerState.data,
                isOwnerConfigSaving = isOwnerConfigSaving,
                isOwnerWithdrawing = isOwnerWithdrawing,
                onSaveOwnerConfig = onSaveOwnerConfig,
                onWithdrawOwnerIncome = onWithdrawOwnerIncome,
            )
        }
    }
}

@Composable
private fun OwnerConsoleContent(
    data: OwnerConsoleData,
    isOwnerConfigSaving: Boolean,
    isOwnerWithdrawing: Boolean,
    onSaveOwnerConfig: (PayoutMethod, String, Boolean) -> Unit,
    onWithdrawOwnerIncome: (Long, PayoutMethod, String, () -> Unit) -> Unit,
) {
    val config = data.payoutConfig
    var selectedMethod by remember(config.method) { mutableStateOf(config.parsedMethod()) }
    var destinationText by remember(config.destinationLabel) {
        mutableStateOf(config.destinationLabel.ifBlank { APP_OWNER_EMAIL })
    }
    var autoSweepEnabled by remember(config.autoSweepEnabled) {
        mutableStateOf(config.autoSweepEnabled)
    }
    var withdrawDollarsInput by remember { mutableStateOf("1.00") }

    val availableIncomeCents = data.summary.availableIncomeCents
    val parsedWithdrawCents = remember(withdrawDollarsInput) {
        val parsed = withdrawDollarsInput.toDoubleOrNull() ?: 0.0
        (parsed * 100.0).roundToLong().coerceAtLeast(0L)
    }
    val canSaveConfig = destinationText.trim().length >= 3 && !isOwnerConfigSaving
    val canWithdraw = parsedWithdrawCents >= OWNER_MIN_WITHDRAW_CENTS &&
        parsedWithdrawCents <= availableIncomeCents &&
        destinationText.trim().length >= 3 &&
        !isOwnerWithdrawing

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("owner_revenue_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Executive Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "OWNER REVENUE CONSOLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        text = "Platform Income",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(50),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = "Verified owner account",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "VERIFIED OWNER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
        }

        // Hero Owner Income Account Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Emerald15,
                                Emerald30,
                                Gold20,
                            )
                        )
                    )
                    .padding(22.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AVAILABLE OWNER INCOME",
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald90,
                            )
                            Text(
                                text = Formatters.formatCents(availableIncomeCents),
                                style = NumericDisplayStyle.copy(
                                    fontSize = MaterialTheme.typography.displayLarge.fontSize
                                ),
                                color = Color.White,
                            )
                            Text(
                                text = "60% owner profit • 40% viewer reward • ${config.ownerEmail.ifBlank { APP_OWNER_EMAIL }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Emerald90,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Payments,
                            contentDescription = null,
                            tint = Emerald90,
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
                                text = "Lifetime Net Income",
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald90,
                            )
                            Text(
                                text = Formatters.formatCents(data.summary.lifetimeIncomeCents),
                                style = NumericDisplayStyle,
                                color = Color.White,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Owner Withdrawn",
                                style = MaterialTheme.typography.labelSmall,
                                color = Emerald90,
                            )
                            Text(
                                text = Formatters.formatCents(data.summary.withdrawnIncomeCents),
                                style = NumericDisplayStyle,
                                color = Color.White,
                            )
                        }
                    }

                    Surface(
                        color = Color.Black.copy(alpha = 0.22f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            OwnerMiniMetric(
                                label = "Gross Sponsor",
                                value = Formatters.formatCents(data.summary.grossSponsorCents),
                            )
                            OwnerMiniMetric(
                                label = "User Rewards",
                                value = Formatters.formatCents(data.summary.totalUserRewardsCents),
                            )
                            OwnerMiniMetric(
                                label = "Verified Views",
                                value = data.summary.totalVerifiedViews.toString(),
                            )
                        }
                    }
                }
            }
        }

        // Owner Payout Account Configuration
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Owner Payout Account",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PayoutMethod.entries.take(2).forEach { method ->
                            OwnerPayoutMethodChip(
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
                            OwnerPayoutMethodChip(
                                method = method,
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    OutlinedTextField(
                        value = destinationText,
                        onValueChange = { destinationText = it },
                        label = { Text("Account Destination (${selectedMethod.destinationHint})") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_destination_input"),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-sweep daily earnings",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Queue settlement once balance exceeds $5.00",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = autoSweepEnabled,
                            onCheckedChange = { autoSweepEnabled = it },
                            modifier = Modifier.testTag("owner_autosweep_switch"),
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            onSaveOwnerConfig(selectedMethod, destinationText, autoSweepEnabled)
                        },
                        enabled = canSaveConfig,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_owner_config_button"),
                    ) {
                        if (isOwnerConfigSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving account...")
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Payout Destination")
                        }
                    }
                }
            }
        }

        // Withdraw Owner Income Form
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                            contentDescription = null,
                            tint = Gold40,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Withdraw Owner Income",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Text(
                        text = "Dispatch settled ad-margin earnings via ${selectedMethod.label} to ${destinationText.trim().ifBlank { APP_OWNER_EMAIL }} (Min $1.00).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(100L to "$1", 500L to "$5", 1000L to "$10").forEach { (cents, label) ->
                            FilterChip(
                                selected = parsedWithdrawCents == cents,
                                onClick = {
                                    withdrawDollarsInput = String.format(Locale.US, "%.2f", cents / 100.0)
                                },
                                label = { Text(label, style = NumericDisplayStyle) },
                                enabled = availableIncomeCents >= cents,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                        }
                        FilterChip(
                            selected = availableIncomeCents > 0L && parsedWithdrawCents == availableIncomeCents,
                            onClick = {
                                withdrawDollarsInput = String.format(
                                    Locale.US,
                                    "%.2f",
                                    availableIncomeCents / 100.0,
                                )
                            },
                            label = { Text("Max", style = MaterialTheme.typography.labelLarge) },
                            enabled = availableIncomeCents >= OWNER_MIN_WITHDRAW_CENTS,
                        )
                    }

                    OutlinedTextField(
                        value = withdrawDollarsInput,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d{0,5}(\\.\\d{0,2})?$"))) {
                                withdrawDollarsInput = input
                            }
                        },
                        label = { Text("Withdraw Amount (USD)") },
                        prefix = { Text("$") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_withdraw_amount_input"),
                    )

                    Button(
                        onClick = {
                            onWithdrawOwnerIncome(parsedWithdrawCents, selectedMethod, destinationText) {
                                withdrawDollarsInput = "1.00"
                            }
                        },
                        enabled = canWithdraw,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("withdraw_owner_income_button"),
                    ) {
                        if (isOwnerWithdrawing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Transferring income...")
                        } else {
                            Text(
                                text = if (availableIncomeCents < OWNER_MIN_WITHDRAW_CENTS) {
                                    "Need ${Formatters.formatCents(OWNER_MIN_WITHDRAW_CENTS - availableIncomeCents)} more to withdraw"
                                } else {
                                    "Withdraw ${Formatters.formatCents(parsedWithdrawCents)} to Account"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }
        }

        // Real-Time Income Ledger
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Real-Time Income Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${data.entries.size} verified impressions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (data.entries.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "No verified ad-view credits recorded yet. Every advert watched by any user automatically credits your owner balance here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp),
                    )
                }
            }
        } else {
            items(data.entries.take(40), key = { it.id }) { entry ->
                OwnerRevenueEntryCard(entry)
            }
        }

        // Owner Withdrawal History
        item {
            Text(
                text = "Owner Withdrawal History",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 6.dp),
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
                        text = "No owner withdrawals yet. Reach $1.00 in available income to dispatch a payout.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp),
                    )
                }
            }
        } else {
            items(data.payouts, key = { it.id }) { payout ->
                OwnerPayoutHistoryCard(payout)
            }
        }
    }
}

@Composable
private fun OwnerMiniMetric(
    label: String,
    value: String,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Emerald90,
        )
        Text(
            text = value,
            style = NumericDisplayStyle.copy(
                fontSize = MaterialTheme.typography.titleSmall.fontSize
            ),
            color = Color.White,
        )
    }
}

@Composable
private fun OwnerPayoutMethodChip(
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
private fun OwnerRevenueEntryCard(entry: OwnerRevenueEntry) {
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = entry.campaignTitle.ifBlank { "Verified Ad Impression" },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (entry.preferenceMatched) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                text = "PREF MATCH",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    text = "${entry.category} • Gross ${Formatters.formatCents(entry.grossSponsorCents)} − User ${Formatters.formatCents(entry.userRewardCents)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${Formatters.friendlyDateLabel(entry.dateKey)} • ${Formatters.formatTimestampTime(entry.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "+${Formatters.formatCents(entry.ownerIncomeCents)}",
                style = NumericDisplayStyle.copy(
                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                ),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun OwnerPayoutHistoryCard(payout: OwnerPayoutRecord) {
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
