package com.example.ai

import com.example.data.local.entity.LearningRuleEntity
import com.example.data.local.entity.TaskEntity

data class ExtractionResult(
    val taskTitle: String,
    val taskDescription: String,
    val deadlineMillis: Long?,
    val deadlineType: String, // SUBMISSION_DEADLINE, MEETING_DATE, APPOINTMENT, REMINDER, etc.
    val recipient: String?,
    val priority: String, // LOW, MEDIUM, HIGH, URGENT
    val evidenceText: String?,
    val confidence: Float,
    val suggestedSubtasks: List<String>,
    val detectedLanguage: String,
    val isProcessedByCloudAi: Boolean = false,
    val processingNotes: String? = null
)

interface SecretaryAiService {
    suspend fun extractTask(text: String, rules: List<LearningRuleEntity>): ExtractionResult
    suspend fun breakdownTask(taskTitle: String, description: String): List<String>
    suspend fun generateDailyBriefing(tasks: List<TaskEntity>, overdueCount: Int, isMorning: Boolean): String
    suspend fun answerAssistantQuery(query: String, contextTasks: List<TaskEntity>): String
}
