package com.spendly.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.spendly.app.core.database.dao.AccountDao
import com.spendly.app.core.database.dao.BudgetDao
import com.spendly.app.core.database.dao.CategoryDao
import com.spendly.app.core.database.dao.MerchantDao
import com.spendly.app.core.database.dao.TagDao
import com.spendly.app.core.database.dao.TransactionDao
import com.spendly.app.core.database.dao.TransactionTagDao
import com.spendly.app.core.database.entity.AccountEntity
import com.spendly.app.core.database.entity.BudgetEntity
import com.spendly.app.core.database.entity.CategoryEntity
import com.spendly.app.core.database.entity.MerchantEntity
import com.spendly.app.core.database.entity.TagEntity
import com.spendly.app.core.database.entity.TransactionEntity
import com.spendly.app.core.database.entity.TransactionTagCrossRef

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        AccountEntity::class,
        TagEntity::class,
        TransactionTagCrossRef::class,
        BudgetEntity::class,
        MerchantEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SpendlyDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun tagDao(): TagDao
    abstract fun transactionTagDao(): TransactionTagDao
    abstract fun budgetDao(): BudgetDao
    abstract fun merchantDao(): MerchantDao
}
