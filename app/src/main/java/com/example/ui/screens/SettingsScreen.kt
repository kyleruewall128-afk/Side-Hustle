package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BankAccountType
import com.example.data.PayoutMethod
import com.example.viewmodel.DashboardData

@Composable
fun SettingsScreen(
    data: DashboardData,
    isSavingPaymentSettings: Boolean,
    onBack: () -> Unit,
    onSaveLinkedPaymentMethod: (
        method: PayoutMethod,
        accountHolderName: String,
        destinationOrEmail: String,
        bankName: String,
        accountType: BankAccountType,
        routingNumber: String,
        accountNumber: String,
        autoPayoutEnabled: Boolean,
        onSaved: () -> Unit,
    ) -> Unit,
    onUnlinkPaymentMethod: () -> Unit,
    onOpenPreferences: () -> Unit,
    onSignOut: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val linked = data.linkedPaymentMethod
    var selectedMethod by remember(linked?.method) {
        mutableStateOf(linked?.parsedMethod() ?: PayoutMethod.PAYPAL)
    }
    var accountHolderName by remember(linked?.accountHolderName, data.profile.displayName) {
        mutableStateOf(
            linked?.accountHolderName?.takeIf { it.isNotBlank() } ?: data.profile.displayName
        )
    }
    var destinationInput by remember(linked?.maskedDestination, linked?.method, data.profile.email) {
        mutableStateOf(
            when {
                linked != null && linked.parsedMethod() != PayoutMethod.BANK_ACH -> linked.maskedDestination
                else -> data.profile.email
            }
        )
    }
    var bankName by remember(linked?.bankName) {
        mutableStateOf(linked?.bankName ?: "")
    }
    var bankAccountType by remember(linked?.accountType) {
        mutableStateOf(linked?.parsedBankAccountType() ?: BankAccountType.CHECKING)
    }
    var routingNumber by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var showBankNumbers by remember { mutableStateOf(false) }
    var autoPayoutEnabled by remember(linked?.autoPayoutEnabled) {
        mutableStateOf(linked?.autoPayoutEnabled ?: true)
    }
    var showUnlinkDialog by remember { mutableStateOf(false) }

    val routingDigits = remember(routingNumber) { routingNumber.filter { it.isDigit() } }
    val accountDigits = remember(accountNumber) { accountNumber.filter { it.isDigit() } }

    val holderValid = accountHolderName.trim().length >= 2
    val bankFormValid = bankName.trim().length >= 2 &&
        routingDigits.length == 9 &&
        accountDigits.length in 4..17
    val emailFormValid = destinationInput.trim().let {
        it.length >= 5 && it.contains("@") && it.contains(".")
    }
    val venmoFormValid = destinationInput.trim().length >= 3

    val isMethodFormValid = when (selectedMethod) {
        PayoutMethod.BANK_ACH -> bankFormValid
        PayoutMethod.PAYPAL, PayoutMethod.GIFT_CARD -> emailFormValid
        PayoutMethod.VENMO -> venmoFormValid
    }
    val canSubmit = holderValid && isMethodFormValid && !isSavingPaymentSettings

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Top Header with Back Navigation
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    onClick = onBack,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("settings_back_button"),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PAYOUT & SECURITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Payment Settings",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }

        // Currently Linked Withdrawal Account Summary
        item {
            if (linked != null) {
                val linkedMethod = linked.parsedMethod()
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("linked_method_summary_card"),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
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
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = "Verified linked account",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = "LINKED PREFERRED ACCOUNT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            TextButton(
                                onClick = { showUnlinkDialog = true },
                                enabled = !isSavingPaymentSettings,
                                modifier = Modifier.testTag("unlink_payment_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LinkOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Unlink")
                            }
                        }

                        Text(
                            text = linkedMethod.label,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${linked.accountHolderName} • ${linked.maskedDestination}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (linkedMethod == PayoutMethod.BANK_ACH && linked.routingLast4.isNotEmpty()) {
                            Text(
                                text = "Routing •••• ${linked.routingLast4} • Account •••• ${linked.accountLast4}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "No preferred withdrawal method linked",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Link PayPal, Bank ACH, Venmo, or Gift Card below for 1-tap Wallet cashouts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // Security Protection Notice
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Protected storage: Full bank routing and account numbers are masked on-device before saving — only the last 4 digits and verified payout label are stored.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Link / Update Withdrawal Method Form
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
                        text = if (linked == null) "Link Withdrawal Method" else "Update Withdrawal Method",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PayoutMethod.entries.take(2).forEach { method ->
                            SettingsMethodChip(
                                method = method,
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_method_${method.name}"),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PayoutMethod.entries.drop(2).forEach { method ->
                            SettingsMethodChip(
                                method = method,
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_method_${method.name}"),
                            )
                        }
                    }

                    Text(
                        text = "${selectedMethod.label} • ${selectedMethod.subtitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    OutlinedTextField(
                        value = accountHolderName,
                        onValueChange = { accountHolderName = it.take(80) },
                        label = { Text("Account Holder Full Name") },
                        singleLine = true,
                        isError = accountHolderName.isNotEmpty() && !holderValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_holder_input"),
                    )

                    AnimatedVisibility(visible = selectedMethod == PayoutMethod.BANK_ACH) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = bankName,
                                onValueChange = { bankName = it.take(60) },
                                label = { Text("Bank / Institution Name") },
                                placeholder = { Text("e.g. Chase Bank, Wells Fargo") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_bank_name_input"),
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                BankAccountType.entries.forEach { type ->
                                    FilterChip(
                                        selected = bankAccountType == type,
                                        onClick = { bankAccountType = type },
                                        label = { Text("${type.label} Account") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        ),
                                        modifier = Modifier.testTag("settings_account_type_${type.name}"),
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = routingNumber,
                                onValueChange = { input ->
                                    routingNumber = input.filter { it.isDigit() }.take(9)
                                },
                                label = { Text("9-Digit Routing Number") },
                                supportingText = {
                                    Text("${routingDigits.length}/9 digits • Stored masked as last 4")
                                },
                                isError = routingNumber.isNotEmpty() && routingDigits.length != 9,
                                singleLine = true,
                                visualTransformation = if (showBankNumbers) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showBankNumbers = !showBankNumbers }) {
                                        Icon(
                                            imageVector = if (showBankNumbers) {
                                                Icons.Rounded.VisibilityOff
                                            } else {
                                                Icons.Rounded.Visibility
                                            },
                                            contentDescription = if (showBankNumbers) "Hide numbers" else "Show numbers",
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_routing_input"),
                            )

                            OutlinedTextField(
                                value = accountNumber,
                                onValueChange = { input ->
                                    accountNumber = input.filter { it.isDigit() }.take(17)
                                },
                                label = { Text("Account Number (4–17 digits)") },
                                supportingText = {
                                    Text("Only the last 4 digits are persisted to your profile")
                                },
                                isError = accountNumber.isNotEmpty() && accountDigits.length !in 4..17,
                                singleLine = true,
                                visualTransformation = if (showBankNumbers) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_account_input"),
                            )
                        }
                    }

                    AnimatedVisibility(visible = selectedMethod != PayoutMethod.BANK_ACH) {
                        val fieldLabel = when (selectedMethod) {
                            PayoutMethod.PAYPAL -> "PayPal Account Email"
                            PayoutMethod.VENMO -> "Venmo @Handle or US Phone"
                            PayoutMethod.GIFT_CARD -> "Gift Card Delivery Email"
                            PayoutMethod.BANK_ACH -> ""
                        }
                        OutlinedTextField(
                            value = destinationInput,
                            onValueChange = { destinationInput = it.take(120) },
                            label = { Text(fieldLabel) },
                            placeholder = { Text(selectedMethod.destinationHint) },
                            isError = destinationInput.isNotEmpty() && !isMethodFormValid,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (selectedMethod == PayoutMethod.VENMO) {
                                    KeyboardType.Text
                                } else {
                                    KeyboardType.Email
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_destination_input"),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Use as default Wallet payout rail",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Auto-fill this linked destination when cashing out",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = autoPayoutEnabled,
                            onCheckedChange = { autoPayoutEnabled = it },
                            modifier = Modifier.testTag("settings_default_switch"),
                        )
                    }

                    Button(
                        onClick = {
                            onSaveLinkedPaymentMethod(
                                selectedMethod,
                                accountHolderName,
                                destinationInput,
                                bankName,
                                bankAccountType,
                                routingDigits,
                                accountDigits,
                                autoPayoutEnabled,
                            ) {
                                routingNumber = ""
                                accountNumber = ""
                            }
                        },
                        enabled = canSubmit,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("save_linked_payment_button"),
                    ) {
                        if (isSavingPaymentSettings) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Linking payment method...")
                        } else {
                            Text(
                                text = "Save & Link ${selectedMethod.label}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }
        }

        // General App Account & Ad Preferences Section
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
                        text = "Account & Ad Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ad Interests", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (data.profile.preferredCategories.isEmpty()) {
                                    "All categories active"
                                } else {
                                    data.profile.preferredCategories.joinToString(", ")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenPreferences,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Categories")
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
                                text = data.profile.displayName,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = data.profile.email.ifBlank { "Google Account" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("settings_sign_out_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign Out")
                        }
                    }
                }
            }
        }
    }

    if (showUnlinkDialog) {
        AlertDialog(
            onDismissRequest = { showUnlinkDialog = false },
            title = { Text("Unlink withdrawal account?") },
            text = {
                Text("This removes your saved preferred payout method from Payment Settings. You can link a new account anytime.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnlinkDialog = false
                        onUnlinkPaymentMethod()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Unlink")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SettingsMethodChip(
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
