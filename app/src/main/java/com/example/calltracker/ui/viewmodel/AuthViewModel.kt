package com.example.calltracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calltracker.data.remote.LoginRequest
import com.example.calltracker.data.remote.RetrofitClient
import com.example.calltracker.data.remote.SignupRequest
import com.example.calltracker.repository.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String?) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    val userSession = sessionManager.userSession

    private val _loginState = MutableStateFlow<AuthState>(AuthState.Idle)
    val loginState: StateFlow<AuthState> = _loginState

    private val _signupState = MutableStateFlow<AuthState>(AuthState.Idle)
    val signupState: StateFlow<AuthState> = _signupState

    fun login(mobileNumber: String, password: String) {
        if (mobileNumber.isBlank() || password.isBlank()) {
            _loginState.value = AuthState.Error("Mobile number and password are required")
            return
        }

        _loginState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.login(LoginRequest(mobileNumber, password))
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null && authResponse.success) {
                        sessionManager.saveSession(
                            token = authResponse.token ?: "",
                            userId = authResponse.userId ?: 0,
                            fullName = authResponse.fullName,
                            mobileNumber = authResponse.mobileNumber
                        )
                        _loginState.value = AuthState.Success(authResponse.message)
                    } else {
                        _loginState.value = AuthState.Error(authResponse?.message ?: "Login failed")
                    }
                } else {
                    _loginState.value = AuthState.Error("Login failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _loginState.value = AuthState.Error("Network error: ${e.message}")
            }
        }
    }

    fun signup(fullName: String, email: String, mobileNumber: String, password: String) {
        if (fullName.isBlank() || email.isBlank() || mobileNumber.isBlank() || password.isBlank()) {
            _signupState.value = AuthState.Error("All fields are required")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _signupState.value = AuthState.Error("Invalid email format")
            return
        }

        if (mobileNumber.length != 10 || !mobileNumber.all { it.isDigit() }) {
            _signupState.value = AuthState.Error("Invalid mobile number")
            return
        }

        _signupState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val request = SignupRequest(fullName, email, mobileNumber, password)
                val response = RetrofitClient.apiService.signup(request)
                
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse != null && authResponse.success) {
                        _signupState.value = AuthState.Success(authResponse.message ?: "Signup successful")
                    } else {
                        _signupState.value = AuthState.Error(authResponse?.message ?: "Signup failed")
                    }
                } else {
                    _signupState.value = AuthState.Error("Signup failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _signupState.value = AuthState.Error("Network error: ${e.message}")
            }
        }
    }

    fun resetStates() {
        _loginState.value = AuthState.Idle
        _signupState.value = AuthState.Idle
    }
    
    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
        }
    }
}
