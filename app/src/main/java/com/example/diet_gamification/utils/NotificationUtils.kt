package com.example.diet_gamification.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.example.diet_gamification.R
import com.example.diet_gamification.utils.ReminderBroadcastReceiver
import java.util.Calendar
import kotlin.random.Random

object NotificationUtils {

    // Function to create the notification
    fun createNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = Random.nextInt(1000, 10000)

        val notificationChannelId = "diet_gamification_channel"
        // Create channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "Reminder Notifications",
                NotificationManager.IMPORTANCE_HIGH // Must be HIGH to show heads-up
            ).apply {
                description = "Channel for reminder notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, notificationChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Set your icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
    fun rescheduleAllReminders(context: Context) {
        setReminder(context, 8, 0, "Breakfast")
        setReminder(context, 12, 0, "Lunch")
        setReminder(context, 18, 0, "Dinner")
        setReminder(context, 7, 0, "Workout")
    }

    @SuppressLint("ScheduleExactAlarm")
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    fun setReminder(context: Context, hour: Int, minute: Int, reminderType: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w("NotificationUtils", "Exact alarm permission not granted. Skipping $reminderType reminder.")
            return
        }

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("reminderType", reminderType)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderType.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d("NotificationUtils", "Scheduled $reminderType at ${calendar.time}")
        } catch (e: SecurityException) {
            Log.e("NotificationUtils", "Failed to schedule exact alarm for $reminderType: ${e.message}")
        }
    }
}
