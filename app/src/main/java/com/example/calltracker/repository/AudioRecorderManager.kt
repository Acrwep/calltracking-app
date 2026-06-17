package com.example.calltracker.repository

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

object AudioRecorderManager {

    private var mediaRecorder: MediaRecorder? = null
    var currentFilePath: String? = null
    private var startTime: Long = 0

    fun startRecording(context: Context, fileNamePrefix: String): String? {
        try {
            val audioDir = File(context.filesDir, "recordings")
            if (!audioDir.exists()) audioDir.mkdirs()

            val fileName = "${fileNamePrefix}_${System.currentTimeMillis()}.mp4"
            val file = File(audioDir, fileName)
            currentFilePath = file.absolutePath

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            mediaRecorder?.apply {
                // Using MIC to capture the user's voice during a call
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(currentFilePath)
                prepare()
                start()
            }
            startTime = System.currentTimeMillis()
            return currentFilePath
        } catch (e: Exception) {
            e.printStackTrace()
            // On modern Android (10+), starting the microphone during an active call often throws an exception
            // because the Phone app has exclusive access. We still return the file path so the database 
            // records the attempt, and the UI can show the "blocked by privacy" message.
            return currentFilePath
        }
    }

    fun stopRecording(): Long {
        var duration = 0L
        if (startTime > 0) {
            duration = (System.currentTimeMillis() - startTime) / 1000
        }
        
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            startTime = 0
        }
        return duration
    }
}
