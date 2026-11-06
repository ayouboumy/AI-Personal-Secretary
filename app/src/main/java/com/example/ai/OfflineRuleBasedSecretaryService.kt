package com.example.ai

import com.example.data.local.entity.LearningRuleEntity
import com.example.data.local.entity.TaskEntity
import java.util.Calendar
import java.util.Locale

class OfflineRuleBasedSecretaryService : SecretaryAiService {

    private val frenchMonths = mapOf(
        "janvier" to 0, "février" to 1, "mars" to 2, "avril" to 3, "mai" to 4, "juin" to 5,
        "juillet" to 6, "août" to 7, "septembre" to 8, "octobre" to 9, "novembre" to 10, "décembre" to 11
    )

    private val englishMonths = mapOf(
        "january" to 0, "february" to 1, "march" to 2, "april" to 3, "may" to 4, "june" to 5,
        "july" to 6, "august" to 7, "september" to 8, "october" to 9, "november" to 10, "december" to 11,
        "jan" to 0, "feb" to 1, "mar" to 2, "apr" to 3, "jun" to 5, "jul" to 6, "aug" to 7, "sep" to 8, "oct" to 9, "nov" to 10, "dec" to 11
    )

    private val arabicMonths = mapOf(
        "يناير" to 0, "فبراير" to 1, "مارس" to 2, "أبريل" to 3, "ابريل" to 3, "مايو" to 4,
        "يونيو" to 5, "يوليو" to 6, "أغسطس" to 7, "اغسطس" to 7, "سبتمبر" to 8, "أكتوبر" to 9, "اكتوبر" to 9,
        "نوفمبر" to 10, "ديسمبر" to 11
    )

    override suspend fun extractTask(text: String, rules: List<LearningRuleEntity>): ExtractionResult {
        val lowerText = text.lowercase()

        // 1. Detect language
        val isArabic = text.any { it in '\u0600'..'\u06FF' }
        val isFrench = !isArabic && (lowerText.contains("le ") || lowerText.contains("avant") || lowerText.contains("rapport") || lowerText.contains("merci"))
        val language = if (isArabic) "ar" else if (isFrench) "fr" else "en"

        // 2. Parse Date & Deadline Type
        val (deadlineMillis, evidenceText, deadlineType) = parseDateAndType(text, language)

        // 3. Determine Priority
        val isUrgent = lowerText.contains("urgent") || lowerText.contains("عاجل") || lowerText.contains("asap") || lowerText.contains("impératif")
        val isHigh = lowerText.contains("important") || lowerText.contains("ضروري") || lowerText.contains("vital")
        val priority = when {
            isUrgent -> "URGENT"
            isHigh -> "HIGH"
            else -> "MEDIUM"
        }

        // 4. Extract Recipient
        val recipient = extractRecipient(text, language)

        // 5. Generate Title
        val taskTitle = generateTitle(text, language)

        // 6. Generate Subtasks
        val subtasks = breakdownTask(taskTitle, text)

        // 7. Apply active learned user rules
        var finalDeadlineType = deadlineType
        for (rule in rules) {
            if (rule.isActive && lowerText.contains(rule.patternTrigger.lowercase())) {
                if (rule.interpretationAction.contains("SUBMISSION_DEADLINE", ignoreCase = true)) {
                    finalDeadlineType = "SUBMISSION_DEADLINE"
                } else if (rule.interpretationAction.contains("MEETING_DATE", ignoreCase = true)) {
                    finalDeadlineType = "MEETING_DATE"
                }
            }
        }

        return ExtractionResult(
            taskTitle = taskTitle,
            taskDescription = text.trim(),
            deadlineMillis = deadlineMillis,
            deadlineType = finalDeadlineType,
            recipient = recipient,
            priority = priority,
            evidenceText = evidenceText,
            confidence = 0.92f,
            suggestedSubtasks = subtasks,
            detectedLanguage = language,
            isProcessedByCloudAi = false,
            processingNotes = "On-device deterministic secretary engine (Zero data leakage, fully offline)"
        )
    }

    private fun parseDateAndType(text: String, language: String): Triple<Long?, String?, String> {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        val hasMeetingKeyword = text.contains("réunion", ignoreCase = true) || text.contains("meeting", ignoreCase = true) || text.contains("اجتماع") || text.contains("rdv", ignoreCase = true)
        val hasSubmissionKeyword = text.contains("avant", ignoreCase = true) || text.contains("soumettre", ignoreCase = true) || text.contains("délai", ignoreCase = true) || text.contains("تسليم") || text.contains("قبل") || text.contains("deadline", ignoreCase = true)

        // French regex: (avant le|le|au plus tard le)\s+(\d{1,2})\s+([a-zéû]+)
        val frMatch = Regex("(?i)(avant le|au plus tard le|d'ici le|pour le|réunion le|le)\\s+(\\d{1,2})\\s+([a-zéû]+)").find(text)
        if (frMatch != null) {
            val day = frMatch.groupValues[2].toIntOrNull() ?: 1
            val monthName = frMatch.groupValues[3].lowercase()
            val monthIdx = frenchMonths[monthName]
            if (monthIdx != null) {
                calendar.set(currentYear, monthIdx, day, 18, 0, 0)
                val type = if (hasMeetingKeyword && !hasSubmissionKeyword) "MEETING_DATE" else "SUBMISSION_DEADLINE"
                return Triple(calendar.timeInMillis, frMatch.value, type)
            }
        }

        // Arabic regex: (قبل تاريخ|قبل|يوم|بتاريخ|اجتماع يوم)\s+(\d{1,2})\s+([^\s]+)
        val arMatch = Regex("(قبل تاريخ|في أجل أقصاه|قبل|اجتماع يوم|بتاريخ|يوم)\\s+(\\d{1,2})\\s+([\\u0621-\\u064A]+)").find(text)
        if (arMatch != null) {
            val day = arMatch.groupValues[2].toIntOrNull() ?: 1
            val monthName = arMatch.groupValues[3]
            val monthIdx = arabicMonths[monthName]
            if (monthIdx != null) {
                calendar.set(currentYear, monthIdx, day, 18, 0, 0)
                val type = if (hasMeetingKeyword && !hasSubmissionKeyword) "MEETING_DATE" else "SUBMISSION_DEADLINE"
                return Triple(calendar.timeInMillis, arMatch.value, type)
            }
        }

        // English regex: (before|by|due date:|meeting on)\s+([a-zA-Z]+)\s+(\\d{1,2})
        val enMatch = Regex("(?i)(before|by|due date|deadline|meeting on|on)\\s+([a-zA-Z]+)\\s+(\\d{1,2})(?:st|nd|rd|th)?").find(text)
        if (enMatch != null) {
            val monthName = enMatch.groupValues[2].lowercase()
            val day = enMatch.groupValues[3].toIntOrNull() ?: 1
            val monthIdx = englishMonths[monthName]
            if (monthIdx != null) {
                calendar.set(currentYear, monthIdx, day, 18, 0, 0)
                val type = if (hasMeetingKeyword && !hasSubmissionKeyword) "MEETING_DATE" else "SUBMISSION_DEADLINE"
                return Triple(calendar.timeInMillis, enMatch.value, type)
            }
        }

        // Default: 3 days from now
        calendar.add(Calendar.DAY_OF_YEAR, 3)
        return Triple(calendar.timeInMillis, "Detected implicit short-term requirement", "SUBMISSION_DEADLINE")
    }

    private fun extractRecipient(text: String, language: String): String? {
        val lower = text.lowercase()
        return when (language) {
            "fr" -> {
                val match = Regex("(?i)(à l'administration|à la direction|au bureau d'ordre|à la mairie|au client)").find(text)
                match?.value ?: if (lower.contains("direction")) "Direction" else "Administration"
            }
            "ar" -> {
                val match = Regex("(إلى مصلحة التعمير|إلى الإدارة|لكتابة الضبط|إلى الوزارة|للعميل)").find(text)
                match?.value ?: if (text.contains("إدارة") || text.contains("الإدارة")) "الإدارة المعنية" else null
            }
            else -> {
                val match = Regex("(?i)(to the administration|to the board|to client|to management)").find(text)
                match?.value ?: if (lower.contains("board")) "Board of Directors" else "Management"
            }
        }
    }

    private fun generateTitle(text: String, language: String): String {
        val firstSentence = text.lines().firstOrNull { it.isNotBlank() } ?: text
        return if (firstSentence.length <= 60) {
            firstSentence
        } else {
            when (language) {
                "fr" -> {
                    if (text.contains("rapport", ignoreCase = true)) "Préparation et soumission du rapport"
                    else if (text.contains("prix", ignoreCase = true)) "Transmission du bordereau des prix"
                    else "Instruction reçue: " + firstSentence.take(45) + "..."
                }
                "ar" -> {
                    if (text.contains("المخططات")) "إعداد وتسليم المخططات الهندسية"
                    else if (text.contains("المناقصة")) "تجهيز وإيداع ملف المناقصة"
                    else "مهمة إدارية: " + firstSentence.take(40) + "..."
                }
                else -> {
                    if (text.contains("presentation", ignoreCase = true)) "Prepare presentation deck and projections"
                    else "Action item: " + firstSentence.take(45) + "..."
                }
            }
        }
    }

    override suspend fun breakdownTask(taskTitle: String, description: String): List<String> {
        val lower = (taskTitle + " " + description).lowercase()
        return when {
            lower.contains("rapport") || lower.contains("report") || lower.contains("تقرير") -> listOf(
                "Collect primary data & financial figures",
                "Draft initial report structure & executive summary",
                "Review findings with technical lead",
                "Finalize document layout & verification",
                "Submit official copy to recipient"
            )
            lower.contains("مخطط") || lower.contains("plans") || lower.contains("technique") -> listOf(
                "Gather architecture and structural blueprints",
                "Verify compliance with administrative norms",
                "Consolidate technical specifications file",
                "Signatory authorization & official stamping",
                "Deposit dossier before deadline"
            )
            lower.contains("مناقصة") || lower.contains("tender") || lower.contains("marché") -> listOf(
                "Review administrative tender requirements",
                "Request bank guarantees and tax clearance",
                "Prepare cost breakdown and unit prices",
                "Assemble dossier with supporting attachments",
                "Official submission to registry"
            )
            else -> listOf(
                "Gather required reference documents",
                "Draft main deliverables",
                "Internal verification and quality check",
                "Send final submission to recipient"
            )
        }
    }

    override suspend fun generateDailyBriefing(tasks: List<TaskEntity>, overdueCount: Int, isMorning: Boolean): String {
        val urgentTasks = tasks.filter { it.priority == "URGENT" && it.status != "COMPLETED" }
        val pendingCount = tasks.count { it.status != "COMPLETED" }

        return if (isMorning) {
            buildString {
                append("Good morning! You have $pendingCount active tasks today")
                if (urgentTasks.isNotEmpty()) {
                    append(", with ${urgentTasks.size} marked URGENT (${urgentTasks.first().title})")
                }
                if (overdueCount > 0) {
                    append(". Warning: $overdueCount overdue items require immediate attention.")
                } else {
                    append(". All upcoming deadlines are on schedule.")
                }
                append(" Recommendation: Focus on top priority submission items before midday.")
            }
        } else {
            val completedToday = tasks.count { it.status == "COMPLETED" }
            buildString {
                append("Evening review: $completedToday tasks accomplished today. ")
                if (pendingCount > 0) {
                    append("$pendingCount tasks remain open. ")
                    if (urgentTasks.isNotEmpty()) {
                        append("Keep ${urgentTasks.first().title} prioritized for tomorrow morning.")
                    } else {
                        append("Scheduled tasks are aligned for the next work session.")
                    }
                } else {
                    append("All active items completed! Outstanding performance.")
                }
            }
        }
    }

    override suspend fun answerAssistantQuery(query: String, contextTasks: List<TaskEntity>): String {
        val lower = query.lowercase()
        return when {
            lower.contains("urgent") || lower.contains("priority") || lower.contains("عاجل") -> {
                val urgent = contextTasks.filter { it.priority == "URGENT" && it.status != "COMPLETED" }
                if (urgent.isEmpty()) {
                    "You currently have no tasks marked as Urgent. Great job maintaining peace of mind!"
                } else {
                    "You have ${urgent.size} urgent task(s):\n" +
                            urgent.joinToString("\n") { "• ${it.title} (Priority: ${it.priority})" }
                }
            }
            lower.contains("today") || lower.contains("finish") || lower.contains("اليوم") -> {
                val open = contextTasks.filter { it.status != "COMPLETED" }
                "Open items to focus on today (${open.size} total):\n" +
                        open.take(5).joinToString("\n") { "• ${it.title}" }
            }
            lower.contains("waiting") || lower.contains("approval") || lower.contains("انتظار") -> {
                val waiting = contextTasks.filter { it.status == "WAITING_FOR_RESPONSE" }
                if (waiting.isEmpty()) {
                    "No tasks are currently waiting on external parties or approvals."
                } else {
                    "Tasks waiting for response (${waiting.size}):\n" +
                            waiting.joinToString("\n") { "• ${it.title} (Awaiting: ${it.waitingForWhomOrWhat ?: it.recipient ?: "External reply"})" }
                }
            }
            lower.contains("draft") || lower.contains("follow up") || lower.contains("متابعة") -> {
                "Here is a professional follow-up draft for your approval:\n\n" +
                        "\"Dear Colleague,\nFollowing up regarding our submission from earlier this week. Kindly confirm if the file is under review or if additional documentation is required. Thank you.\""
            }
            else -> {
                "I am tracking ${contextTasks.size} tasks in your local secretary database. You can ask me to summarize urgent deadlines, check waiting responses, or generate your daily work schedule."
            }
        }
    }
}
