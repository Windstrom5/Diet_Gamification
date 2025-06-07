package com.example.diet_gamification.utils

import com.example.diet_gamification.room.XpHistoryDao
import com.example.diet_gamification.room.XpHistoryEntity

object XpRepository {

    private lateinit var dao: XpHistoryDao

    fun init(dao: XpHistoryDao) {
        this.dao = dao
    }

    suspend fun insertOrUpdatexp(accountId: Int, date: String, category: String, xpGained: Int) {
        val existing = dao.getXpHistoryByDateAndCategory(date, accountId, category)
        if (existing != null) {
            val updated = existing.copy(xpGained = existing.xpGained + xpGained)
            dao.updatexpHistory(updated)
        } else {
            val newEntry = XpHistoryEntity(
                accountId = accountId,
                date = date,
                category = category,
                xpGained = xpGained
            )
            dao.insertXpHistory(newEntry)
        }
    }
}
