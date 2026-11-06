package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CapturedItemEntity
import com.example.ocr.DocumentProcessor
import com.example.ui.SecretaryViewModel
import com.example.ui.components.DeadlineTypeBadge
import com.example.ui.components.PriorityBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.DangerRose
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate600
import com.example.ui.theme.SuccessEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    viewModel: SecretaryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val capturedItems by viewModel.capturedItems.collectAsState()
    val activeValidationItem by viewModel.activeValidationItem.collectAsState()
    val activeExtraction by viewModel.activeExtraction.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") }

    // Zero-permission modern Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.captureImage(context, uri, "PHOTO")
        }
    }

    val filteredItems = when (selectedFilter) {
        "NEW" -> capturedItems.filter { it.status == "NEW" }
        "NEEDS_REVIEW" -> capturedItems.filter { it.status == "NEEDS_REVIEW" }
        "CONFIRMED" -> capturedItems.filter { it.status == "CONFIRMED" }
        else -> capturedItems
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title & Capture actions header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Smart Inbox",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Capture, OCR, analyze & validate incoming work",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
            }

            IconButton(onClick = { viewModel.openSampleDocs() }) {
                Icon(
                    imageVector = Icons.Filled.Description,
                    contentDescription = "Sample Documents",
                    tint = PrimaryBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Capture Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.openTextCapture() },
                modifier = Modifier
                    .weight(1f)
                    .testTag("capture_text_btn"),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Icon(imageVector = Icons.Filled.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Paste Text", fontSize = 12.sp)
            }

            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("capture_image_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Icon(imageVector = Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import Photo", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { viewModel.openSampleDocs() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Icon(imageVector = Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Samples", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val filters = listOf("ALL" to "All (${capturedItems.size})", "NEW" to "New", "NEEDS_REVIEW" to "Needs Review", "CONFIRMED" to "Confirmed")
            items(filters) { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Captured items list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredItems.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Description,
                                contentDescription = null,
                                tint = Slate600,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Inbox is empty",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Share a WhatsApp message or tap 'Samples' to test multi-language extraction.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate600
                            )
                        }
                    }
                }
            } else {
                items(filteredItems, key = { it.id }) { item ->
                    CapturedItemCard(
                        item = item,
                        onClick = { viewModel.startAnalysis(item) }
                    )
                }
            }
        }
    }

    // Modal Validation & Calibration Bottom Sheet
    if (activeValidationItem != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.cancelValidation() },
            sheetState = sheetState
        ) {
            ValidationSheetContent(
                item = activeValidationItem!!,
                extraction = activeExtraction,
                isAnalyzing = isAnalyzing,
                onConfirm = { title, desc, deadline, type, priority, recipient, projId, subtasks, note ->
                    viewModel.confirmExtractedTask(title, desc, deadline, type, priority, recipient, projId, subtasks, note)
                },
                onCancel = { viewModel.cancelValidation() }
            )
        }
    }
}

@Composable
fun CapturedItemCard(
    item: CapturedItemEntity,
    onClick: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(item.createdAtMillis))
    val statusColor = when (item.status) {
        "CONFIRMED" -> SuccessEmerald
        "NEEDS_REVIEW" -> AccentAmber
        "ANALYZING" -> PrimaryBlue
        else -> Slate600
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("inbox_item_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (item.sourceChannel) {
                        "PHOTO", "SCREENSHOT" -> Icons.Filled.Image
                        "PDF" -> Icons.Filled.PictureAsPdf
                        else -> Icons.Filled.ChatBubbleOutline
                    }
                    Icon(imageVector = icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.sourceChannel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.status.replace("_", " "),
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.rawText,
                style = MaterialTheme.typography.bodySmall,
                color = Slate600,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dateStr,
                fontSize = 11.sp,
                color = Slate600
            )
        }
    }
}

@Composable
fun ValidationSheetContent(
    item: CapturedItemEntity,
    extraction: com.example.ai.ExtractionResult?,
    isAnalyzing: Boolean,
    onConfirm: (
        title: String,
        description: String,
        deadlineMillis: Long?,
        deadlineType: String,
        priority: String,
        recipient: String?,
        projectId: String?,
        subtasks: List<String>,
        correctionNote: String?
    ) -> Unit,
    onCancel: () -> Unit
) {
    if (isAnalyzing || extraction == null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = PrimaryBlue)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Understanding document & extracting deadlines...",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Applying language intelligence and personal rules",
                style = MaterialTheme.typography.bodySmall,
                color = Slate600
            )
        }
        return
    }

    var editTitle by remember { mutableStateOf(extraction.taskTitle) }
    var editDesc by remember { mutableStateOf(extraction.taskDescription) }
    var editDeadlineType by remember { mutableStateOf(extraction.deadlineType) }
    var editPriority by remember { mutableStateOf(extraction.priority) }
    var editRecipient by remember { mutableStateOf(extraction.recipient ?: "") }
    var userCorrectionExplanation by remember { mutableStateOf("") }

    val formattedDeadline = extraction.deadlineMillis?.let {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
    } ?: "No explicit deadline"

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Validate Extracted Task",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = extraction.processingNotes ?: "Inspect and correct before saving to calendar and reminders.",
                style = MaterialTheme.typography.bodySmall,
                color = PrimaryBlue
            )
        }

        // Source snippet
        item {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Original Source Text", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate600)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = item.rawText, style = MaterialTheme.typography.bodySmall)
                    if (!extraction.evidenceText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Evidence: \"${extraction.evidenceText}\"",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentAmber
                        )
                    }
                }
            }
        }

        // Editable Title
        item {
            OutlinedTextField(
                value = editTitle,
                onValueChange = { editTitle = it },
                label = { Text("Task Title") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Editable Description
        item {
            OutlinedTextField(
                value = editDesc,
                onValueChange = { editDesc = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
        }

        // Deadline & Type
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = formattedDeadline,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Deadline") },
                    modifier = Modifier.weight(1f)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text("Type", fontSize = 12.sp, color = Slate600)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = editDeadlineType == "SUBMISSION_DEADLINE",
                            onClick = { editDeadlineType = "SUBMISSION_DEADLINE" },
                            label = { Text("Submission", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = editDeadlineType == "MEETING_DATE",
                            onClick = { editDeadlineType = "MEETING_DATE" },
                            label = { Text("Meeting", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Priority & Recipient
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Priority", fontSize = 12.sp, color = Slate600)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("LOW", "MEDIUM", "HIGH", "URGENT").forEach { p ->
                            FilterChip(
                                selected = editPriority == p,
                                onClick = { editPriority = p },
                                label = { Text(p.take(3), fontSize = 10.sp) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = editRecipient,
                    onValueChange = { editRecipient = it },
                    label = { Text("Recipient / Org") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Subtasks preview
        if (extraction.suggestedSubtasks.isNotEmpty()) {
            item {
                Text("Suggested Subtasks (${extraction.suggestedSubtasks.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    extraction.suggestedSubtasks.forEach { st ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = SuccessEmerald, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = st, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Continuous Learning Note (Part 4)
        item {
            OutlinedTextField(
                value = userCorrectionExplanation,
                onValueChange = { userCorrectionExplanation = it },
                label = { Text("Explain Correction (Saved to Personal Learning Rules)") },
                placeholder = { Text("e.g. In French administrative letters, distinguish meeting from final deposit.") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        onConfirm(
                            editTitle,
                            editDesc,
                            extraction.deadlineMillis,
                            editDeadlineType,
                            editPriority,
                            editRecipient.ifBlank { null },
                            null,
                            extraction.suggestedSubtasks,
                            userCorrectionExplanation.ifBlank { null }
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("confirm_validated_task_btn")
                ) {
                    Text("Confirm Task")
                }
            }
        }
    }
}
