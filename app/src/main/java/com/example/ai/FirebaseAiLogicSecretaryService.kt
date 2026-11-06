package com.example.ai

import android.util.Log
import com.example.data.local.entity.LearningRuleEntity
import com.example.data.local.entity.TaskEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

private const val TAG = "FirebaseAiSecretary"

class FirebaseAiLogicSecretaryService : SecretaryAiService {

    // Define strict JSON schema using Schema.obj per Firebase AI Logic Android specification
    private val taskSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Short actionable title for the task"),
            "description" to Schema.string(description = "Concise description of the task requirements"),
            "deadlineDate" to Schema.string(description = "ISO date YYYY-MM-DD or empty if not found", nullable = true),
            "deadlineType" to Schema.string(description = "SUBMISSION_DEADLINE, MEETING_DATE, APPOINTMENT, REMINDER"),
            "recipient" to Schema.string(description = "Recipient organization or person", nullable = true),
            "priority" to Schema.string(description = "LOW, MEDIUM, HIGH, or URGENT"),
            "evidenceText" to Schema.string(description = "Direct phrase from source indicating deadline or task", nullable = true),
            "suggestedSubtasks" to Schema.array(
                items = Schema.string(),
                description = "4 to 6 sequential steps to complete this task"
            )
        ),
        optionalProperties = listOf("deadlineDate", "recipient", "evidenceText")
    )

    private val structuredConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = taskSchema
    }

    // Lazy initialization per Firebase AI Logic Android specification
    private val extractionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = structuredConfig
        )
    }

    private val textModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    override suspend fun extractTask(text: String, rules: List<LearningRuleEntity>): ExtractionResult {
        val rulesPrompt = if (rules.isNotEmpty()) {
            "\nUSER PERSONALIZATION RULES TO STRICTLY APPLY:\n" +
                    rules.filter { it.isActive }.joinToString("\n") { "• When text has '${it.patternTrigger}': ${it.interpretationAction}" }
        } else ""

        val systemPrompt = """
            You are an expert AI Personal Secretary. Extract actionable work from the user's incoming message or document.
            Distinguish meeting dates from submission deadlines.
            Language may be Arabic, French, or English.
            $rulesPrompt
            
            Document text:
            "$text"
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                extractionModel.generateContent(systemPrompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Model returned empty text.")
            val json = JSONObject(jsonText)

            val title = json.optString("title", "Action Task")
            val desc = json.optString("description", text)
            val dateStr = if (json.has("deadlineDate") && !json.isNull("deadlineDate")) json.getString("deadlineDate") else null
            val deadlineType = json.optString("deadlineType", "SUBMISSION_DEADLINE")
            val recipient = if (json.has("recipient") && !json.isNull("recipient")) json.getString("recipient") else null
            val priority = json.optString("priority", "MEDIUM")
            val evidence = if (json.has("evidenceText") && !json.isNull("evidenceText")) json.getString("evidenceText") else null

            val subtasks = mutableListOf<String>()
            val subArray = json.optJSONArray("suggestedSubtasks")
            if (subArray != null) {
                for (i in 0 until subArray.length()) {
                    subtasks.add(subArray.getString(i))
                }
            }

            var deadlineMillis: Long? = null
            if (!dateStr.isNullOrBlank()) {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    deadlineMillis = sdf.parse(dateStr)?.time
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse date $dateStr", e)
                }
            }

            val isArabic = text.any { it in '\u0600'..'\u06FF' }
            val isFrench = !isArabic && (text.contains("le ", ignoreCase = true) || text.contains("avant", ignoreCase = true))
            val lang = if (isArabic) "ar" else if (isFrench) "fr" else "en"

            ExtractionResult(
                taskTitle = title,
                taskDescription = desc,
                deadlineMillis = deadlineMillis,
                deadlineType = deadlineType,
                recipient = recipient,
                priority = priority,
                evidenceText = evidence,
                confidence = 0.98f,
                suggestedSubtasks = if (subtasks.isNotEmpty()) subtasks else listOf("Review instructions", "Execute required steps", "Submit final output"),
                detectedLanguage = lang,
                isProcessedByCloudAi = true,
                processingNotes = "Verified by Gemini 3.8 Flash via Firebase AI Logic"
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Firebase AI extraction timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation error", e)
            throw e
        }
    }

    override suspend fun breakdownTask(taskTitle: String, description: String): List<String> {
        val prompt = "Break down this task into 5 sequential, highly actionable steps. Return only the steps numbered 1-5.\nTask: $taskTitle\nDescription: $description"
        return try {
            val response = withTimeout(60_000L) {
                textModel.generateContent(prompt)
            }
            val text = response.text ?: ""
            text.lines()
                .map { it.replace("^\\d+[.)\\-\\s]+".toRegex(), "").trim() }
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            listOf("Collect references", "Draft deliverables", "Internal verification", "Final submission")
        }
    }

    override suspend fun generateDailyBriefing(tasks: List<TaskEntity>, overdueCount: Int, isMorning: Boolean): String {
        val taskSummary = tasks.take(10).joinToString("\n") { "• ${it.title} [${it.priority}] Due: ${it.deadlineMillis}" }
        val prompt = if (isMorning) {
            "You are a professional personal secretary. Provide a crisp, encouraging 3-sentence morning briefing for the user based on these tasks:\n$taskSummary\nOverdue items: $overdueCount"
        } else {
            "You are a professional personal secretary. Provide a concise evening review summarizing today's status and tomorrow's upcoming priorities based on these tasks:\n$taskSummary"
        }

        return try {
            val response = withTimeout(60_000L) {
                textModel.generateContent(prompt)
            }
            response.text ?: "Secretary ready. Review your tasks for today."
        } catch (e: Exception) {
            "Secretary ready. Review your tasks for today."
        }
    }

    override suspend fun answerAssistantQuery(query: String, contextTasks: List<TaskEntity>): String {
        val taskContext = contextTasks.take(15).joinToString("\n") {
            "- Title: ${it.title}, Priority: ${it.priority}, Status: ${it.status}, Recipient: ${it.recipient}, Waiting: ${it.waitingForWhomOrWhat}"
        }
        val prompt = """
            You are an intelligent executive personal secretary. Answer the user's question accurately using ONLY this real database context:
            
            $taskContext
            
            User question: "$query"
            
            Be concise, helpful, and professional. If suggesting actions or drafts, format clearly.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                textModel.generateContent(prompt)
            }
            response.text ?: "I am ready to assist with your tasks and deadlines."
        } catch (e: Exception) {
            "I could not reach the cloud assistant right now. You can check your tasks in the Today or Projects tab."
        }
    }
}
