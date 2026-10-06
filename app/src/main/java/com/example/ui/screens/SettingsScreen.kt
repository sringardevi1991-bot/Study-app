package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: StudyViewModel
) {
    val grade by viewModel.defaultGrade.collectAsState()
    val subject by viewModel.defaultSubject.collectAsState()
    val language by viewModel.defaultLanguage.collectAsState()
    val level by viewModel.defaultExplanationLevel.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()
    val customKey by viewModel.customApiKey.collectAsState()
    val files by viewModel.files.collectAsState()

    var showKeyDialog by remember { mutableStateOf(false) }
    var tempKey by remember { mutableStateOf(customKey) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val totalSizeKb = remember(files) {
        files.sumOf { it.sizeBytes } / 1024
    }

    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = { Text("Gemini API Key") },
            text = {
                Column {
                    Text(
                        text = "By default, StudyMate AI uses the GEMINI_API_KEY injected automatically via the Secrets panel in AI Studio.\n\nYou may optionally enter an override key here:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.customApiKey.value = tempKey.trim()
                        showKeyDialog = false
                    }
                ) {
                    Text("Save Key")
                }
            },
            dismissButton = {
                TextButton(onClick = { showKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear All Chat & Search History?") },
            text = { Text("This will clear your conversation sessions with Gemini.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearChat()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings & Preferences",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        // 1. Appearance / Theme
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Mode", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        Text("Toggle between light and dark themes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Switch(
                        checked = isDark,
                        onCheckedChange = { viewModel.isDarkMode.value = it },
                        modifier = Modifier.testTag("toggle_dark_mode")
                    )
                }
            }
        }

        // 2. Study Tutor Preferences
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Academic Preferences", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                // Default Grade
                Column {
                    Text("Default Class / Grade:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Class 9", "Class 10", "Class 11", "Class 12", "College").forEach { g ->
                            FilterChip(
                                selected = grade == g,
                                onClick = { viewModel.defaultGrade.value = g },
                                label = { Text(g, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Default Subject
                Column {
                    Text("Default Subject:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Science", "Maths", "Social", "English", "Physics").forEach { s ->
                            FilterChip(
                                selected = subject == s,
                                onClick = { viewModel.defaultSubject.value = s },
                                label = { Text(s, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Default Explanation Level
                Column {
                    Text("Default Explanation Level:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Beginner", "Simple", "Detailed").forEach { l ->
                            FilterChip(
                                selected = level == l,
                                onClick = { viewModel.defaultExplanationLevel.value = l },
                                label = { Text(l, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Language
                Column {
                    Text("Default Language:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("English", "Hindi", "Hinglish").forEach { lang ->
                            FilterChip(
                                selected = language == lang,
                                onClick = { viewModel.defaultLanguage.value = lang },
                                label = { Text(lang, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // 3. AI & API Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Gemini AI Configuration", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = "Powered by Google Gemini 3.5 Flash for academic tutoring, step-by-step reasoning, notes, and quiz generation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        tempKey = customKey
                        showKeyDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (customKey.isNotBlank()) "Change Custom API Key" else "Set Custom Gemini API Key", fontSize = 12.sp)
                }
            }
        }

        // 4. Storage Management
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Storage Management", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text("App Storage Used: $totalSizeKb KB across ${files.size} study files", style = MaterialTheme.typography.bodySmall)

                OutlinedButton(
                    onClick = { showClearHistoryDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Chat History", fontSize = 12.sp)
                }
            }
        }

        // 5. About App
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("StudyMate AI", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                Text("Version 1.0 • Built with Google Gemini", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "A student-friendly study assistant that creates structured notes, interactive quizzes, conversational tutoring in English/Hindi/Hinglish, and downloadable PDF & Word documents.",
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
