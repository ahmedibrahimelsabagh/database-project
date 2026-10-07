package com.example.meditriage

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AnalyzingFragment : Fragment(R.layout.fragment_analyzing) {

    // ⚠️ PASTE YOUR API KEY HERE
    private val apiKey = "AIzaSyBcpmrgFUii4HL-j6LCsv857fcmvuvUKE8"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Models to try in order
    private val models = listOf(
        "gemini-2.0-flash-lite",
        "gemini-2.0-flash",
        "gemini-2.5-flash",
        "gemini-flash-latest"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        analyzeSymptoms()
    }

    private fun analyzeSymptoms() {
        val answers = TriageSession.answers
        if (answers.isEmpty()) {
            navigateWithResult(makeDefaultResult("No answers found"))
            return
        }

        val labels = listOf(
            "Fever", "Breathing Difficulty", "Pain",
            "Dizziness", "Nausea or Vomiting", "Bleeding", "Duration"
        )
        val answersText = answers.mapIndexed { i, a ->
            "${labels.getOrElse(i) { "Question ${i + 1}" }}: $a"
        }.joinToString("\n")

        val prompt = """
You are a medical triage assistant. Analyze these symptoms:

$answersText

Reply with EXACTLY these 5 lines:
Severity: Low Risk
Possible Condition: Common Cold
Confidence: 75%
Advice: Rest and drink fluids. See a doctor if symptoms worsen.
Recommended Doctor: General Practitioner

Rules:
- Severity must be: Low Risk, Medium Risk, or High Risk
- Recommended Doctor must be ONE of: General Practitioner, Cardiology, Pulmonology, Gastroenterology, Neurology, Dermatology, Orthopedics, ENT, Emergency Medicine, Internal Medicine
- Cardiology for chest pain or heart issues
- Pulmonology for breathing difficulty or cough
- Gastroenterology for stomach pain, nausea, vomiting
- Neurology for severe headache, dizziness, numbness
- Dermatology for skin problems
- Orthopedics for bone or joint pain
- ENT for ear, nose, or throat issues
- Emergency Medicine for very severe symptoms
- Internal Medicine for chronic or general issues
- General Practitioner for mild or unclear symptoms
- No extra text, no markdown, no asterisks
        """.trimIndent()

        lifecycleScope.launch {
            var rawText = ""
            var success = false

            // Try each model via direct HTTP
            for (model in models) {
                try {
                    Log.d("MediTriage", "Trying model: $model")
                    val result = callGeminiHttp(model, prompt)
                    if (!result.isNullOrBlank()) {
                        rawText = result
                        success = true
                        Log.d("MediTriage", "Success with $model:\n$rawText")
                        break
                    }
                } catch (e: Exception) {
                    Log.e("MediTriage", "$model failed: ${e.message}")
                }
            }

            val parsed = if (success) parseFlexible(rawText) else makeDefaultResult("AI unavailable")

            // Find matching doctor from Oracle
            var matchedDoctor: DoctorResponse? = null
            try {
                val specialty = parsed.recommendedDoctor.trim()
                val docResp = ApiClient.api.getDoctorBySpecialty(specialty)
                if (docResp.isSuccessful && !docResp.body().isNullOrEmpty()) {
                    matchedDoctor = docResp.body()!!.first()
                    Log.d("MediTriage", "Doctor: Dr. ${matchedDoctor.firstName} ${matchedDoctor.lastName}")
                }
            } catch (e: Exception) {
                Log.e("MediTriage", "Doctor lookup: ${e.message}")
            }

            TriageSession.matchedDoctor = matchedDoctor

            val finalResult = if (matchedDoctor != null) {
                parsed.copy(
                    recommendedDoctor = "Dr. ${matchedDoctor.firstName} ${matchedDoctor.lastName} — ${matchedDoctor.specialtyName}"
                )
            } else parsed

            // Save to Oracle
            try {
                val sessionResp = ApiClient.api.createSession(SessionRequest(TriageSession.patientId))
                val sessionId = sessionResp.body()?.sessionId ?: 1
                TriageSession.currentSessionId = sessionId

                ApiClient.api.saveSymptoms(
                    sessionId,
                    SymptomsRequest(answers.mapIndexed { i, ans ->
                        SymptomAnswer(
                            symptomName   = labels.getOrElse(i) { "Symptom ${i + 1}" },
                            answerValue   = ans.take(10),
                            severityScore = when {
                                ans.contains("severe", true) || ans.contains("high", true) -> 4
                                ans.contains("mild", true) || ans.contains("low", true) -> 2
                                else -> 3
                            },
                            durationDays = 1
                        )
                    })
                )

                val diagResp = ApiClient.api.saveDiagnosis(
                    DiagnosisRequest(
                        sessionId            = sessionId,
                        primaryCondition     = finalResult.possibleCondition,
                        otherConditions      = "",
                        severityLevel        = finalResult.severity,
                        basicAdvice          = finalResult.advice,
                        recommendedSpecialty = finalResult.recommendedDoctor,
                        confidenceScore      = finalResult.confidence
                            .replace("%", "").trim().toDoubleOrNull() ?: 0.0,
                        doctorId             = matchedDoctor?.doctorId
                    )
                )
                TriageSession.currentDiagnosisId = diagResp.body()?.diagnosisId ?: -1
                ApiClient.api.completeSession(sessionId)

            } catch (e: Exception) {
                Log.e("MediTriage", "Oracle save: ${e.message}")
            }

            TriageSession.aiResult = finalResult
            if (isAdded) findNavController().navigate(R.id.action_analyzing_to_result)
        }
    }

    private suspend fun callGeminiHttp(modelName: String, prompt: String): String? {
        return withContext(Dispatchers.IO) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val body = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("maxOutputTokens", 300)
                })
            }.toString()

            val request = Request.Builder()
                .url(url)
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            Log.d("MediTriage", "HTTP ${response.code} from $modelName")

            if (!response.isSuccessful) {
                Log.e("MediTriage", "Error body: $responseBody")
                return@withContext null
            }

            val json = JSONObject(responseBody)
            json.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
        }
    }

    private fun parseFlexible(text: String): TriageAiResult {
        val lines = text.lines().map { it.trim() }

        fun find(vararg keys: String): String {
            for (key in keys) {
                val line = lines.firstOrNull { l ->
                    l.replace("*", "").replace("#", "")
                        .startsWith(key, ignoreCase = true)
                }
                if (line != null) {
                    val v = line.substringAfter(":").trim()
                        .replace("**", "").replace("*", "").trim()
                    if (v.isNotEmpty()) return v
                }
            }
            return ""
        }

        val severity = find("Severity").let {
            when {
                it.contains("high", true) || it.contains("critical", true) -> "High Risk"
                it.contains("medium", true) || it.contains("moderate", true) -> "Medium Risk"
                it.contains("low", true) -> "Low Risk"
                it.isNotEmpty() -> it
                else -> "Medium Risk"
            }
        }

        return TriageAiResult(
            severity          = severity,
            possibleCondition = find("Possible Condition", "Condition").ifEmpty { "Consult a doctor" },
            confidence        = find("Confidence").let { if (it.contains("%")) it else "$it%" }.ifEmpty { "70%" },
            advice            = find("Advice", "Recommendation").ifEmpty { "Please consult a doctor." },
            recommendedDoctor = find("Recommended Doctor", "Doctor").ifEmpty { "General Practitioner" }
        )
    }

    private fun makeDefaultResult(reason: String) = TriageAiResult(
        severity = "Medium Risk", possibleCondition = reason,
        confidence = "N/A", advice = "Please consult a doctor.",
        recommendedDoctor = "General Practitioner"
    )

    private fun navigateWithResult(result: TriageAiResult) {
        TriageSession.aiResult = result
        if (isAdded) findNavController().navigate(R.id.action_analyzing_to_result)
    }
}
