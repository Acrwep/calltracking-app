package com.example.calltracker

import android.app.Application
import android.content.Context
import com.example.calltracker.repository.SessionManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

class CallTrackerApplication : Application() {

    companion object {
        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        
        // Synchronously read the token on app startup so it's ready for background services
        runBlocking {
            val sessionManager = SessionManager(applicationContext)
            sessionManager.userSession.firstOrNull()
        }
    }
}
