package com.example.diet_gamification.firebase

data class XpHistoryFirebase (
    var firebaseId: String? = null,
    var accountId: Int = 0,
    var xpGained: Int = 0,
    var category: String = "", // e.g., "Workout", "FoodLog"
    var date: String = ""
)
