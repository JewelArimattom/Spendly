package com.spendly.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.spendly.app.core.database.dao.AppSettingsDao
import com.spendly.app.core.database.dao.BudgetDao
import com.spendly.app.core.database.dao.CategoryDao
import com.spendly.app.core.database.dao.MerchantRuleDao
import com.spendly.app.core.database.dao.ReminderPreferenceDao
import com.spendly.app.core.database.dao.TransactionDao
import com.spendly.app.core.database.dao.UpiSessionDao
import com.spendly.app.core.database.entity.AppSettingsEntity
import com.spendly.app.core.database.entity.BudgetEntity
import com.spendly.app.core.database.entity.CategoryEntity
import com.spendly.app.core.database.entity.MerchantRuleEntity
import com.spendly.app.core.database.entity.ReminderPreferenceEntity
import com.spendly.app.core.database.entity.TransactionEntity
import com.spendly.app.core.database.entity.UpiSessionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        MerchantRuleEntity::class,
        UpiSessionEntity::class,
        BudgetEntity::class,
        ReminderPreferenceEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SpendlyDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun merchantRuleDao(): MerchantRuleDao
    abstract fun upiSessionDao(): UpiSessionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun reminderPreferenceDao(): ReminderPreferenceDao
    abstract fun appSettingsDao(): AppSettingsDao
}
