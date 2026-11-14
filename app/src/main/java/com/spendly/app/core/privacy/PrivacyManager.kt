package com.spendly.app.core.privacy

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import androidx.core.app.NotificationManagerCompat
import com.spendly.app.core.database.dao.AppSettingsDao
import com.spendly.app.core.database.dao.CategoryDao
import com.spendly.app.core.database.dao.MerchantRuleDao
import com.spendly.app.core.database.dao.TransactionDao
import com.spendly.app.core.database.dao.UpiSessionDao
import com.spendly.app.core.designsystem.CategoryUiHelper
import com.spendly.app.data.repository.toEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettingsDao: AppSettingsDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val merchantRuleDao: MerchantRuleDao,
    private val upiSessionDao: UpiSessionDao
) {

    fun isUsageAccessGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isNotificationAccessGranted(): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    suspend fun clearAllFinancialData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete all transactions
            transactionDao.deleteAll()

            // Delete all UPI sessions
            upiSessionDao.deleteOlderThan(System.currentTimeMillis() + 86400000L)

            // Re-seed default categories
            val defaultEntities = CategoryUiHelper.defaultCategories.map { it.toEntity() }
            categoryDao.insertAll(defaultEntities)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
