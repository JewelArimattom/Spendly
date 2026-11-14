package com.spendly.app.detection.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.spendly.app.MainActivity
import com.spendly.app.core.database.dao.ReminderPreferenceDao
import com.spendly.app.core.database.dao.TransactionDao
import com.spendly.app.core.database.dao.UpiSessionDao
import com.spendly.app.core.database.entity.UpiSessionEntity
import com.spendly.app.detection.sessions.UpiAppSessionMonitor
import com.spendly.app.receiver.ReminderActionReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val upiSessionDao: UpiSessionDao,
    private val transactionDao: TransactionDao,
    private val reminderPreferenceDao: ReminderPreferenceDao
) {

    companion object {
        private const val TAG = "SmartReminderScheduler"

        const val CHANNEL_ID = "upi_payment_reminders"
        const val CHANNEL_NAME = "Payment Reminders"

        // Default debounce interval after UPI session ends (90 seconds)
        const val DEFAULT_DEBOUNCE_MILLIS = 90_000L

        // Snooze duration for 'Remind me later' (15 minutes)
        const val SNOOZE_DURATION_MILLIS = 15 * 60 * 1000L

        const val EXTRA_ACTION = "EXTRA_ACTION"
        const val ACTION_QUICK_ENTRY = "com.spendly.app.QUICK_ENTRY"
        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
        const val EXTRA_PACKAGE_NAME = "EXTRA_PACKAGE_NAME"
        const val EXTRA_APP_NAME = "EXTRA_APP_NAME"
        const val EXTRA_TIMESTAMP = "EXTRA_TIMESTAMP"
    }

    init {
        createNotificationChannel()
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle reminders to record transactions after using UPI apps"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Schedules a debounced reminder check after a UPI app session ends.
     */
    fun scheduleReminderForSession(
        session: UpiSessionEntity,
        delayMillis: Long = DEFAULT_DEBOUNCE_MILLIS
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderActionReceiver.EXTRA_SESSION_ID, session.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            session.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + delayMillis
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.d(TAG, "Scheduled reminder alarm for session ${session.id} in ${delayMillis / 1000}s")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule reminder alarm", e)
        }
    }

    /**
     * Cancels any pending reminder alarm for this session.
     */
    fun cancelScheduledReminder(sessionId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderActionReceiver.EXTRA_SESSION_ID, sessionId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Evaluates suppression rules and posts a notification if no transaction exists.
     * Returns true if notification was shown, false if suppressed.
     */
    suspend fun evaluateAndShowReminder(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        val session = upiSessionDao.getById(sessionId) ?: return@withContext false

        // Do not remind if session is still active or already confirmed / not needed
        if (session.sessionStatus == "ACTIVE") {
            Log.d(TAG, "Session $sessionId is still active, postponing reminder")
            return@withContext false
        }
        if (session.reminderStatus in setOf("NOT_NEEDED", "CONFIRMED_PAID", "CONFIRMED_NO_PAYMENT", "DISMISSED")) {
            Log.d(TAG, "Session $sessionId already resolved (${session.reminderStatus}), skipping reminder")
            return@withContext false
        }

        // Rule 1: Check if payment notification was already detected for this session
        if (!session.associatedTransactionIds.isNullOrBlank()) {
            Log.d(TAG, "Session $sessionId already has associated transaction, skipping reminder")
            upiSessionDao.update(session.copy(reminderStatus = "NOT_NEEDED"))
            return@withContext false
        }

        // Rule 2: Check if user already logged a transaction during or shortly after the session
        val windowStart = session.startedAt - 60_000L
        val windowEnd = (session.endedAt ?: session.startedAt) + 120_000L
        val existingTransactions = transactionDao.getTransactionsBetween(windowStart, windowEnd)
        if (existingTransactions.isNotEmpty()) {
            Log.d(TAG, "Found existing transaction in window for session $sessionId, marking NOT_NEEDED")
            upiSessionDao.update(
                session.copy(
                    associatedTransactionIds = existingTransactions.first().id,
                    reminderStatus = "NOT_NEEDED"
                )
            )
            return@withContext false
        }

        // Rule 3: Check user preferences & enablement
        val pref = reminderPreferenceDao.getPreferenceForPackage(session.packageName)
        if (pref != null && !pref.enabled) {
            Log.d(TAG, "Reminders disabled for package ${session.packageName}")
            upiSessionDao.update(session.copy(reminderStatus = "DISMISSED"))
            return@withContext false
        }

        // Rule 4: Check Quiet Hours
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val quietStart = pref?.quietHoursStart ?: 22 // 10 PM default
        val quietEnd = pref?.quietHoursEnd ?: 7    // 7 AM default
        val isQuiet = if (quietStart > quietEnd) {
            currentHour >= quietStart || currentHour < quietEnd
        } else if (quietStart < quietEnd) {
            currentHour in quietStart until quietEnd
        } else {
            false
        }
        if (isQuiet) {
            Log.d(TAG, "Suppressed reminder during quiet hours ($currentHour:00)")
            return@withContext false
        }

        // Rule 5: Check Cooldown Period
        val cooldownMinutes = pref?.cooldownMinutes ?: 30
        val cooldownMillis = cooldownMinutes * 60 * 1000L
        val lastReminderTime = upiSessionDao.getLastReminderTime(session.packageName)
        if (lastReminderTime != null && (System.currentTimeMillis() - lastReminderTime) < cooldownMillis) {
            Log.d(TAG, "Suppressed reminder due to cooldown ($cooldownMinutes mins)")
            return@withContext false
        }

        // Rule 6: Check Maximum Daily Reminders
        val maxDaily = pref?.maximumDailyReminders ?: 5
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val midnight = calendar.timeInMillis
        val countToday = upiSessionDao.countRemindersSentSince(session.packageName, midnight)
        if (countToday >= maxDaily) {
            Log.d(TAG, "Suppressed reminder: daily limit of $maxDaily reached for ${session.packageName}")
            return@withContext false
        }

        // All checks passed -> Post gentle system notification
        postReminderNotification(session)
        upiSessionDao.update(session.copy(reminderStatus = "SHOWN"))
        return@withContext true
    }

    private fun postReminderNotification(session: UpiSessionEntity) {
        val appName = UpiAppSessionMonitor.getAppName(session.packageName)
        val notificationId = session.id.hashCode()

        // Action 1: "Yes, I paid" -> Open Quick Entry in MainActivity
        val paidIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION, ACTION_QUICK_ENTRY)
            putExtra(EXTRA_SESSION_ID, session.id)
            putExtra(EXTRA_PACKAGE_NAME, session.packageName)
            putExtra(EXTRA_APP_NAME, appName)
            putExtra(EXTRA_TIMESTAMP, session.startedAt)
        }
        val paidPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1,
            paidIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: "No payment" -> Broadcast to dismiss and update status
        val noPaymentIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_NO_PAYMENT
            putExtra(ReminderActionReceiver.EXTRA_SESSION_ID, session.id)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val noPaymentPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            noPaymentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: "Remind me later" -> Broadcast to snooze for 15 minutes
        val remindLaterIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_REMIND_LATER
            putExtra(ReminderActionReceiver.EXTRA_SESSION_ID, session.id)
            putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val remindLaterPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 3,
            remindLaterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Did you make a payment using $appName?")
            .setContentText("Tap to quickly log your expense in Spendly.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(paidPendingIntent)
            .addAction(0, "Yes, I paid", paidPendingIntent)
            .addAction(0, "No payment", noPaymentPendingIntent)
            .addAction(0, "Remind me later", remindLaterPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.d(TAG, "Posted payment reminder notification for $appName (session: ${session.id})")
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission missing, cannot post reminder", e)
        }
    }
}
