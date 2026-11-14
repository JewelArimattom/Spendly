package com.spendly.app.data.di

import com.spendly.app.data.repository.AccountRepositoryImpl
import com.spendly.app.data.repository.BudgetRepositoryImpl
import com.spendly.app.data.repository.CategoryRepositoryImpl
import com.spendly.app.data.repository.MerchantRepositoryImpl
import com.spendly.app.data.repository.TagRepositoryImpl
import com.spendly.app.data.repository.TransactionRepositoryImpl
import com.spendly.app.domain.repository.AccountRepository
import com.spendly.app.domain.repository.BudgetRepository
import com.spendly.app.domain.repository.CategoryRepository
import com.spendly.app.domain.repository.MerchantRepository
import com.spendly.app.domain.repository.TagRepository
import com.spendly.app.domain.repository.TransactionRepository
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
    abstract fun bindAccountRepository(
        accountRepositoryImpl: AccountRepositoryImpl
    ): AccountRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(
        tagRepositoryImpl: TagRepositoryImpl
    ): TagRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        budgetRepositoryImpl: BudgetRepositoryImpl
    ): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindMerchantRepository(
        merchantRepositoryImpl: MerchantRepositoryImpl
    ): MerchantRepository
}
