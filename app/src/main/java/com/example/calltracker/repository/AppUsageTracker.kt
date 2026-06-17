package com.example.calltracker.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.app.usage.UsageEvents
import android.content.pm.ApplicationInfo
import com.example.calltracker.data.local.entity.AppUsageEntity
import com.example.calltracker.data.local.entity.InstalledAppEntity

class AppUsageTracker(private val context: Context) {

    fun getAppUsageStats(startTime: Long, endTime: Long): List<AppUsageEntity> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager
        
        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return emptyList()

        return usageStatsList.filter { it.totalTimeInForeground > 0 }
            .map { stats ->
                val appName = try {
                    val appInfo = pm.getApplicationInfo(stats.packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    stats.packageName
                }

                AppUsageEntity(
                    packageName = stats.packageName,
                    appName = appName,
                    usageDurationMillis = stats.totalTimeInForeground,
                    timestamp = System.currentTimeMillis()
                )
            }
    }

    fun getInstalledApps(): List<InstalledAppEntity> {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)
        val installedApps = mutableListOf<InstalledAppEntity>()

        for (packageInfo in packages) {
            val appInfo = packageInfo.applicationInfo ?: continue
            // Filter out system apps, or keep them if requested. Usually, we filter out non-launchable or system apps.
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!isSystemApp || pm.getLaunchIntentForPackage(packageInfo.packageName) != null) {
                val appName = pm.getApplicationLabel(appInfo).toString()
                installedApps.add(
                    InstalledAppEntity(
                        packageName = packageInfo.packageName,
                        appName = appName,
                        installTime = packageInfo.firstInstallTime
                    )
                )
            }
        }
        return installedApps
    }

    fun getCurrentlyOpenedApp(): AppUsageEntity? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 5 // Check last 5 minutes

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var currentPackageName: String? = null
        var lastEventTime: Long = 0

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                if (event.timeStamp > lastEventTime) {
                    currentPackageName = event.packageName
                    lastEventTime = event.timeStamp
                }
            }
        }

        if (currentPackageName != null) {
            val pm = context.packageManager
            val appName = try {
                val appInfo = pm.getApplicationInfo(currentPackageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                currentPackageName
            }
            
            return AppUsageEntity(
                packageName = currentPackageName,
                appName = appName,
                usageDurationMillis = 0, // Duration is 0 for an open event
                timestamp = lastEventTime
            )
        }
        return null
    }
}
