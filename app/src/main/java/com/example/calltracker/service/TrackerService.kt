package com.example.calltracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import com.example.calltracker.repository.TrackerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TrackerService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var repository: TrackerRepository
    private var screenshotManager: ScreenshotManager? = null

    private val phoneStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                
                when (state) {
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        serviceScope.launch {
                            delay(1000)
                            repository.startRecording("Call")
                        }
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        serviceScope.launch {
                            val path = com.example.calltracker.repository.AudioRecorderManager.currentFilePath
                            repository.stopAndSaveRecording(path, incomingNumber)
                            
                            // Give Android a moment to write the call log to the provider
                            delay(2000)
                            repository.syncLatestCallLogToApi()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = TrackerRepository(this)
        createNotificationChannel()
        
        val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        registerReceiver(phoneStateReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()

        var fgsType = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fgsType = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            
            // Only add microphone if permission is granted
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    fgsType = fgsType or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                }
            }
        }

        var hasScreenCapture = false
        intent?.let {
            val resultCode = it.getIntExtra("EXTRA_RESULT_CODE", android.app.Activity.RESULT_CANCELED)
            val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelableExtra("EXTRA_RESULT_DATA", Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelableExtra("EXTRA_RESULT_DATA")
            }
            
            if (resultCode == android.app.Activity.RESULT_OK && resultData != null) {
                hasScreenCapture = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    fgsType = fgsType or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                }
            }
            
            android.util.Log.d("TrackerService", "onStartCommand: resultCode=$resultCode, resultData=$resultData, manager=$screenshotManager")

            if (hasScreenCapture && screenshotManager == null && resultData != null) {
                android.util.Log.d("TrackerService", "Initializing ScreenshotManager")
                screenshotManager = ScreenshotManager(this, resultCode, resultData, repository)
                screenshotManager?.start()
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, fgsType)
            } else {
                startForeground(1, notification)
            }
        } catch (e: SecurityException) {
            android.util.Log.e("TrackerService", "Failed to start FGS with type $fgsType", e)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } catch (e2: Exception) {
                    android.util.Log.e("TrackerService", "Fallback FGS start failed", e2)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TrackerService", "Error starting foreground service", e)
        }

        startPeriodicSync()
        monitorForegroundApps()

        return START_STICKY
    }

    private fun startPeriodicSync() {
        serviceScope.launch {
            while (true) {
                // Sync every 15 minutes
                repository.syncCallLogsToLocal()
                repository.syncInstalledAppsToLocal()
                repository.syncLocalDataToRemote()
                delay(15 * 60 * 1000L)
            }
        }
    }

    private fun monitorForegroundApps() {
        serviceScope.launch {
            while (true) {
                repository.saveCurrentlyOpenedApp()
                delay(5000L) // Poll every 5 seconds
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "TrackerServiceChannel",
                "Tracker Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "TrackerServiceChannel")
            .setContentTitle("CallTracker is running")
            .setContentText("Monitoring in background...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(phoneStateReceiver)
        } catch (e: IllegalArgumentException) {
            android.util.Log.e("TrackerService", "Receiver not registered", e)
        }
        screenshotManager?.stop()
        serviceJob.cancel()
    }
}
