package com.spendly.app.intelligence.di

import com.spendly.app.core.database.dao.CategoryDao
import com.spendly.app.core.database.dao.MerchantRuleDao
import com.spendly.app.core.database.dao.TransactionDao
import com.spendly.app.intelligence.merchant.MerchantRecognitionEngine
import com.spendly.app.intelligence.reconciliation.TransactionReconciliationEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IntelligenceModule {

    @Provides
    @Singleton
    fun provideMerchantRecognitionEngine(
        merchantRuleDao: MerchantRuleDao,
        categoryDao: CategoryDao
    ): MerchantRecognitionEngine {
        return MerchantRecognitionEngine(
            merchantRuleDao = merchantRuleDao,
            categoryDao = categoryDao
        )
    }

    @Provides
    @Singleton
    fun provideTransactionReconciliationEngine(
        transactionDao: TransactionDao,
        merchantEngine: MerchantRecognitionEngine
    ): TransactionReconciliationEngine {
        return TransactionReconciliationEngine(
            transactionDao = transactionDao,
            merchantEngine = merchantEngine
        )
    }
}
