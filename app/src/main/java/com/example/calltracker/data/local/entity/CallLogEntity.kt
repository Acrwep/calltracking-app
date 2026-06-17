package com.example.calltracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val contactName: String? = null,
    val callType: String, // Incoming, Outgoing, Missed
    val durationSeconds: Long,
    val timestamp: Long,
    val isSynced: Boolean = false
)
