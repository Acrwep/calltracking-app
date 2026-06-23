package com.example.calltracker.data.remote

import com.example.calltracker.data.local.entity.AppUsageEntity
import com.example.calltracker.data.local.entity.CallLogEntity
import com.example.calltracker.data.local.entity.RecordingEntity
import com.example.calltracker.data.local.entity.SmsEntity
import com.example.calltracker.data.local.entity.InstalledAppEntity
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @POST("api/add-messages")
    suspend fun addMessage(@Body request: AddMessageRequest): Response<Void>

    @POST("api/add-logs")
    suspend fun addCallLog(@Body request: AddCallLogRequest): Response<Void>

    @POST("api/signup")
    suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>

    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/add-app-usage")
    suspend fun addAppUsage(@Body request: AddAppUsageRequest): Response<Void>

    @POST("api/v1/sync/calls")
    suspend fun syncCallLogs(@Body logs: List<CallLogEntity>): Response<Void>

    @POST("api/v1/sync/sms")
    suspend fun syncSmsLogs(@Body logs: List<SmsEntity>): Response<Void>

    @POST("api/v1/sync/app-usage")
    suspend fun syncAppUsage(@Body logs: List<AppUsageEntity>): Response<Void>

    @Multipart
    @POST("api/v1/sync/recordings")
    suspend fun uploadRecording(
        @Part("recordingInfo") recording: RecordingEntity,
        @Part file: MultipartBody.Part
    ): Response<Void>

    @POST("api/v1/sync/installed-apps")
    suspend fun syncInstalledApps(@Body apps: List<InstalledAppEntity>): Response<Void>

    @Multipart
    @POST("api/add-screenshot")
    suspend fun uploadScreenshot(
        @Part screenshot_file: MultipartBody.Part,
        @Part("user_id") userId: RequestBody,
        @Part("capture_time") captureTime: RequestBody
    ): Response<Void>

    @POST("api/add-notifications")
    suspend fun addNotification(@Body request: NotificationRequest): Response<Void>

    @POST("api/add-whatsapp-chat-logs")
    suspend fun addWhatsappChatLog(@Body request: AddWhatsappChatLogRequest): Response<Void>

    @POST("api/add-whatsapp-call-logs")
    suspend fun addWhatsappCallLog(@Body request: AddWhatsappCallLogRequest): Response<Void>
}
