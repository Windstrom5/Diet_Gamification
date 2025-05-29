package com.example.diet_gamification.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface XpHistoryDao {

    @Query("""
        SELECT * FROM xp_history 
        WHERE accountId = :accountId 
        AND date BETWEEN :startDate AND :endDate
    """)
    suspend fun getXpHistoryForWeek(accountId: Int, startDate: String, endDate: String): List<XpHistoryEntity>

    @Query("SELECT * FROM xp_history WHERE date = :date AND accountId = :accountId AND category = :category LIMIT 1")
    suspend fun getXpHistoryByDateAndCategory(date: String, accountId: Int, category: String): XpHistoryEntity?

    @Insert
    suspend fun insertXpHistory(xpHistory: XpHistoryEntity)

    @Update
    suspend fun updateXpHistory(xpHistory: XpHistoryEntity)
}

