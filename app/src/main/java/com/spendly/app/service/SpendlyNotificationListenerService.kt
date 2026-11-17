package com.spendly.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.spendly.app.core.database.dao.TransactionDao
import com.spendly.app.core.database.dao.UpiSessionDao
import com.spendly.app.core.database.entity.TransactionEntity
import com.spendly.app.detection.parsing.NotificationTransactionParser
import com.spendly.app.domain.model.TransactionSource
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.intelligence.reconciliation.TransactionReconciliationEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SpendlyNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var parser: NotificationTransactionParser
    @Inject lateinit var reconciliationEngine: TransactionReconciliationEngine
    @Inject lateinit var transactionDao: TransactionDao
    @Inject lateinit var upiSessionDao: UpiSessionDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = buildString {
            listOf(
                Notification.EXTRA_TEXT,
                Notification.EXTRA_BIG_TEXT,
                Notification.EXTRA_SUB_TEXT,
                Notification.EXTRA_SUMMARY_TEXT
            ).forEach { key ->
                extras.getCharSequence(key)?.toString()?.takeIf { it.isNotBlank() }?.let { append(it).append(' ') }
            }
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { line ->
                line?.toString()?.takeIf { it.isNotBlank() }?.let { append(it).append(' ') }
            }
        }.trim().ifBlank { null }

        val candidate = parser.parseNotification(sbn.packageName, title, text, sbn.postTime) ?: return
        if (candidate.isFailed) return

        serviceScope.launch {
            try {
                val isSmsNotification = sbn.packageName in NotificationTransactionParser.SMS_NOTIFICATION_PACKAGES
                val now = System.currentTimeMillis()
                val incoming = TransactionEntity(
                    amountMinorUnits = candidate.amountMinorUnits,
                    currency = candidate.currency,
                    transactionType = candidate.transactionType.name,
                    merchantName = candidate.merchantName,
                    purpose = candidate.purpose,
                    paymentMethod = candidate.paymentMethod.name,
                    source = if (isSmsNotification) TransactionSource.BANK_SMS.name else TransactionSource.NOTIFICATION.name,
                    sourceReference = candidate.referenceId,
                    confidence = candidate.confidence,
                    status = TransactionStatus.PENDING_CONFIRMATION.name,
                    transactionDate = candidate.timestamp,
                    createdAt = now,
                    updatedAt = now
                )
                reconciliationEngine.reconcile(incoming)

                val savedTransaction = candidate.referenceId?.let { transactionDao.getBySourceReference(it) }
                    ?: transactionDao.findDuplicate(
                        candidate.amountMinorUnits,
                        candidate.timestamp - 5 * 60_000L,
                        candidate.timestamp + 5 * 60_000L
                    )
                val session = if (isSmsNotification) {
                    upiSessionDao.getAnyRecentSession(candidate.timestamp - 15 * 60_000L)
                } else {
                    upiSessionDao.getRecentSession(sbn.packageName, candidate.timestamp - 15 * 60_000L)
                }
                session?.let {
                    upiSessionDao.update(
                        it.copy(
                            associatedTransactionIds = savedTransaction?.id ?: incoming.id,
                            reminderStatus = "NOT_NEEDED"
                        )
                    )
                }
            } catch (error: Exception) {
                Log.e("SpendlyNotification", "Could not save detected payment notification", error)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No-op
    }
}
