package com.example.calltracker.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.calltracker.data.remote.AuthTokenHolder

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_session")

class SessionManager(private val context: Context) {

    companion object {
        val TOKEN_KEY = stringPreferencesKey("jwt_token")
        val USER_ID_KEY = intPreferencesKey("user_id")
        val FULL_NAME_KEY = stringPreferencesKey("full_name")
        val MOBILE_NUMBER_KEY = stringPreferencesKey("mobile_number")
    }

    val userSession: Flow<UserSessionData?> = context.dataStore.data.map { preferences ->
        val token = preferences[TOKEN_KEY]
        val userId = preferences[USER_ID_KEY]
        val fullName = preferences[FULL_NAME_KEY]
        val mobileNumber = preferences[MOBILE_NUMBER_KEY]

        if (token != null && userId != null) {
            AuthTokenHolder.token = token
            AuthTokenHolder.userId = userId
            UserSessionData(token, userId, fullName, mobileNumber)
        } else {
            AuthTokenHolder.token = null
            AuthTokenHolder.userId = null
            null
        }
    }

    suspend fun saveSession(token: String, userId: Int, fullName: String?, mobileNumber: String?) {
        AuthTokenHolder.token = token
        AuthTokenHolder.userId = userId
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USER_ID_KEY] = userId
            if (fullName != null) preferences[FULL_NAME_KEY] = fullName
            if (mobileNumber != null) preferences[MOBILE_NUMBER_KEY] = mobileNumber
        }
    }

    suspend fun clearSession() {
        AuthTokenHolder.token = null
        AuthTokenHolder.userId = null
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

data class UserSessionData(
    val token: String,
    val userId: Int,
    val fullName: String?,
    val mobileNumber: String?
)
