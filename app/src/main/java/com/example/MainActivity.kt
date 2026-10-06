package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AiLoadingDialog
import com.example.ui.components.StudyBottomNavigationBar
import com.example.ui.components.StudyTopAppBar
import com.example.ui.screens.*
import com.example.ui.theme.StudyMateTheme
import com.example.ui.viewmodel.ScreenTab
import com.example.ui.viewmodel.StudyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val studyViewModel: StudyViewModel = viewModel()
            val isDark by studyViewModel.isDarkMode.collectAsState()
            val currentScreen by studyViewModel.currentScreen.collectAsState()
            val studyMode by studyViewModel.studyModeActive.collectAsState()
            val isAiLoading by studyViewModel.isAiLoading.collectAsState()
            val aiStatusMessage by studyViewModel.aiStatusMessage.collectAsState()

            val snackbarHostState = remember { SnackbarHostState() }
            val context = LocalContext.current

            // Toast / Snackbar listener
            LaunchedEffect(Unit) {
                studyViewModel.toastMessage.collect { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }

            // Android Back button handler
            BackHandler(enabled = currentScreen != ScreenTab.HOME) {
                studyViewModel.navigateBack()
            }

            StudyMateTheme(darkTheme = isDark) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        val title = when (currentScreen) {
                            ScreenTab.HOME -> "StudyMate AI"
                            ScreenTab.CHAT -> "Talk with Gemini"
                            ScreenTab.NOTES -> "Study Notes"
                            ScreenTab.QUIZ -> "Quiz Generator"
                            ScreenTab.FILES -> "My Files"
                            ScreenTab.TOOLS -> "AI Study Tools"
                            ScreenTab.STUDY_MATERIAL -> "Study Material"
                            ScreenTab.SEARCH -> "Search"
                            ScreenTab.SETTINGS -> "Settings"
                            ScreenTab.PDF_CREATOR -> "Create PDF"
                            ScreenTab.DOCX_CREATOR -> "Create Word Document"
                        }

                        val canGoBack = currentScreen != ScreenTab.HOME

                        StudyTopAppBar(
                            title = title,
                            onBackClick = if (canGoBack) { { studyViewModel.navigateBack() } } else null,
                            onSearchClick = if (currentScreen != ScreenTab.SEARCH) { { studyViewModel.navigateTo(ScreenTab.SEARCH) } } else null,
                            onSettingsClick = if (currentScreen != ScreenTab.SETTINGS) { { studyViewModel.navigateTo(ScreenTab.SETTINGS) } } else null,
                            studyModeActive = studyMode && (currentScreen == ScreenTab.CHAT || currentScreen == ScreenTab.HOME),
                            onToggleStudyMode = { studyViewModel.studyModeActive.value = !studyMode }
                        )
                    },
                    bottomBar = {
                        val isMainTab = currentScreen in listOf(
                            ScreenTab.HOME,
                            ScreenTab.CHAT,
                            ScreenTab.NOTES,
                            ScreenTab.QUIZ,
                            ScreenTab.FILES
                        )
                        if (isMainTab) {
                            StudyBottomNavigationBar(
                                currentTab = currentScreen,
                                onTabSelected = { studyViewModel.navigateTo(it) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            ScreenTab.HOME -> HomeScreen(
                                viewModel = studyViewModel,
                                onNavigateTo = { studyViewModel.navigateTo(it) },
                                onOpenCreatePdf = { studyViewModel.navigateTo(ScreenTab.PDF_CREATOR) },
                                onOpenCreateDocx = { studyViewModel.navigateTo(ScreenTab.DOCX_CREATOR) }
                            )
                            ScreenTab.CHAT -> ChatScreen(
                                viewModel = studyViewModel
                            )
                            ScreenTab.NOTES -> NotesScreen(
                                viewModel = studyViewModel,
                                onExportPdf = { title, content ->
                                    studyViewModel.exportPdf(title, "Study Notes", content)
                                },
                                onExportDocx = { title, content ->
                                    studyViewModel.exportDocx(title, "Study Notes", content)
                                }
                            )
                            ScreenTab.QUIZ -> QuizScreen(
                                viewModel = studyViewModel,
                                onExportPdf = { title, content ->
                                    studyViewModel.exportPdf(title, "Quiz Results", content)
                                }
                            )
                            ScreenTab.FILES -> MyFilesScreen(
                                viewModel = studyViewModel
                            )
                            ScreenTab.TOOLS -> ToolsScreen(
                                viewModel = studyViewModel,
                                onExportPdf = { title, content ->
                                    studyViewModel.exportPdf(title, "AI Study Tool Result", content)
                                }
                            )
                            ScreenTab.STUDY_MATERIAL -> StudyMaterialScreen(
                                viewModel = studyViewModel,
                                onExportPdf = { title, content ->
                                    studyViewModel.exportPdf(title, "Analyzed Study Material", content)
                                },
                                onExportDocx = { title, content ->
                                    studyViewModel.exportDocx(title, "Analyzed Study Material", content)
                                }
                            )
                            ScreenTab.SEARCH -> SearchScreen(
                                viewModel = studyViewModel,
                                onNavigateTo = { studyViewModel.navigateTo(it) }
                            )
                            ScreenTab.SETTINGS -> SettingsScreen(
                                viewModel = studyViewModel
                            )
                            ScreenTab.PDF_CREATOR -> PdfDocCreatorScreen(
                                viewModel = studyViewModel,
                                initialIsPdf = true
                            )
                            ScreenTab.DOCX_CREATOR -> PdfDocCreatorScreen(
                                viewModel = studyViewModel,
                                initialIsPdf = false
                            )
                        }

                        if (isAiLoading) {
                            AiLoadingDialog(statusText = aiStatusMessage)
                        }
                    }
                }
            }
        }
    }
}
