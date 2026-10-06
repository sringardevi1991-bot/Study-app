package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.QuickActionCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.ScreenTab
import com.example.ui.viewmodel.StudyViewModel

@Composable
fun HomeScreen(
    viewModel: StudyViewModel,
    onNavigateTo: (ScreenTab) -> Unit,
    onOpenCreatePdf: () -> Unit,
    onOpenCreateDocx: () -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val files by viewModel.files.collectAsState()
    val studyMode by viewModel.studyModeActive.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Greeting Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_greeting_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🎓", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Hello, Student!",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = if (studyMode) "AI Tutor is ready to help you learn" else "Study Assistant Ready",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Ask Gemini Quick Hero Button
                    Button(
                        onClick = { onNavigateTo(ScreenTab.CHAT) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_ask_gemini_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ask Gemini a Question",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // 2. Main Action Cards
        item {
            Text(
                text = "Study Essentials",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickActionCard(
                    title = "Create Notes",
                    subtitle = "Generate structured topic notes & summaries",
                    icon = Icons.Default.EditNote,
                    containerColor = CardNotesBg,
                    contentColor = StudyPrimary,
                    onClick = { onNavigateTo(ScreenTab.NOTES) }
                )

                QuickActionCard(
                    title = "Create Quiz",
                    subtitle = "Test your knowledge with instant AI feedback",
                    icon = Icons.Default.Quiz,
                    containerColor = CardQuizBg,
                    contentColor = Color(0xFFDC2626),
                    onClick = { onNavigateTo(ScreenTab.QUIZ) }
                )

                QuickActionCard(
                    title = "Scan / Upload Study Material",
                    subtitle = "Analyze photos of notes, textbooks & documents",
                    icon = Icons.Default.DocumentScanner,
                    containerColor = CardChatBg,
                    contentColor = Color(0xFF9333EA),
                    onClick = { onNavigateTo(ScreenTab.STUDY_MATERIAL) }
                )

                QuickActionCard(
                    title = "Create PDF",
                    subtitle = "Format & download study notes to printable PDF",
                    icon = Icons.Default.PictureAsPdf,
                    containerColor = CardPdfBg,
                    contentColor = Color(0xFFD97706),
                    onClick = onOpenCreatePdf
                )

                QuickActionCard(
                    title = "Create Word Document",
                    subtitle = "Generate editable .docx assignments & essays",
                    icon = Icons.Default.Article,
                    containerColor = CardDocBg,
                    contentColor = Color(0xFF16A34A),
                    onClick = onOpenCreateDocx
                )
            }
        }

        // 3. Quick Study Tools
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Study Tools",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                TextButton(onClick = { onNavigateTo(ScreenTab.TOOLS) }) {
                    Text("View All (${13})", fontSize = 13.sp)
                }
            }
        }

        item {
            val quickTools = listOf(
                Pair("ELI10", "Explain like I'm 10"),
                Pair("Formula Finder", "Formulas & laws"),
                Pair("Flashcards", "Interactive cards"),
                Pair("Answer Checker", "Check exam answers"),
                Pair("Essay Helper", "Essay outlines")
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickTools) { (name, desc) ->
                    Card(
                        modifier = Modifier
                            .width(150.dp)
                            .clickable { onNavigateTo(ScreenTab.TOOLS) }
                            .testTag("tool_chip_${name.lowercase().replace(" ", "_")}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 4. Recent Notes & Files
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Study Items",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                TextButton(onClick = { onNavigateTo(ScreenTab.FILES) }) {
                    Text("My Files", fontSize = 13.sp)
                }
            }
        }

        if (notes.isEmpty() && files.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No study material yet",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = "Create your first notes or quiz to see them here!",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(notes.take(3)) { note ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateTo(ScreenTab.NOTES) }
                        .testTag("recent_note_${note.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${note.subject} • ${note.grade.ifBlank { "General" }}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
