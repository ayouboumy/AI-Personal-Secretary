package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.LearningRuleEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.ocr.DocumentProcessor
import com.example.ocr.SampleDocument
import com.example.ui.SecretaryViewModel
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate600
import com.example.ui.theme.SuccessEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningLaboratoryDialog(
    viewModel: SecretaryViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val userProfile by viewModel.userProfile.collectAsState()
    val learningRules by viewModel.learningRules.collectAsState()

    var currentStep by remember { mutableIntStateOf(0) } // 0=Welcome, 1=Habits, 2=Samples, 3=Report & Rules
    var selectedUsage by remember { mutableStateOf(userProfile?.usageType ?: "mixed") }
    var selectedLanguage by remember { mutableStateOf(userProfile?.primaryLanguage ?: "en") }
    var selectedReminderDays by remember { mutableIntStateOf(userProfile?.advanceReminderDays ?: 3) }
    var selectedSample by remember { mutableStateOf<SampleDocument?>(null) }
    var correctionText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
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
                            .background(AccentAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Learning Laboratory",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Calibrate how your secretary interprets instructions",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step Progress Indicator
            LinearProgressIndicator(
                progress = { (currentStep + 1) / 4f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AccentAmber
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Step Content
            when (currentStep) {
                0 -> {
                    // STEP 1: WELCOME & USAGE
                    Text(text = "Step 1: Welcome & Work Context", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tell your assistant about your primary context so it can adapt priority rules and language models.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Primary Usage Domain", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("professional" to "Professional", "business" to "Business", "academic" to "Academic", "mixed" to "Mixed").forEach { (k, label) ->
                            FilterChip(
                                selected = selectedUsage == k,
                                onClick = { selectedUsage = k },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Primary Language", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("en" to "English", "fr" to "Français", "ar" to "العربية").forEach { (langCode, langName) ->
                            FilterChip(
                                selected = selectedLanguage == langCode,
                                onClick = { selectedLanguage = langCode },
                                label = { Text(langName, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { currentStep = 1 },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Next: Working Habits")
                    }
                }

                1 -> {
                    // STEP 2: WORKING HABITS
                    Text(text = "Step 2: Working Habits & Reminders", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Customize default advance notice before deadlines and daily briefing hours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Preferred Deadline Advance Notice", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1 to "1 Day", 2 to "2 Days", 3 to "3 Days", 7 to "7 Days").forEach { (days, label) ->
                            FilterChip(
                                selected = selectedReminderDays == days,
                                onClick = { selectedReminderDays = days },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(onClick = { currentStep = 0 }, modifier = Modifier.weight(1f)) {
                            Text("Back")
                        }
                        Button(onClick = { currentStep = 2 }, modifier = Modifier.weight(1f)) {
                            Text("Next: Test Samples")
                        }
                    }
                }

                2 -> {
                    // STEP 3: SAMPLE COLLECTION & CALIBRATION
                    Text(text = "Step 3: Sample Calibration", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Select a synthetic demonstration document to test deadline extraction and save personal interpretation rules.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(DocumentProcessor.SAMPLE_DOCUMENTS) { sample ->
                            val isSel = selectedSample?.id == sample.id
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) PrimaryBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSample = sample }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = sample.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = sample.language.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = sample.content, maxLines = 2, fontSize = 11.sp, color = Slate600)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (selectedSample != null) {
                        OutlinedTextField(
                            value = correctionText,
                            onValueChange = { correctionText = it },
                            label = { Text("Interpretation Rule to Learn") },
                            placeholder = { Text("e.g. In administrative letters, distinguish meeting date from deposit deadline.") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(onClick = { currentStep = 1 }, modifier = Modifier.weight(1f)) {
                            Text("Back")
                        }
                        Button(
                            onClick = {
                                if (selectedSample != null) {
                                    viewModel.captureText(
                                        title = selectedSample!!.title,
                                        text = selectedSample!!.content,
                                        sourceChannel = selectedSample!!.category
                                    )
                                }
                                currentStep = 3
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Finish Calibration")
                        }
                    }
                }

                3 -> {
                    // STEP 4: PERSONALIZATION REPORT & ACTIVE RULES
                    Text(text = "Step 4: Personalization Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Here are the personal interpretation rules your secretary currently uses to avoid confusing deadlines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate600
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (learningRules.isEmpty()) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No custom rules yet", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Every time you correct a deadline in the Inbox, a rule is saved here.", fontSize = 11.sp, color = Slate600)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(learningRules, key = { it.id }) { rule ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = rule.ruleTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(text = "Trigger: \"${rule.patternTrigger}\"", fontSize = 11.sp, color = AccentAmber)
                                            Text(text = rule.ruleDescription, fontSize = 11.sp, color = Slate600)
                                        }

                                        IconButton(onClick = { viewModel.deleteLearningRule(rule) }) {
                                            Icon(imageVector = Icons.Filled.Delete, contentDescription = null, tint = Slate600, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Apply & Start Working")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
