package com.example.diet_gamification.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.diet_gamification.utils.NotificationUtils.createNotification

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderType = intent.getStringExtra("reminderType")

        val title: String
        val message: String

        when (reminderType) {
            "Breakfast" -> {
                title = "Reminder"
                message = "Don't forget your breakfast! Stay on track with your calories."
            }
            "Lunch" -> {
                title = "Reminder"
                message = "It's lunch time! Keep an eye on your calories."
            }
            "Dinner" -> {
                title = "Reminder"
                message = "Dinner time! Stay within your calorie target for the day."
            }
            "Workout" -> {
                title = "Reminder"
                message = "It's workout time! Keep active and stay healthy."
            }
            "WeeklyReport" -> {
                title = "Weekly Report"
                message = "Don't forget to check your weekly progress report."
            }
            "CheckCalories" -> {
                // 👇 Here you need to check if calories < goal
                val sharedPrefsManager = SharedPrefsManager(context)
                val calorieGoal = sharedPrefsManager.getCalorieTarget()
//                val totalCalories = sharedPrefsManager.getTotalCaloriesToday()

//                if (totalCalories < calorieGoal) {
//                    title = "Calorie Reminder"
//                    message = "You're still below your calorie goal today! Remember to eat enough."
//                    NotificationUtils.createNotification(context, title, message)
//                }

                // Important: if goal reached or over, do nothing
                return
            }
            else -> {
                title = "Reminder"
                message = "Reminder!"
            }
        }

        NotificationUtils.createNotification(context, title, message)
    }
}

