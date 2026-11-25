package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AdCampaignCatalog

fun categoryIcon(category: String): ImageVector = when (category) {
    "Finance" -> Icons.Rounded.AccountBalance
    "Technology" -> Icons.Rounded.Memory
    "Gaming" -> Icons.Rounded.SportsEsports
    "Fashion" -> Icons.Rounded.Checkroom
    "Travel" -> Icons.Rounded.Flight
    "Wellness" -> Icons.Rounded.SelfImprovement
    else -> Icons.Rounded.AutoAwesome
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdPreferencesBottomSheet(
    currentPreferences: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by remember(currentPreferences) {
        mutableStateOf(currentPreferences.toSet())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                    Text(
                        text = "Preferred Ad Categories",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "We prioritize matching high-CPM sponsors at the top of your feed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AdCampaignCatalog.preferenceCategories.forEach { category ->
                    val isSelected = category in selected
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selected = if (isSelected) selected - category else selected + category
                        },
                        label = {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isSelected) Icons.Rounded.Check else categoryIcon(category),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        ),
                        modifier = Modifier.testTag("pref_chip_$category"),
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (selected.isEmpty()) {
                        "No preference filter set — all categories will appear ranked by payout."
                    } else {
                        "Prioritized: ${selected.joinToString(", ")}. Matching campaigns unlock higher advertiser bids."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { selected = emptySet() },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                ) {
                    Text("Reset")
                }
                Button(
                    onClick = {
                        onSave(selected.toList())
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(2f)
                        .height(50.dp)
                        .testTag("save_preferences_button"),
                ) {
                    Text(
                        text = "Save Preferences",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}
