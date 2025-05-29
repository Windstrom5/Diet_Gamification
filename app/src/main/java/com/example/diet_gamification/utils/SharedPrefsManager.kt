package com.example.diet_gamification.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SharedPrefsManager(context: Context) {
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("DietAppPrefs", Context.MODE_PRIVATE)
    private val calorieKey = "user_calorie_target"
    private val lastSetDateKey = "last_set_date"
    // Default calorie target
    private val defaultCalories = 2000

    // Get calorie target from SharedPreferences
    fun getCalorieTarget(): Int {
        return sharedPrefs.getInt(calorieKey, defaultCalories)
    }

    // Save calorie target to SharedPreferences
    fun saveCalorieTarget(calories: Int) {
        sharedPrefs.edit() { putInt(calorieKey, calories) }
    }

    // Reset to default calorie target
    fun resetCalorieTarget() {
        saveCalorieTarget(defaultCalories)
    }

    fun saveLastSetDate(date: String) {
        sharedPrefs.edit() { putString(lastSetDateKey, date) }
    }

    fun getLastSetDate(): Long {
        // Get the last set date or set the current date if null
        return sharedPrefs.getLong(lastSetDateKey, System.currentTimeMillis())
    }

    fun setLastSetDate(date: Long) {
        // Save the current date to SharedPreferences
        val editor = sharedPrefs.edit()
        editor.putLong(lastSetDateKey, date)
        editor.apply()
    }

    fun checkAndSetLastSetDate() {
        // Get last set date, if it's null, set the current date
        val lastSetDate = getLastSetDate()

        // If the value is the default (meaning it was never set), you can update it
        if (lastSetDate == System.currentTimeMillis()) {
            // Set the current date as the 'last set date' in preferences
            setLastSetDate(System.currentTimeMillis())
        }
    }
}
