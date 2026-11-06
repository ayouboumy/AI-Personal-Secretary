package com.example.ai

import android.content.Context
import android.util.Log
import com.example.data.local.SecretaryDatabase
import com.example.data.local.entity.AIProcessingLogEntity
import com.example.data.local.entity.LearningRuleEntity
import com.example.data.local.entity.TaskEntity
import com.example.security.DataCategory
import com.example.security.PrivacyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

private const val TAG = "SecretaryAiRepo"

class SecretaryAiRepository(private val context: Context) {

    private val offlineService = OfflineRuleBasedSecretaryService()
    private val cloudService = FirebaseAiLogicSecretaryService()
    private val db = SecretaryDatabase.getDatabase(context)

    suspend fun extractTaskFromContent(
        text: String,
        rules: List<LearningRuleEntity>
    ): ExtractionResult = withContext(Dispatchers.IO) {
        val sensitiveCheck = PrivacyManager.evaluateContent(text)
        val profile = db.userDao().getUserProfileSync()
        val hasCloudConsent = profile?.allowCloudAiConsent == true

        if (sensitiveCheck.category == DataCategory.SENSITIVE_CONFIDENTIAL) {
            // Mandated rule: Confidential/sensitive information stays local only!
            logAudit("LOCAL_EXTRACTION", "Confidential text detected - enforced on-device processing", "OFFLINE_RULE_BASED")
            val localResult = offlineService.extractTask(text, rules)
            return@withContext localResult.copy(
                processingNotes = sensitiveCheck.warningMessage ?: "Processed locally for security."
            )
        }

        if (!hasCloudConsent) {
            logAudit("LOCAL_EXTRACTION", "Cloud AI consent not granted - executed on-device", "OFFLINE_RULE_BASED")
            return@withContext offlineService.extractTask(text, rules)
        }

        // User granted cloud consent and text is non-confidential: Attempt Gemini 3.8 Flash via Firebase AI Logic
        try {
            val result = cloudService.extractTask(text, rules)
            logAudit("GEMINI_EXTRACTION", "Successfully extracted task via Gemini 3.8 Flash", "SUCCESS")
            result
        } catch (e: Exception) {
            Log.w(TAG, "Cloud AI unavailable or timed out, executing offline deterministic fallback", e)
            logAudit("FALLBACK_EXTRACTION", "Cloud AI unavailable (${e.localizedMessage}) - used offline fallback", "OFFLINE_RULE_BASED")
            val fallback = offlineService.extractTask(text, rules)
            fallback.copy(processingNotes = "Cloud AI unreachable. Extracted with high-precision offline engine.")
        }
    }

    suspend fun breakdownTask(taskTitle: String, description: String): List<String> = withContext(Dispatchers.IO) {
        val profile = db.userDao().getUserProfileSync()
        if (profile?.allowCloudAiConsent == true) {
            try {
                return@withContext cloudService.breakdownTask(taskTitle, description)
            } catch (e: Exception) {
                Log.w(TAG, "Cloud breakdown failed, using offline breakdown", e)
            }
        }
        offlineService.breakdownTask(taskTitle, description)
    }

    suspend fun generateBriefing(tasks: List<TaskEntity>, overdueCount: Int, isMorning: Boolean): String = withContext(Dispatchers.IO) {
        val profile = db.userDao().getUserProfileSync()
        if (profile?.allowCloudAiConsent == true && tasks.isNotEmpty()) {
            try {
                return@withContext cloudService.generateDailyBriefing(tasks, overdueCount, isMorning)
            } catch (e: Exception) {
                Log.w(TAG, "Cloud briefing failed, using offline briefing", e)
            }
        }
        offlineService.generateDailyBriefing(tasks, overdueCount, isMorning)
    }

    suspend fun answerAssistantQuery(query: String, contextTasks: List<TaskEntity>): String = withContext(Dispatchers.IO) {
        val profile = db.userDao().getUserProfileSync()
        if (profile?.allowCloudAiConsent == true) {
            try {
                return@withContext cloudService.answerAssistantQuery(query, contextTasks)
            } catch (e: Exception) {
                Log.w(TAG, "Cloud assistant query failed, using offline answer", e)
            }
        }
        offlineService.answerAssistantQuery(query, contextTasks)
    }

    private suspend fun logAudit(action: String, details: String, status: String) {
        try {
            db.assistantDao().insertLog(
                AIProcessingLogEntity(
                    id = UUID.randomUUID().toString(),
                    action = action,
                    documentTitle = "User Document",
                    status = status,
                    details = details
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not write audit log", e)
        }
    }
}
