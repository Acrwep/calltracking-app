package com.example.calltracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whatsapp_calls")
data class WhatsAppCallEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactName: String,
    val number: String,
    val direction: String,
    val sessionType: String,
    val duration: String,
    val timestamp: Long,
    val isSynced: Boolean = false
)
