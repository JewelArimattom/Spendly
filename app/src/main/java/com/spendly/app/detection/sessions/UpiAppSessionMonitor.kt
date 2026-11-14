package com.spendly.app.detection.sessions

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import com.spendly.app.core.database.dao.UpiSessionDao
import com.spendly.app.core.database.entity.UpiSessionEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpiAppSessionMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val upiSessionDao: UpiSessionDao
) {

    companion object {
        private const val TAG = "UpiAppSessionMonitor"

        const val PACKAGE_GOOGLE_PAY = "com.google.android.apps.nbu.paisa.user"
        const val PACKAGE_PHONEPE = "com.phonepe.app"
        const val PACKAGE_PAYTM = "net.one97.paytm"
        const val PACKAGE_BHIM = "in.org.npci.upiapp"
        const val PACKAGE_CRED = "com.dreamplug.androidapp"

        val MONITORED_PACKAGES = setOf(
            PACKAGE_GOOGLE_PAY,
            PACKAGE_PHONEPE,
            PACKAGE_PAYTM,
            PACKAGE_BHIM,
            PACKAGE_CRED
        )

        val APP_NAMES = mapOf(
            PACKAGE_GOOGLE_PAY to "Google Pay",
            PACKAGE_PHONEPE to "PhonePe",
            PACKAGE_PAYTM to "Paytm",
            PACKAGE_BHIM to "BHIM",
            PACKAGE_CRED to "CRED"
        )

        // Merge app switches happening within 45 seconds
        const val SHORT_SWITCH_MERGE_THRESHOLD_MILLIS = 45_000L

        // Timeout for considering a session ended if no background event was received (5 mins)
        const val SESSION_TIMEOUT_MILLIS = 5 * 60 * 1000L

        fun getAppName(packageName: String): String = APP_NAMES[packageName] ?: packageName
        fun isMonitoredPackage(packageName: String?): Boolean = packageName != null && MONITORED_PACKAGES.contains(packageName)
    }

    /**
     * Checks whether the app has been granted PACKAGE_USAGE_STATS permission via AppOpsManager.
     */
    fun hasUsageAccessPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Records a foreground transition (e.g. ACTIVITY_RESUMED) for a monitored UPI app.
     * Merges short app switches within the 45-second threshold.
     */
    suspend fun onAppForeground(
        packageName: String,
        timestamp: Long = System.currentTimeMillis()
    ): UpiSessionEntity = withContext(Dispatchers.IO) {
        if (!MONITORED_PACKAGES.contains(packageName)) {
            throw IllegalArgumentException("Package $packageName is not a monitored UPI app")
        }

        val latest = upiSessionDao.getLatestSessionForPackage(packageName)
        if (latest != null) {
            // If already active, keep it active
            if (latest.sessionStatus == "ACTIVE") {
                return@withContext latest
            }

            // If ended recently within 45 seconds, merge into existing session
            val endedAt = latest.endedAt
            if (endedAt != null && (timestamp - endedAt) <= SHORT_SWITCH_MERGE_THRESHOLD_MILLIS) {
                Log.d(TAG, "Merging app switch within ${timestamp - endedAt}ms for $packageName")
                val merged = latest.copy(
                    endedAt = null,
                    sessionStatus = "ACTIVE",
                    reminderStatus = "SCHEDULED"
                )
                upiSessionDao.update(merged)
                return@withContext merged
            }
        }

        // Otherwise create a new session
        val newSession = UpiSessionEntity(
            id = UUID.randomUUID().toString(),
            packageName = packageName,
            startedAt = timestamp,
            endedAt = null,
            sessionStatus = "ACTIVE",
            reminderStatus = "SCHEDULED",
            associatedTransactionIds = null,
            createdAt = timestamp
        )
        upiSessionDao.insert(newSession)
        Log.d(TAG, "Started new UPI session for $packageName at $timestamp")
        return@withContext newSession
    }

    /**
     * Records a background transition (e.g. ACTIVITY_PAUSED / ACTIVITY_STOPPED) for a monitored UPI app.
     */
    suspend fun onAppBackground(
        packageName: String,
        timestamp: Long = System.currentTimeMillis()
    ): UpiSessionEntity? = withContext(Dispatchers.IO) {
        val active = upiSessionDao.getActiveSession(packageName)
        if (active != null) {
            val ended = active.copy(
                endedAt = timestamp,
                sessionStatus = "ENDED"
            )
            upiSessionDao.update(ended)
            Log.d(TAG, "Ended UPI session for $packageName at $timestamp (duration: ${timestamp - active.startedAt}ms)")
            return@withContext ended
        }
        return@withContext null
    }

    /**
     * Queries UsageStatsManager events in the lookback window, identifies transitions,
     * merges switches, and reconciles sessions in Room database.
     */
    suspend fun scanAndReconcileSessions(
        lookbackMillis: Long = 15 * 60 * 1000L
    ): List<UpiSessionEntity> = withContext(Dispatchers.IO) {
        if (!hasUsageAccessPermission()) {
            Log.w(TAG, "Usage access permission not granted, skipping scan")
            return@withContext emptyList()
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usageStatsManager == null) {
            Log.w(TAG, "UsageStatsManager service not available")
            return@withContext emptyList()
        }

        val endTime = System.currentTimeMillis()
        val startTime = endTime - lookbackMillis
        val events = usageStatsManager.queryEvents(startTime, endTime) ?: return@withContext emptyList()

        val reconciledSessions = mutableListOf<UpiSessionEntity>()
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            if (!MONITORED_PACKAGES.contains(pkg)) continue

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    val session = onAppForeground(pkg, event.timeStamp)
                    reconciledSessions.add(session)
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val session = onAppBackground(pkg, event.timeStamp)
                    if (session != null) {
                        reconciledSessions.add(session)
                    }
                }
            }
        }

        // Close stale active sessions that exceeded timeout
        val activeSessions = upiSessionDao.getAllActiveSessions()
        for (session in activeSessions) {
            if (endTime - session.startedAt > SESSION_TIMEOUT_MILLIS) {
                val closed = session.copy(
                    endedAt = session.startedAt + 60_000L,
                    sessionStatus = "ENDED"
                )
                upiSessionDao.update(closed)
                reconciledSessions.add(closed)
                Log.d(TAG, "Closed timed-out active session for ${session.packageName}")
            }
        }

        return@withContext reconciledSessions
    }
}
