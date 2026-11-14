package com.spendly.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SpendlyNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        // Will be implemented to parse financial notifications
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No-op
    }
}
