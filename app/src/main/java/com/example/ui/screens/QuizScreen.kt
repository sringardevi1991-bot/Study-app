package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: StudyViewModel,
    onExportPdf: (String, String) -> Unit
) {
    val quizState by viewModel.quizSession.collectAsState()
    val defaultSubject by viewModel.defaultSubject.collectAsState()

    var subject by remember { mutableStateOf(defaultSubject) }
    var topic by remember { mutableStateOf("") }
    var questionType by remember { mutableStateOf("MCQ") }
    var difficulty by remember { mutableStateOf("Medium") }
    var questionCount by remember { mutableIntStateOf(5) }

    val currentSession = quizState

    if (currentSession == null) {
        // QUIZ GENERATOR FORM
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AI Quiz Generator",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Test your grasp on any topic. Gemini creates instant interactive quizzes with step-by-step explanations for each question.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    )
                }
            }

            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Chapter / Topic Name *") },
                placeholder = { Text("e.g. Periodic Table, Quadratic Equations, French Revolution") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_input_topic"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quiz_input_subject"),
                shape = RoundedCornerShape(12.dp)
            )

            // Question Type selector
            Column {
                Text(
                    text = "Question Type:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("MCQ", "True/False", "Fill in blanks").forEach { type ->
                        FilterChip(
                            selected = questionType == type,
                            onClick = { questionType = type },
                            label = { Text(type, fontSize = 11.sp) },
                            modifier = Modifier.testTag("chip_type_${type.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }

            // Difficulty selector
            Column {
                Text(
                    text = "Difficulty Level:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Easy", "Medium", "Hard").forEach { diff ->
                        FilterChip(
                            selected = difficulty == diff,
                            onClick = { difficulty = diff },
                            label = { Text(diff, fontSize = 11.sp) },
                            modifier = Modifier.testTag("chip_diff_${diff.lowercase()}")
                        )
                    }
                }
            }

            // Number of Questions
            Column {
                Text(
                    text = "Number of Questions: $questionCount",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Slider(
                    value = questionCount.toFloat(),
                    onValueChange = { questionCount = it.toInt() },
                    valueRange = 3f..10f,
                    steps = 6,
                    modifier = Modifier.testTag("quiz_slider_count")
                )
            }

            // Start Quiz Button
            Button(
                onClick = {
                    val t = topic.ifBlank { "General Fundamentals of $subject" }
                    viewModel.generateQuiz(
                        subject = subject,
                        topic = t,
                        count = questionCount,
                        difficulty = difficulty,
                        questionType = questionType
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_quiz_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate & Start Quiz", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    } else if (currentSession.isQuizFinished) {
        // FINAL RESULTS SCREEN
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🏆", fontSize = 42.sp)
            }

            Text(
                text = "Quiz Completed!",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            val percentage = (currentSession.score * 100) / currentSession.questions.size.coerceAtLeast(1)
            Text(
                text = "You scored ${currentSession.score} out of ${currentSession.questions.size} ($percentage%)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Text(
                text = when {
                    percentage >= 80 -> "Outstanding performance! You have mastered this concept."
                    percentage >= 60 -> "Good effort! Review the explanations below to strengthen your understanding."
                    else -> "Keep practicing! Review each answer below to improve your score."
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.retryQuiz() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry Quiz")
                }

                Button(
                    onClick = { viewModel.quizSession.value = null },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("New Quiz")
                }
            }

            // Export to PDF
            OutlinedButton(
                onClick = {
                    val content = buildString {
                        append("Quiz Results: ${currentSession.quizTitle}\n")
                        append("Score: ${currentSession.score}/${currentSession.questions.size} ($percentage%)\n\n")
                        currentSession.questions.forEachIndexed { i, q ->
                            append("Q${i + 1}: ${q.question}\n")
                            q.options.forEachIndexed { oIdx, opt ->
                                val marker = if (oIdx == q.correctIndex) " [CORRECT]" else ""
                                append("  • $opt$marker\n")
                            }
                            append("Explanation: ${q.explanation}\n\n")
                        }
                    }
                    onExportPdf("${currentSession.quizTitle} Results", content)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Quiz & Answers to PDF")
            }

            Divider()

            // Review all questions
            Text(
                text = "Question Review:",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth()
            )

            currentSession.questions.forEachIndexed { idx, q ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Q${idx + 1}. ${q.question}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        q.options.forEachIndexed { oIdx, option ->
                            val isCorrect = oIdx == q.correctIndex
                            val bg = if (isCorrect) Color(0xFFDCFCE7) else Color.Transparent
                            val txtColor = if (isCorrect) Color(0xFF166534) else MaterialTheme.colorScheme.onSurfaceVariant
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(bg, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${('A'.code + oIdx).toChar()}) $option",
                                    fontSize = 13.sp,
                                    color = txtColor,
                                    fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isCorrect) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Correct",
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 Explanation: ${q.explanation}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    } else {
        // ACTIVE QUIZ (ONE QUESTION AT A TIME)
        val currentQuestion = currentSession.questions.getOrNull(currentSession.currentIndex)
        if (currentQuestion == null) {
            viewModel.quizSession.value = null
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Question X of Y + Current Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question ${currentSession.currentIndex + 1} of ${currentSession.questions.size}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Score: ${currentSession.score}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Progress Indicator
            LinearProgressIndicator(
                progress = { (currentSession.currentIndex + 1) / currentSession.questions.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Question Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = currentQuestion.question,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 24.sp
                        )
                    )
                }
            }

            // Options list
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                currentQuestion.options.forEachIndexed { index, option ->
                    val isSelected = currentSession.selectedOption == index
                    val isSubmitted = currentSession.isAnswerSubmitted
                    val isCorrect = index == currentQuestion.correctIndex

                    val containerColor = when {
                        isSubmitted && isCorrect -> Color(0xFFDCFCE7) // Light Green
                        isSubmitted && isSelected && !isCorrect -> Color(0xFFFEE2E2) // Light Red
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }

                    val borderColor = when {
                        isSubmitted && isCorrect -> Color(0xFF22C55E)
                        isSubmitted && isSelected && !isCorrect -> Color(0xFFEF4444)
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                            .clickable(enabled = !isSubmitted) {
                                viewModel.selectQuizOption(index)
                            }
                            .testTag("quiz_option_$index"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = containerColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(borderColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${('A'.code + index).toChar()}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (isSubmitted) {
                                if (isCorrect) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
                                } else if (isSelected) {
                                    Icon(imageVector = Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFDC2626))
                                }
                            }
                        }
                    }
                }
            }

            // Explanation Card (revealed after submission)
            if (currentSession.isAnswerSubmitted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentSession.selectedOption == currentQuestion.correctIndex)
                            Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (currentSession.selectedOption == currentQuestion.correctIndex) "🎉 Correct Answer!" else "❌ Incorrect",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (currentSession.selectedOption == currentQuestion.correctIndex)
                                    Color(0xFF166534) else Color(0xFF991B1B)
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "💡 Explanation:\n${currentQuestion.explanation}",
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Button: Submit Answer OR Next Question
            if (!currentSession.isAnswerSubmitted) {
                Button(
                    onClick = { viewModel.submitQuizAnswer() },
                    enabled = currentSession.selectedOption != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("quiz_submit_answer"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Submit Answer", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            } else {
                Button(
                    onClick = { viewModel.nextQuizQuestion() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("quiz_next_question"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (currentSession.currentIndex + 1 < currentSession.questions.size) "Next Question →" else "Finish Quiz & View Results",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
