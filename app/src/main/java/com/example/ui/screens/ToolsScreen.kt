package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StudyViewModel

data class ToolDefinition(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val inputPlaceholder: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: StudyViewModel,
    onExportPdf: (String, String) -> Unit
) {
    val context = LocalContext.current
    val output by viewModel.studyToolOutput.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    val toolList = remember {
        listOf(
            ToolDefinition("ELI10", "Explain Like I'm 10", "Simplifies complex academic topics using friendly metaphors", Icons.Default.ChildCare, "Enter concept to simplify (e.g. Quantum tunneling, Inflation)"),
            ToolDefinition("Formula Finder", "Formula Finder", "Finds equations, variables, and SI units for any topic", Icons.Default.Functions, "Enter topic (e.g. Electromagnetism, Circles)"),
            ToolDefinition("Flashcards", "Flashcard Generator", "Generates question-answer revision flashcards", Icons.Default.Style, "Enter chapter or topic for flashcards"),
            ToolDefinition("Question Generator", "Question Generator", "Creates practice questions for exams", Icons.Default.HelpOutline, "Enter subject and chapter"),
            ToolDefinition("Answer Checker", "Answer Checker", "Grades and gives feedback on your written answers", Icons.Default.FactCheck, "Paste question and your answer here"),
            ToolDefinition("Essay Helper", "Essay Helper", "Generates structured thesis and essay outlines", Icons.Default.Article, "Enter essay topic (e.g. Climate change impact)"),
            ToolDefinition("Grammar Helper", "Grammar Helper", "Fixes grammatical errors and elevates academic tone", Icons.Default.Spellcheck, "Paste text to review"),
            ToolDefinition("Translator", "Academic Translation", "Translates study material between English, Hindi & Hinglish", Icons.Default.Translate, "Paste text to translate"),
            ToolDefinition("Vocabulary", "Vocabulary Builder", "Extracts key terminology and definitions with sentences", Icons.Default.MenuBook, "Enter topic or paste text"),
            ToolDefinition("Study Planner", "Study Planner", "Creates day-by-day exam prep schedules", Icons.Default.CalendarToday, "Enter exam date, subjects and hours per day"),
            ToolDefinition("Important Questions", "Important Questions", "High-yield frequently asked board and school exam questions", Icons.Default.Star, "Enter subject and chapter name")
        )
    }

    var selectedTool by remember { mutableStateOf(toolList[0]) }
    var toolInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "AI Study Tools",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "Select a specialized tool to assist your study session:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Tool Selector Carousel / Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(toolList) { tool ->
                val isSelected = selectedTool.id == tool.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedTool = tool }
                        .testTag("tool_card_${tool.id.lowercase().replace(" ", "_")}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tool.title,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Selected Tool Execution Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = selectedTool.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedTool.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = selectedTool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = toolInput,
                    onValueChange = { toolInput = it },
                    placeholder = { Text(selectedTool.inputPlaceholder, fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("tool_input_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val input = toolInput.ifBlank { "Please run ${selectedTool.title} on this study topic." }
                        viewModel.runTool(selectedTool.id, input)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tool_run_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Run ${selectedTool.title}")
                }
            }
        }

        // Result Card
        if (output.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tool_output_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedTool.title} Output",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText(selectedTool.title, output))
                                }
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = output,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { onExportPdf("${selectedTool.title} Notes", output) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Result to PDF")
                    }
                }
            }
        }
    }
}
