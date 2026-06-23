package com.example.calltracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whatsapp_chats")
data class WhatsAppChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val contactName: String,
    val messageText: String,
    val timestamp: Long,
    val direction: String,
    val status: String = "Active",
    val isSynced: Boolean = false
)
