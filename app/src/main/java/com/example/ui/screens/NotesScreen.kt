package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteItem
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: StudyViewModel,
    onExportPdf: (String, String) -> Unit,
    onExportDocx: (String, String) -> Unit
) {
    val context = LocalContext.current
    val savedNotes by viewModel.notes.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val generatedContent by viewModel.generatedNoteContent.collectAsState()
    val defaultGrade by viewModel.defaultGrade.collectAsState()
    val defaultSubject by viewModel.defaultSubject.collectAsState()
    val defaultLanguage by viewModel.defaultLanguage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(if (generatedContent.isNotBlank()) 0 else 0) }

    // Input States
    var topic by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf(defaultSubject) }
    var grade by remember { mutableStateOf(defaultGrade) }
    var chapter by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("Medium") }
    var language by remember { mutableStateOf(defaultLanguage) }
    var isDetailed by remember { mutableStateOf(false) }

    // Edit/View Dialog State
    var activeNoteToView by remember { mutableStateOf<NoteItem?>(null) }
    var noteSearchQuery by remember { mutableStateOf("") }
    var renameDialogNote by remember { mutableStateOf<NoteItem?>(null) }
    var renameNewTitle by remember { mutableStateOf("") }

    val filteredNotes = remember(savedNotes, noteSearchQuery) {
        if (noteSearchQuery.isBlank()) savedNotes
        else savedNotes.filter {
            it.title.contains(noteSearchQuery, ignoreCase = true) ||
            it.content.contains(noteSearchQuery, ignoreCase = true) ||
            it.subject.contains(noteSearchQuery, ignoreCase = true)
        }
    }

    // Rename Dialog
    if (renameDialogNote != null) {
        AlertDialog(
            onDismissRequest = { renameDialogNote = null },
            title = { Text("Rename Note") },
            text = {
                OutlinedTextField(
                    value = renameNewTitle,
                    onValueChange = { renameNewTitle = it },
                    label = { Text("Note Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val note = renameDialogNote
                        if (note != null && renameNewTitle.isNotBlank()) {
                            viewModel.saveEditedNote(note.copy(title = renameNewTitle.trim()))
                        }
                        renameDialogNote = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogNote = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // View/Edit Note Dialog
    if (activeNoteToView != null) {
        var editTitle by remember { mutableStateOf(activeNoteToView!!.title) }
        var editContent by remember { mutableStateOf(activeNoteToView!!.content) }

        AlertDialog(
            onDismissRequest = { activeNoteToView = null },
            title = {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        label = { Text("Note Content") }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                onExportPdf(editTitle, editContent)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("PDF", fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                onExportDocx(editTitle, editContent)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text("Word", fontSize = 11.sp)
                        }
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, editTitle)
                                    putExtra(Intent.EXTRA_TEXT, "$editTitle\n\n$editContent")
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Note"))
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = activeNoteToView
                        if (current != null) {
                            viewModel.saveEditedNote(current.copy(title = editTitle, content = editContent))
                        }
                        activeNoteToView = null
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeNoteToView = null }) {
                    Text("Close")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Header: Generate vs Saved
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Generate Notes") },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_generate_notes")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Saved Notes (${savedNotes.size})") },
                icon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_saved_notes")
            )
        }

        if (selectedTab == 0) {
            // GENERATE TAB
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "AI Study Notes Generator",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Topic / Concept Name *") },
                    placeholder = { Text("e.g. Newton's Laws of Motion, Photosynthesis, Thermodynamics") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notes_input_topic"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("notes_input_subject"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = grade,
                        onValueChange = { grade = it },
                        label = { Text("Class / Grade") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("notes_input_grade"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = chapter,
                    onValueChange = { chapter = it },
                    label = { Text("Chapter (Optional)") },
                    placeholder = { Text("e.g. Chapter 4: Light Reflection") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Difficulty selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Difficulty:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    listOf("Easy", "Medium", "Hard").forEach { diff ->
                        FilterChip(
                            selected = difficulty == diff,
                            onClick = { difficulty = diff },
                            label = { Text(diff, fontSize = 11.sp) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }

                // Language selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Language:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    listOf("English", "Hindi", "Hinglish").forEach { lang ->
                        FilterChip(
                            selected = language == lang,
                            onClick = { language = lang },
                            label = { Text(lang, fontSize = 11.sp) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }

                // Short vs Detailed Switch
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isDetailed) "Comprehensive Detailed Notes" else "Concise Quick Notes",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isDetailed) "Includes in-depth reasoning, proofs, formulas & examples" else "Summary bullet points & key definitions",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                            )
                        }
                        Switch(
                            checked = isDetailed,
                            onCheckedChange = { isDetailed = it }
                        )
                    }
                }

                // Generate Button
                Button(
                    onClick = {
                        val t = topic.ifBlank { "Fundamental Concepts of $subject" }
                        viewModel.generateNotes(
                            topic = t,
                            subject = subject,
                            grade = grade,
                            chapter = chapter,
                            difficulty = difficulty,
                            language = language,
                            isDetailed = isDetailed
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("generate_notes_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Notes with Gemini", fontWeight = FontWeight.Bold)
                }

                // Result Preview Card (if generated content available)
                if (generatedContent.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generated_notes_card"),
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
                                    text = viewModel.activeNoteTitle.value.ifBlank { "Generated Study Notes" },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Notes", generatedContent))
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = generatedContent,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(12.dp))

                            // Action buttons: Export to PDF & Word
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val t = viewModel.activeNoteTitle.value.ifBlank { "Study Notes" }
                                        onExportPdf(t, generatedContent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export PDF", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        val t = viewModel.activeNoteTitle.value.ifBlank { "Study Document" }
                                        onExportDocx(t, generatedContent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                ) {
                                    Icon(imageVector = Icons.Default.Article, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export Word", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // SAVED NOTES TAB
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = noteSearchQuery,
                    onValueChange = { noteSearchQuery = it },
                    placeholder = { Text("Search saved notes...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_saved_notes"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredNotes.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.MenuBook,
                        title = "No notes found",
                        description = "Generate your first study notes in the 'Generate Notes' tab!",
                        actionButtonText = "Create Notes Now",
                        onActionClick = { selectedTab = 0 }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeNoteToView = note }
                                    .testTag("saved_note_item_${note.id}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = note.title,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${note.subject} • ${note.grade.ifBlank { "Class 10" }} • ${note.difficulty}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row {
                                            IconButton(
                                                onClick = {
                                                    renameDialogNote = note
                                                    renameNewTitle = note.title
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DriveFileRenameOutline,
                                                    contentDescription = "Rename",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    onExportPdf(note.title, note.content)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PictureAsPdf,
                                                    contentDescription = "PDF",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteNote(note.id) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = note.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
