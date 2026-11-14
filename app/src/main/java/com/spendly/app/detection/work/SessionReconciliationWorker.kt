package com.spendly.app.detection.work

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.spendly.app.core.database.dao.UpiSessionDao
import com.spendly.app.detection.reminders.SmartReminderScheduler
import com.spendly.app.detection.sessions.UpiAppSessionMonitor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SessionReconciliationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SessionReconciliationWorkerEntryPoint {
        fun upiSessionDao(): UpiSessionDao
        fun upiAppSessionMonitor(): UpiAppSessionMonitor
        fun smartReminderScheduler(): SmartReminderScheduler
    }

    companion object {
        private const val TAG = "SessionReconcileWorker"
        const val UNIQUE_WORK_NAME = "SessionReconciliationWorker"

        // 30 days in milliseconds
        private const val THIRTY_DAYS_MILLIS = 30L * 24 * 60 * 60 * 1000L

        /**
         * Enqueues or updates the periodic reconciliation worker (runs every 15 minutes).
         */
        fun schedulePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .build()

            val workRequest = PeriodicWorkRequestBuilder<SessionReconciliationWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES // Flex interval
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
            Log.d(TAG, "Scheduled periodic session reconciliation work")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting periodic session reconciliation work")

        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                SessionReconciliationWorkerEntryPoint::class.java
            )
            val upiSessionDao = entryPoint.upiSessionDao()
            val sessionMonitor = entryPoint.upiAppSessionMonitor()
            val reminderScheduler = entryPoint.smartReminderScheduler()

            // 1. Reconcile unfinished or missed sessions using historical UsageStats queries
            val reconciled = sessionMonitor.scanAndReconcileSessions(lookbackMillis = 60 * 60 * 1000L)
            Log.d(TAG, "Reconciled ${reconciled.size} UPI sessions from usage stats")

            // 2. Check for unhandled or pending reminders
            val pendingSessions = upiSessionDao.getPendingScheduledReminders()
            val now = System.currentTimeMillis()
            for (session in pendingSessions) {
                val sessionEndTime = session.endedAt ?: session.startedAt
                if (now - sessionEndTime >= SmartReminderScheduler.DEFAULT_DEBOUNCE_MILLIS) {
                    reminderScheduler.evaluateAndShowReminder(session.id)
                }
            }

            // 3. Clean old session records older than 30 days
            val cutoffTime = now - THIRTY_DAYS_MILLIS
            upiSessionDao.deleteOlderThan(cutoffTime)
            Log.d(TAG, "Purged UPI sessions older than 30 days (cutoff: $cutoffTime)")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error running session reconciliation worker", e)
            Result.retry()
        }
    }
}
