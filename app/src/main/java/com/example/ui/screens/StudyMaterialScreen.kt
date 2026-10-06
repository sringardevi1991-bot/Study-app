package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyMaterialScreen(
    viewModel: StudyViewModel,
    onExportPdf: (String, String) -> Unit,
    onExportDocx: (String, String) -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedDocName by remember { mutableStateOf<String?>(null) }

    val analyzedResult by viewModel.analyzedMaterialResult.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiStatusMessage by viewModel.aiStatusMessage.collectAsState()

    // 1. Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            selectedBitmap = bitmap
            selectedDocName = "Camera Photo of Notes"
        }
    }

    // 2. Image Picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                selectedBitmap = bitmap
                selectedDocName = "Uploaded Image of Notes"
            } catch (_: Exception) {}
        }
    }

    // 3. Document / Text File Picker
    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader().readText()
                    inputText = text.take(6000) // Read initial content
                    selectedDocName = uri.lastPathSegment ?: "Document"
                }
            } catch (_: Exception) {
                selectedDocName = "Imported Study File"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Analyze Study Material",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Snap a photo of your textbook, upload a PDF/doc, or paste material to generate summaries, key points, flashcards, or practice questions.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                )
            }
        }

        // Upload and Camera Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { cameraLauncher.launch(null) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("scan_camera_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Take Photo", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("upload_image_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Upload Image", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { docPicker.launch("*/*") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("upload_doc_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pick Doc/PDF", fontSize = 12.sp)
            }
        }

        // Preview of attached image or document
        if (selectedBitmap != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedDocName ?: "Captured Notes Photo",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { selectedBitmap = null }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        bitmap = selectedBitmap!!.asImageBitmap(),
                        contentDescription = "Notes photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }

        // Text input for Study Material
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Study Material Text / Chapter Content / Questions") },
            placeholder = { Text("Paste textbook excerpt, lecture notes, or ask specific doubts...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .testTag("study_material_text_input"),
            shape = RoundedCornerShape(14.dp)
        )

        // Actions Grid
        Text(
            text = "Select Analysis Action:",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        val actions = listOf(
            Pair("Summarize", Icons.Default.Summarize),
            Pair("Key Points", Icons.Default.FormatListBulleted),
            Pair("Revision Notes", Icons.Default.MenuBook),
            Pair("Flashcards", Icons.Default.Style),
            Pair("Practice Questions", Icons.Default.HelpOutline)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            actions.chunked(2).forEach { rowPair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowPair.forEach { (actionName, icon) ->
                        Button(
                            onClick = {
                                viewModel.analyzeStudyMaterial(
                                    text = inputText.ifBlank { "Please analyze this study material comprehensively." },
                                    action = actionName,
                                    bitmap = selectedBitmap
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("action_${actionName.lowercase().replace(" ", "_")}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(actionName, fontSize = 12.sp)
                        }
                    }
                    if (rowPair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Output Result Card
        if (analyzedResult.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("analysis_result_card"),
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
                            text = "Analysis Result",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Study Analysis", analyzedResult))
                                }
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = analyzedResult,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // Export Buttons for Analyzed Material
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onExportPdf("Study Material Summary", analyzedResult) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export PDF", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { onExportDocx("Study Material Document", analyzedResult) },
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
}
