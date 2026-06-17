package com.example.calltracker.ui.viewmodel

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calltracker.repository.TrackerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TrackerRepository(application)

    val allCallLogs = repository.allCallLogs
    val allSmsLogs = repository.allSmsLogs
    val allAppUsage = repository.allAppUsage
    val allRecordings = repository.allRecordings
    val allInstalledApps = repository.allInstalledApps
    val allNotifications = repository.allNotifications

    // Notifications State
    val notificationSearchQuery = MutableStateFlow("")
    val notificationAppFilter = MutableStateFlow("All")

    // SMS State
    val smsSearchQuery = MutableStateFlow("")
    val smsFilter = MutableStateFlow("All")

    // Calls State
    val callsSearchQuery = MutableStateFlow("")
    val callsTimeFilter = MutableStateFlow("All time")
    val callsTypeFilter = MutableStateFlow("All")

    fun triggerManualSync() {
        viewModelScope.launch {
            repository.syncCallLogsToLocal()
            repository.syncSmsLogsToLocal()
            repository.syncInstalledAppsToLocal()
            
            repository.syncLocalDataToRemote()
        }
    }

    // Audio Player State
    val currentlyPlayingPath = MutableStateFlow<String?>(null)
    private var mediaPlayer: MediaPlayer? = null

    fun playAudio(filePath: String) {
        if (currentlyPlayingPath.value == filePath) {
            stopAudio()
            return
        }
        
        stopAudio()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                
                val file = File(filePath)
                if (file.exists() && file.length() > 0) {
                    FileInputStream(file).use { fis ->
                        setDataSource(fis.fd)
                    }
                    prepare()
                    start()
                    setOnCompletionListener {
                        stopAudio()
                    }
                } else {
                    stopAudio()
                    Toast.makeText(getApplication(), "Recording file is empty or missing. It was likely blocked by Android privacy features.", Toast.LENGTH_LONG).show()
                    return
                }
            }
            currentlyPlayingPath.value = filePath
        } catch (e: Exception) {
            e.printStackTrace()
            stopAudio()
        }
    }

    fun stopAudio() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        currentlyPlayingPath.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
    }
}
