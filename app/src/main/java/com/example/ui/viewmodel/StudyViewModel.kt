package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.StudyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.OutputStream

enum class ScreenTab {
    HOME,
    CHAT,
    NOTES,
    QUIZ,
    FILES,
    TOOLS,
    STUDY_MATERIAL,
    SEARCH,
    SETTINGS,
    PDF_CREATOR,
    DOCX_CREATOR
}

data class QuizSessionState(
    val quizTitle: String = "",
    val subject: String = "",
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOption: Int? = null,
    val isAnswerSubmitted: Boolean = false,
    val score: Int = 0,
    val isQuizFinished: Boolean = false
)

class StudyViewModel(application: Application) : AndroidViewModel(application) {
    val repository = StudyRepository(application)

    // Current navigation state
    private val _currentScreen = MutableStateFlow(ScreenTab.HOME)
    val currentScreen: StateFlow<ScreenTab> = _currentScreen.asStateFlow()

    private val _previousScreen = MutableStateFlow<ScreenTab?>(null)

    fun navigateTo(screen: ScreenTab) {
        if (_currentScreen.value != screen) {
            _previousScreen.value = _currentScreen.value
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        val prev = _previousScreen.value
        return if (prev != null && prev != _currentScreen.value) {
            _currentScreen.value = prev
            _previousScreen.value = null
            true
        } else if (_currentScreen.value != ScreenTab.HOME) {
            _currentScreen.value = ScreenTab.HOME
            true
        } else {
            false
        }
    }

    // App Preferences / Settings
    val defaultGrade = MutableStateFlow("Class 10")
    val defaultSubject = MutableStateFlow("Science")
    val defaultLanguage = MutableStateFlow("English")
    val defaultExplanationLevel = MutableStateFlow("Simple")
    val studyModeActive = MutableStateFlow(true)
    val isDarkMode = MutableStateFlow(false)
    val customApiKey = MutableStateFlow("")

    // Data streams
    val notes: StateFlow<List<NoteItem>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quizzes: StateFlow<List<QuizItem>> = repository.allQuizzes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessageItem>> = repository.getChatMessages("default")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val files: StateFlow<List<StudyFileItem>> = repository.allFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Loading State & Feedback
    val isAiLoading = MutableStateFlow(false)
    val aiStatusMessage = MutableStateFlow("")
    val toastMessage = MutableSharedFlow<String>()

    // Search query & results
    val searchQuery = MutableStateFlow("")
    private val _searchResultsNotes = MutableStateFlow<List<NoteItem>>(emptyList())
    val searchResultsNotes: StateFlow<List<NoteItem>> = _searchResultsNotes.asStateFlow()

    private val _searchResultsFiles = MutableStateFlow<List<StudyFileItem>>(emptyList())
    val searchResultsFiles: StateFlow<List<StudyFileItem>> = _searchResultsFiles.asStateFlow()

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
        if (query.isBlank()) {
            _searchResultsNotes.value = emptyList()
            _searchResultsFiles.value = emptyList()
            return
        }
        viewModelScope.launch {
            repository.searchNotes(query).firstOrNull()?.let {
                _searchResultsNotes.value = it
            }
            repository.searchFiles(query).firstOrNull()?.let {
                _searchResultsFiles.value = it
            }
        }
    }

    // --- Chat with Gemini ---
    fun sendChatMessage(
        message: String,
        attachedBitmap: Bitmap? = null,
        explanationLevel: String = defaultExplanationLevel.value
    ) {
        if (message.isBlank() && attachedBitmap == null) return

        viewModelScope.launch {
            // Save user message
            val userMsg = ChatMessageItem(
                sessionId = "default",
                isUser = true,
                message = message.ifBlank { "[Attached Image/Photo of Study Material]" },
                explanationLevel = explanationLevel,
                studyMode = studyModeActive.value
            )
            repository.saveChatMessage(userMsg)

            isAiLoading.value = true
            aiStatusMessage.value = "Gemini is analyzing and preparing tutor response..."

            val history = chatMessages.value.takeLast(6).map { it.isUser to it.message }

            val result = repository.sendChatMessage(
                message = message.ifBlank { "Please explain this study material step-by-step." },
                sessionId = "default",
                history = history,
                explanationLevel = explanationLevel,
                studyMode = studyModeActive.value,
                attachedBitmap = attachedBitmap,
                customKey = customApiKey.value
            )

            isAiLoading.value = false
            aiStatusMessage.value = ""

            result.onSuccess { reply ->
                val geminiMsg = ChatMessageItem(
                    sessionId = "default",
                    isUser = false,
                    message = reply,
                    explanationLevel = explanationLevel,
                    studyMode = studyModeActive.value
                )
                repository.saveChatMessage(geminiMsg)
            }.onFailure { err ->
                toastMessage.emit("Failed to get response: ${err.localizedMessage}")
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatSession("default")
            toastMessage.emit("Chat conversation cleared.")
        }
    }

    // --- Notes Generator & Management ---
    val generatedNoteContent = MutableStateFlow("")
    val activeNoteTitle = MutableStateFlow("")

    fun generateNotes(
        topic: String,
        subject: String,
        grade: String,
        chapter: String,
        difficulty: String,
        language: String,
        isDetailed: Boolean
    ) {
        viewModelScope.launch {
            isAiLoading.value = true
            aiStatusMessage.value = "Gemini is generating structured study notes..."

            val result = repository.generateNotes(
                topic = topic,
                subject = subject,
                grade = grade,
                chapter = chapter,
                difficulty = difficulty,
                language = language,
                isDetailed = isDetailed,
                customKey = customApiKey.value
            )

            isAiLoading.value = false
            aiStatusMessage.value = ""

            result.onSuccess { notesText ->
                generatedNoteContent.value = notesText
                activeNoteTitle.value = "$topic ($subject)"
                // Automatically save into Notes database
                val note = NoteItem(
                    title = activeNoteTitle.value,
                    topic = topic,
                    subject = subject,
                    grade = grade,
                    chapter = chapter,
                    difficulty = difficulty,
                    language = language,
                    content = notesText,
                    isDetailed = isDetailed
                )
                repository.insertNote(note)
                toastMessage.emit("Notes generated & saved successfully!")
            }.onFailure { err ->
                toastMessage.emit("Error generating notes: ${err.localizedMessage}")
            }
        }
    }

    fun saveEditedNote(note: NoteItem) {
        viewModelScope.launch {
            if (note.id == 0L) {
                repository.insertNote(note)
            } else {
                repository.updateNote(note)
            }
            toastMessage.emit("Note saved.")
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
            toastMessage.emit("Note deleted.")
        }
    }

    // --- Quiz Session & Generation ---
    val quizSession = MutableStateFlow<QuizSessionState?>(null)

    fun generateQuiz(
        subject: String,
        topic: String,
        count: Int,
        difficulty: String,
        questionType: String
    ) {
        viewModelScope.launch {
            isAiLoading.value = true
            aiStatusMessage.value = "Gemini is generating $questionType quiz questions..."

            val result = repository.generateQuiz(
                subject = subject,
                topic = topic,
                questionCount = count,
                difficulty = difficulty,
                questionType = questionType,
                customKey = customApiKey.value
            )

            isAiLoading.value = false
            aiStatusMessage.value = ""

            result.onSuccess { questions ->
                quizSession.value = QuizSessionState(
                    quizTitle = "$topic Quiz",
                    subject = subject,
                    questions = questions,
                    currentIndex = 0,
                    selectedOption = null,
                    isAnswerSubmitted = false,
                    score = 0,
                    isQuizFinished = false
                )
            }.onFailure { err ->
                toastMessage.emit("Quiz generation error: ${err.localizedMessage}")
            }
        }
    }

    fun selectQuizOption(index: Int) {
        val current = quizSession.value ?: return
        if (!current.isAnswerSubmitted) {
            quizSession.value = current.copy(selectedOption = index)
        }
    }

    fun submitQuizAnswer() {
        val current = quizSession.value ?: return
        val currentQuestion = current.questions.getOrNull(current.currentIndex) ?: return
        val isCorrect = current.selectedOption == currentQuestion.correctIndex
        val newScore = if (isCorrect) current.score + 1 else current.score

        quizSession.value = current.copy(
            isAnswerSubmitted = true,
            score = newScore
        )
    }

    fun nextQuizQuestion() {
        val current = quizSession.value ?: return
        val nextIdx = current.currentIndex + 1
        if (nextIdx < current.questions.size) {
            quizSession.value = current.copy(
                currentIndex = nextIdx,
                selectedOption = null,
                isAnswerSubmitted = false
            )
        } else {
            // Finish quiz and save results in Room
            quizSession.value = current.copy(isQuizFinished = true)
            viewModelScope.launch {
                val questionsArray = JSONArray()
                for (q in current.questions) {
                    val qObj = org.json.JSONObject()
                    qObj.put("question", q.question)
                    qObj.put("correctIndex", q.correctIndex)
                    qObj.put("explanation", q.explanation)
                    questionsArray.put(qObj)
                }
                val quizItem = QuizItem(
                    title = current.quizTitle,
                    subject = current.subject,
                    topic = current.quizTitle,
                    questionsJson = questionsArray.toString(),
                    score = current.score,
                    totalQuestions = current.questions.size,
                    isCompleted = true
                )
                repository.insertQuiz(quizItem)
            }
        }
    }

    fun retryQuiz() {
        val current = quizSession.value ?: return
        quizSession.value = current.copy(
            currentIndex = 0,
            selectedOption = null,
            isAnswerSubmitted = false,
            score = 0,
            isQuizFinished = false
        )
    }

    // --- Study Material Analysis ---
    val analyzedMaterialResult = MutableStateFlow("")
    val studyMaterialInputText = MutableStateFlow("")

    fun analyzeStudyMaterial(
        text: String,
        action: String,
        bitmap: Bitmap? = null
    ) {
        viewModelScope.launch {
            isAiLoading.value = true
            aiStatusMessage.value = "Gemini is analyzing study material ($action)..."

            val result = repository.analyzeMaterial(
                content = text,
                action = action,
                bitmap = bitmap,
                customKey = customApiKey.value
            )

            isAiLoading.value = false
            aiStatusMessage.value = ""

            result.onSuccess { output ->
                analyzedMaterialResult.value = output
                toastMessage.emit("$action complete!")
            }.onFailure { err ->
                toastMessage.emit("Analysis failed: ${err.localizedMessage}")
            }
        }
    }

    // --- Study Tools ---
    val studyToolOutput = MutableStateFlow("")

    fun runTool(toolName: String, input: String) {
        if (input.isBlank()) return
        viewModelScope.launch {
            isAiLoading.value = true
            aiStatusMessage.value = "Running $toolName..."

            val result = repository.runStudyTool(toolName, input, customApiKey.value)

            isAiLoading.value = false
            aiStatusMessage.value = ""

            result.onSuccess { out ->
                studyToolOutput.value = out
            }.onFailure { err ->
                toastMessage.emit("Tool error: ${err.localizedMessage}")
            }
        }
    }

    // --- File Exports (PDF, DOCX, TXT) & Management ---
    fun exportPdf(
        title: String,
        subtitle: String,
        content: String,
        customFileName: String? = null,
        onSuccess: ((StudyFileItem) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val item = repository.exportAndSavePdf(title, subtitle, content, "PDFs", customFileName)
                toastMessage.emit("PDF created: ${item.name}")
                onSuccess?.invoke(item)
            } catch (e: Exception) {
                toastMessage.emit("PDF creation failed: ${e.localizedMessage}")
            }
        }
    }

    fun exportDocx(
        title: String,
        subtitle: String,
        content: String,
        customFileName: String? = null,
        onSuccess: ((StudyFileItem) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val item = repository.exportAndSaveDocx(title, subtitle, content, "Documents", customFileName)
                toastMessage.emit("Word Document created: ${item.name}")
                onSuccess?.invoke(item)
            } catch (e: Exception) {
                toastMessage.emit("Document creation failed: ${e.localizedMessage}")
            }
        }
    }

    fun exportTxt(
        title: String,
        content: String,
        customFileName: String? = null,
        onSuccess: ((StudyFileItem) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val item = repository.exportAndSaveTxt(title, content, "Notes", customFileName)
                toastMessage.emit("Text file created: ${item.name}")
                onSuccess?.invoke(item)
            } catch (e: Exception) {
                toastMessage.emit("Failed to create file: ${e.localizedMessage}")
            }
        }
    }

    fun writeToUri(uri: Uri, isPdf: Boolean, title: String, subtitle: String, content: String) {
        viewModelScope.launch {
            try {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out: OutputStream ->
                    if (isPdf) {
                        com.example.data.exporter.PdfExporter.createPdf(title, subtitle, content, out)
                    } else {
                        com.example.data.exporter.DocxExporter.createDocx(title, subtitle, content, out)
                    }
                }
                toastMessage.emit("File saved directly to device storage!")
            } catch (e: Exception) {
                toastMessage.emit("Failed to save to storage: ${e.localizedMessage}")
            }
        }
    }

    fun renameFile(item: StudyFileItem, newName: String) {
        viewModelScope.launch {
            try {
                repository.renameFile(item, newName)
                toastMessage.emit("File renamed.")
            } catch (e: Exception) {
                toastMessage.emit("Rename failed: ${e.localizedMessage}")
            }
        }
    }

    fun deleteFile(item: StudyFileItem) {
        viewModelScope.launch {
            try {
                repository.deleteFile(item)
                toastMessage.emit("File deleted.")
            } catch (e: Exception) {
                toastMessage.emit("Delete failed: ${e.localizedMessage}")
            }
        }
    }

    fun importFileFromUri(uri: Uri, name: String, fileType: String, folder: String) {
        viewModelScope.launch {
            try {
                repository.registerImportedFile(name, fileType, folder, uri)
                toastMessage.emit("File imported to $folder!")
            } catch (e: Exception) {
                toastMessage.emit("Import failed: ${e.localizedMessage}")
            }
        }
    }
}
