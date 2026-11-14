package com.spendly.app.core.database

import android.content.Context
import com.spendly.app.core.database.dao.AppSettingsDao
import com.spendly.app.core.database.dao.BudgetDao
import com.spendly.app.core.database.dao.CategoryDao
import com.spendly.app.core.database.dao.MerchantRuleDao
import com.spendly.app.core.database.dao.ReminderPreferenceDao
import com.spendly.app.core.database.dao.TransactionDao
import com.spendly.app.core.database.dao.UpiSessionDao
import com.spendly.app.core.security.EncryptedDatabaseManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        encryptedDatabaseManager: EncryptedDatabaseManager
    ): SpendlyDatabase {
        return encryptedDatabaseManager.getDatabase(context)
    }

    @Provides
    fun provideTransactionDao(database: SpendlyDatabase): TransactionDao =
        database.transactionDao()

    @Provides
    fun provideCategoryDao(database: SpendlyDatabase): CategoryDao =
        database.categoryDao()

    @Provides
    fun provideMerchantRuleDao(database: SpendlyDatabase): MerchantRuleDao =
        database.merchantRuleDao()

    @Provides
    fun provideUpiSessionDao(database: SpendlyDatabase): UpiSessionDao =
        database.upiSessionDao()

    @Provides
    fun provideBudgetDao(database: SpendlyDatabase): BudgetDao =
        database.budgetDao()

    @Provides
    fun provideReminderPreferenceDao(database: SpendlyDatabase): ReminderPreferenceDao =
        database.reminderPreferenceDao()

    @Provides
    fun provideAppSettingsDao(database: SpendlyDatabase): AppSettingsDao =
        database.appSettingsDao()
}
