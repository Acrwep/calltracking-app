package com.example.calltracker.repository

import android.content.Context
import com.example.calltracker.data.local.AppDatabase
import com.example.calltracker.data.local.entity.RecordingEntity
import com.example.calltracker.data.local.entity.SmsEntity
import com.example.calltracker.data.local.entity.AppUsageEntity
import com.example.calltracker.data.local.entity.NotificationEntity
import com.example.calltracker.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody

class TrackerRepository(private val context: Context) {

    private val dao = AppDatabase.getDatabase(context).trackerDao()
    private val api = RetrofitClient.apiService

    private val callLogTracker = CallLogTracker(context)
    private val smsTracker = SmsTracker(context)
    private val usageTracker = AppUsageTracker(context)

    private var lastOpenedPackage: String? = null
    private var lastOpenedAppName: String? = null
    private var lastOpenedTime: Long = 0

    // Flow exposes for UI
    val allCallLogs = dao.getAllCallLogs()
    val allSmsLogs = dao.getAllSmsLogs()
    val allAppUsage = dao.getAllAppUsage()
    val allRecordings = dao.getAllRecordings()
    val allInstalledApps = dao.getAllInstalledApps()
    val allNotifications = dao.getAllNotifications()
    val allWhatsAppChats = dao.getAllWhatsAppChats()
    val allWhatsAppCalls = dao.getAllWhatsAppCalls()

    init {
        // Clear all unsynced app usage when the repository is initialized (app re-run)
        // This ensures only new app usages generated during this session are sent to the API.
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            dao.clearAllUnsyncedAppUsage()
        }
    }

    suspend fun syncCallLogsToLocal() = withContext(Dispatchers.IO) {
        val logs = callLogTracker.getRecentCallLogs()
        logs.forEach { 
            if (dao.checkCallLogExists(it.timestamp, it.phoneNumber) == 0) {
                dao.insertCallLog(it) 
            }
        }
    }

    suspend fun syncSmsLogsToLocal() = withContext(Dispatchers.IO) {
        val logs = smsTracker.getRecentSms()
        logs.forEach { 
            if (dao.checkSmsExists(it.timestamp, it.senderNumber) == 0) {
                dao.insertSmsLog(it) 
            }
        }
    }
    
    suspend fun insertSmsDirectly(sms: SmsEntity) = withContext(Dispatchers.IO) {
        if (dao.checkSmsExists(sms.timestamp, sms.senderNumber) == 0) {
            dao.insertSmsLog(sms)
        }
    }

    suspend fun insertSmsFromNotification(sender: String, body: String, timestamp: Long) = withContext(Dispatchers.IO) {
        // Use a loose check to avoid duplicating standard SMS that Google Messages also posts notifications for
        if (dao.checkSmsExistsLoose(timestamp, sender, body) == 0) {
            val entity = SmsEntity(
                senderNumber = sender,
                contactName = sender,
                messageBody = body,
                timestamp = timestamp,
                type = "Inbox",
                isSynced = false
            )
            dao.insertSmsLog(entity)
            // Force a sync to push this newly injected RCS message to the API
            syncLatestSmsToApi()
        }
    }

    suspend fun syncInstalledAppsToLocal() = withContext(Dispatchers.IO) {
        val apps = usageTracker.getInstalledApps()
        apps.forEach { dao.insertInstalledApp(it) }
    }

    suspend fun saveCurrentlyOpenedApp() = withContext(Dispatchers.IO) {
        val app = usageTracker.getCurrentlyOpenedApp()
        if (app != null && app.packageName != lastOpenedPackage) {
            val currentTime = app.timestamp
            
            // If we have a previous app recorded, calculate its duration and save it
            if (lastOpenedPackage != null && lastOpenedAppName != null && lastOpenedTime > 0) {
                val durationMillis = currentTime - lastOpenedTime
                if (durationMillis > 0) {
                    val previousAppUsage = AppUsageEntity(
                        packageName = lastOpenedPackage!!,
                        appName = lastOpenedAppName!!,
                        usageDurationMillis = durationMillis,
                        timestamp = lastOpenedTime
                    )
                    
                    val generatedId = dao.insertAppUsageAndGetId(previousAppUsage)
                    val insertedUsage = previousAppUsage.copy(id = generatedId)
                    
                    // Immediately send the previous app usage session to the API
                    syncSingleAppUsageToApi(insertedUsage)
                }
            }
            
            // Update state to the newly opened app
            lastOpenedPackage = app.packageName
            lastOpenedAppName = app.appName
            lastOpenedTime = currentTime
        }
    }
    
    private suspend fun syncSingleAppUsageToApi(usage: AppUsageEntity) = withContext(Dispatchers.IO) {
        try {
            val userId = com.example.calltracker.data.remote.AuthTokenHolder.userId ?: return@withContext
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            
            val request = com.example.calltracker.data.remote.AddAppUsageRequest(
                userId = userId,
                packageName = usage.packageName,
                appName = usage.appName,
                usageDuration = usage.usageDurationMillis / 1000,
                datePeriode = sdf.format(java.util.Date(usage.timestamp))
            )
            
            val response = api.addAppUsage(request)
            if (response.isSuccessful) {
                dao.updateAppUsage(listOf(usage.copy(isSynced = true)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun insertNotification(notification: NotificationEntity): Boolean = withContext(Dispatchers.IO) {
        if (dao.checkNotificationExists(notification.notificationKey, notification.timestamp) > 0) {
            return@withContext false
        }
        dao.insertNotification(notification)
        return@withContext true
    }

    suspend fun markNotificationAsRemoved(key: String) = withContext(Dispatchers.IO) {
        dao.markNotificationAsRemoved(key)
    }

    suspend fun insertWhatsAppChat(contactName: String, messageText: String, timestamp: Long, direction: String) = withContext(Dispatchers.IO) {
        if (dao.checkWhatsAppChatExistsLoose(timestamp, contactName, messageText) == 0) {
            val entity = com.example.calltracker.data.local.entity.WhatsAppChatEntity(
                packageName = "com.whatsapp",
                contactName = contactName,
                messageText = messageText,
                timestamp = timestamp,
                direction = direction,
                status = "Active",
                isSynced = false
            )
            dao.insertWhatsAppChat(entity)
            syncWhatsAppChatsToApi()
        }
    }

    suspend fun insertWhatsAppCall(contactName: String, number: String, direction: String, sessionType: String, duration: String, timestamp: Long) = withContext(Dispatchers.IO) {
        if (dao.checkWhatsAppCallExistsLoose(timestamp, contactName, duration) == 0) {
            val entity = com.example.calltracker.data.local.entity.WhatsAppCallEntity(
                contactName = contactName,
                number = number,
                direction = direction,
                sessionType = sessionType,
                duration = duration,
                timestamp = timestamp,
                isSynced = false
            )
            dao.insertWhatsAppCall(entity)
        }
    }

    // Audio Recording
    fun startRecording(prefix: String) = AudioRecorderManager.startRecording(context, prefix)
    
    suspend fun stopAndSaveRecording(filePath: String?, phoneNumber: String?) = withContext(Dispatchers.IO) {
        val duration = AudioRecorderManager.stopRecording()
        if (filePath != null) {
            dao.insertRecording(
                RecordingEntity(
                    filePath = filePath,
                    durationSeconds = duration,
                    timestamp = System.currentTimeMillis(),
                    relatedPhoneNumber = phoneNumber ?: "Unknown"
                )
            )
        }
    }

    // Remote Sync
    private var lastSyncedCallLogTimestamp: Long = 0

    suspend fun syncLatestCallLogToApi() = withContext(Dispatchers.IO) {
        try {
            val userId = com.example.calltracker.data.remote.AuthTokenHolder.userId ?: return@withContext
            
            // Get the most recent call log from the device
            val recentLogs = callLogTracker.getRecentCallLogs()
            if (recentLogs.isEmpty()) return@withContext
            
            val latestLog = recentLogs.first() // Assuming ORDER BY DATE DESC
            
            // Prevent duplicate submission
            if (latestLog.timestamp <= lastSyncedCallLogTimestamp) return@withContext
            lastSyncedCallLogTimestamp = latestLog.timestamp

            // Format date string as requested: "2026-06-08 11:30:00"
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val callTimeStr = sdf.format(java.util.Date(latestLog.timestamp))

            // Map "Incoming" to "INCOMING" etc.
            val typeStr = latestLog.callType.uppercase()

            val request = com.example.calltracker.data.remote.AddCallLogRequest(
                userId = userId,
                contactName = latestLog.contactName ?: "",
                phoneNumber = latestLog.phoneNumber,
                callType = typeStr,
                callTime = callTimeStr,
                duration = latestLog.durationSeconds,
                sourceApp = null
            )

            val response = api.addCallLog(request)
            if (!response.isSuccessful) {
                android.util.Log.e("TrackerRepository", "Failed to add call log to API: ${response.code()}")
            } else {
                android.util.Log.d("TrackerRepository", "Successfully added call log to API")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("TrackerRepository", "Error adding call log: ${e.message}")
        }
    }

    suspend fun syncLatestSmsToApi() = withContext(Dispatchers.IO) {
        try {
            val userId = com.example.calltracker.data.remote.AuthTokenHolder.userId ?: return@withContext

            // Fetch up to 10 unsynced SMS logs, prioritizing the newest ones
            // Only fetch from the last 5 minutes to prevent old backlog messages from being sent
            val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
            val unsyncedLogs = dao.getUnsyncedSmsLogsSince(fiveMinutesAgo).take(10)
            if (unsyncedLogs.isEmpty()) return@withContext

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())

            for (latestSms in unsyncedLogs) {
                val smsTimeStr = sdf.format(java.util.Date(latestSms.timestamp))

                val request = com.example.calltracker.data.remote.AddMessageRequest(
                    userId = userId,
                    senderId = latestSms.senderNumber,
                    messageBody = latestSms.messageBody,
                    timePeriod = smsTimeStr,
                    isRead = false,
                    attachmentUrl = null
                )

                val response = api.addMessage(request)
                if (response.isSuccessful) {
                    android.util.Log.d("TrackerRepository", "Successfully added SMS to API: ${latestSms.messageBody.take(10)}")
                    val updatedSms = latestSms.copy(isSynced = true)
                    dao.updateSmsLogs(listOf(updatedSms))
                } else {
                    android.util.Log.e("TrackerRepository", "Failed to add SMS to API: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("TrackerRepository", "Error adding SMS: ${e.message}")
        }
    }
    suspend fun syncLocalDataToRemote() = withContext(Dispatchers.IO) {
        try {
            val unsyncedCalls = dao.getUnsyncedCallLogs()
            if (unsyncedCalls.isNotEmpty()) {
                val response = api.syncCallLogs(unsyncedCalls)
                if (response.isSuccessful) {
                    dao.updateCallLogs(unsyncedCalls.map { it.copy(isSynced = true) })
                }
            }
            
            val unsyncedSms = dao.getUnsyncedSmsLogs()
            if (unsyncedSms.isNotEmpty()) {
                val response = api.syncSmsLogs(unsyncedSms)
                if (response.isSuccessful) {
                    dao.updateSmsLogs(unsyncedSms.map { it.copy(isSynced = true) })
                }
            }
            
            syncWhatsAppChatsToApi()
            syncWhatsAppCallsToApi()
            
            val unsyncedAppUsage = dao.getUnsyncedAppUsage().take(50)
            if (unsyncedAppUsage.isNotEmpty()) {
                val userId = com.example.calltracker.data.remote.AuthTokenHolder.userId
                if (userId != null) {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    for (usage in unsyncedAppUsage) {
                        try {
                            val request = com.example.calltracker.data.remote.AddAppUsageRequest(
                                userId = userId,
                                packageName = usage.packageName,
                                appName = usage.appName,
                                usageDuration = usage.usageDurationMillis / 1000,
                                datePeriode = sdf.format(java.util.Date(usage.timestamp))
                            )
                            val response = api.addAppUsage(request)
                            if (response.isSuccessful) {
                                dao.updateAppUsage(listOf(usage.copy(isSynced = true)))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            
            val unsyncedInstalledApps = dao.getUnsyncedInstalledApps()
            if (unsyncedInstalledApps.isNotEmpty()) {
                val response = api.syncInstalledApps(unsyncedInstalledApps)
                if (response.isSuccessful) {
                    dao.updateInstalledApps(unsyncedInstalledApps.map { it.copy(isSynced = true) })
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun uploadScreenshot(file: File, timestamp: Long) = withContext(Dispatchers.IO) {
        try {
            val userId = com.example.calltracker.data.remote.AuthTokenHolder.userId ?: return@withContext
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val timeStr = sdf.format(java.util.Date(timestamp))

            val requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file)
            val filePart = MultipartBody.Part.createFormData("screenshot_file", file.name, requestFile)
            val userIdPart = RequestBody.create(MediaType.parse("text/plain"), userId.toString())
            val timePart = RequestBody.create(MediaType.parse("text/plain"), timeStr)

            val response = api.uploadScreenshot(filePart, userIdPart, timePart)
            if (response.isSuccessful) {
                android.util.Log.d("TrackerRepository", "Successfully uploaded screenshot")
            } else {
                android.util.Log.e("TrackerRepository", "Failed to upload screenshot: ${response.code()}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("TrackerRepository", "Error uploading screenshot: ${e.message}")
        }
    }

    suspend fun syncNotificationToApi(entity: NotificationEntity) = withContext(Dispatchers.IO) {
        try {
            val userId = com.example.calltracker.data.remote.AuthTokenHolder.userId ?: return@withContext
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val timeStr = sdf.format(java.util.Date(entity.timestamp))

            val request = com.example.calltracker.data.remote.NotificationRequest(
                userId = userId,
                packageName = entity.packageName,
                appName = entity.appName,
                title = entity.title,
                textContent = entity.text ?: entity.bigText,
                postTime = timeStr
            )

            val response = api.addNotification(request)
            if (response.isSuccessful) {
                android.util.Log.d("TrackerRepository", "Successfully added notification to API")
            } else {
                android.util.Log.e("TrackerRepository", "Failed to add notification to API: ${response.code()}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("TrackerRepository", "Error adding notification: ${e.message}")
        }
    }

    suspend fun syncWhatsAppChatsToApi() = withContext(Dispatchers.IO) {
        try {
            val userId = com.example.calltracker.data.remote.AuthTokenHolder.userId ?: return@withContext
            val unsyncedChats = dao.getUnsyncedWhatsAppChats().take(20)
            if (unsyncedChats.isEmpty()) return@withContext

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())

            for (chat in unsyncedChats) {
                val timeStr = sdf.format(java.util.Date(chat.timestamp))

                val request = com.example.calltracker.data.remote.AddWhatsappChatLogRequest(
                    userId = userId,
                    diraction = chat.direction,
                    contactName = chat.contactName,
                    message = chat.messageText,
                    createdAt = timeStr
                )

                val response = api.addWhatsappChatLog(request)
                if (response.isSuccessful) {
                    android.util.Log.d("TrackerRepository", "Successfully added WhatsApp chat to API")
                    val updatedChat = chat.copy(isSynced = true)
                    dao.updateWhatsAppChats(listOf(updatedChat))
                } else {
                    android.util.Log.e("TrackerRepository", "Failed to add WhatsApp chat to API: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("TrackerRepository", "Error adding WhatsApp chat: ${e.message}")
        }
    }

    suspend fun syncWhatsAppCallsToApi() = withContext(Dispatchers.IO) {
        try {
            val userId = com.example.calltracker.data.remote.AuthTokenHolder.userId ?: return@withContext
            val unsyncedCalls = dao.getUnsyncedWhatsAppCalls().take(20)
            if (unsyncedCalls.isEmpty()) return@withContext

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())

            for (call in unsyncedCalls) {
                val timeStr = sdf.format(java.util.Date(call.timestamp))

                val request = com.example.calltracker.data.remote.AddWhatsappCallLogRequest(
                    userId = userId,
                    diraction = call.direction,
                    contactName = call.contactName,
                    callType = call.sessionType,
                    duration = call.duration,
                    createdAt = timeStr
                )

                val response = api.addWhatsappCallLog(request)
                if (response.isSuccessful) {
                    android.util.Log.d("TrackerRepository", "Successfully added WhatsApp call to API")
                    val updatedCall = call.copy(isSynced = true)
                    dao.updateWhatsAppCalls(listOf(updatedCall))
                } else {
                    android.util.Log.e("TrackerRepository", "Failed to add WhatsApp call to API: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("TrackerRepository", "Error adding WhatsApp call: ${e.message}")
        }
    }
}
