package com.example.diet_gamification.shop

import android.util.Log
import com.example.diet_gamification.utils.Buff

object ShopRepository {
    val shopItems = listOf(
        ShopItem("FT-1", "Super Golden", "70", "Change your font style", "ft_01"),
        ShopItem("TW-1", "Strong tiTWe", "175", "Buff For Active workout ppl", "crown"),
        ShopItem("TW-2", "Healthy TiTWe", "150", "Buff For Healthy Workout ppl", "clover"),
        ShopItem("TW-3", "XP-Boost", "200", "Buff For Ultimate Workout ppl", "sparkle"),
        ShopItem("CAL-1", "Calorie Boost", "125", "Flat bonus XP when calories are logged", "fire"),
        ShopItem("CAL-2", "Metabolism Tune", "150", "BMI-based XP bonus based on your body type", "leaf"),
        ShopItem("CAL-3", "Appetite Aid", "100", "Gives bonus XP when you're below your calorie goal", "bell"),
        ShopItem("CAL-4", "Energy Saver", "175", "XP bonus retained even when app is restarted", "battery"),
        ShopItem("CAL-5", "Meal Multiplier", "200", "Increases XP multiplier when daily calorie goal is met", "star"),
        ShopItem("PP-1", "Ambatukam", "150", "Unlock a new profile picture", "isla")
    )

    fun getUnlockedItems(inventory: String?): List<String> {
        if (inventory.isNullOrBlank()) return emptyList()
        return inventory.split(",").map { it.trim() }
    }
    fun getCalorieBuff(inventory: String?, weight: Double, height: Double): Buff {
        val unlocked = getUnlockedItems(inventory)
        var additive = 0
        var multiplier = 1.0

        // CAL-1: Flat bonus XP when calories are logged
        if (unlocked.contains("CAL-1")) {
            additive += 25
            Log.d("ShopRepo", "CAL-1 buff: +25 XP for logging calories")
        }

        // CAL-2: BMI-based calorie bonus
        if (unlocked.contains("CAL-2")) {
            val bmi = weight / ((height / 100) * (height / 100))
            additive += when {
                bmi < 18.5 -> 10
                bmi < 25 -> 15
                else -> 12
            }
            Log.d("ShopRepo", "CAL-2 buff: BMI-based XP for calories")
        }

        // CAL-3: Buff for calorie reminders (encourage timely logging)
        if (unlocked.contains("CAL-3")) {
            additive += 10
            Log.d("ShopRepo", "CAL-3 buff: Reminder bonus +10 XP")
        }

        // CAL-4: Persistence bonus
        if (unlocked.contains("CAL-4")) {
            additive += 15
            Log.d("ShopRepo", "CAL-4 buff: XP retained on app restart")
        }

        // CAL-5: Multiplier when calorie goal is met
        if (unlocked.contains("CAL-5")) {
            multiplier *= 1.2
            Log.d("ShopRepo", "CAL-5 buff: +20% XP multiplier on goal met")
        }

        return Buff(additivexp = additive, multiplier = multiplier)
    }

    fun getWorkoutBuff(inventory: String?, weight: Double, height: Double): Buff {
        val unlocked = getUnlockedItems(inventory)
        var additive = 0
        var multiplier = 1.0

        // TW-1: flat +50 XP
        if (unlocked.contains("TW-1")) {
            additive += 50
            Log.d("ShopRepo", "TW-1 buff: +50 XP")
        }

        // TW-2: BMI-based bonus
        if (unlocked.contains("TW-2")) {
            val bmi = weight / ((height/100)*(height/100))
            additive += when {
                bmi < 18.5 -> 15
                bmi < 25   -> 10
                else       -> 12
            }
            Log.d("ShopRepo", "TW-2 buff: BMI bonus XP")
        }

        // TW-3: multiplier (e.g. 10% more XP)
        if (unlocked.contains("TW-3")) {
            multiplier = 1.10  // 10% extra
            Log.d("ShopRepo", "TW-3 buff: +10% XP multiplier")
        }

        return Buff(additivexp = additive, multiplier = multiplier)
    }
}

