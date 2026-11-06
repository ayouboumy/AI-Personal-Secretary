package com.example

import com.example.ai.OfflineRuleBasedSecretaryService
import com.example.security.DataCategory
import com.example.security.PrivacyManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretaryLogicTest {

    private val offlineService = OfflineRuleBasedSecretaryService()

    @Test
    fun testFrenchTaskAndDeadlineExtraction() = runBlocking {
        val frenchSample = "Bonjour, merci de préparer le rapport technique et de l'envoyer avant le 15 décembre à l'administration. C'est urgent."
        val result = offlineService.extractTask(frenchSample, emptyList())

        assertEquals("fr", result.detectedLanguage)
        assertEquals("URGENT", result.priority)
        assertEquals("SUBMISSION_DEADLINE", result.deadlineType)
        assertNotNull(result.deadlineMillis)
        assertTrue(result.suggestedSubtasks.isNotEmpty())
    }

    @Test
    fun testArabicAdministrativeTaskExtraction() = runBlocking {
        val arabicSample = "نرجو منكم إعداد الملف التقني وإرساله قبل تاريخ 20 ديسمبر إلى مصلحة التعمير."
        val result = offlineService.extractTask(arabicSample, emptyList())

        assertEquals("ar", result.detectedLanguage)
        assertEquals("SUBMISSION_DEADLINE", result.deadlineType)
        assertNotNull(result.deadlineMillis)
    }

    @Test
    fun testDistinguishMeetingDateFromSubmission() = runBlocking {
        val meetingText = "Une réunion préparatoire est fixée le 22 novembre."
        val result = offlineService.extractTask(meetingText, emptyList())

        assertEquals("MEETING_DATE", result.deadlineType)
    }

    @Test
    fun testSensitiveDataDetection() {
        val sensitiveText = "Here is the confidential payroll and password: admin"
        val eval = PrivacyManager.evaluateContent(sensitiveText)

        assertEquals(DataCategory.SENSITIVE_CONFIDENTIAL, eval.category)
        assertFalse(eval.canSendToCloudAi)
        assertNotNull(eval.warningMessage)

        val nonSensitive = "Please review the presentation slides before Friday."
        val evalNormal = PrivacyManager.evaluateContent(nonSensitive)
        assertEquals(DataCategory.ORDINARY_USER_AUTHORIZED, evalNormal.category)
        assertTrue(evalNormal.canSendToCloudAi)
    }
}
