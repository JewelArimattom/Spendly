package com.spendly.app.data.di

import com.spendly.app.data.repository.AppSettingsRepositoryImpl
import com.spendly.app.data.repository.BudgetRepositoryImpl
import com.spendly.app.data.repository.CategoryRepositoryImpl
import com.spendly.app.data.repository.MerchantRuleRepositoryImpl
import com.spendly.app.data.repository.ReminderPreferenceRepositoryImpl
import com.spendly.app.data.repository.TransactionRepositoryImpl
import com.spendly.app.data.repository.UpiSessionRepositoryImpl
import com.spendly.app.domain.repository.AppSettingsRepository
import com.spendly.app.domain.repository.BudgetRepository
import com.spendly.app.domain.repository.CategoryRepository
import com.spendly.app.domain.repository.MerchantRuleRepository
import com.spendly.app.domain.repository.ReminderPreferenceRepository
import com.spendly.app.domain.repository.TransactionRepository
import com.spendly.app.domain.repository.UpiSessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindMerchantRuleRepository(
        merchantRuleRepositoryImpl: MerchantRuleRepositoryImpl
    ): MerchantRuleRepository

    @Binds
    @Singleton
    abstract fun bindUpiSessionRepository(
        upiSessionRepositoryImpl: UpiSessionRepositoryImpl
    ): UpiSessionRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        budgetRepositoryImpl: BudgetRepositoryImpl
    ): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindReminderPreferenceRepository(
        reminderPreferenceRepositoryImpl: ReminderPreferenceRepositoryImpl
    ): ReminderPreferenceRepository

    @Binds
    @Singleton
    abstract fun bindAppSettingsRepository(
        appSettingsRepositoryImpl: AppSettingsRepositoryImpl
    ): AppSettingsRepository
}
