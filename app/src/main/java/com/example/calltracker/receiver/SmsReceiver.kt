package com.example.calltracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.calltracker.data.local.entity.SmsEntity
import com.example.calltracker.repository.TrackerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val repository = TrackerRepository(context)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Give the default SMS app time to write to the provider
                    kotlinx.coroutines.delay(2000)
                    repository.syncSmsLogsToLocal()
                    repository.syncLatestSmsToApi()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
