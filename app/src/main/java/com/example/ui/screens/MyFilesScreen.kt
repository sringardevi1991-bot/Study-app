package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.StudyFileItem
import com.example.ui.components.EmptyStateView
import com.example.ui.viewmodel.StudyViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFilesScreen(
    viewModel: StudyViewModel
) {
    val context = LocalContext.current
    val allFiles by viewModel.files.collectAsState()

    var selectedFolder by remember { mutableStateOf("All") }
    var renameDialogFile by remember { mutableStateOf<StudyFileItem?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var fileDetailItem by remember { mutableStateOf<StudyFileItem?>(null) }

    val folders = listOf("All", "PDFs", "Documents", "Notes", "Quizzes", "Study Material")

    val filteredFiles = remember(allFiles, selectedFolder) {
        if (selectedFolder == "All") allFiles
        else allFiles.filter { it.folder.equals(selectedFolder, ignoreCase = true) }
    }

    // System File Picker for importing existing study documents
    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast("/") ?: "Imported_Study_Doc"
            val type = when {
                name.endsWith(".pdf", ignoreCase = true) -> "PDF"
                name.endsWith(".docx", ignoreCase = true) || name.endsWith(".doc", ignoreCase = true) -> "DOCX"
                name.endsWith(".txt", ignoreCase = true) -> "TXT"
                else -> "FILE"
            }
            val targetFolder = if (type == "PDF") "PDFs" else if (type == "DOCX") "Documents" else "Study Material"
            viewModel.importFileFromUri(uri, name, type, targetFolder)
        }
    }

    // Rename Dialog
    if (renameDialogFile != null) {
        AlertDialog(
            onDismissRequest = { renameDialogFile = null },
            title = { Text("Rename File") },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    label = { Text("New File Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val item = renameDialogFile
                        if (item != null && renameInputText.isNotBlank()) {
                            viewModel.renameFile(item, renameInputText.trim())
                        }
                        renameDialogFile = null
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogFile = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // File Details & Actions Bottom Sheet / Dialog
    if (fileDetailItem != null) {
        val item = fileDetailItem!!
        val file = File(item.filePath)
        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(item.createdAt))
        val sizeKb = (item.sizeBytes / 1024).coerceAtLeast(1)

        AlertDialog(
            onDismissRequest = { fileDetailItem = null },
            title = {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📁 Folder: ${item.folder}", style = MaterialTheme.typography.bodySmall)
                    Text("📄 Type: ${item.fileType}", style = MaterialTheme.typography.bodySmall)
                    Text("💾 Size: $sizeKb KB", style = MaterialTheme.typography.bodySmall)
                    Text("🕒 Date: $dateStr", style = MaterialTheme.typography.bodySmall)
                    Text("🏷️ Source: ${if (item.source == "AI_GENERATED") "Created by Gemini AI" else "Imported by User"}", style = MaterialTheme.typography.bodySmall)
                    Text("📍 Path: ${item.filePath}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.outline)

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                    val mime = if (item.fileType == "PDF") "application/pdf"
                                    else if (item.fileType == "DOCX") "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                    else "text/plain"
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, mime)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open with app"))
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Open")
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                    val mime = if (item.fileType == "PDF") "application/pdf"
                                    else if (item.fileType == "DOCX") "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                    else "text/plain"
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = mime
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share file"))
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Share")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { fileDetailItem = null }) {
                    Text("Close")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header & Import Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Study Files",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${allFiles.size} total items created & imported",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { fileImportLauncher.launch("*/*") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("import_file_button")
            ) {
                Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import File", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Folder Chips Row
        ScrollableTabRow(
            selectedTabIndex = folders.indexOf(selectedFolder).coerceAtLeast(0),
            edgePadding = 0.dp,
            divider = {}
        ) {
            folders.forEach { folder ->
                Tab(
                    selected = selectedFolder == folder,
                    onClick = { selectedFolder = folder },
                    text = { Text(folder) },
                    modifier = Modifier.testTag("folder_tab_${folder.lowercase().replace(" ", "_")}")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // File List
        if (filteredFiles.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.FolderOpen,
                title = "No files in $selectedFolder",
                description = "Create a PDF, Word document, or note from the Home dashboard to view here.",
                actionButtonText = "Import a File",
                onActionClick = { fileImportLauncher.launch("*/*") }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredFiles, key = { it.id }) { item ->
                    val file = File(item.filePath)
                    val sizeKb = (item.sizeBytes / 1024).coerceAtLeast(1)
                    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(item.createdAt))

                    val (icon, badgeColor) = when (item.fileType) {
                        "PDF" -> Icons.Default.PictureAsPdf to Color(0xFFDC2626)
                        "DOCX" -> Icons.Default.Article to Color(0xFF2563EB)
                        "TXT" -> Icons.Default.Description to Color(0xFFD97706)
                        else -> Icons.Default.InsertDriveFile to Color(0xFF16A34A)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { fileDetailItem = item }
                            .testTag("file_item_${item.id}"),
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
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = item.fileType,
                                    tint = badgeColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${item.folder} • $sizeKb KB • $dateStr",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Actions: Rename, Share, Delete
                            IconButton(
                                onClick = {
                                    renameDialogFile = item
                                    renameInputText = item.name
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.DriveFileRenameOutline, contentDescription = "Rename", modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = {
                                    try {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                        val mime = if (item.fileType == "PDF") "application/pdf"
                                        else if (item.fileType == "DOCX") "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                        else "text/plain"
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = mime
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share file"))
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = { viewModel.deleteFile(item) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
