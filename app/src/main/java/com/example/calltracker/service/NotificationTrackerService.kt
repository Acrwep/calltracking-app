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
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var repository: TrackerRepository

    override fun onCreate() {
        super.onCreate()
        repository = TrackerRepository(applicationContext)
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
        serviceJob.cancel()
    }
}
