package com.example.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest
import kotlin.coroutines.resume

data class SampleDocument(
    val id: String,
    val title: String,
    val language: String, // "ar", "fr", "en"
    val category: String,
    val content: String,
    val description: String
)

object DocumentProcessor {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeTextFromBitmap(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    if (cont.isActive) cont.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    if (cont.isActive) cont.resume("")
                }
        } catch (e: Exception) {
            if (cont.isActive) cont.resume("")
        }
    }

    suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val input: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            null
        }
    }

    fun computeTextHash(text: String): String {
        val normalized = text.lowercase().replace("\\s+".toRegex(), " ").trim()
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(normalized.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Built-in synthetic demonstration documents for Arabic, French, and English
    // Ideal for the Learning Laboratory and privacy-preserving testing!
    val SAMPLE_DOCUMENTS = listOf(
        SampleDocument(
            id = "sample_fr_1",
            title = "Message WhatsApp — Rapport Technique",
            language = "fr",
            category = "WhatsApp",
            content = "Bonjour Alex, merci de préparer le rapport technique trimestriel et de le soumettre à l'administration avant le 15 décembre. C'est urgent pour le renouvellement de l'agrément.",
            description = "Instruction WhatsApp avec deadline claire et destinataire administratif."
        ),
        SampleDocument(
            id = "sample_ar_1",
            title = "خطاب إداري — تسليم المخططات الهندسية",
            language = "ar",
            category = "Administrative Letter",
            content = "المملكة المغربية - وزارة التجهيز والماء\nالموضوع: إعداد وموافاة الإدارة بالمخططات الهندسية والمصادقة عليها.\nنرجو منكم إعداد الملف التقني وإرساله إلى مصلحة التعمير قبل تاريخ 20 ديسمبر، علماً أن اجتماع اللجنة التقنية مقرر يوم 25 ديسمبر.",
            description = "خطاب إداري يحتوي على موعد نهائي للتسليم وتاريخ اجتماع منفصل للتمييز بينهما."
        ),
        SampleDocument(
            id = "sample_en_1",
            title = "Project Brief — Client Presentation",
            language = "en",
            category = "Email Brief",
            content = "Hi team, please review the Q4 financial projections and prepare the presentation deck for the Board of Directors before December 10th. Make sure to coordinate with Sarah on legal clearance.",
            description = "Corporate project brief with deliverables, reviewers, and deadline."
        ),
        SampleDocument(
            id = "sample_fr_2",
            title = "Courrier Marché Public — Révision des Prix",
            language = "fr",
            category = "Administrative Letter",
            content = "Réf: MP-2026/892 - Avis à l'entrepreneur.\nVous êtes prié de fournir le bordereau des prix unitaires révisé auprès du bureau d'ordre avant le 28 novembre à 16h00. La réunion préparatoire aura lieu le 22 novembre.",
            description = "Courrier formel distinguant date de réunion et délai impératif de soumission."
        ),
        SampleDocument(
            id = "sample_ar_2",
            title = "رسالة تذكير — ملف الترشح والمناقصة",
            language = "ar",
            category = "WhatsApp",
            content = "السلام عليكم، تذكير بضرورة تجميع وثائق الضمان البنكي وإيداع ملف المناقصة لدى كتابة الضبط قبل 18 نوفمبر. لا تنس إرفاق شهادة الإبراء الضريبي.",
            description = "رسالة مهنية عاجلة تتضمن مهام فرعية متعددة ووثائق مطلوبة."
        )
    )
}
