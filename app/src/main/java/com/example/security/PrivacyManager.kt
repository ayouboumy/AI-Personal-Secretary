package com.example.security

import android.content.Context
import com.example.data.local.SecretaryDatabase
import com.example.data.local.entity.UserProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class DataCategory {
    DEMO_ANONYMIZED,
    ORDINARY_USER_AUTHORIZED,
    SENSITIVE_CONFIDENTIAL
}

data class SensitiveCheckResult(
    val category: DataCategory,
    val matchedPatterns: List<String>,
    val canSendToCloudAi: Boolean,
    val warningMessage: String?
)

object PrivacyManager {

    private val CONFIDENTIAL_PATTERNS = listOf(
        Regex("(?i)\\b(password|mot de passe|كلمة المرور|secret|confidential|confidentiel|سري للغاية|strictement confidentiel)\\b"),
        Regex("(?i)\\b(rib|iban|credit card|carte bancaire|compte bancaire|بطاقة ائتمان|حساب بنكي)\\b"),
        Regex("(?i)\\b(salary|salaire|bulletin de paie|كشف الراتب|remuneration)\\b"),
        Regex("(?i)\\b(national id|carte d'identité|passeport|passport|رقم بطاقة التعريف|رقم الهوية|رقم الجواز)\\b"),
        Regex("(?i)\\b(medical|médical|dossier médical|rapport médical|تقرير طبي)\\b")
    )

    fun evaluateContent(text: String): SensitiveCheckResult {
        val matches = mutableListOf<String>()
        for (pattern in CONFIDENTIAL_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                matches.add(match.value)
            }
        }

        return if (matches.isNotEmpty()) {
            SensitiveCheckResult(
                category = DataCategory.SENSITIVE_CONFIDENTIAL,
                matchedPatterns = matches,
                canSendToCloudAi = false,
                warningMessage = "Sensitive content detected (${matches.joinToString(", ")}). Unpaid Gemini policy prohibits confidential data. Processed locally with on-device intelligence."
            )
        } else {
            SensitiveCheckResult(
                category = DataCategory.ORDINARY_USER_AUTHORIZED,
                matchedPatterns = emptyList(),
                canSendToCloudAi = true,
                warningMessage = null
            )
        }
    }

    suspend fun exportDataAsJson(context: Context): String = withContext(Dispatchers.IO) {
        val db = SecretaryDatabase.getDatabase(context)
        val root = JSONObject()
        root.put("exportTimestamp", System.currentTimeMillis())
        root.put("appVersion", "1.0")

        // User Profile
        val profile = db.userDao().getUserProfileSync()
        if (profile != null) {
            val profileObj = JSONObject().apply {
                put("userName", profile.userName)
                put("primaryLanguage", profile.primaryLanguage)
                put("workDays", profile.workDays)
                put("advanceReminderDays", profile.advanceReminderDays)
                put("allowCloudAiConsent", profile.allowCloudAiConsent)
            }
            root.put("profile", profileObj)
        }

        // Projects
        val projects = db.projectDao().getAllProjects()
        // Tasks
        val tasks = db.taskDao().getAllTasks()
        // Learning Rules
        val rules = db.learningDao().getActiveRulesSync()
        val rulesArray = JSONArray()
        for (r in rules) {
            val rObj = JSONObject().apply {
                put("id", r.id)
                put("title", r.ruleTitle)
                put("pattern", r.patternTrigger)
                put("action", r.interpretationAction)
            }
            rulesArray.put(rObj)
        }
        root.put("learningRules", rulesArray)

        root.toString(2)
    }

    suspend fun wipeAllUserData(context: Context) = withContext(Dispatchers.IO) {
        val db = SecretaryDatabase.getDatabase(context)
        db.clearAllTables()
        // Re-initialize a blank profile with local settings
        db.userDao().insertOrUpdate(
            UserProfileEntity(
                id = 1,
                userName = "User",
                isOnboardingCompleted = false,
                allowCloudAiConsent = false
            )
        )
    }
}
