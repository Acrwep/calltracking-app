package com.example.calltracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recording_logs")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val durationSeconds: Long,
    val timestamp: Long,
    val relatedPhoneNumber: String? = null, // Null if manual recording
    val isSynced: Boolean = false
)
