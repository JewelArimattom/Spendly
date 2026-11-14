package com.spendly.app.core.database

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.spendly.app.core.database.dao.AccountDao
import com.spendly.app.core.database.dao.BudgetDao
import com.spendly.app.core.database.dao.CategoryDao
import com.spendly.app.core.database.dao.MerchantDao
import com.spendly.app.core.database.dao.TagDao
import com.spendly.app.core.database.dao.TransactionDao
import com.spendly.app.core.database.dao.TransactionTagDao
import com.spendly.app.core.security.EncryptedDatabaseManager
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
    fun provideAccountDao(database: SpendlyDatabase): AccountDao =
        database.accountDao()

    @Provides
    fun provideTagDao(database: SpendlyDatabase): TagDao =
        database.tagDao()

    @Provides
    fun provideTransactionTagDao(database: SpendlyDatabase): TransactionTagDao =
        database.transactionTagDao()

    @Provides
    fun provideBudgetDao(database: SpendlyDatabase): BudgetDao =
        database.budgetDao()

    @Provides
    fun provideMerchantDao(database: SpendlyDatabase): MerchantDao =
        database.merchantDao()
}
