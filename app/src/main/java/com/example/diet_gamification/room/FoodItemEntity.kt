package com.example.diet_gamification.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "food_items")
data class FoodItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val id_account: Int?,
    val category: String,  // category field
    val name: String,
    val calories: Int,
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) // default to today
)