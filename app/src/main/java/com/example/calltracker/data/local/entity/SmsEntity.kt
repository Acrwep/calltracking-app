package com.example.calltracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_logs")
data class SmsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderNumber: String,
    val contactName: String? = null,
    val messageBody: String,
    val timestamp: Long,
    val type: String, // Inbox, Sent
    val isSynced: Boolean = false
)
