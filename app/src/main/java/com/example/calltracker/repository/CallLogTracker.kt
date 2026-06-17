package com.example.calltracker.repository

import android.content.Context
import android.provider.CallLog
import com.example.calltracker.data.local.entity.CallLogEntity
import java.util.Date

class CallLogTracker(private val context: Context) {

    fun getRecentCallLogs(sinceTimestamp: Long = 0): List<CallLogEntity> {
        val logs = mutableListOf<CallLogEntity>()
        
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null,
                "${CallLog.Calls.DATE} > ?",
                arrayOf(sinceTimestamp.toString()),
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                val numberColumn = it.getColumnIndex(CallLog.Calls.NUMBER)
                val typeColumn = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateColumn = it.getColumnIndex(CallLog.Calls.DATE)
                val durationColumn = it.getColumnIndex(CallLog.Calls.DURATION)
                val nameColumn = it.getColumnIndex(CallLog.Calls.CACHED_NAME)

                while (it.moveToNext()) {
                    val number = it.getString(numberColumn)
                    val type = when (it.getInt(typeColumn)) {
                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        CallLog.Calls.MISSED_TYPE -> "Missed"
                        CallLog.Calls.REJECTED_TYPE -> "Rejected"
                        else -> "Unknown"
                    }
                    val timestamp = it.getLong(dateColumn)
                    val duration = it.getLong(durationColumn)
                    val contactName = if (nameColumn != -1) it.getString(nameColumn) else null

                    logs.add(
                        CallLogEntity(
                            phoneNumber = number ?: "Unknown",
                            contactName = contactName,
                            callType = type,
                            durationSeconds = duration,
                            timestamp = timestamp
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            // Handled when permission is missing
        }

        return logs
    }
}
