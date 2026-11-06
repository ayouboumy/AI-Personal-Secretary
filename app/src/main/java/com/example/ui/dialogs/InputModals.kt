package com.example.ui.dialogs

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ocr.DocumentProcessor
import com.example.ui.SecretaryViewModel
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate600
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CreateTaskDialog(
    viewModel: SecretaryViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var deadlineType by remember { mutableStateOf("SUBMISSION_DEADLINE") }
    var recipient by remember { mutableStateOf("") }
    var waitingFor by remember { mutableStateOf("") }
    var selectedProjectId by remember { mutableStateOf<String?>(null) }
    var selectedDeadlineMillis by remember { mutableStateOf<Long?>(null) }

    val formattedDeadline = selectedDeadlineMillis?.let {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
    } ?: "Select date"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Action Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                // Date Picker trigger
                OutlinedTextField(
                    value = formattedDeadline,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Deadline") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = "Pick Date",
                            modifier = Modifier.clickable {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        cal.set(year, month, dayOfMonth, 18, 0, 0)
                                        selectedDeadlineMillis = cal.timeInMillis
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Priority
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("LOW", "MEDIUM", "HIGH", "URGENT").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.take(3), fontSize = 11.sp) }
                        )
                    }
                }

                // Recipient / Waiting For
                OutlinedTextField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = { Text("Recipient / Administration") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = waitingFor,
                    onValueChange = { waitingFor = it },
                    label = { Text("Waiting for Whom/What (Optional)") },
                    placeholder = { Text("e.g. Architect stamp, bank guarantee") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        viewModel.createManualTask(
                            title = title.trim(),
                            description = description.trim(),
                            projectId = selectedProjectId,
                            priority = priority,
                            deadlineMillis = selectedDeadlineMillis,
                            deadlineType = deadlineType,
                            recipient = recipient.ifBlank { null },
                            waitingFor = waitingFor.ifBlank { null }
                        )
                        onDismiss()
                    }
                }
            ) {
                Text("Create Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateProjectDialog(
    viewModel: SecretaryViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Professional") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Project", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Project Title *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Professional", "Administrative", "Technical", "Personal").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        viewModel.createProject(title.trim(), description.trim(), "#3B82F6", category, null)
                        onDismiss()
                    }
                }
            ) {
                Text("Create Project")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddEventDialog(
    viewModel: SecretaryViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf("WORK_SESSION") }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Calendar Event", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("WORK_SESSION" to "Work Session", "MEETING" to "Meeting", "DEADLINE" to "Deadline").forEach { (type, label) ->
                        FilterChip(
                            selected = eventType == type,
                            onClick = { eventType = type },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        viewModel.createCalendarEvent(
                            title = title.trim(),
                            description = description.trim(),
                            startTimeMillis = selectedDateMillis,
                            endTimeMillis = selectedDateMillis + 3600000L,
                            eventType = eventType
                        )
                        onDismiss()
                    }
                }
            ) {
                Text("Add Event")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CaptureTextDialog(
    viewModel: SecretaryViewModel,
    onDismiss: () -> Unit
) {
    var rawText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paste Text or WhatsApp Message", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Paste text instructions in Arabic, French, or English. The secretary will extract deadlines and action items.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = { Text("e.g. Merci de préparer le rapport et de le soumettre avant le 15 décembre.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (rawText.isNotBlank()) {
                        val title = rawText.lines().firstOrNull { it.isNotBlank() }?.take(35) ?: "Captured Text"
                        viewModel.captureText(title, rawText.trim(), "TEXT")
                        onDismiss()
                    }
                }
            ) {
                Text("Capture & Analyze")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SampleDocumentsDialog(
    viewModel: SecretaryViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Built-in Test Documents", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Select any synthetic document to test multi-language OCR and deadline intelligence without uploading private files:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(DocumentProcessor.SAMPLE_DOCUMENTS) { doc ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.captureText(doc.title, doc.content, doc.category)
                                    onDismiss()
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = doc.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = doc.language.uppercase(), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = PrimaryBlue)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = doc.content, maxLines = 2, fontSize = 11.sp, color = Slate600)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
