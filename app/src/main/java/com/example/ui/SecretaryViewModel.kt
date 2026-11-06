package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ExtractionResult
import com.example.data.local.entity.CalendarEventEntity
import com.example.data.local.entity.CapturedItemEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.CorrectionRecordEntity
import com.example.data.local.entity.LearningRuleEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.SubtaskEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.repository.SecretaryRepository
import com.example.ocr.DocumentProcessor
import com.example.ocr.SampleDocument
import com.example.security.PrivacyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class NavTab {
    TODAY, INBOX, CALENDAR, PROJECTS, ASSISTANT
}

enum class CalendarViewMode {
    MONTH, WEEK, DAY, AGENDA
}

data class TodayUiState(
    val greeting: String = "Hello",
    val todayDateFormatted: String = "",
    val morningBriefing: String = "",
    val isBriefingLoading: Boolean = false,
    val urgentTasks: List<TaskEntity> = emptyList(),
    val todayTasks: List<TaskEntity> = emptyList(),
    val riskTasks: List<Pair<TaskEntity, String>> = emptyList(),
    val waitingTasks: List<TaskEntity> = emptyList(),
    val overdueCount: Int = 0
)

class SecretaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SecretaryRepository(application)

    // Current navigation tab
    private val _currentTab = MutableStateFlow(NavTab.TODAY)
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    fun selectTab(tab: NavTab) {
        _currentTab.value = tab
    }

    // User Profile
    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Projects & Tasks
    val projects: StateFlow<List<ProjectEntity>> = repository.projects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calendar
    val allCalendarEvents: StateFlow<List<CalendarEventEntity>> = repository.allCalendarEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDateMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedDateMillis: StateFlow<Long> = _selectedDateMillis.asStateFlow()

    private val _calendarViewMode = MutableStateFlow(CalendarViewMode.AGENDA)
    val calendarViewMode: StateFlow<CalendarViewMode> = _calendarViewMode.asStateFlow()

    fun setCalendarViewMode(mode: CalendarViewMode) {
        _calendarViewMode.value = mode
    }

    fun setSelectedDate(millis: Long) {
        _selectedDateMillis.value = millis
    }

    // Captured Items
    val capturedItems: StateFlow<List<CapturedItemEntity>> = repository.capturedItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Learning Rules & Corrections
    val learningRules: StateFlow<List<LearningRuleEntity>> = repository.learningRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val correctionRecords: StateFlow<List<CorrectionRecordEntity>> = repository.correctionRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chat
    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<com.example.data.local.entity.AIProcessingLogEntity>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Dialog State flags
    var isLearningLabOpen = MutableStateFlow(false)
        private set
    var isPrivacyCenterOpen = MutableStateFlow(false)
        private set
    var isCreateTaskOpen = MutableStateFlow(false)
        private set
    var isCreateProjectOpen = MutableStateFlow(false)
        private set
    var isAddEventOpen = MutableStateFlow(false)
        private set
    var isTextCaptureOpen = MutableStateFlow(false)
        private set
    var isSampleDocsOpen = MutableStateFlow(false)
        private set

    // Validation Sheet State
    private val _activeValidationItem = MutableStateFlow<CapturedItemEntity?>(null)
    val activeValidationItem: StateFlow<CapturedItemEntity?> = _activeValidationItem.asStateFlow()

    private val _activeExtraction = MutableStateFlow<ExtractionResult?>(null)
    val activeExtraction: StateFlow<ExtractionResult?> = _activeExtraction.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // Subtasks for detail view
    private val _selectedTaskForDetail = MutableStateFlow<TaskEntity?>(null)
    val selectedTaskForDetail: StateFlow<TaskEntity?> = _selectedTaskForDetail.asStateFlow()

    fun selectTaskDetail(task: TaskEntity?) {
        _selectedTaskForDetail.value = task
    }

    fun getSubtasks(taskId: String) = repository.getSubtasks(taskId)

    // Today Briefing calculation
    private val _morningBriefingText = MutableStateFlow<String>("")
    val morningBriefingText: StateFlow<String> = _morningBriefingText.asStateFlow()

    private val _isBriefingLoading = MutableStateFlow(false)
    val isBriefingLoading: StateFlow<Boolean> = _isBriefingLoading.asStateFlow()

    init {
        // Initialize default profile if absent
        viewModelScope.launch {
            val existing = repository.userProfile.stateIn(viewModelScope).value
            if (existing == null) {
                repository.saveUserProfile(
                    UserProfileEntity(
                        id = 1,
                        userName = "Executive",
                        primaryLanguage = "en",
                        isOnboardingCompleted = false,
                        allowCloudAiConsent = false
                    )
                )
            }
            refreshBriefing(true)
        }
    }

    fun refreshBriefing(isMorning: Boolean = true) {
        viewModelScope.launch {
            _isBriefingLoading.value = true
            val tasks = allTasks.value
            val now = System.currentTimeMillis()
            val overdue = tasks.count { (it.deadlineMillis ?: Long.MAX_VALUE) < now && it.status != "COMPLETED" }
            val briefing = repository.aiRepo.generateBriefing(tasks, overdue, isMorning)
            _morningBriefingText.value = briefing
            _isBriefingLoading.value = false
        }
    }

    // Today UI Combined State
    val todayUiState: StateFlow<TodayUiState> = combine(
        allTasks,
        userProfile,
        morningBriefingText,
        isBriefingLoading
    ) { tasks, profile, briefing, loading ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        } + ", " + (profile?.userName ?: "Executive")

        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        val todayTasks = tasks.filter {
            it.status != "COMPLETED" && (it.deadlineMillis == null || it.deadlineMillis <= endOfDay)
        }
        val urgentTasks = tasks.filter {
            it.priority == "URGENT" && it.status != "COMPLETED"
        }
        val overdueTasks = tasks.filter {
            (it.deadlineMillis ?: Long.MAX_VALUE) < now && it.status != "COMPLETED"
        }
        val waitingTasks = tasks.filter {
            it.status == "WAITING_FOR_RESPONSE"
        }

        // Deadline Risk Detector logic
        val riskList = mutableListOf<Pair<TaskEntity, String>>()
        for (task in tasks) {
            if (task.status != "COMPLETED" && task.deadlineMillis != null) {
                val hoursRemaining = (task.deadlineMillis - now) / (1000 * 3600)
                if (hoursRemaining in 0..48) {
                    riskList.add(task to "Due in $hoursRemaining hours with active execution pending")
                } else if (!task.waitingForWhomOrWhat.isNullOrBlank()) {
                    riskList.add(task to "Blocked awaiting response from ${task.waitingForWhomOrWhat}")
                }
            }
        }

        val dateStr = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date())

        TodayUiState(
            greeting = greeting,
            todayDateFormatted = dateStr,
            morningBriefing = briefing,
            isBriefingLoading = loading,
            urgentTasks = urgentTasks,
            todayTasks = todayTasks,
            riskTasks = riskList,
            waitingTasks = waitingTasks,
            overdueCount = overdueTasks.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    // Capture & Extraction Actions
    fun captureText(title: String, text: String, sourceChannel: String = "TEXT") {
        viewModelScope.launch {
            val item = repository.captureItem(title, text, sourceChannel)
            startAnalysis(item)
        }
    }

    fun captureImage(context: android.content.Context, uri: Uri, sourceChannel: String = "PHOTO") {
        viewModelScope.launch {
            val bitmap = DocumentProcessor.loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                val ocrText = DocumentProcessor.recognizeTextFromBitmap(bitmap)
                val finalTitle = if (ocrText.isNotBlank()) ocrText.lines().first().take(30) else "Scanned Document"
                val item = repository.captureItem(finalTitle, ocrText.ifBlank { "No text recognized" }, sourceChannel, uri.toString())
                startAnalysis(item)
            }
        }
    }

    fun startAnalysis(item: CapturedItemEntity) {
        viewModelScope.launch {
            _activeValidationItem.value = item
            _isAnalyzing.value = true
            val extraction = repository.analyzeCapturedItem(item)
            _activeExtraction.value = extraction
            _isAnalyzing.value = false
        }
    }

    fun confirmExtractedTask(
        taskTitle: String,
        description: String,
        deadlineMillis: Long?,
        deadlineType: String,
        priority: String,
        recipient: String?,
        projectId: String?,
        subtasks: List<String>,
        correctionNote: String?
    ) {
        val item = _activeValidationItem.value ?: return
        viewModelScope.launch {
            val validated = TaskEntity(
                id = java.util.UUID.randomUUID().toString(),
                title = taskTitle,
                description = description,
                projectId = projectId,
                priority = priority,
                deadlineMillis = deadlineMillis,
                deadlineType = deadlineType,
                recipient = recipient,
                evidenceText = _activeExtraction.value?.evidenceText
            )
            repository.confirmCapturedItem(item, validated, subtasks, correctionNote)
            _activeValidationItem.value = null
            _activeExtraction.value = null
            refreshBriefing()
        }
    }

    fun cancelValidation() {
        _activeValidationItem.value = null
        _activeExtraction.value = null
    }

    // Task Management Operations
    fun toggleTaskComplete(task: TaskEntity) {
        viewModelScope.launch {
            val newStatus = if (task.status == "COMPLETED") "NOT_STARTED" else "COMPLETED"
            repository.updateTaskStatus(task, newStatus)
            refreshBriefing()
        }
    }

    fun updateTaskStatus(task: TaskEntity, status: String) {
        viewModelScope.launch {
            repository.updateTaskStatus(task, status)
            refreshBriefing()
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            refreshBriefing()
        }
    }

    fun createManualTask(
        title: String,
        description: String,
        projectId: String?,
        priority: String,
        deadlineMillis: Long?,
        deadlineType: String,
        recipient: String?,
        waitingFor: String?
    ) {
        viewModelScope.launch {
            repository.createTask(
                title = title,
                description = description,
                projectId = projectId,
                priority = priority,
                deadlineMillis = deadlineMillis,
                deadlineType = deadlineType,
                recipient = recipient,
                assignedPerson = null,
                waitingFor = waitingFor,
                sourceCapturedId = null,
                evidence = null
            )
            refreshBriefing()
        }
    }

    fun breakdownTaskAutomatic(taskId: String, title: String, description: String) {
        viewModelScope.launch {
            val steps = repository.aiRepo.breakdownTask(title, description)
            for (step in steps) {
                repository.addSubtask(taskId, step)
            }
        }
    }

    fun toggleSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch {
            repository.toggleSubtask(subtask)
        }
    }

    fun addSubtask(taskId: String, title: String) {
        viewModelScope.launch {
            repository.addSubtask(taskId, title)
        }
    }

    // Project Operations
    fun createProject(title: String, description: String, colorHex: String, category: String, deadlineMillis: Long?) {
        viewModelScope.launch {
            repository.createProject(title, description, colorHex, category, deadlineMillis)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
        }
    }

    // Calendar Operations
    fun createCalendarEvent(
        title: String,
        description: String,
        startTimeMillis: Long,
        endTimeMillis: Long,
        eventType: String
    ) {
        viewModelScope.launch {
            repository.createCalendarEvent(
                title = title,
                description = description,
                startTimeMillis = startTimeMillis,
                endTimeMillis = endTimeMillis,
                eventType = eventType
            )
        }
    }

    fun deleteCalendarEvent(id: String) {
        viewModelScope.launch {
            repository.deleteCalendarEvent(id)
        }
    }

    // Assistant Chat
    private val _isAssistantThinking = MutableStateFlow(false)
    val isAssistantThinking: StateFlow<Boolean> = _isAssistantThinking.asStateFlow()

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isAssistantThinking.value = true
            repository.sendChatMessage(text)
            _isAssistantThinking.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatMessages()
        }
    }

    // Learning Rules & Lab
    fun toggleLearningRule(rule: LearningRuleEntity) {
        viewModelScope.launch {
            repository.toggleLearningRule(rule)
        }
    }

    fun deleteLearningRule(rule: LearningRuleEntity) {
        viewModelScope.launch {
            repository.deleteLearningRule(rule)
        }
    }

    fun clearAllLearning() {
        viewModelScope.launch {
            repository.clearLearningRules()
        }
    }

    // Privacy & Preferences
    fun setCloudAiConsent(consent: Boolean) {
        viewModelScope.launch {
            val cur = userProfile.value ?: UserProfileEntity()
            repository.saveUserProfile(cur.copy(allowCloudAiConsent = consent))
        }
    }

    fun setNotificationPreviewProtection(protect: Boolean) {
        viewModelScope.launch {
            val cur = userProfile.value ?: UserProfileEntity()
            repository.saveUserProfile(cur.copy(protectNotificationPreviews = protect))
        }
    }

    fun wipeAllData(context: android.content.Context) {
        viewModelScope.launch {
            PrivacyManager.wipeAllUserData(context)
        }
    }

    fun openLearningLab() { isLearningLabOpen.value = true }
    fun closeLearningLab() { isLearningLabOpen.value = false }

    fun openPrivacyCenter() { isPrivacyCenterOpen.value = true }
    fun closePrivacyCenter() { isPrivacyCenterOpen.value = false }

    fun openCreateTask() { isCreateTaskOpen.value = true }
    fun closeCreateTask() { isCreateTaskOpen.value = false }

    fun openCreateProject() { isCreateProjectOpen.value = true }
    fun closeCreateProject() { isCreateProjectOpen.value = false }

    fun openAddEvent() { isAddEventOpen.value = true }
    fun closeAddEvent() { isAddEventOpen.value = false }

    fun openTextCapture() { isTextCaptureOpen.value = true }
    fun closeTextCapture() { isTextCaptureOpen.value = false }

    fun openSampleDocs() { isSampleDocsOpen.value = true }
    fun closeSampleDocs() { isSampleDocsOpen.value = false }
}
