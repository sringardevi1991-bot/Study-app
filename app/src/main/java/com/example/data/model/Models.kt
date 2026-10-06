package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val topic: String,
    val subject: String,
    val grade: String = "",
    val chapter: String = "",
    val difficulty: String = "Medium",
    val language: String = "English",
    val content: String,
    val isDetailed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quizzes")
data class QuizItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subject: String,
    val topic: String,
    val difficulty: String = "Medium",
    val questionType: String = "MCQ",
    val questionsJson: String,
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class QuizQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    var selectedIndex: Int? = null
)

@Entity(tableName = "chat_messages")
data class ChatMessageItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String = "default",
    val isUser: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val explanationLevel: String = "Simple",
    val studyMode: Boolean = true
)

@Entity(tableName = "study_files")
data class StudyFileItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val fileType: String, // PDF, DOCX, TXT, IMAGE
    val folder: String, // Notes, PDFs, Documents, Quizzes, Study Material
    val filePath: String,
    val sizeBytes: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val source: String = "AI_GENERATED" // AI_GENERATED, USER_IMPORTED, CAMERA
)

data class Flashcard(
    val question: String,
    val answer: String
)

data class StudyToolResult(
    val toolTitle: String,
    val input: String,
    val output: String,
    val timestamp: Long = System.currentTimeMillis()
)
