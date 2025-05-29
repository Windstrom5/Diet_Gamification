package com.example.diet_gamification.room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorkoutItemDao{
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(workout: WorkoutItemEntity)

    @Query("SELECT DISTINCT name FROM workout_items")
    suspend fun getAllNames(): List<String>

    @Query("SELECT * FROM workout_items WHERE name = :name LIMIT 1")
    suspend fun getworkoutByName(name: String): WorkoutItemEntity?

    @Query("SELECT SUM(calories) FROM workout_items WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalCaloriesForWeek(startDate: String, endDate: String): Int?
}