package com.example.diet_gamification.model

import androidx.lifecycle.LiveData
import com.example.diet_gamification.room.FoodItemDao
import com.example.diet_gamification.room.FoodItemEntity

class FoodRepository(private val dao: FoodItemDao) {

    suspend fun insert(food: FoodItemEntity) = dao.insert(food)

    fun getFoodsByCategory(category: String): LiveData<List<FoodItemEntity>> = dao.getFoodsByCategory(category)

    suspend fun getAllNames(): List<String> = dao.getAllNames()

    suspend fun getFoodByName(name: String): FoodItemEntity? = dao.getFoodByName(name)

    suspend fun getFoodItemsByCategoryAndDate(category: String, date: String): List<FoodItemEntity> {
        return dao.getFoodItemsByCategoryAndDate(category, date)
    }
    suspend fun getFoodItemsByCategoryAndDateAccount(category: String, date: String,id:Int): List<FoodItemEntity> {
        return dao.getFoodItemsByCategoryAndDateAccount(category, date,id)
    }
    suspend fun getTotalCaloriesForWeek(startDate: String, endDate: String): Int? {
        return dao.getTotalCaloriesForWeek(startDate, endDate)
    }
}

