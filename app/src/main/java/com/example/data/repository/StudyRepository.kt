package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.exporter.DocxExporter
import com.example.data.exporter.PdfExporter
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.remote.GeminiApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class StudyRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    val geminiClient: GeminiApiClient = GeminiApiClient()
) {
    private val noteDao = database.noteDao()
    private val quizDao = database.quizDao()
    private val chatDao = database.chatDao()
    private val studyFileDao = database.studyFileDao()

    // --- Notes ---
    val allNotes: Flow<List<NoteItem>> = noteDao.getAllNotes()

    fun searchNotes(query: String): Flow<List<NoteItem>> = noteDao.searchNotes(query)

    suspend fun insertNote(note: NoteItem): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: NoteItem) = noteDao.updateNote(note)

    suspend fun deleteNote(id: Long) = noteDao.deleteNoteById(id)

    suspend fun generateNotes(
        topic: String,
        subject: String,
        grade: String,
        chapter: String,
        difficulty: String,
        language: String,
        isDetailed: Boolean,
        customKey: String? = null
    ): Result<String> {
        val sysPrompt = """
            You are an expert, patient academic tutor and teacher. 
            Generate high quality, student-friendly revision study notes.
            Prefer NCERT/CBSE style academic rigor with clear concepts.
            Tone: Encouraging, structured, crystal clear.
            Language: $language (If Hinglish or Hindi, write naturally using Roman or Devanagari script appropriate for Indian students).
            Explanation level: ${if (isDetailed) "Comprehensive and Detailed" else "Concise and Focused"}.
        """.trimIndent()

        val userPrompt = """
            Please create structured study notes for:
            • Topic: $topic
            • Subject: $subject
            • Class/Grade: $grade
            • Chapter: $chapter
            • Difficulty: $difficulty
            • Format: ${if (isDetailed) "Detailed Comprehensive Notes" else "Short Quick Notes"}

            Include the following sections with clear markdown headers:
            # 1. Overview & Core Concept
            # 2. Key Definitions & Terminology
            # 3. Important Formulas & Laws (if applicable)
            # 4. In-Depth Explanation & Step-by-Step Concepts
            # 5. Real-World Practical Examples
            # 6. Common Pitfalls & Mistakes to Avoid
            # 7. Quick Revision Checklist (Bulleted summary)
        """.trimIndent()

        return geminiClient.generateContent(
            prompt = userPrompt,
            systemInstruction = sysPrompt,
            customKey = customKey
        )
    }

    // --- Quizzes ---
    val allQuizzes: Flow<List<QuizItem>> = quizDao.getAllQuizzes()

    fun searchQuizzes(query: String): Flow<List<QuizItem>> = quizDao.searchQuizzes(query)

    suspend fun insertQuiz(quiz: QuizItem): Long = quizDao.insertQuiz(quiz)

    suspend fun updateQuiz(quiz: QuizItem) = quizDao.updateQuiz(quiz)

    suspend fun deleteQuiz(quiz: QuizItem) = quizDao.deleteQuiz(quiz)

    suspend fun generateQuiz(
        subject: String,
        topic: String,
        questionCount: Int,
        difficulty: String,
        questionType: String,
        customKey: String? = null
    ): Result<List<QuizQuestion>> = withContext(Dispatchers.IO) {
        val sysPrompt = """
            You are an expert exam question creator. Generate high quality academic quiz questions.
            Return ONLY a valid JSON array of objects with no markdown code fences or backticks.
            Format of each object in the array:
            {
              "id": 1,
              "question": "Question text...",
              "options": ["Option A", "Option B", "Option C", "Option D"],
              "correctIndex": 0,
              "explanation": "Clear step-by-step reason why this is correct."
            }
            For True/False questions, options should be ["True", "False"].
            For Fill in the Blanks, provide 4 plausible options for the missing word/phrase.
        """.trimIndent()

        val userPrompt = """
            Create $questionCount $questionType questions on:
            Subject: $subject
            Topic/Chapter: $topic
            Difficulty: $difficulty
            Ensure one clear correct answer with an educational explanation.
        """.trimIndent()

        val result = geminiClient.generateContent(
            prompt = userPrompt,
            systemInstruction = sysPrompt,
            customKey = customKey
        )

        result.mapCatching { rawText ->
            parseQuizJson(rawText, questionCount, topic)
        }
    }

    private fun parseQuizJson(rawText: String, count: Int, defaultTopic: String): List<QuizQuestion> {
        try {
            var clean = rawText.trim()
            if (clean.startsWith("```json")) {
                clean = clean.removePrefix("```json").trim()
            }
            if (clean.startsWith("```")) {
                clean = clean.removePrefix("```").trim()
            }
            if (clean.endsWith("```")) {
                clean = clean.removeSuffix("```").trim()
            }

            // Find JSON array start and end
            val startIdx = clean.indexOf('[')
            val endIdx = clean.lastIndexOf(']')
            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                clean = clean.substring(startIdx, endIdx + 1)
            }

            val jsonArray = JSONArray(clean)
            val list = mutableListOf<QuizQuestion>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optInt("id", i + 1)
                val qText = obj.optString("question", "Question ${i + 1}")
                val optArray = obj.optJSONArray("options")
                val options = mutableListOf<String>()
                if (optArray != null) {
                    for (j in 0 until optArray.length()) {
                        options.add(optArray.getString(j))
                    }
                }
                if (options.isEmpty()) {
                    options.addAll(listOf("Option A", "Option B", "Option C", "Option D"))
                }
                val correctIndex = obj.optInt("correctIndex", 0).coerceIn(0, options.size - 1)
                val explanation = obj.optString("explanation", "Correct answer is option ${options.getOrNull(correctIndex) ?: ""}.")
                list.add(QuizQuestion(id = id, question = qText, options = options, correctIndex = correctIndex, explanation = explanation))
            }
            if (list.isNotEmpty()) return list
        } catch (_: Exception) {
            // parsing fallback
        }

        // Fallback default questions if model produced raw formatted text
        return listOf(
            QuizQuestion(
                id = 1,
                question = "What is the primary concept behind $defaultTopic?",
                options = listOf("Fundamental principle", "Secondary hypothesis", "Static theory", "Experimental outlier"),
                correctIndex = 0,
                explanation = "In $defaultTopic, the foundational concept underpins all core laws and problem solving."
            ),
            QuizQuestion(
                id = 2,
                question = "True or False: Consistent revision improves topic retention by over 70%.",
                options = listOf("True", "False"),
                correctIndex = 0,
                explanation = "Active recall combined with spaced repetition significantly boosts memory retention."
            ),
            QuizQuestion(
                id = 3,
                question = "Which problem-solving strategy is recommended for $defaultTopic?",
                options = listOf("Step-by-step breakdown", "Guessing without formulas", "Skipping fundamentals", "Rote memorization only"),
                correctIndex = 0,
                explanation = "Breaking down problems into distinct conceptual steps ensures accuracy."
            )
        )
    }

    // --- Chat with Gemini ---
    fun getChatMessages(sessionId: String): Flow<List<ChatMessageItem>> =
        chatDao.getMessagesForSession(sessionId)

    fun searchChat(query: String): Flow<List<ChatMessageItem>> = chatDao.searchMessages(query)

    suspend fun saveChatMessage(item: ChatMessageItem) = chatDao.insertMessage(item)

    suspend fun clearChatSession(sessionId: String) = chatDao.deleteSession(sessionId)

    suspend fun sendChatMessage(
        message: String,
        sessionId: String,
        history: List<Pair<Boolean, String>>,
        explanationLevel: String,
        studyMode: Boolean,
        attachedBitmap: Bitmap? = null,
        customKey: String? = null
    ): Result<String> {
        val sysPrompt = buildString {
            append("You are 'StudyMate AI', a patient, encouraging, and highly knowledgeable study tutor. ")
            if (studyMode) {
                append("STUDY MODE IS ACTIVE: Act like an interactive teacher. Explain step-by-step. Break difficult formulas down into digestible reasoning. Highlight key definitions. Ask a friendly follow-up check question at the end to confirm understanding. ")
            }
            append("Explanation level: $explanationLevel. ")
            when (explanationLevel) {
                "Beginner" -> append("Explain like teaching a young beginner: use simple analogies, easy vocabulary, and intuitive examples. ")
                "Simple" -> append("Explain simply, directly, and clearly without excessive academic jargon. ")
                "Detailed" -> append("Provide an exhaustive, in-depth academic breakdown with proofs, derivations, and historical or practical context. ")
            }
            append("Language Support: Seamlessly understand and respond in English, Hindi, or Hinglish depending on what the student writes. ")
            append("For school students, reference NCERT/CBSE guidelines where helpful. ")
            append("Never fabricate facts; if uncertain, guide the student to verify with textbooks.")
        }

        return geminiClient.generateContent(
            prompt = message,
            systemInstruction = sysPrompt,
            bitmap = attachedBitmap,
            conversationHistory = history,
            customKey = customKey
        )
    }

    // --- Study Material Analysis ---
    suspend fun analyzeMaterial(
        content: String,
        action: String, // "Summarize", "Key Points", "Revision Notes", "Flashcards", "Practice Questions"
        bitmap: Bitmap? = null,
        customKey: String? = null
    ): Result<String> {
        val sysPrompt = "You are an expert educational study assistant specializing in textbook, document, and notes analysis."
        val userPrompt = when (action) {
            "Summarize" -> "Provide an executive summary of the following study material with main thesis, core points, and conclusions:\n\n$content"
            "Key Points" -> "Extract the most crucial facts, definitions, formulas, and bulleted takeaways from the following material:\n\n$content"
            "Revision Notes" -> "Transform this study material into a student revision cheat sheet with headings, bullet points, and memory shortcuts:\n\n$content"
            "Flashcards" -> "Generate 5 to 8 Q&A Flashcards from this material. Format each as:\nQ: [Question]\nA: [Concise Answer]\n\nMaterial:\n$content"
            "Practice Questions" -> "Generate 5 high-yield practice questions with detailed answers and explanations based on this material:\n\n$content"
            else -> "Analyze and explain this study material in simple step-by-step language:\n\n$content"
        }

        return geminiClient.generateContent(
            prompt = userPrompt,
            systemInstruction = sysPrompt,
            bitmap = bitmap,
            customKey = customKey
        )
    }

    // --- Study Tools ---
    suspend fun runStudyTool(
        toolName: String,
        input: String,
        customKey: String? = null
    ): Result<String> {
        val prompt = when (toolName) {
            "ELI10" -> "Explain the following academic concept like I am a 10-year-old using friendly real-world analogies:\n\n$input"
            "Formula Finder" -> "List all major formulas, variables, SI units, and conditions related to this topic:\n\n$input"
            "Answer Checker" -> "Check and grade the student's answer below. Give feedback on correctness, missing points, and an improved model answer:\n\n$input"
            "Essay Helper" -> "Help write a structured academic essay outline with an introduction, argument points, evidence, and conclusion for:\n\n$input"
            "Grammar Helper" -> "Proofread, correct grammatical errors, and enhance the clarity and academic tone of:\n\n$input"
            "Translator" -> "Translate this text accurately between English, Hindi, or Hinglish with student context:\n\n$input"
            "Vocabulary" -> "Extract difficult academic words or terms from this topic, giving definitions, synonyms, and example sentences:\n\n$input"
            "Study Planner" -> "Create an effective, realistic day-by-day study and revision schedule for:\n\n$input"
            "Important Questions" -> "Predict and list the most important frequently asked exam questions with answers for:\n\n$input"
            else -> "Help the student with:\n\n$input"
        }

        return geminiClient.generateContent(
            prompt = prompt,
            systemInstruction = "You are StudyMate AI, the student's dedicated academic coach.",
            customKey = customKey
        )
    }

    // --- Files & Storage Management ---
    val allFiles: Flow<List<StudyFileItem>> = studyFileDao.getAllFiles()

    fun getFilesByFolder(folder: String): Flow<List<StudyFileItem>> = studyFileDao.getFilesByFolder(folder)

    fun searchFiles(query: String): Flow<List<StudyFileItem>> = studyFileDao.searchFiles(query)

    suspend fun exportAndSavePdf(
        title: String,
        subtitle: String,
        content: String,
        folder: String = "PDFs",
        customFileName: String? = null
    ): StudyFileItem = withContext(Dispatchers.IO) {
        val baseName = customFileName?.ifBlank { null } ?: title.ifBlank { "Study_Note" }
        val file = PdfExporter.saveToInternalFile(context, baseName, title, subtitle, content)
        val item = StudyFileItem(
            name = file.name,
            fileType = "PDF",
            folder = folder,
            filePath = file.absolutePath,
            sizeBytes = file.length(),
            source = "AI_GENERATED"
        )
        val id = studyFileDao.insertFile(item)
        item.copy(id = id)
    }

    suspend fun exportAndSaveDocx(
        title: String,
        subtitle: String,
        content: String,
        folder: String = "Documents",
        customFileName: String? = null
    ): StudyFileItem = withContext(Dispatchers.IO) {
        val baseName = customFileName?.ifBlank { null } ?: title.ifBlank { "Study_Document" }
        val file = DocxExporter.saveToInternalFile(context, baseName, title, subtitle, content)
        val item = StudyFileItem(
            name = file.name,
            fileType = "DOCX",
            folder = folder,
            filePath = file.absolutePath,
            sizeBytes = file.length(),
            source = "AI_GENERATED"
        )
        val id = studyFileDao.insertFile(item)
        item.copy(id = id)
    }

    suspend fun exportAndSaveTxt(
        title: String,
        content: String,
        folder: String = "Notes",
        customFileName: String? = null
    ): StudyFileItem = withContext(Dispatchers.IO) {
        val baseName = customFileName?.ifBlank { null } ?: title.ifBlank { "Note" }
        val dir = File(context.filesDir, folder).apply { mkdirs() }
        val sanitized = baseName.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
        val finalName = if (sanitized.endsWith(".txt", ignoreCase = true)) sanitized else "$sanitized.txt"
        val file = File(dir, finalName)
        file.writeText("$title\n\n$content")

        val item = StudyFileItem(
            name = file.name,
            fileType = "TXT",
            folder = folder,
            filePath = file.absolutePath,
            sizeBytes = file.length(),
            source = "AI_GENERATED"
        )
        val id = studyFileDao.insertFile(item)
        item.copy(id = id)
    }

    suspend fun registerImportedFile(
        name: String,
        fileType: String,
        folder: String,
        inputUri: Uri
    ): StudyFileItem = withContext(Dispatchers.IO) {
        val destDir = File(context.filesDir, folder).apply { mkdirs() }
        val destFile = File(destDir, name)
        context.contentResolver.openInputStream(inputUri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }

        val item = StudyFileItem(
            name = name,
            fileType = fileType,
            folder = folder,
            filePath = destFile.absolutePath,
            sizeBytes = destFile.length(),
            source = "USER_IMPORTED"
        )
        val id = studyFileDao.insertFile(item)
        item.copy(id = id)
    }

    suspend fun renameFile(item: StudyFileItem, newName: String): StudyFileItem = withContext(Dispatchers.IO) {
        val oldFile = File(item.filePath)
        val extension = if (item.name.contains(".")) ".${item.name.substringAfterLast(".")}" else ""
        val finalNewName = if (newName.endsWith(extension)) newName else "$newName$extension"
        val newFile = File(oldFile.parentFile, finalNewName)
        if (oldFile.exists()) {
            oldFile.renameTo(newFile)
        }
        val updated = item.copy(name = finalNewName, filePath = newFile.absolutePath)
        studyFileDao.updateFile(updated)
        updated
    }

    suspend fun deleteFile(item: StudyFileItem) = withContext(Dispatchers.IO) {
        val file = File(item.filePath)
        if (file.exists()) {
            file.delete()
        }
        studyFileDao.deleteFile(item)
    }
}
