package com.example.data.network

import com.example.data.model.CreateJobRequest
import com.example.data.model.CreateJobResponse
import com.example.data.model.HealthResponse
import com.example.data.model.JobStatusResponse
import com.example.data.model.LogsResponse
import com.example.data.model.ProjectDetailsResponse
import com.example.data.model.ProjectListResponse
import com.example.data.model.SimpleSuccessResponse
import retrofit2.Response
import retrofit2.http.*

interface AiVideoApiService {

    @GET("/health")
    suspend fun checkHealth(): Response<HealthResponse>

    @POST("/jobs")
    suspend fun createJob(@Body request: CreateJobRequest): Response<CreateJobResponse>

    @GET("/jobs/{id}")
    suspend fun getJobStatus(@Path("id") jobId: String): Response<JobStatusResponse>

    @POST("/jobs/{id}/cancel")
    suspend fun cancelJob(@Path("id") jobId: String): Response<SimpleSuccessResponse>

    @GET("/projects")
    suspend fun getProjects(): Response<ProjectListResponse>

    @GET("/projects/{id}")
    suspend fun getProjectDetails(@Path("id") projectId: String): Response<ProjectDetailsResponse>

    @DELETE("/projects/{id}")
    suspend fun deleteProject(@Path("id") projectId: String): Response<SimpleSuccessResponse>

    @GET("/logs")
    suspend fun getLogs(): Response<LogsResponse>
}
