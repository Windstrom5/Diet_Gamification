package com.example.diet_gamification.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.diet_gamification.room.AppDatabase
import com.example.diet_gamification.room.XpHistoryEntity

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit


class DailyCalorieXpWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(context)
        val foodItemDao = db.foodItemDao()
        val xpHistoryDao = db.xpHistoryDao()

        val sharedPrefsManager = SharedPrefsManager(context)
        val calorieTarget = sharedPrefsManager.getCalorieTarget()

        val accountId = inputData.getInt("accountId", -1)
        if (accountId == -1) return Result.failure()

        val today = LocalDate.now().minusDays(1).toString() // Previous day
        val totalCalories = foodItemDao.getTotalCaloriesForDateAndAccount(today, accountId) ?: 0

        val xpEarned = calculateXpFromCalories(totalCalories, calorieTarget)

        // Insert XP if not already done
        val existing = xpHistoryDao.getXpHistoryByDateAndCategory(today, accountId, "calories")
        if (existing == null) {
            val xpHistory = XpHistoryEntity(
                accountId = accountId,
                xpGained = xpEarned,
                category = "calories",
                date = today
            )
            xpHistoryDao.insertXpHistory(xpHistory)
        }

        return Result.success()
    }

    private fun calculateXpFromCalories(actual: Int, target: Int): Int {
        return when {
            actual >= target -> 50
            actual >= target * 0.75 -> 30
            else -> 10
        }
    }
}
