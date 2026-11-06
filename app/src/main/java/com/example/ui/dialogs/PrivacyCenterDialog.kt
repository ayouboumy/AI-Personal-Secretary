package com.example.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.PrivacyManager
import com.example.ui.SecretaryViewModel
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.DangerRose
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate600
import com.example.ui.theme.SuccessEmerald
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyCenterDialog(
    viewModel: SecretaryViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val userProfile by viewModel.userProfile.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    val allowCloud = userProfile?.allowCloudAiConsent == true
    val protectPreviews = userProfile?.protectNotificationPreviews ?: true

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SuccessEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Filled.Shield, contentDescription = null, tint = SuccessEmerald, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Privacy & Security Center",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Local Room storage, zero cloud tracking, transparent AI",
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            }

            // Data Categories Breakdown (Part 3.1)
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Data Classification & Protection",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CategoryRow(
                            label = "A. Demonstration / Anonymized",
                            desc = "Synthetic samples calibrated with zero personal details.",
                            color = PrimaryBlue
                        )
                        CategoryRow(
                            label = "B. Ordinary User Authorized",
                            desc = "Explicitly authorized work instructions and deadlines.",
                            color = AccentAmber
                        )
                        CategoryRow(
                            label = "C. Sensitive / Confidential",
                            desc = "Strictly local-only. Never sent to unpaid cloud AI under any circumstance.",
                            color = DangerRose
                        )
                    }
                }
            }

            // Cloud AI Consent Toggle (Part 2.3 & 3.1)
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Gemini Cloud AI (Free Tier)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (allowCloud) "Gemini 3.8 Flash enabled for non-confidential text" else "Disabled: Complete on-device deterministic processing only",
                                    fontSize = 12.sp,
                                    color = if (allowCloud) SuccessEmerald else Slate600
                                )
                            }
                            Switch(
                                checked = allowCloud,
                                onCheckedChange = { viewModel.setCloudAiConsent(it) },
                                modifier = Modifier.testTag("cloud_ai_consent_toggle")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Note: Free-tier Gemini AI is covered by Google's Developer API Terms. Confidential workplace documents are automatically blocked from transmission and processed locally on your phone.",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }
                }
            }

            // Notification Preview Protection (Part 3.4)
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Protect Notification Previews",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (protectPreviews) "Masks sensitive task text on device lockscreen" else "Shows full task title on notifications",
                                fontSize = 12.sp,
                                color = Slate600
                            )
                        }
                        Switch(
                            checked = protectPreviews,
                            onCheckedChange = { viewModel.setNotificationPreviewProtection(it) }
                        )
                    }
                }
            }

            // Export & Data Portability (Part 3.4 & 14)
            item {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            val json = PrivacyManager.exportDataAsJson(context)
                            clipboardManager.setText(AnnotatedString(json))
                            Toast.makeText(context, "Full local database JSON copied to clipboard!", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export All Local Data (JSON)")
                }
            }

            // Reset Learning Rules
            item {
                OutlinedButton(
                    onClick = {
                        viewModel.clearAllLearning()
                        Toast.makeText(context, "Learned preferences & rules reset to defaults.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset Learned Secretary Rules")
                }
            }

            // Audit Logs Section (Part 3.4)
            if (auditLogs.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Processing Audit Logs (${auditLogs.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                items(auditLogs.take(5)) { log ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = log.action, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryBlue)
                                Text(text = log.status, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = if (log.status == "SUCCESS") SuccessEmerald else AccentAmber)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = log.details, fontSize = 11.sp, color = Slate600)
                        }
                    }
                }
            }

            // Permanent Local Wipe (Part 3.4)
            item {
                Button(
                    onClick = {
                        viewModel.wipeAllData(context)
                        Toast.makeText(context, "All local application data wiped permanently.", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRose),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete All Local Application Data")
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(label: String, desc: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = color)
            Text(text = desc, fontSize = 11.sp, color = Slate600)
        }
    }
}
