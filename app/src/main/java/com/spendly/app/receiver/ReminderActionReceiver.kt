package com.spendly.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.spendly.app.core.database.dao.UpiSessionDao
import com.spendly.app.detection.reminders.SmartReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var upiSessionDao: UpiSessionDao

    @Inject
    lateinit var reminderScheduler: SmartReminderScheduler

    companion object {
        private const val TAG = "ReminderActionReceiver"

        const val ACTION_NO_PAYMENT = "com.spendly.app.ACTION_NO_PAYMENT"
        const val ACTION_REMIND_LATER = "com.spendly.app.ACTION_REMIND_LATER"
        const val ACTION_TRIGGER_REMINDER = "com.spendly.app.ACTION_TRIGGER_REMINDER"

        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        Log.d(TAG, "Received reminder action: $action for session $sessionId")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_NO_PAYMENT -> {
                        if (notificationId != -1) {
                            NotificationManagerCompat.from(context).cancel(notificationId)
                        }
                        val session = upiSessionDao.getById(sessionId)
                        if (session != null) {
                            upiSessionDao.update(session.copy(reminderStatus = "CONFIRMED_NO_PAYMENT"))
                            Log.d(TAG, "Marked session $sessionId as CONFIRMED_NO_PAYMENT")
                        }
                    }

                    ACTION_REMIND_LATER -> {
                        if (notificationId != -1) {
                            NotificationManagerCompat.from(context).cancel(notificationId)
                        }
                        val session = upiSessionDao.getById(sessionId)
                        if (session != null) {
                            val updated = session.copy(reminderStatus = "SCHEDULED")
                            upiSessionDao.update(updated)
                            reminderScheduler.scheduleReminderForSession(
                                session = updated,
                                delayMillis = SmartReminderScheduler.SNOOZE_DURATION_MILLIS
                            )
                            Log.d(TAG, "Snoozed reminder for session $sessionId for 15 minutes")
                        }
                    }

                    ACTION_TRIGGER_REMINDER -> {
                        reminderScheduler.evaluateAndShowReminder(sessionId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling reminder action $action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
