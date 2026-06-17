package com.example.calltracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.calltracker.data.local.entity.AppUsageEntity
import com.example.calltracker.data.local.entity.CallLogEntity
import com.example.calltracker.data.local.entity.RecordingEntity
import com.example.calltracker.data.local.entity.SmsEntity
import com.example.calltracker.data.local.entity.InstalledAppEntity
import com.example.calltracker.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {

    // Call Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCallLog(callLog: CallLogEntity)

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogs(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE isSynced = 0")
    fun getUnsyncedCallLogs(): List<CallLogEntity>

    @Query("SELECT COUNT(*) FROM call_logs WHERE timestamp = :timestamp AND phoneNumber = :phoneNumber")
    fun checkCallLogExists(timestamp: Long, phoneNumber: String): Int

    @Update
    fun updateCallLogs(logs: List<CallLogEntity>)

    // SMS Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSmsLog(sms: SmsEntity)

    @Query("SELECT * FROM sms_logs ORDER BY timestamp DESC")
    fun getAllSmsLogs(): Flow<List<SmsEntity>>

    @Query("SELECT * FROM sms_logs WHERE isSynced = 0 ORDER BY timestamp DESC")
    fun getUnsyncedSmsLogs(): List<SmsEntity>

    @Query("SELECT * FROM sms_logs WHERE isSynced = 0 AND timestamp > :minTimestamp ORDER BY timestamp DESC")
    fun getUnsyncedSmsLogsSince(minTimestamp: Long): List<SmsEntity>

    @Query("SELECT COUNT(*) FROM sms_logs WHERE timestamp = :timestamp AND senderNumber = :senderNumber")
    fun checkSmsExists(timestamp: Long, senderNumber: String): Int

    @Query("SELECT COUNT(*) FROM sms_logs WHERE senderNumber = :senderNumber AND messageBody = :messageBody AND timestamp BETWEEN :timestamp - 60000 AND :timestamp + 60000")
    fun checkSmsExistsLoose(timestamp: Long, senderNumber: String, messageBody: String): Int

    @Update
    fun updateSmsLogs(logs: List<SmsEntity>)

    // App Usage
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAppUsage(appUsage: AppUsageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAppUsageAndGetId(appUsage: AppUsageEntity): Long

    @Query("SELECT * FROM app_usage_logs ORDER BY timestamp DESC")
    fun getAllAppUsage(): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage_logs WHERE isSynced = 0")
    fun getUnsyncedAppUsage(): List<AppUsageEntity>

    @Query("DELETE FROM app_usage_logs WHERE isSynced = 0 AND usageDurationMillis > 0")
    fun clearUnsyncedCumulativeAppUsage()

    @Query("DELETE FROM app_usage_logs WHERE isSynced = 0")
    fun clearAllUnsyncedAppUsage()

    @Update
    fun updateAppUsage(usage: List<AppUsageEntity>)

    // Recordings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecording(recording: RecordingEntity)

    @Query("SELECT * FROM recording_logs ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    // Installed Apps
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertInstalledApp(app: InstalledAppEntity)

    @Query("SELECT * FROM installed_apps ORDER BY appName ASC")
    fun getAllInstalledApps(): Flow<List<InstalledAppEntity>>

    @Query("SELECT * FROM installed_apps WHERE isSynced = 0")
    fun getUnsyncedInstalledApps(): List<InstalledAppEntity>

    @Update
    fun updateInstalledApps(apps: List<InstalledAppEntity>)

    // Notifications
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNotification(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("UPDATE notifications SET isRemoved = 1 WHERE notificationKey = :key")
    fun markNotificationAsRemoved(key: String)

    @Query("SELECT COUNT(*) FROM notifications WHERE notificationKey = :key AND timestamp = :timestamp")
    fun checkNotificationExists(key: String, timestamp: Long): Int
}
