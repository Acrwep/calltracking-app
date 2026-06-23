package com.example.calltracker.data.remote

import com.google.gson.annotations.SerializedName

data class SignupRequest(
    @SerializedName("full_name") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("mobile_number") val mobileNumber: String,
    @SerializedName("password_hash") val passwordHash: String
)

data class AuthResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("token") val token: String?,
    @SerializedName("user_id") val userId: Int?,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("mobile_number") val mobileNumber: String?
)

data class LoginRequest(
    @SerializedName("mobile_number") val mobileNumber: String,
    @SerializedName("password") val password: String
)

data class AddCallLogRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("contact_name") val contactName: String,
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("call_type") val callType: String,
    @SerializedName("call_time") val callTime: String,
    @SerializedName("duration") val duration: Long,
    @SerializedName("source_app") val sourceApp: String? = null
)

data class AddMessageRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("message_body") val messageBody: String,
    @SerializedName("time_periode") val timePeriod: String,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("attachment_url") val attachmentUrl: String? = null
)

data class AddAppUsageRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("package_name") val packageName: String,
    @SerializedName("app_name") val appName: String,
    @SerializedName("usage_duration") val usageDuration: Long,
    @SerializedName("date_periode") val datePeriode: String
)

data class NotificationRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("package_name") val packageName: String,
    @SerializedName("app_name") val appName: String,
    @SerializedName("title") val title: String?,
    @SerializedName("text_content") val textContent: String?,
    @SerializedName("post_time") val postTime: String
)

data class AddWhatsappChatLogRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("diraction") val diraction: String,
    @SerializedName("contact_name") val contactName: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("created_at") val createdAt: String? = null
)

data class AddWhatsappCallLogRequest(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("diraction") val diraction: String,
    @SerializedName("contact_name") val contactName: String?,
    @SerializedName("call_type") val callType: String?,
    @SerializedName("duration") val duration: String?,
    @SerializedName("created_at") val createdAt: String? = null
)

