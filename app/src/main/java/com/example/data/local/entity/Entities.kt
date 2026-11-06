package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val userName: String = "User",
    val usageType: String = "mixed", // professional, personal, academic, business, mixed
    val primaryLanguage: String = "en", // ar, fr, en
    val workDays: String = "Mon,Tue,Wed,Thu,Fri",
    val workStartTime: String = "09:00",
    val workEndTime: String = "18:00",
    val timeZone: String = "UTC",
    val morningBriefingTime: String = "08:30",
    val eveningReviewTime: String = "18:00",
    val advanceReminderDays: Int = 3,
    val isOnboardingCompleted: Boolean = false,
    val allowCloudAiConsent: Boolean = false,
    val protectNotificationPreviews: Boolean = true
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val colorHex: String = "#3B82F6",
    val category: String = "General",
    val deadlineMillis: Long? = null,
    val isArchived: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tasks",
    indices = [Index("projectId"), Index("status"), Index("deadlineMillis")]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val projectId: String? = null,
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH, URGENT
    val status: String = "NOT_STARTED", // NOT_STARTED, IN_PROGRESS, WAITING_FOR_RESPONSE, COMPLETED, CANCELLED
    val deadlineMillis: Long? = null,
    val deadlineType: String = "SUBMISSION_DEADLINE", // SUBMISSION_DEADLINE, MEETING_DATE, APPOINTMENT, REMINDER, PROJECT_START, PROJECT_COMPLETION
    val plannedStartMillis: Long? = null,
    val plannedCompletionMillis: Long? = null,
    val estimatedDurationMinutes: Int = 60,
    val assignedPerson: String? = null,
    val recipient: String? = null,
    val waitingForWhomOrWhat: String? = null,
    val sourceCapturedItemId: String? = null,
    val evidenceText: String? = null,
    val confidence: Float = 1.0f,
    val requiresConfirmation: Boolean = false,
    val isConfirmed: Boolean = true,
    val reminderDaysBefore: Int = 3,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "subtasks",
    indices = [Index("taskId")],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SubtaskEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0
)

@Entity(
    tableName = "calendar_events",
    indices = [Index("startTimeMillis"), Index("taskId")]
)
data class CalendarEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val taskId: String? = null,
    val projectId: String? = null,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val eventType: String = "DEADLINE" // DEADLINE, WORK_SESSION, MEETING, REMINDER, MILESTONE
)

@Entity(
    tableName = "captured_items",
    indices = [Index("status"), Index("createdAtMillis")]
)
data class CapturedItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceChannel: String = "TEXT", // WHATSAPP, EMAIL, PDF, PHOTO, SCREENSHOT, TEXT, NATIVE_SHARE
    val rawText: String,
    val imageUri: String? = null,
    val status: String = "NEW", // NEW, ANALYZING, NEEDS_REVIEW, CONFIRMED, ORGANIZED, ARCHIVED, FAILED
    val mimeType: String = "text/plain",
    val detectedDocumentType: String? = null,
    val failureReason: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "learning_rules")
data class LearningRuleEntity(
    @PrimaryKey val id: String,
    val ruleTitle: String,
    val ruleDescription: String,
    val patternTrigger: String,
    val interpretationAction: String,
    val exampleBefore: String? = null,
    val exampleAfter: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Entity(tableName = "correction_records")
data class CorrectionRecordEntity(
    @PrimaryKey val id: String,
    val capturedItemId: String? = null,
    val originalValue: String,
    val correctedValue: String,
    val fieldCorrected: String,
    val explanation: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sender: String, // USER, ASSISTANT
    val message: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val actionType: String? = null,
    val actionPayload: String? = null
)

@Entity(tableName = "ai_processing_logs")
data class AIProcessingLogEntity(
    @PrimaryKey val id: String,
    val action: String,
    val documentTitle: String,
    val status: String, // SUCCESS, FAILED, OFFLINE_RULE_BASED, BLOCKED_NO_CONSENT
    val timestampMillis: Long = System.currentTimeMillis(),
    val details: String
)
