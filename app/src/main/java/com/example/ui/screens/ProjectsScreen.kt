package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.SubtaskEntity
import com.example.data.local.entity.TaskEntity
import com.example.ui.SecretaryViewModel
import com.example.ui.components.PriorityBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.TaskCard
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate600
import com.example.ui.theme.SuccessEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: SecretaryViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val selectedTaskDetail by viewModel.selectedTaskForDetail.collectAsState()

    var selectedProjectId by remember { mutableStateOf<String?>(null) }
    var statusFilter by remember { mutableStateOf("ALL") }

    val filteredTasks = allTasks.filter { task ->
        val projectMatch = selectedProjectId == null || task.projectId == selectedProjectId
        val statusMatch = when (statusFilter) {
            "NOT_STARTED" -> task.status == "NOT_STARTED"
            "IN_PROGRESS" -> task.status == "IN_PROGRESS"
            "WAITING" -> task.status == "WAITING_FOR_RESPONSE"
            "COMPLETED" -> task.status == "COMPLETED"
            else -> true
        }
        projectMatch && statusMatch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Projects & Work",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Track deliverables, milestones and subtasks",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { viewModel.openCreateProject() },
                    modifier = Modifier.testTag("create_project_btn")
                ) {
                    Icon(imageVector = Icons.Filled.Folder, contentDescription = "New Project", tint = PrimaryBlue)
                }
                IconButton(
                    onClick = { viewModel.openCreateTask() },
                    modifier = Modifier.testTag("create_task_btn")
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "New Task", tint = PrimaryBlue)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Projects Horizontal List
        if (projects.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val isAllSelected = selectedProjectId == null
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAllSelected) PrimaryBlue else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .width(130.dp)
                            .height(84.dp)
                            .clickable { selectedProjectId = null }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "All Tasks",
                                fontWeight = FontWeight.Bold,
                                color = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${allTasks.size} tasks",
                                fontSize = 12.sp,
                                color = if (isAllSelected) Color.White.copy(alpha = 0.8f) else Slate600
                            )
                        }
                    }
                }

                items(projects, key = { it.id }) { proj ->
                    val projTasks = allTasks.filter { it.projectId == proj.id }
                    val completed = projTasks.count { it.status == "COMPLETED" }
                    val progress = if (projTasks.isNotEmpty()) completed.toFloat() / projTasks.size else 0f
                    val isSelected = selectedProjectId == proj.id

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .width(150.dp)
                            .height(84.dp)
                            .clickable { selectedProjectId = if (isSelected) null else proj.id }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = proj.title,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$completed/${projTasks.size} done",
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else Slate600
                                    )
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else PrimaryBlue
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = if (isSelected) Color.White else SuccessEmerald,
                                    trackColor = if (isSelected) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Status Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val filters = listOf(
                "ALL" to "All (${filteredTasks.size})",
                "NOT_STARTED" to "Not Started",
                "IN_PROGRESS" to "In Progress",
                "WAITING" to "Waiting",
                "COMPLETED" to "Completed"
            )
            items(filters) { (key, label) ->
                FilterChip(
                    selected = statusFilter == key,
                    onClick = { statusFilter = key },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tasks List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredTasks.isEmpty()) {
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
                            Text(
                                text = "No tasks found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the + button to create a task or import from Inbox.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate600
                            )
                        }
                    }
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onToggleComplete = { viewModel.toggleTaskComplete(task) },
                        onClick = { viewModel.selectTaskDetail(task) }
                    )
                }
            }
        }
    }

    // Task Detail Bottom Sheet (Subtasks & AI Breakdown)
    if (selectedTaskDetail != null) {
        val task = selectedTaskDetail!!
        val subtasks by viewModel.getSubtasks(task.id).collectAsState(initial = emptyList())
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { viewModel.selectTaskDetail(null) },
            sheetState = sheetState
        ) {
            TaskDetailSheetContent(
                task = task,
                subtasks = subtasks,
                onToggleSubtask = { viewModel.toggleSubtask(it) },
                onAddSubtask = { viewModel.addSubtask(task.id, it) },
                onBreakdown = { viewModel.breakdownTaskAutomatic(task.id, task.title, task.description) },
                onDeleteTask = {
                    viewModel.deleteTask(task.id)
                    viewModel.selectTaskDetail(null)
                },
                onClose = { viewModel.selectTaskDetail(null) }
            )
        }
    }
}

@Composable
fun TaskDetailSheetContent(
    task: TaskEntity,
    subtasks: List<SubtaskEntity>,
    onToggleSubtask: (SubtaskEntity) -> Unit,
    onAddSubtask: (String) -> Unit,
    onBreakdown: () -> Unit,
    onDeleteTask: () -> Unit,
    onClose: () -> Unit
) {
    var newSubtaskText by remember { mutableStateOf("") }
    var isBreakingDown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    PriorityBadge(task.priority)
                }

                IconButton(onClick = onDeleteTask) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete Task", tint = Slate600)
                }
            }
        }

        if (task.description.isNotBlank()) {
            item {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate600
                )
            }
        }

        // Subtasks & Automatic Breakdown Section (Part 8)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Actionable Steps (${subtasks.count { it.isCompleted }}/${subtasks.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedButton(
                    onClick = {
                        isBreakingDown = true
                        onBreakdown()
                        isBreakingDown = false
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("ai_breakdown_btn")
                ) {
                    Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto Breakdown", fontSize = 11.sp)
                }
            }
        }

        items(subtasks, key = { it.id }) { subtask ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleSubtask(subtask) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (subtask.isCompleted) Icons.Filled.Check else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (subtask.isCompleted) SuccessEmerald else Slate600,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = subtask.title,
                    fontSize = 14.sp,
                    textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (subtask.isCompleted) Slate600 else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Add step input
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newSubtaskText,
                    onValueChange = { newSubtaskText = it },
                    placeholder = { Text("Add next actionable step...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newSubtaskText.isNotBlank()) {
                            onAddSubtask(newSubtaskText.trim())
                            newSubtaskText = ""
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Add")
                }
            }
        }
    }
}
