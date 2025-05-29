package com.example.diet_gamification.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "xp_history")
data class XpHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountId: Int,
    val xpGained: Int,
    val category: String, // e.g., "Workout", "FoodLog"
    val date: String // "yyyy-MM-dd"
)
