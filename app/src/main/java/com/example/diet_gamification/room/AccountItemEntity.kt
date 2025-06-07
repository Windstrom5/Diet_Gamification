package com.example.diet_gamification.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "account")
data class AccountItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var email: String,
    var name: String,
    var password: String,
    var gender: String,
    var exp: Int,
    var berat: Int,
    var tinggi: Int,
    var inventory: String? = null,
    var setting: String?= null
)