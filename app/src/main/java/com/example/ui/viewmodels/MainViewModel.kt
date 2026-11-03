package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MainApplication
import com.example.data.local.AppSettings
import com.example.data.local.ProjectEntity
import com.example.data.model.*
import com.example.data.repository.Resource
import com.example.pipeline.dom.DomAnimation10ScenePackage
import com.example.pipeline.VideoRequest
import com.example.worker.AutoGenerationLoopManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardUiState(
    val isServerOnline: Boolean = false,
    val serverVersion: String = "",
    val recentProjects: List<ProjectSummary> = emptyList(),
    val totalVideosGenerated: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class CreateJobUiState(
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val createdJobId: String? = null
)

data class AutoGenerationLoopUiState(
    val isScheduled: Boolean = false,
    val isUpdating: Boolean = false,
    val intervalMinutes: Int = 60,
    val error: String? = null
)

private val imageGenerationPattern = Regex("Generating image (\\d+)/(\\d+)")
private val imagePreviewPattern = Regex("\\[IMAGE_PREVIEW] (\\d+)/(\\d+) (.+)")

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MainApplication
    private val repository = app.videoRepository
    private val settingsManager = app.settingsManager

    val settingsFlow: StateFlow<AppSettings> = settingsManager.settingsFlow

    private val _dashboardState = MutableStateFlow(DashboardUiState())
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()

    private val _createJobState = MutableStateFlow(CreateJobUiState())
    val createJobState: StateFlow<CreateJobUiState> = _createJobState.asStateFlow()

    private val _autoGenerationLoopState = MutableStateFlow(AutoGenerationLoopUiState())
    val autoGenerationLoopState: StateFlow<AutoGenerationLoopUiState> = _autoGenerationLoopState.asStateFlow()

    private val _currentJobProgress = MutableStateFlow<JobStatusResponse?>(null)
    val currentJobProgress: StateFlow<JobStatusResponse?> = _currentJobProgress.asStateFlow()

    private val _libraryProjects = MutableStateFlow<List<ProjectSummary>>(emptyList())
    val libraryProjects: StateFlow<List<ProjectSummary>> = _libraryProjects.asStateFlow()

    private val _currentProjectDetails = MutableStateFlow<ProjectDetailsResponse?>(null)
    val currentProjectDetails: StateFlow<ProjectDetailsResponse?> = _currentProjectDetails.asStateFlow()

    private val _serverLogs = MutableStateFlow<List<String>>(emptyList())
    val serverLogs: StateFlow<List<String>> = _serverLogs.asStateFlow()

    val networkMonitor = com.example.util.NetworkMonitor(app)
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    val isPipelineGenerating: StateFlow<Boolean> = com.example.pipeline.PipelineExecutionCoordinator.isGenerating
    val activePipelineInfo = com.example.pipeline.PipelineExecutionCoordinator.activeJobInfo
    val livePipelineProgress: StateFlow<com.example.notification.LivePipelineProgress> =
        com.example.notification.PipelineNotificationManager.liveProgressFlow

    private var pollingJob: Job? = null
    private var activeNativeJob: Job? = null

    init {
        checkServerHealth()
        loadProjects()
        observeLocalDatabase()
        refreshAutoGenerationLoopStatus()
    }

    fun startAutoGenerationLoop(request: CreateJobRequest, intervalMinutes: Int) {
        if (intervalMinutes < AutoGenerationLoopManager.MIN_INTERVAL_MINUTES) {
            _autoGenerationLoopState.update {
                it.copy(error = "Background interval must be at least 15 minutes.")
            }
            return
        }
        if (intervalMinutes > AutoGenerationLoopManager.MAX_INTERVAL_MINUTES) {
            _autoGenerationLoopState.update {
                it.copy(error = "Background interval cannot exceed 1440 minutes.")
            }
            return
        }
        viewModelScope.launch {
            _autoGenerationLoopState.update { it.copy(isUpdating = true, error = null) }
            try {
                withContext(Dispatchers.IO) {
                    AutoGenerationLoopManager.schedule(app, request, intervalMinutes)
                }
                _autoGenerationLoopState.value = AutoGenerationLoopUiState(
                    isScheduled = true,
                    intervalMinutes = intervalMinutes.coerceAtMost(AutoGenerationLoopManager.MAX_INTERVAL_MINUTES)
                )
            } catch (error: Exception) {
                _autoGenerationLoopState.update {
                    it.copy(isUpdating = false, error = error.localizedMessage ?: "Could not schedule auto-generation.")
                }
            }
        }
    }

    fun stopAutoGenerationLoop() {
        viewModelScope.launch {
            _autoGenerationLoopState.update { it.copy(isUpdating = true, error = null) }
            try {
                withContext(Dispatchers.IO) { AutoGenerationLoopManager.stop(app) }
                _autoGenerationLoopState.value = AutoGenerationLoopUiState()
            } catch (error: Exception) {
                _autoGenerationLoopState.update {
                    it.copy(isUpdating = false, error = error.localizedMessage ?: "Could not stop auto-generation.")
                }
            }
        }
    }

    private fun refreshAutoGenerationLoopStatus() {
        viewModelScope.launch {
            runCatching { AutoGenerationLoopManager.getStatus(app) }
                .onSuccess { status ->
                    _autoGenerationLoopState.value = AutoGenerationLoopUiState(
                        isScheduled = status.isScheduled,
                        intervalMinutes = status.intervalMinutes
                    )
                }
                .onFailure { error ->
                    _autoGenerationLoopState.update {
                        it.copy(error = error.localizedMessage ?: "Could not read auto-generation status.")
                    }
                }
        }
    }

    private fun observeLocalDatabase() {
        viewModelScope.launch {
            repository.localProjectsFlow.collect { localProjects ->
                if (_libraryProjects.value.isEmpty()) {
                    _libraryProjects.value = localProjects
                }
                _dashboardState.update { state ->
                    state.copy(
                        recentProjects = localProjects.take(5),
                        totalVideosGenerated = localProjects.size
                    )
                }
            }
        }
    }

    fun checkServerHealth() {
        viewModelScope.launch {
            _dashboardState.update { it.copy(isLoading = true) }
            when (val result = repository.checkHealth()) {
                is Resource.Success -> _dashboardState.update {
                    it.copy(
                        isServerOnline = true,
                        serverVersion = result.data.version,
                        isLoading = false,
                        errorMessage = null
                    )
                }
                is Resource.Error -> _dashboardState.update {
                    it.copy(isServerOnline = false, isLoading = false, errorMessage = result.message)
                }
                else -> _dashboardState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadProjects() {
        viewModelScope.launch {
            when (val result = repository.fetchAndSyncProjects()) {
                is Resource.Success -> {
                    _libraryProjects.value = result.data
                    _dashboardState.update {
                        it.copy(
                            recentProjects = result.data.take(5),
                            totalVideosGenerated = result.data.size
                        )
                    }
                }
                is Resource.Error -> _dashboardState.update {
                    it.copy(errorMessage = result.message)
                }
                else -> Unit
            }
        }
    }

    suspend fun saveGeneratedDomVideo(
        packageResult: DomAnimation10ScenePackage,
        topic: String,
        language: String,
        category: String,
        voiceName: String,
        aspectRatio: String,
        showCaptions: Boolean
    ) = withContext(Dispatchers.IO) {
        require(topic.isNotBlank()) { "Enter a topic before saving the video." }
        val videoFile = packageResult.finalRenderedVideoFile
            ?.takeIf { it.isFile && it.length() > 1_000L }
            ?: throw IllegalStateException("The generated video file is unavailable.")
        val scenes = packageResult.scenes.sortedBy { it.sceneNumber }
        require(scenes.map { it.sceneNumber } == (1..10).toList()) {
            "The completed video is missing one or more scenes."
        }

        var startSecond = 0
        val promptsJson = org.json.JSONArray()
        scenes.forEach { scene ->
            val endSecond = startSecond + scene.script.durationSeconds
            promptsJson.put(
                org.json.JSONObject().apply {
                    put("index", scene.sceneNumber)
                    put("startSec", startSecond)
                    put("endSec", endSecond)
                    put("purpose", scene.script.sceneTitle)
                    put("prompt", scene.script.visualPrompt)
                    put("narration", scene.script.narration)
                }
            )
            startSecond = endSecond
        }
        val settingsJson = org.json.JSONObject().apply {
            put("category", category)
            put("language", language)
            put("voice", voiceName)
            put("aspectRatio", aspectRatio)
            put("captions", showCaptions)
            put("durationSeconds", packageResult.totalDurationSeconds)
        }
        val storyJson = org.json.JSONObject().apply {
            put("title", packageResult.title)
            put("topic", topic)
            put("summary100", packageResult.theme)
            put("description500", packageResult.moral)
            put("story", packageResult.theme)
            put("narration", scenes.joinToString(" ") { it.script.narration })
            put("selectedTitle", packageResult.title)
            put("productionSettings", settingsJson)
        }.toString()
        val firstFrame = File(videoFile.parentFile, "scene_1_frame.jpg")
        val entity = ProjectEntity(
            id = packageResult.projectId,
            title = packageResult.title,
            topic = topic,
            language = language,
            category = category,
            duration = packageResult.totalDurationSeconds,
            style = "HTML/CSS/JavaScript animation",
            aspectRatio = aspectRatio,
            createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            status = "completed",
            videoUrl = "file://${videoFile.absolutePath}",
            thumbnailUrl = if (firstFrame.isFile) "file://${firstFrame.absolutePath}" else "",
            storyJson = storyJson,
            promptsJson = promptsJson.toString(),
            pipelineStep = "DOM_VIDEO_COMPLETED"
        )
        repository.saveGeneratedDomProject(entity)
        val summary = entity.toProjectSummary()
        withContext(Dispatchers.Main) {
            _libraryProjects.update { current ->
                listOf(summary) + current.filterNot { it.id == summary.id }
            }
            _dashboardState.update { state ->
                val recent = listOf(summary) + state.recentProjects.filterNot { it.id == summary.id }
                state.copy(
                    recentProjects = recent.take(5),
                    totalVideosGenerated = maxOf(state.totalVideosGenerated, _libraryProjects.value.size)
                )
            }
        }
        summary
    }

    fun createVideo(request: CreateJobRequest, onNavigateToProgress: (String) -> Unit) {
        viewModelScope.launch {
            if (request.topic.isBlank()) {
                _createJobState.value = CreateJobUiState(error = "Enter a topic before starting video generation.")
                return@launch
            }

            _createJobState.value = CreateJobUiState(isSubmitting = true)
            if (settingsManager.settingsFlow.value.engineMode != "Remote Server Only") {
                startLocalVideo(request, onNavigateToProgress)
                return@launch
            }

            when (val result = repository.createJob(request)) {
                is Resource.Success -> {
                    _createJobState.value = CreateJobUiState(createdJobId = result.data.id)
                    onNavigateToProgress(result.data.id)
                    startPollingJob(result.data.id)
                }
                is Resource.Error -> {
                    _createJobState.value = CreateJobUiState()
                    startLocalVideo(request, onNavigateToProgress)
                }
                else -> _createJobState.value = CreateJobUiState(error = "The video service did not return a job.")
            }
        }
    }

    private fun startLocalVideo(request: CreateJobRequest, onNavigateToProgress: (String) -> Unit) {
        val jobId = "local_${System.currentTimeMillis()}"
        _createJobState.value = CreateJobUiState(createdJobId = jobId)
        onNavigateToProgress(jobId)
        startNativePipelineExecution(jobId, request)
    }

    private fun startNativePipelineExecution(jobId: String, request: CreateJobRequest) {
        activeNativeJob?.cancel()
        activeNativeJob = viewModelScope.launch {
            val acquired = com.example.pipeline.PipelineExecutionCoordinator.tryAcquireExecution(
                jobId = jobId,
                title = request.topic,
                isWorkerTask = false
            )
            if (!acquired) {
                _currentJobProgress.value = JobStatusResponse(
                    id = jobId,
                    status = "failed",
                    step = "Another video is already being generated. Please wait for it to finish.",
                    error = JobError(message = "Only one video can be generated at a time.")
                )
                return@launch
            }

            com.example.pipeline.PipelineExecutionCoordinator.registerJobHandle(coroutineContext[Job]!!)
            _currentJobProgress.value = JobStatusResponse(
                id = jobId,
                status = "processing",
                progress = 5,
                step = "Preparing video generation"
            )

            try {
                val settings = settingsManager.settingsFlow.value
                val requestForPipeline = VideoRequest(
                    topic = request.topic,
                    language = request.language,
                    category = request.category,
                    targetDurationSec = request.durationSec,
                    style = request.style,
                    aspect = request.aspectRatio,
                    platform = request.platform,
                    audience = request.audience,
                    voiceName = request.voiceName,
                    imageDurationSec = request.imageDurationSec,
                    showCaptions = request.showCaptions,
                    captionStyle = request.captionStyle,
                    captionHighlightColor = request.captionHighlightColor,
                    captionFontFamily = request.captionFontFamily,
                    captionStrokeWidth = request.captionStrokeWidth,
                    captionGlow = request.captionGlow,
                    captionWordsPerChunk = request.captionWordsPerChunk,
                    captionUppercase = request.captionUppercase,
                    bgmEnabled = request.bgmEnabled,
                    bgmTrackId = request.bgmTrackId,
                    bgmVolume = request.bgmVolume,
                    autoDucking = request.autoDucking,
                    visualRenderMode = settings.visualRenderMode,
                    htmlTheme = settings.htmlTheme,
                    customNarration = request.customNarration,
                    customHook = request.customHook
                )
                val result = repository.runNativePipeline(requestForPipeline) { progress, status ->
                    val previous = _currentJobProgress.value
                    val imagePreview = imagePreviewPattern.find(status)
                    val imageGeneration = imageGenerationPattern.find(status)
                    val currentImage = imagePreview?.groupValues?.get(1)?.toIntOrNull()
                        ?: imageGeneration?.groupValues?.get(1)?.toIntOrNull()?.minus(1)
                        ?: previous?.currentImage
                        ?: 0
                    val totalImages = imagePreview?.groupValues?.get(2)?.toIntOrNull()
                        ?: imageGeneration?.groupValues?.get(2)?.toIntOrNull()
                        ?: previous?.totalImages
                        ?: 0
                    val imagePath = imagePreview?.groupValues?.get(3)
                    val imagePreviews = if (imagePath != null) {
                        (previous?.imagePreviews.orEmpty() + imagePath).distinct()
                    } else {
                        previous?.imagePreviews.orEmpty()
                    }
                    val visibleStatus = if (imagePreview != null) {
                        "Image $currentImage of $totalImages ready"
                    } else {
                        status
                    }
                    _currentJobProgress.value = JobStatusResponse(
                        id = jobId,
                        status = "processing",
                        progress = progress,
                        step = visibleStatus,
                        currentImage = currentImage,
                        totalImages = totalImages,
                        imagePreviews = imagePreviews
                    )
                    com.example.notification.PipelineNotificationManager.showProgressNotification(
                        context = app,
                        title = "Generating video",
                        statusMessage = "[$progress%] $visibleStatus",
                        progressPercent = progress
                    )
                }

                _currentJobProgress.value = JobStatusResponse(
                    id = jobId,
                    status = "completed",
                    progress = 100,
                    step = "Video completed",
                    output = JobOutput(
                        videoUrl = "file://${result.video.absolutePath}",
                        thumbnailUrl = "file://${result.thumbnail.absolutePath}",
                        title = result.metadata.title,
                        category = request.category,
                        summary = result.metadata.summary,
                        description = result.metadata.description,
                        visualCount = result.visualCount,
                        audioDurationSec = result.audioDuration
                    ),
                    currentImage = _currentJobProgress.value?.currentImage ?: result.visualCount,
                    totalImages = _currentJobProgress.value?.totalImages ?: result.visualCount,
                    imagePreviews = _currentJobProgress.value?.imagePreviews.orEmpty()
                )
                com.example.notification.PipelineNotificationManager.showCompletionNotification(
                    context = app,
                    title = result.metadata.title,
                    category = request.category,
                    language = request.language,
                    durationSec = request.durationSec
                )
                loadProjects()
            } catch (error: kotlinx.coroutines.CancellationException) {
                _currentJobProgress.value = JobStatusResponse(
                    id = jobId,
                    status = "failed",
                    step = "Cancelled",
                    error = JobError(message = "Video generation was cancelled.")
                )
                throw error
            } catch (error: Exception) {
                com.example.notification.PipelineNotificationManager.showErrorNotification(
                    context = app,
                    title = "Video generation failed",
                    errorMessage = error.localizedMessage ?: "The video could not be generated."
                )
                _currentJobProgress.value = JobStatusResponse(
                    id = jobId,
                    status = "failed",
                    step = "Video generation failed",
                    error = JobError(message = error.localizedMessage ?: "The video could not be generated.")
                )
            } finally {
                com.example.pipeline.PipelineExecutionCoordinator.releaseExecution(jobId)
            }
        }
    }

    fun startPollingJob(jobId: String) {
        if (jobId.startsWith("local_") || jobId.isBlank()) {
            pollingJob?.cancel()
            pollingJob = viewModelScope.launch {
                livePipelineProgress.collect { live ->
                    _currentJobProgress.value = JobStatusResponse(
                        id = live.jobId.ifBlank { jobId },
                        status = if (live.isCompleted) "completed" else if (live.error != null) "failed" else "processing",
                        progress = live.progressPercent,
                        step = live.statusMessage.ifBlank { live.stage }.ifBlank { "Generating video" },
                        error = live.error?.let(::JobError)
                    )
                    if (live.isCompleted || live.error != null) loadProjects()
                }
            }
            return
        }

        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                when (val result = repository.getJobStatus(jobId)) {
                    is Resource.Success -> {
                        _currentJobProgress.value = result.data
                        if (result.data.status == "completed" || result.data.status == "failed") {
                            loadProjects()
                            break
                        }
                    }
                    is Resource.Error -> {
                        _currentJobProgress.value = JobStatusResponse(
                            id = jobId,
                            status = "failed",
                            step = "Could not check the video job",
                            error = JobError(message = result.message)
                        )
                        break
                    }
                    else -> Unit
                }
                delay(2000)
            }
        }
    }

    fun updateUnifiedCategory(newCategory: String) {
        settingsManager.updateSettings(settingsManager.settingsFlow.value.copy(defaultCategory = newCategory))
    }

    fun cancelActiveJob(jobId: String) {
        viewModelScope.launch {
            com.example.pipeline.PipelineExecutionCoordinator.cancelActiveJob(jobId)
            activeNativeJob?.cancel()
            activeNativeJob = null
            if (!jobId.startsWith("local_")) repository.cancelJob(jobId)
            pollingJob?.cancel()
            _currentJobProgress.value = null
        }
    }

    fun loadProjectDetails(projectId: String) {
        viewModelScope.launch {
            _currentProjectDetails.value = null
            when (val result = repository.getProjectDetails(projectId)) {
                is Resource.Success -> _currentProjectDetails.value = result.data
                is Resource.Error -> _dashboardState.update { it.copy(errorMessage = result.message) }
                else -> Unit
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            loadProjects()
        }
    }

    fun loadLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = repository.getLogs()) {
                is Resource.Success -> _serverLogs.value = result.data
                is Resource.Error -> _dashboardState.update { it.copy(errorMessage = result.message) }
                else -> Unit
            }
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        settingsManager.updateSettings(newSettings)
    }

    fun updateAppSettings(newSettings: AppSettings) {
        updateSettings(newSettings)
    }
}
