package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.viewmodel.StudyViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfDocCreatorScreen(
    viewModel: StudyViewModel,
    initialIsPdf: Boolean = true,
    initialTitle: String = "",
    initialContent: String = ""
) {
    val context = LocalContext.current
    var isPdf by remember { mutableStateOf(initialIsPdf) }
    var title by remember { mutableStateOf(initialTitle.ifBlank { if (initialIsPdf) "Study Notes" else "Assignment Document" }) }
    var subtitle by remember { mutableStateOf("Subject: Science | Class: 10 | StudyMate AI") }
    var content by remember {
        mutableStateOf(
            initialContent.ifBlank {
                """
                # 1. Overview & Objectives
                • Core concept and learning goals.
                • Detailed study breakdown.

                # 2. Key Definitions
                • Term 1: Definition and scientific context.
                • Term 2: Fundamental formula and units.

                # 3. Practice Questions & Examples
                • Example Problem 1: Step-by-step solution.
                • Quick Review Summary: Bulleted takeaways for exam preparation.
                """.trimIndent()
            }
        )
    }

    var lastSavedFile by remember { mutableStateOf<File?>(null) }
    var lastSavedName by remember { mutableStateOf<String?>(null) }

    // System File Picker for direct user-selected location (SAF)
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            if (isPdf) "application/pdf" else "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.writeToUri(uri, isPdf, title, subtitle, content)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mode Selector: PDF vs Word (.docx)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = isPdf,
                onClick = { isPdf = true },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("PDF Creator")
            }
            SegmentedButton(
                selected = !isPdf,
                onClick = { isPdf = false },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Icon(imageVector = Icons.Default.Article, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Word (.docx) Creator")
            }
        }

        // Title and Subtitle inputs
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Document Title *") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("doc_creator_title"),
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = subtitle,
            onValueChange = { subtitle = it },
            label = { Text("Subtitle / Subject / Class / Student Info") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("doc_creator_subtitle"),
            shape = RoundedCornerShape(12.dp)
        )

        // Template Insertion Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Templates:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            listOf("Revision Notes", "Assignment", "Question Paper", "Essay").forEach { templ ->
                SuggestionChip(
                    onClick = {
                        content = when (templ) {
                            "Revision Notes" -> "# Quick Revision Notes\n• Key Point 1\n• Formula: x = y + z\n• Exam Tip: Remember to write SI units."
                            "Assignment" -> "# Academic Assignment\n• Question 1: Explain the law.\n• Answer: Step-by-step reasoning.\n• Conclusion: Final remark."
                            "Question Paper" -> "# Mock Test Paper\nTime: 1 Hour | Max Marks: 25\n\nSection A: 1 Mark Questions\n1. Define velocity.\n2. State Ohm's law.\n\nSection B: 3 Mark Questions\n3. Derive kinetic energy formula."
                            else -> "# Essay Outline\n\n## Introduction\nBrief background and thesis statement.\n\n## Body Paragraphs\n1. Main evidence\n2. Real-world application\n\n## Conclusion\nSummary of insights."
                        }
                    },
                    label = { Text(templ, fontSize = 11.sp) }
                )
            }
        }

        // Content Area
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("Document Content (Markdown headings '#' supported)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .testTag("doc_creator_content"),
            shape = RoundedCornerShape(12.dp)
        )

        // Creation & Storage Actions
        Text(
            text = "Storage & Export Options:",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        // 1. Direct Save using Android System SAF File Picker
        Button(
            onClick = {
                val sanitized = title.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
                val ext = if (isPdf) ".pdf" else ".docx"
                createDocLauncher.launch("$sanitized$ext")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_to_device_saf_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.SaveAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Choose Location & Save to Device Storage", fontWeight = FontWeight.Bold)
        }

        // 2. Save to App Library (My Files)
        OutlinedButton(
            onClick = {
                if (isPdf) {
                    viewModel.exportPdf(title, subtitle, content) { item ->
                        lastSavedFile = File(item.filePath)
                        lastSavedName = item.name
                    }
                } else {
                    viewModel.exportDocx(title, subtitle, content) { item ->
                        lastSavedFile = File(item.filePath)
                        lastSavedName = item.name
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_to_app_files_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.FolderSpecial, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save to 'My Files' in App")
        }

        // Success Card with Open / Share Actions
        if (lastSavedFile != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Created Successfully: ${lastSavedName ?: ""}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534),
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val file = lastSavedFile
                                if (file != null) {
                                    try {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, if (isPdf) "application/pdf" else "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Open Document"))
                                    } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Open File")
                        }

                        OutlinedButton(
                            onClick = {
                                val file = lastSavedFile
                                if (file != null) {
                                    try {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = if (isPdf) "application/pdf" else "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share Document"))
                                    } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Share File")
                        }
                    }
                }
            }
        }
    }
}
