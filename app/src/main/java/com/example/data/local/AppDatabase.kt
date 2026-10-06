package com.example.data.local

import android.content.Context
import androidx.room.*
import com.example.data.model.ChatMessageItem
import com.example.data.model.NoteItem
import com.example.data.model.QuizItem
import com.example.data.model.StudyFileItem
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteItem>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteItem?

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR subject LIKE '%' || :query || '%'")
    fun searchNotes(query: String): Flow<List<NoteItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteItem): Long

    @Update
    suspend fun updateNote(note: NoteItem)

    @Delete
    suspend fun deleteNote(note: NoteItem)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
}

@Dao
interface QuizDao {
    @Query("SELECT * FROM quizzes ORDER BY createdAt DESC")
    fun getAllQuizzes(): Flow<List<QuizItem>>

    @Query("SELECT * FROM quizzes WHERE id = :id")
    suspend fun getQuizById(id: Long): QuizItem?

    @Query("SELECT * FROM quizzes WHERE title LIKE '%' || :query || '%' OR subject LIKE '%' || :query || '%'")
    fun searchQuizzes(query: String): Flow<List<QuizItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizItem): Long

    @Update
    suspend fun updateQuiz(quiz: QuizItem)

    @Delete
    suspend fun deleteQuiz(quiz: QuizItem)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageItem>>

    @Query("SELECT DISTINCT sessionId FROM chat_messages ORDER BY timestamp DESC")
    fun getSessionIds(): Flow<List<String>>

    @Query("SELECT * FROM chat_messages WHERE message LIKE '%' || :query || '%'")
    fun searchMessages(query: String): Flow<List<ChatMessageItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageItem): Long

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
}

@Dao
interface StudyFileDao {
    @Query("SELECT * FROM study_files ORDER BY createdAt DESC")
    fun getAllFiles(): Flow<List<StudyFileItem>>

    @Query("SELECT * FROM study_files WHERE folder = :folder ORDER BY createdAt DESC")
    fun getFilesByFolder(folder: String): Flow<List<StudyFileItem>>

    @Query("SELECT * FROM study_files WHERE name LIKE '%' || :query || '%'")
    fun searchFiles(query: String): Flow<List<StudyFileItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: StudyFileItem): Long

    @Update
    suspend fun updateFile(file: StudyFileItem)

    @Delete
    suspend fun deleteFile(file: StudyFileItem)

    @Query("DELETE FROM study_files WHERE id = :id")
    suspend fun deleteFileById(id: Long)
}

@Database(
    entities = [NoteItem::class, QuizItem::class, ChatMessageItem::class, StudyFileItem::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun quizDao(): QuizDao
    abstract fun chatDao(): ChatDao
    abstract fun studyFileDao(): StudyFileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "studymate_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
