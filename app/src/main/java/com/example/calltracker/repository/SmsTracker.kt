package com.example.calltracker.repository

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import com.example.calltracker.data.local.entity.SmsEntity

class SmsTracker(private val context: Context) {

    fun getRecentSms(sinceTimestamp: Long = 0): List<SmsEntity> {
        val smsList = mutableListOf<SmsEntity>()

        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                "${Telephony.Sms.DATE} > ?",
                arrayOf(sinceTimestamp.toString()),
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use {
                val addressColumn = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyColumn = it.getColumnIndex(Telephony.Sms.BODY)
                val dateColumn = it.getColumnIndex(Telephony.Sms.DATE)
                val typeColumn = it.getColumnIndex(Telephony.Sms.TYPE)

                while (it.moveToNext()) {
                    val address = it.getString(addressColumn) ?: "Unknown"
                    val body = it.getString(bodyColumn) ?: ""
                    val timestamp = it.getLong(dateColumn)
                    val type = when (it.getInt(typeColumn)) {
                        Telephony.Sms.MESSAGE_TYPE_INBOX -> "Inbox"
                        Telephony.Sms.MESSAGE_TYPE_SENT -> "Sent"
                        else -> "Other"
                    }
                    val contactName = getContactName(address)

                    smsList.add(
                        SmsEntity(
                            senderNumber = address,
                            contactName = contactName,
                            messageBody = body,
                            timestamp = timestamp,
                            type = type
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        return smsList
    }

    private fun getContactName(phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        var contactName: String? = null
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        contactName = it.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return contactName
    }
}
