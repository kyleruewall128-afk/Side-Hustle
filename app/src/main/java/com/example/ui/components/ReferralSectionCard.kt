package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Formatters
import com.example.data.ReferralConstants
import com.example.data.ReferralRecord
import com.example.ui.theme.NumericDisplayStyle

@Composable
fun ReferralSectionCard(
    myReferralCode: String,
    incomingReferral: ReferralRecord?,
    outgoingReferrals: List<ReferralRecord>,
    isApplyingCode: Boolean,
    onApplyCode: (String, () -> Unit) -> Unit,
    onClaimPendingReferral: (ReferralRecord) -> Unit,
    onCodeCopied: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    var codeInput by remember { mutableStateOf("") }

    val completedCount = remember(outgoingReferrals) {
        outgoingReferrals.count { it.isCompleted() }
    }
    val earnedReferralCents = remember(outgoingReferrals) {
        outgoingReferrals.filter { it.referrerCredited }.sumOf { it.bonusCents }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("referral_section_card"),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header + Bonus Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CardGiftcard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Column {
                        Text(
                            text = "INVITE & EARN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Refer Friends",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = "+${Formatters.formatCents(ReferralConstants.BONUS_CENTS)} FOR BOTH",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            Text(
                text = "Share your unique code with friends. Once they watch their first ${ReferralConstants.REQUIRED_ADS} ads, you both unlock a ${Formatters.formatCents(ReferralConstants.BONUS_CENTS)} cash bonus.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // User's Shareable Referral Code Box
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "YOUR INVITE CODE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                            Text(
                                text = myReferralCode.ifBlank { "GENERATING..." },
                                style = NumericDisplayStyle.copy(
                                    fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                                    fontWeight = FontWeight.ExtraBold,
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.testTag("my_referral_code_text"),
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                text = "${outgoingReferrals.size} joined",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (myReferralCode.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(myReferralCode))
                                    onCodeCopied(myReferralCode)
                                }
                            },
                            enabled = myReferralCode.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("copy_referral_code_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy invite code",
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Code")
                        }

                        Button(
                            onClick = {
                                if (myReferralCode.isNotBlank()) {
                                    val shareText = "Join me on Side Hustle! Enter my invite code $myReferralCode and watch ${ReferralConstants.REQUIRED_ADS} ads so we both get +${Formatters.formatCents(ReferralConstants.BONUS_CENTS)} cash."
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Get +$0.50 on Side Hustle")
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(sendIntent, "Share Side Hustle Invite")
                                    )
                                }
                            },
                            enabled = myReferralCode.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_referral_code_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share invite code",
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Invite")
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            // Incoming Referral Redemption / Milestone Tracker
            if (incomingReferral == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Have a friend's invite code?",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { raw ->
                                val filtered = raw.uppercase().filter { it.isLetterOrDigit() }.take(12)
                                codeInput = filtered
                            },
                            placeholder = { Text("Enter code (e.g. SH8K2P)") },
                            singleLine = true,
                            enabled = !isApplyingCode,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (codeInput.length >= 6 && !isApplyingCode) {
                                        onApplyCode(codeInput) { codeInput = "" }
                                    }
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("referral_code_input"),
                        )

                        Button(
                            onClick = {
                                onApplyCode(codeInput) { codeInput = "" }
                            },
                            enabled = codeInput.length >= 6 && !isApplyingCode,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("apply_referral_button"),
                        ) {
                            if (isApplyingCode) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Apply")
                            }
                        }
                    }
                }
            } else {
                val progress = (incomingReferral.adsCompleted.toFloat() /
                    incomingReferral.requiredAds.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                val remainingAds = (incomingReferral.requiredAds - incomingReferral.adsCompleted).coerceAtLeast(0L)
                val completed = incomingReferral.isCompleted()

                Surface(
                    color = if (completed) {
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(
                        1.dp,
                        if (completed) {
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("incoming_referral_status_card"),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                Icon(
                                    imageVector = if (completed) Icons.Rounded.CheckCircle else Icons.Rounded.GroupAdd,
                                    contentDescription = null,
                                    tint = if (completed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "Invited by ${incomingReferral.referrerFirstName} (${incomingReferral.referralCode})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                text = "${incomingReferral.adsCompleted}/${incomingReferral.requiredAds} ads",
                                style = NumericDisplayStyle,
                                color = if (completed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = if (completed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainer,
                        )

                        Text(
                            text = if (completed) {
                                "Unlocked! +${Formatters.formatCents(incomingReferral.bonusCents)} bonus credited to both you & ${incomingReferral.referrerFirstName}."
                            } else {
                                "Watch $remainingAds more verified ${if (remainingAds == 1L) "ad" else "ads"} to unlock +${Formatters.formatCents(incomingReferral.bonusCents)} for both of you."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Invited Friends Ledger
            if (outgoingReferrals.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Invited Friends (${outgoingReferrals.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "$completedCount completed • +${Formatters.formatCents(earnedReferralCents)} earned",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    outgoingReferrals.take(5).forEach { invite ->
                        val completed = invite.isCompleted()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = invite.inviteeFirstName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = if (completed) {
                                        if (invite.referrerCredited) {
                                            "Bonus settled (+${Formatters.formatCents(invite.bonusCents)})"
                                        } else {
                                            "Milestone reached — ready to claim"
                                        }
                                    } else {
                                        "Progress: ${invite.adsCompleted}/${invite.requiredAds} ads watched"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            if (completed && !invite.referrerCredited) {
                                TextButton(
                                    onClick = { onClaimPendingReferral(invite) },
                                    modifier = Modifier.testTag("claim_referral_${invite.id}"),
                                ) {
                                    Text("Claim +${Formatters.formatCents(invite.bonusCents)}")
                                }
                            } else {
                                Surface(
                                    color = if (completed) {
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                    shape = RoundedCornerShape(50),
                                ) {
                                    Text(
                                        text = if (completed) "+${Formatters.formatCents(invite.bonusCents)}" else "${invite.adsCompleted}/${invite.requiredAds}",
                                        style = NumericDisplayStyle.copy(
                                            fontSize = MaterialTheme.typography.labelMedium.fontSize
                                        ),
                                        color = if (completed) {
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
