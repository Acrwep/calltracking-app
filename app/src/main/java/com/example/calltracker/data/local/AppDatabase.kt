package com.example.calltracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.calltracker.data.local.dao.TrackerDao
import com.example.calltracker.data.local.entity.AppUsageEntity
import com.example.calltracker.data.local.entity.CallLogEntity
import com.example.calltracker.data.local.entity.RecordingEntity
import com.example.calltracker.data.local.entity.SmsEntity
import com.example.calltracker.data.local.entity.InstalledAppEntity
import com.example.calltracker.data.local.entity.NotificationEntity

@Database(
    entities = [CallLogEntity::class, SmsEntity::class, AppUsageEntity::class, RecordingEntity::class, InstalledAppEntity::class, NotificationEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trackerDao(): TrackerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tracker_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
