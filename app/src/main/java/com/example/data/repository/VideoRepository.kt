package com.example.data.repository

import android.content.Context
import com.example.data.local.ProjectDao
import com.example.data.local.ProjectEntity
import com.example.data.local.SettingsManager
import com.example.data.model.CreateJobRequest
import com.example.data.model.CreateJobResponse
import com.example.data.model.HealthResponse
import com.example.data.model.JobStatusResponse
import com.example.data.model.LogsResponse
import com.example.data.model.ProjectDetailsResponse
import com.example.data.model.ProjectSummary
import com.example.data.network.ApiClient
import com.example.pipeline.AndroidFFmpegRenderer
import com.example.pipeline.AndroidVoiceGenerator
import com.example.pipeline.AndroidWhisperTranscriber
import com.example.pipeline.DeepAIWebViewClient
import com.example.pipeline.NotrackWebViewClient
import com.example.pipeline.PipelineResult
import com.example.pipeline.VideoPipeline
import com.example.pipeline.VideoRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}

class VideoRepository(
    private val context: Context,
    private val apiClient: ApiClient,
    private val projectDao: ProjectDao,
    private val settingsManager: SettingsManager
) {
    // ----------------------------------------------------
    // LOCAL ROOM DATABASE (OFFLINE FIRST)
    // ----------------------------------------------------
    val localProjectsFlow: Flow<List<ProjectSummary>> = projectDao.getAllProjectsFlow().map { entities ->
        entities.map { it.toProjectSummary() }
    }

    suspend fun getLocalProjectDetails(projectId: String): ProjectDetailsResponse? = withContext(Dispatchers.IO) {
        val entity = projectDao.getProjectById(projectId)
        entity?.toProjectDetails()
    }

    suspend fun saveGeneratedDomProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        require(project.pipelineStep == "DOM_VIDEO_COMPLETED") {
            "Only completed DOM video projects can be saved to the local library."
        }
        require(project.videoUrl.startsWith("file://")) {
            "A local rendered video path is required to save this project."
        }
        val videoFile = File(project.videoUrl.removePrefix("file://"))
        require(videoFile.isFile && videoFile.length() > 1_000L) {
            "The rendered video file is unavailable; the project was not saved."
        }
        projectDao.insertProject(project)
    }

    suspend fun saveCompletedLocalProject(result: PipelineResult, request: VideoRequest) = withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = sdf.format(Date())

        val storyJson = JSONObject().apply {
            put("title", result.metadata.title)
            put("summary100", result.metadata.summary)
            put("description500", result.metadata.description)
            put("story", result.metadata.story)
            put("narration", result.metadata.narration)
            put("selectedTitle", result.metadata.title)
        }.toString()

        val promptsJson = JSONArray().apply {
            result.visualPrompts.forEach { vp ->
                put(JSONObject().apply {
                    put("index", vp.index)
                    put("startSec", vp.startSec)
                    put("endSec", vp.endSec)
                    put("purpose", vp.purpose)
                    put("prompt", vp.prompt)
                })
            }
        }.toString()

        val imagesJson = JSONArray().apply {
            result.images.forEachIndexed { idx, imgFile ->
                put(JSONObject().apply {
                    put("index", idx + 1)
                    put("fileName", imgFile.name)
                    put("url", "file://${imgFile.absolutePath}")
                })
            }
        }.toString()

        val entity = ProjectEntity(
            id = result.projectId,
            title = result.metadata.title.ifBlank { request.topic },
            topic = request.topic,
            language = request.language,
            category = request.category,
            duration = result.audioDuration.toInt(),
            style = request.style,
            aspectRatio = request.aspect,
            createdAt = dateStr,
            status = "completed",
            videoUrl = "file://${result.video.absolutePath}",
            thumbnailUrl = "file://${result.thumbnail.absolutePath}",
            storyJson = storyJson,
            promptsJson = promptsJson,
            imagesJson = imagesJson,
            pipelineStep = "COMPLETED"
        )
        projectDao.insertProject(entity)

    }

    // ----------------------------------------------------
    // NATIVE PIPELINE RUNNER
    // ----------------------------------------------------
    suspend fun runNativePipeline(
        request: VideoRequest,
        onProgress: (Int, String) -> Unit
    ): PipelineResult = withContext(Dispatchers.IO) {
        val notrack = NotrackWebViewClient.create(context)
        val deepAi = DeepAIWebViewClient.create(context)
        val tts = AndroidVoiceGenerator(context)
        val whisper = AndroidWhisperTranscriber()
        val ffmpeg = AndroidFFmpegRenderer()
        val settings = settingsManager.settingsFlow.value

        val pipeline = VideoPipeline(
            context = context,
            notrack = notrack,
            deepAi = deepAi,
            tts = tts,
            whisper = whisper,
            ffmpeg = ffmpeg,
            pollinationsEnabled = settings.pollinationsEnabled,
            globalFallbackEnabled = settings.globalFallbackEnabled
        )

        val result = pipeline.generate(request) { prog, msg ->
            com.example.notification.PipelineNotificationManager.showProgressNotification(
                context = context,
                title = "GLITCH AI: Generating Video",
                statusMessage = msg,
                progressPercent = prog
            )
            onProgress(prog, msg)
        }
        saveCompletedLocalProject(result, request)
        result
    }

    // ----------------------------------------------------
    // REMOTE API / SERVER CLIENT
    // ----------------------------------------------------
    suspend fun checkHealth(): Resource<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.getApiService().checkHealth()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Server error: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("Cannot connect to server at ${settingsManager.getServerUrl()}: ${e.localizedMessage ?: "Unknown network error"}", e)
        }
    }

    suspend fun createJob(request: CreateJobRequest): Resource<CreateJobResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.getApiService().createJob(request)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Failed to create job: ${response.code()} ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.localizedMessage}", e)
        }
    }

    suspend fun getJobStatus(jobId: String): Resource<JobStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.getApiService().getJobStatus(jobId)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Failed to get job status: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.localizedMessage}", e)
        }
    }

    suspend fun cancelJob(jobId: String): Resource<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.getApiService().cancelJob(jobId)
            if (response.isSuccessful) {
                Resource.Success(true)
            } else {
                Resource.Error("Failed to cancel job: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.localizedMessage}", e)
        }
    }

    suspend fun fetchAndSyncProjects(): Resource<List<ProjectSummary>> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.getApiService().getProjects()
            if (response.isSuccessful && response.body() != null) {
                val serverProjects = response.body()!!.projects
                val entities = serverProjects.map { ProjectEntity.fromProjectSummary(it) }
                projectDao.insertProjects(entities)
                val serverIds = serverProjects.mapTo(mutableSetOf()) { it.id }
                val localGeneratedProjects = projectDao.getAllProjectsList()
                    .filter { it.pipelineStep == "DOM_VIDEO_COMPLETED" && it.id !in serverIds }
                    .map { it.toProjectSummary() }
                Resource.Success((serverProjects + localGeneratedProjects).sortedByDescending { it.createdAt })
            } else {
                val local = projectDao.getAllProjectsList().map { it.toProjectSummary() }
                Resource.Success(local)
            }
        } catch (e: Exception) {
            val local = projectDao.getAllProjectsList().map { it.toProjectSummary() }
            Resource.Success(local)
        }
    }

    suspend fun getProjectDetails(projectId: String): Resource<ProjectDetailsResponse> = withContext(Dispatchers.IO) {
        val localDetails = getLocalProjectDetails(projectId)
        if (localDetails != null) {
            return@withContext Resource.Success(localDetails)
        }

        try {
            val response = apiClient.getApiService().getProjectDetails(projectId)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Failed to fetch project details: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.localizedMessage}", e)
        }
    }

    suspend fun deleteProject(projectId: String): Resource<Boolean> = withContext(Dispatchers.IO) {
        try {
            projectDao.deleteProjectById(projectId)
            val response = apiClient.getApiService().deleteProject(projectId)
            Resource.Success(true)
        } catch (e: Exception) {
            projectDao.deleteProjectById(projectId)
            Resource.Success(true)
        }
    }

    suspend fun getLogs(): Resource<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.getApiService().getLogs()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.logs)
            } else {
                Resource.Error("Failed to fetch logs: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.localizedMessage}", e)
        }
    }
}
