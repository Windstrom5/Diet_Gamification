package com.example.diet_gamification.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AccountItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountItemEntity)

    @Query("SELECT * FROM account WHERE email = :email LIMIT 1")
    suspend fun getAccountByEmail(email: String): AccountItemEntity?

    @Query("SELECT * FROM account")
    suspend fun getAllAccounts(): List<AccountItemEntity>
//
//    @Query("SELECT calorieTarget FROM account WHERE id = :accountId")
//    suspend fun getCalorieTargetForAccount(accountId: Int): Int?

    @Update
    suspend fun updateAccount(account: AccountItemEntity)

}