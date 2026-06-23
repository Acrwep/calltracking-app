package com.example.calltracker.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.calltracker.data.local.entity.NotificationEntity
import com.example.calltracker.repository.TrackerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class NotificationTrackerService : NotificationListenerService() {

    private val serviceJob = Job()
    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("NotificationTracker", "Coroutine exception", throwable)
    }
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob + exceptionHandler)
    private lateinit var repository: TrackerRepository

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("NotificationTracker", "onCreate called")
        repository = TrackerRepository(applicationContext)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        android.util.Log.d("NotificationTracker", "onListenerDisconnected called")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            val activeNotifications = activeNotifications
            activeNotifications?.forEach { sbn ->
                processNotification(sbn)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let { processNotification(it) }
    }

    private fun processNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val notificationKey = sbn.key
        val timestamp = sbn.postTime

        val pm = applicationContext.packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }

        // Bridge RCS/Google Messages into SMS
        if (packageName == "com.google.android.apps.messaging") {
            if (!title.isNullOrBlank() && !text.isNullOrBlank()) {
                serviceScope.launch {
                    repository.insertSmsFromNotification(title, text, timestamp)
                }
            }
        }

        // WhatsApp Chat Monitoring
        if (packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b") {
            val contactName = title ?: "Unknown"
            
            // Try to extract from MessagingStyle
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (messages != null && messages.isNotEmpty()) {
                for (msg in messages) {
                    val bundle = msg as? android.os.Bundle ?: continue
                    val msgText = bundle.getCharSequence("text")?.toString() ?: continue
                    val msgTimestamp = bundle.getLong("time", timestamp)
                    
                    val senderPerson = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        bundle.getParcelable("sender_person") as? android.app.Person
                    } else {
                        @Suppress("DEPRECATION")
                        bundle.getParcelable("sender_person") as? android.app.Person
                    }
                    val senderName = senderPerson?.name?.toString()
                    
                    // If senderPerson is null, it typically means it's the user's own sent message (Outgoing)
                    val direction = if (senderPerson == null || senderName == null) "Outgoing" else "Incoming"
                    
                    serviceScope.launch {
                        repository.insertWhatsAppChat(contactName, msgText, msgTimestamp, direction)
                    }
                }
            } else {
                // Fallback for non-MessagingStyle notifications
                if (!text.isNullOrBlank() && !text.contains("new messages") && !text.contains("Checking for new messages")) {
                    serviceScope.launch {
                        repository.insertWhatsAppChat(contactName, text, timestamp, "Incoming")
                    }
                }
            }
        }

        // Specifically tracking WhatsApp and others
        val entity = NotificationEntity(
            packageName = packageName,
            appName = appName,
            title = title,
            text = text,
            bigText = bigText,
            timestamp = timestamp,
            notificationKey = notificationKey,
            isRemoved = false
        )

        serviceScope.launch {
            val isNew = repository.insertNotification(entity)
            if (isNew) {
                // Sync to remote API immediately only if it's a new notification
                repository.syncNotificationToApi(entity)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn ?: return

        serviceScope.launch {
            repository.markNotificationAsRemoved(sbn.key)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("NotificationTracker", "onDestroy called")
        serviceJob.cancel()
    }
}
