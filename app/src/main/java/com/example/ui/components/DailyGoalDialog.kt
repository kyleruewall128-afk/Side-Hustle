package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.Formatters
import com.example.ui.theme.NumericDisplayStyle
import java.util.Locale
import kotlin.math.roundToLong

private val GOAL_PRESETS_CENTS = listOf(100L, 200L, 500L, 1000L)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyGoalDialog(
    currentGoalCents: Long,
    currentDisplayName: String,
    showDisplayNameField: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Long, String) -> Unit,
) {
    var dollarsInput by remember(currentGoalCents) {
        mutableStateOf(String.format(Locale.US, "%.2f", currentGoalCents / 100.0))
    }
    var nameInput by remember(currentDisplayName) { mutableStateOf(currentDisplayName) }

    val parsedCents = remember(dollarsInput) {
        val parsed = dollarsInput.toDoubleOrNull()
        if (parsed == null) -1L else (parsed * 100.0).roundToLong()
    }
    val isValidGoal = parsedCents in 10L..50000L
    val isValidName = !showDisplayNameField || nameInput.trim().isNotEmpty()
    val sliderCents = parsedCents.coerceIn(50L, 1500L).toFloat()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Daily Earning Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isValidGoal) {
                            "Target: ${Formatters.formatCents(parsedCents)} / day"
                        } else {
                            "Enter $0.10 – $500.00"
                        },
                        style = NumericDisplayStyle.copy(
                            fontSize = MaterialTheme.typography.titleLarge.fontSize
                        ),
                        color = if (isValidGoal) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GOAL_PRESETS_CENTS.forEach { presetCents ->
                        val selected = parsedCents == presetCents
                        FilterChip(
                            selected = selected,
                            onClick = {
                                dollarsInput = String.format(Locale.US, "%.2f", presetCents / 100.0)
                            },
                            label = { Text(Formatters.formatCents(presetCents)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                            modifier = Modifier.testTag("goal_preset_$presetCents"),
                        )
                    }
                }

                Slider(
                    value = sliderCents,
                    onValueChange = { raw ->
                        val snapped = ((raw / 25f).roundToLong() * 25L).coerceIn(50L, 1500L)
                        dollarsInput = String.format(Locale.US, "%.2f", snapped / 100.0)
                    },
                    valueRange = 50f..1500f,
                )

                OutlinedTextField(
                    value = dollarsInput,
                    onValueChange = { raw ->
                        val filtered = raw.filter { it.isDigit() || it == '.' }.take(8)
                        dollarsInput = filtered
                    },
                    label = { Text("Custom Daily Target (USD)") },
                    prefix = { Text("$") },
                    supportingText = {
                        Text("Earn a +$0.25 bonus once per day when you hit your target")
                    },
                    isError = dollarsInput.isNotEmpty() && !isValidGoal,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_goal_amount_input"),
                )

                if (showDisplayNameField) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it.take(60) },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = nameInput.trim().ifEmpty { currentDisplayName.ifBlank { "Member" } }
                    onConfirm(parsedCents.coerceIn(10L, 50000L), finalName)
                },
                enabled = isValidGoal && isValidName,
                modifier = Modifier.testTag("save_daily_goal_button"),
            ) {
                Text("Save Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
