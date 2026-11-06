package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.notifications.SecretaryNotificationManager
import com.example.security.configureAppCheck
import com.example.ui.NavTab
import com.example.ui.SecretaryViewModel
import com.example.ui.dialogs.AddEventDialog
import com.example.ui.dialogs.CaptureTextDialog
import com.example.ui.dialogs.CreateProjectDialog
import com.example.ui.dialogs.CreateTaskDialog
import com.example.ui.dialogs.LearningLaboratoryDialog
import com.example.ui.dialogs.PrivacyCenterDialog
import com.example.ui.dialogs.SampleDocumentsDialog
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.InboxScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.AISecretaryTheme
import com.example.ui.theme.PrimaryBlue

class MainActivity : ComponentActivity() {

    private val viewModel: SecretaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        // Initialize local notification channels
        SecretaryNotificationManager.initChannels(this)

        // Enable Android Edge-to-Edge
        enableEdgeToEdge()

        // Handle Android Native Sharing (ACTION_SEND / ACTION_SEND_MULTIPLE)
        handleIncomingShareIntent(intent)

        setContent {
            AISecretaryTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingShareIntent(intent)
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type ?: return

        if (Intent.ACTION_SEND == action) {
            if ("text/plain" == type) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!sharedText.isNullOrBlank()) {
                    val title = sharedText.lines().firstOrNull { it.isNotBlank() }?.take(30) ?: "Shared Message"
                    viewModel.captureText(title, sharedText, "NATIVE_SHARE")
                    viewModel.selectTab(NavTab.INBOX)
                }
            } else if (type.startsWith("image/")) {
                val imageUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                }
                if (imageUri != null) {
                    viewModel.captureImage(this, imageUri, "NATIVE_SHARE")
                    viewModel.selectTab(NavTab.INBOX)
                }
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: SecretaryViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isLearningLabOpen by viewModel.isLearningLabOpen.collectAsState()
    val isPrivacyCenterOpen by viewModel.isPrivacyCenterOpen.collectAsState()
    val isCreateTaskOpen by viewModel.isCreateTaskOpen.collectAsState()
    val isCreateProjectOpen by viewModel.isCreateProjectOpen.collectAsState()
    val isAddEventOpen by viewModel.isAddEventOpen.collectAsState()
    val isTextCaptureOpen by viewModel.isTextCaptureOpen.collectAsState()
    val isSampleDocsOpen by viewModel.isSampleDocsOpen.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == NavTab.TODAY,
                    onClick = { viewModel.selectTab(NavTab.TODAY) },
                    icon = { Icon(imageVector = Icons.Filled.Today, contentDescription = "Today") },
                    label = { Text("Today") },
                    modifier = Modifier.testTag("nav_tab_today")
                )
                NavigationBarItem(
                    selected = currentTab == NavTab.INBOX,
                    onClick = { viewModel.selectTab(NavTab.INBOX) },
                    icon = { Icon(imageVector = Icons.Filled.Inbox, contentDescription = "Inbox") },
                    label = { Text("Inbox") },
                    modifier = Modifier.testTag("nav_tab_inbox")
                )
                NavigationBarItem(
                    selected = currentTab == NavTab.CALENDAR,
                    onClick = { viewModel.selectTab(NavTab.CALENDAR) },
                    icon = { Icon(imageVector = Icons.Filled.CalendarMonth, contentDescription = "Calendar") },
                    label = { Text("Calendar") },
                    modifier = Modifier.testTag("nav_tab_calendar")
                )
                NavigationBarItem(
                    selected = currentTab == NavTab.PROJECTS,
                    onClick = { viewModel.selectTab(NavTab.PROJECTS) },
                    icon = { Icon(imageVector = Icons.Filled.FolderSpecial, contentDescription = "Projects") },
                    label = { Text("Projects") },
                    modifier = Modifier.testTag("nav_tab_projects")
                )
                NavigationBarItem(
                    selected = currentTab == NavTab.ASSISTANT,
                    onClick = { viewModel.selectTab(NavTab.ASSISTANT) },
                    icon = { Icon(imageVector = Icons.Filled.Assistant, contentDescription = "Assistant") },
                    label = { Text("Assistant") },
                    modifier = Modifier.testTag("nav_tab_assistant")
                )
            }
        },
        floatingActionButton = {
            if (currentTab == NavTab.TODAY || currentTab == NavTab.INBOX) {
                FloatingActionButton(
                    onClick = { viewModel.openTextCapture() },
                    containerColor = PrimaryBlue,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.testTag("main_fab_capture")
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Capture Incoming Work")
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentTab) {
                NavTab.TODAY -> TodayScreen(viewModel = viewModel)
                NavTab.INBOX -> InboxScreen(viewModel = viewModel)
                NavTab.CALENDAR -> CalendarScreen(viewModel = viewModel)
                NavTab.PROJECTS -> ProjectsScreen(viewModel = viewModel)
                NavTab.ASSISTANT -> AssistantScreen(viewModel = viewModel)
            }
        }
    }

    // Modal Dialog Hosts
    if (isLearningLabOpen) {
        LearningLaboratoryDialog(viewModel = viewModel, onDismiss = { viewModel.closeLearningLab() })
    }
    if (isPrivacyCenterOpen) {
        PrivacyCenterDialog(viewModel = viewModel, onDismiss = { viewModel.closePrivacyCenter() })
    }
    if (isCreateTaskOpen) {
        CreateTaskDialog(viewModel = viewModel, onDismiss = { viewModel.closeCreateTask() })
    }
    if (isCreateProjectOpen) {
        CreateProjectDialog(viewModel = viewModel, onDismiss = { viewModel.closeCreateProject() })
    }
    if (isAddEventOpen) {
        AddEventDialog(viewModel = viewModel, onDismiss = { viewModel.closeAddEvent() })
    }
    if (isTextCaptureOpen) {
        CaptureTextDialog(viewModel = viewModel, onDismiss = { viewModel.closeTextCapture() })
    }
    if (isSampleDocsOpen) {
        SampleDocumentsDialog(viewModel = viewModel, onDismiss = { viewModel.closeSampleDocs() })
    }
}
