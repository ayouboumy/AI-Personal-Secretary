package com.example.data.repository

import android.content.Context
import com.example.ai.ExtractionResult
import com.example.ai.SecretaryAiRepository
import com.example.data.local.SecretaryDatabase
import com.example.data.local.entity.CalendarEventEntity
import com.example.data.local.entity.CapturedItemEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.CorrectionRecordEntity
import com.example.data.local.entity.LearningRuleEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.SubtaskEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.notifications.SecretaryNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID

class SecretaryRepository(private val context: Context) {

    private val db = SecretaryDatabase.getDatabase(context)
    val aiRepo = SecretaryAiRepository(context)

    // User Profile
    val userProfile: Flow<UserProfileEntity?> = db.userDao().getUserProfile()

    suspend fun saveUserProfile(profile: UserProfileEntity) = withContext(Dispatchers.IO) {
        db.userDao().insertOrUpdate(profile)
    }

    // Projects
    val projects: Flow<List<ProjectEntity>> = db.projectDao().getAllProjects()

    suspend fun createProject(title: String, description: String, colorHex: String, category: String, deadlineMillis: Long?) = withContext(Dispatchers.IO) {
        val project = ProjectEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            colorHex = colorHex,
            category = category,
            deadlineMillis = deadlineMillis
        )
        db.projectDao().insert(project)
        project
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        db.projectDao().deleteById(projectId)
    }

    // Tasks & Subtasks
    val allTasks: Flow<List<TaskEntity>> = db.taskDao().getAllTasks()

    fun getSubtasks(taskId: String): Flow<List<SubtaskEntity>> = db.taskDao().getSubtasksForTask(taskId)

    suspend fun createTask(
        title: String,
        description: String,
        projectId: String?,
        priority: String,
        deadlineMillis: Long?,
        deadlineType: String,
        recipient: String?,
        assignedPerson: String?,
        waitingFor: String?,
        sourceCapturedId: String?,
        evidence: String?,
        suggestedSubtasks: List<String> = emptyList()
    ): TaskEntity = withContext(Dispatchers.IO) {
        val task = TaskEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            projectId = projectId,
            priority = priority,
            deadlineMillis = deadlineMillis,
            deadlineType = deadlineType,
            recipient = recipient,
            assignedPerson = assignedPerson,
            waitingForWhomOrWhat = waitingFor,
            sourceCapturedItemId = sourceCapturedId,
            evidenceText = evidence,
            status = if (!waitingFor.isNullOrBlank()) "WAITING_FOR_RESPONSE" else "NOT_STARTED",
            createdAtMillis = System.currentTimeMillis()
        )
        db.taskDao().insert(task)

        // Insert subtasks if any
        if (suggestedSubtasks.isNotEmpty()) {
            val subEntities = suggestedSubtasks.mapIndexed { idx, st ->
                SubtaskEntity(
                    id = UUID.randomUUID().toString(),
                    taskId = task.id,
                    title = st,
                    isCompleted = false,
                    orderIndex = idx
                )
            }
            db.taskDao().insertSubtasks(subEntities)
        }

        // Schedule notification reminder if deadline exists
        if (task.deadlineMillis != null) {
            SecretaryNotificationManager.scheduleTaskReminder(context, task)

            // Also create Calendar Event for the deadline
            db.calendarDao().insert(
                CalendarEventEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Deadline: ${task.title}",
                    description = task.description,
                    taskId = task.id,
                    projectId = task.projectId,
                    startTimeMillis = task.deadlineMillis,
                    endTimeMillis = task.deadlineMillis + 3600000L,
                    eventType = task.deadlineType
                )
            )
        }

        task
    }

    suspend fun updateTaskStatus(task: TaskEntity, newStatus: String) = withContext(Dispatchers.IO) {
        val updated = task.copy(status = newStatus, updatedAtMillis = System.currentTimeMillis())
        db.taskDao().update(updated)
        if (newStatus == "COMPLETED") {
            SecretaryNotificationManager.cancelTaskReminder(context, task.id)
        }
    }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        db.taskDao().update(task)
        if (task.deadlineMillis != null && task.status != "COMPLETED") {
            SecretaryNotificationManager.scheduleTaskReminder(context, task)
        }
    }

    suspend fun deleteTask(taskId: String) = withContext(Dispatchers.IO) {
        SecretaryNotificationManager.cancelTaskReminder(context, taskId)
        db.calendarDao().deleteByTaskId(taskId)
        db.taskDao().deleteById(taskId)
    }

    suspend fun toggleSubtask(subtask: SubtaskEntity) = withContext(Dispatchers.IO) {
        db.taskDao().updateSubtask(subtask.copy(isCompleted = !subtask.isCompleted))
    }

    suspend fun addSubtask(taskId: String, title: String) = withContext(Dispatchers.IO) {
        val count = db.taskDao().getSubtasksForTaskSync(taskId).size
        db.taskDao().insertSubtask(
            SubtaskEntity(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                title = title,
                isCompleted = false,
                orderIndex = count
            )
        )
    }

    // Calendar
    val allCalendarEvents: Flow<List<CalendarEventEntity>> = db.calendarDao().getAllEvents()

    suspend fun createCalendarEvent(
        title: String,
        description: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        eventType: String,
        taskId: String? = null,
        projectId: String? = null
    ) = withContext(Dispatchers.IO) {
        val event = CalendarEventEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            startTimeMillis = startTimeMillis,
            endTimeMillis = endTimeMillis,
            eventType = eventType,
            taskId = taskId,
            projectId = projectId
        )
        db.calendarDao().insert(event)
    }

    suspend fun deleteCalendarEvent(id: String) = withContext(Dispatchers.IO) {
        db.calendarDao().deleteById(id)
    }

    // Captured Items
    val capturedItems: Flow<List<CapturedItemEntity>> = db.capturedItemDao().getAllItems()

    suspend fun captureItem(
        title: String,
        rawText: String,
        sourceChannel: String,
        imageUri: String? = null
    ): CapturedItemEntity = withContext(Dispatchers.IO) {
        // Check for duplicates
        val existing = db.capturedItemDao().findExistingByText(rawText.trim())
        if (existing != null) {
            return@withContext existing
        }

        val item = CapturedItemEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            sourceChannel = sourceChannel,
            rawText = rawText,
            imageUri = imageUri,
            status = "NEW"
        )
        db.capturedItemDao().insert(item)
        item
    }

    suspend fun analyzeCapturedItem(item: CapturedItemEntity): ExtractionResult = withContext(Dispatchers.IO) {
        db.capturedItemDao().update(item.copy(status = "ANALYZING"))
        val activeRules = db.learningDao().getActiveRulesSync()
        val result = aiRepo.extractTaskFromContent(item.rawText, activeRules)
        db.capturedItemDao().update(item.copy(status = "NEEDS_REVIEW"))
        result
    }

    suspend fun confirmCapturedItem(
        item: CapturedItemEntity,
        validatedTask: TaskEntity,
        subtasks: List<String>,
        userCorrectionNote: String? = null
    ) = withContext(Dispatchers.IO) {
        createTask(
            title = validatedTask.title,
            description = validatedTask.description,
            projectId = validatedTask.projectId,
            priority = validatedTask.priority,
            deadlineMillis = validatedTask.deadlineMillis,
            deadlineType = validatedTask.deadlineType,
            recipient = validatedTask.recipient,
            assignedPerson = validatedTask.assignedPerson,
            waitingFor = validatedTask.waitingForWhomOrWhat,
            sourceCapturedId = item.id,
            evidence = validatedTask.evidenceText,
            suggestedSubtasks = subtasks
        )

        // Store user correction if present
        if (!userCorrectionNote.isNullOrBlank()) {
            db.learningDao().insertCorrection(
                CorrectionRecordEntity(
                    id = UUID.randomUUID().toString(),
                    capturedItemId = item.id,
                    originalValue = item.rawText.take(50),
                    correctedValue = validatedTask.title,
                    fieldCorrected = "User Custom Calibration",
                    explanation = userCorrectionNote
                )
            )

            // Extract pattern trigger and create personal rule
            val patternSnippet = if (item.rawText.contains("réunion", ignoreCase = true) || item.rawText.contains("اجتماع")) {
                "réunion / اجتماع"
            } else if (item.rawText.contains("rapport", ignoreCase = true) || item.rawText.contains("تقرير")) {
                "rapport / تقرير"
            } else validatedTask.title.take(20)

            db.learningDao().insertRule(
                LearningRuleEntity(
                    id = UUID.randomUUID().toString(),
                    ruleTitle = "Learned preference: ${validatedTask.deadlineType}",
                    ruleDescription = userCorrectionNote,
                    patternTrigger = patternSnippet,
                    interpretationAction = "Interpret as ${validatedTask.deadlineType}"
                )
            )
        }

        db.capturedItemDao().update(item.copy(status = "CONFIRMED"))
    }

    suspend fun archiveCapturedItem(item: CapturedItemEntity) = withContext(Dispatchers.IO) {
        db.capturedItemDao().update(item.copy(status = "ARCHIVED"))
    }

    suspend fun deleteCapturedItem(id: String) = withContext(Dispatchers.IO) {
        db.capturedItemDao().deleteById(id)
    }

    // Learning Rules & Corrections
    val learningRules: Flow<List<LearningRuleEntity>> = db.learningDao().getAllRules()
    val correctionRecords: Flow<List<CorrectionRecordEntity>> = db.learningDao().getAllCorrections()

    suspend fun addLearningRule(rule: LearningRuleEntity) = withContext(Dispatchers.IO) {
        db.learningDao().insertRule(rule)
    }

    suspend fun toggleLearningRule(rule: LearningRuleEntity) = withContext(Dispatchers.IO) {
        db.learningDao().updateRule(rule.copy(isActive = !rule.isActive))
    }

    suspend fun deleteLearningRule(rule: LearningRuleEntity) = withContext(Dispatchers.IO) {
        db.learningDao().deleteRule(rule)
    }

    suspend fun clearLearningRules() = withContext(Dispatchers.IO) {
        db.learningDao().clearAllRules()
        db.learningDao().clearAllCorrections()
    }

    // Assistant & Logs
    val chatMessages: Flow<List<ChatMessageEntity>> = db.assistantDao().getAllMessages()
    val auditLogs = db.assistantDao().getAllLogs()

    suspend fun sendChatMessage(userText: String): String = withContext(Dispatchers.IO) {
        val userMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            sender = "USER",
            message = userText
        )
        db.assistantDao().insertMessage(userMsg)

        val tasks = db.taskDao().getAllTasks().firstOrNull() ?: emptyList()
        val replyText = aiRepo.answerAssistantQuery(userText, tasks)

        val assistantMsg = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            sender = "ASSISTANT",
            message = replyText
        )
        db.assistantDao().insertMessage(assistantMsg)
        replyText
    }

    suspend fun clearChatMessages() = withContext(Dispatchers.IO) {
        db.assistantDao().clearAllMessages()
    }
}
