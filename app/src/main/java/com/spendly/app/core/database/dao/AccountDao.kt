package com.spendly.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.spendly.app.core.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllFlow(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE is_default = 1 LIMIT 1")
    suspend fun getDefault(): AccountEntity?

    @Query("SELECT * FROM accounts WHERE include_in_total = 1")
    fun getIncludedInTotal(): Flow<List<AccountEntity>>

    @Query("SELECT SUM(balance_minor) FROM accounts WHERE include_in_total = 1")
    fun getTotalBalance(): Flow<Long?>

    @Query("UPDATE accounts SET balance_minor = balance_minor + :deltaMinor WHERE id = :id")
    suspend fun updateBalance(id: Long, deltaMinor: Long)
}
