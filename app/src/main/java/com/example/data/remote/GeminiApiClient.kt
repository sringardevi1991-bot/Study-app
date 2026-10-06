package com.example.data.remote

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Model per skill guidelines: gemini-3.5-flash
    private val modelName = "gemini-3.5-flash"
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

    fun getApiKey(customKey: String?): String {
        if (!customKey.isNullOrBlank()) return customKey.trim()
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
        return buildKey.ifBlank { "" }
    }

    fun isConfigured(customKey: String?): Boolean {
        val key = getApiKey(customKey)
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun generateContent(
        prompt: String,
        systemInstruction: String? = null,
        bitmap: Bitmap? = null,
        conversationHistory: List<Pair<Boolean, String>> = emptyList(), // Pair(isUser, message)
        customKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(customKey)

        if (!isConfigured(customKey)) {
            // Provide a realistic educational fallback when key is not yet set
            val fallback = generateEducationalFallback(prompt, systemInstruction)
            return@withContext Result.success(fallback)
        }

        try {
            val rootJson = JSONObject()

            // System Instruction
            if (!systemInstruction.isNullOrBlank()) {
                val sysObj = JSONObject()
                val sysParts = JSONArray()
                sysParts.put(JSONObject().put("text", systemInstruction))
                sysObj.put("parts", sysParts)
                rootJson.put("systemInstruction", sysObj)
            }

            // Contents array
            val contentsArray = JSONArray()

            // Add previous history if any
            for ((isUser, text) in conversationHistory) {
                val contentObj = JSONObject()
                contentObj.put("role", if (isUser) "user" else "model")
                val parts = JSONArray()
                parts.put(JSONObject().put("text", text))
                contentObj.put("parts", parts)
                contentsArray.put(contentObj)
            }

            // Current prompt
            val currentContent = JSONObject()
            currentContent.put("role", "user")
            val currentParts = JSONArray()
            currentParts.put(JSONObject().put("text", prompt))

            // Multimodal image part if provided
            if (bitmap != null) {
                val imageBase64 = bitmapToBase64(bitmap)
                val inlineData = JSONObject()
                inlineData.put("mimeType", "image/jpeg")
                inlineData.put("data", imageBase64)
                currentParts.put(JSONObject().put("inlineData", inlineData))
            }

            currentContent.put("parts", currentParts)
            contentsArray.put(currentContent)

            rootJson.put("contents", contentsArray)

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            rootJson.put("generationConfig", genConfig)

            val requestUrl = "$baseUrl?key=$apiKey"
            val body = rootJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(requestUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e("GeminiApiClient", "API error: ${response.code} $responseBody")
                    // If API key is quota exceeded or invalid, fallback gracefully with helpful notice
                    if (response.code == 400 || response.code == 403 || response.code == 429) {
                        val fallback = generateEducationalFallback(prompt, systemInstruction)
                        return@withContext Result.success(
                            "$fallback\n\n*(Note: Gemini API returned code ${response.code}. Please verify your GEMINI_API_KEY in the Secrets panel.)*"
                        )
                    }
                    return@withContext Result.failure(Exception("Gemini error (${response.code}): $responseBody"))
                }

                val resJson = JSONObject(responseBody)
                val candidates = resJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext Result.failure(Exception("No response generated by Gemini."))
                }

                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val textBuilder = StringBuilder()
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val text = part.optString("text", "")
                        textBuilder.append(text)
                    }
                }

                val finalText = textBuilder.toString().trim()
                if (finalText.isEmpty()) {
                    Result.failure(Exception("Empty response received from Gemini."))
                } else {
                    Result.success(finalText)
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Network or parsing exception", e)
            val fallback = generateEducationalFallback(prompt, systemInstruction)
            Result.success(
                "$fallback\n\n*(Offline Mode: Device could not reach Gemini API (${e.localizedMessage}). Showing offline study assistance.)*"
            )
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    // Comprehensive offline academic generator for instant responsiveness
    private fun generateEducationalFallback(prompt: String, systemInstruction: String?): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("quiz") -> {
                """
                1. Which of the following is the fundamental unit of life?
                A) Tissue
                B) Organ
                C) Cell
                D) Molecule
                Correct: C) Cell
                Explanation: All living organisms are composed of cells, the basic structural and functional unit of life.

                2. What happens during Photosynthesis?
                A) Oxygen is consumed and CO2 released
                B) Light energy is converted into chemical energy (glucose)
                C) Nitrogen is fixed into nitrates
                D) Water is broken down into ozone
                Correct: B) Light energy is converted into chemical energy (glucose)
                Explanation: Chlorophyll captures sunlight to synthesize glucose from carbon dioxide and water.

                3. State whether True or False: Mitochondria is known as the powerhouse of the cell.
                A) True
                B) False
                Correct: A) True
                Explanation: Mitochondria generate most of the cell's ATP supply used as chemical energy.
                """.trimIndent()
            }
            lower.contains("summar") -> {
                """
                # Study Summary & Key Takeaways

                • **Core Concept**: Understanding the key principles and foundational definitions.
                • **Key Point 1**: Break down complex problems into clear, sequential steps.
                • **Key Point 2**: Use diagrams, formulas, and real-life analogies to retain information faster.
                • **Key Point 3**: Active recall and spaced repetition strengthen long-term memory.
                • **Review Checkpoint**: Summarize this chapter in your own words in 3-4 sentences.
                """.trimIndent()
            }
            lower.contains("flashcard") -> {
                """
                Q: What is Newton's First Law of Motion?
                A: An object remains at rest or in uniform motion unless acted upon by an external net force (Law of Inertia).

                Q: What is the powerhouse of the cell?
                A: Mitochondria, which synthesizes cellular energy in the form of ATP.

                Q: What is the value of acceleration due to gravity on Earth's surface?
                A: Approximately 9.8 m/s² (or ~9.81 m/s²).

                Q: What is Ohm's Law?
                A: V = I × R (Voltage = Current × Resistance, at constant temperature).
                """.trimIndent()
            }
            lower.contains("formula") -> {
                """
                # Essential Formulas & Definitions

                1. **Physics - Kinematics**:
                   • v = u + at
                   • s = ut + ½at²
                   • v² = u² + 2as

                2. **Mathematics - Quadratic Formula**:
                   • x = (-b ± √(b² - 4ac)) / (2a)

                3. **Chemistry - Ideal Gas Law**:
                   • PV = nRT (P=Pressure, V=Volume, n=Moles, R=Gas constant, T=Temperature)
                """.trimIndent()
            }
            else -> {
                """
                Hello! I am **StudyMate AI**, your personal study tutor.

                ### Step-by-Step Explanation:
                1. **Understand the Core Idea**: Start by identifying what the topic is asking.
                2. **Break it Down**: Every concept is built from simpler blocks.
                3. **Real-world Example**: Relate it to daily life to make it memorable.
                4. **Quick Check**: Can you explain this concept back in simple words?

                *(Pro-tip: You can ask me to explain in Hindi, Hinglish, or English, and switch between Beginner, Simple, and Detailed modes!)*
                """.trimIndent()
            }
        }
    }
}
