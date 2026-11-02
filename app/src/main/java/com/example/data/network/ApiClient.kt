package com.example.data.network

import com.example.data.local.SettingsManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(private val settingsManager: SettingsManager) {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private var currentBaseUrl: String = ""
    private var currentService: AiVideoApiService? = null

    @Synchronized
    fun getApiService(): AiVideoApiService {
        val baseUrl = settingsManager.getServerUrl().trim().removeSuffix("/") + "/"
        if (currentService == null || currentBaseUrl != baseUrl) {
            currentBaseUrl = baseUrl
            currentService = createRetrofit(baseUrl).create(AiVideoApiService::class.java)
        }
        return currentService!!
    }

    private fun createRetrofit(baseUrl: String): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            level = if (settingsManager.settingsFlow.value.debugLogging) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
}
