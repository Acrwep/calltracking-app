package com.example.calltracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.example.calltracker.repository.TrackerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PhoneStateReceiver : BroadcastReceiver() {

    companion object {
        private var currentRecordingPath: String? = null
        private var incomingNumber: String? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            
            val repository = TrackerRepository(context)

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    // Ringing
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    // Call answered/started
                    val serviceIntent = Intent(context, com.example.calltracker.service.TrackerService::class.java).apply {
                        action = "ACTION_START_RECORDING"
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    // Call ended
                    val serviceIntent = Intent(context, com.example.calltracker.service.TrackerService::class.java).apply {
                        action = "ACTION_STOP_RECORDING"
                        putExtra("INCOMING_NUMBER", incomingNumber)
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }
        }
    }
}
