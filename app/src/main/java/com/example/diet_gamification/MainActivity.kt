package com.example.diet_gamifikasi

import android.Manifest
import android.animation.ValueAnimator
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.diet_gamification.R
import com.example.diet_gamification.model.AccountModel
import com.example.diet_gamification.profile.UserViewModel
import com.example.diet_gamification.report.ReportFragment
import com.example.diet_gamification.todolist.ToDoListFragment
import com.example.diet_gamification.utils.DailyCalorieXpWorker
import com.example.diet_gamification.utils.NotificationUtils
import com.example.diet_gamification.workout.WorkoutFragment
import com.example.diet_gamifikasi.profile.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar
import java.util.concurrent.TimeUnit
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import androidx.core.content.ContentProviderCompat.requireContext

class MainActivity : AppCompatActivity() {
    private val userViewModel: UserViewModel by viewModels()
    var currentAccountModel: AccountModel? = null
    private lateinit var bottomNavigation: BottomNavigationView
    private var loadingDialog: AlertDialog? = null
    private var loadingAnimator: ValueAnimator? = null
    // Create ActivityResultLauncher to request exact alarm permission
    private val requestExactAlarmPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d("MainActivity", "Exact alarm permission granted.")
            } else {
                Log.d("MainActivity", "Exact alarm permission denied.")
            }
        }

    companion object {
        // Loading TensorFlow Lite library in companion object
        init {
            System.loadLibrary("tensorflowlite_jni")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request permissions
        requestExactAlarmPermission()
        requestNotificationPermission()

        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Set daily calorie XP worker
        scheduleDailyCalorieXpWorker(this)

        // Set reminders
        setDailyReminders()

        if (currentAccountModel == null) {
            val lockedIcon = getLockedIcon(R.drawable.baseline_assignment_24, R.drawable.ic_lock)
            bottomNavigation.menu.findItem(R.id.nav_report).icon = lockedIcon
        } else {
            // User is logged in — show normal report icon
            bottomNavigation.menu.findItem(R.id.nav_report).icon = ContextCompat.getDrawable(this, R.drawable.baseline_assignment_24)
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_todolist -> openFragment(ToDoListFragment())
                R.id.nav_workout -> openFragment(WorkoutFragment())
                R.id.nav_report -> {
                    if (currentAccountModel == null) {
                        Toast.makeText(this, "Please log in to access reports.", Toast.LENGTH_SHORT).show()
                        return@setOnItemSelectedListener false
                    } else {
                        openFragment(ReportFragment())
                    }
                }
                R.id.nav_profile -> openFragment(ProfileFragment())
            }
            true
        }

        // Set default fragment
        if (savedInstanceState == null) {
            openFragment(ToDoListFragment())
        }

        // Observe user data
        userViewModel.username.observe(this, Observer { name ->
            findViewById<TextView>(R.id.tvUsername).text = name
        })

        userViewModel.exp.observe(this, Observer { exp ->
            findViewById<TextView>(R.id.tvExp).text = "EXP: $exp"
        })
    }

    private fun setDailyReminders() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            NotificationUtils.rescheduleAllReminders(this@MainActivity)
        } else {
            Toast.makeText(this, "Reminder permission not granted. Enable exact alarms in settings.", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.d("MainActivity", "Requesting exact alarm permission.")

                Toast.makeText(
                    this,
                    "Please allow exact alarms for timely reminders like meals and workouts.",
                    Toast.LENGTH_LONG
                ).show()

                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    requestExactAlarmPermissionLauncher.launch(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error launching permission request.", e)
                    val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(fallbackIntent)
                }
            } else {
                Log.d("MainActivity", "Exact alarm permission already granted.")
            }
        } else {
            // For SDK < 31, just set reminders directly
            setDailyReminders()
        }
    }
    fun showLoadingDialog() {
        if (loadingDialog == null) {
            val dialogView = LayoutInflater.from(this@MainActivity).inflate(R.layout.dialog_loading, null)
            val loadingText = dialogView.findViewById<TextView>(R.id.loadingText)

            loadingDialog = AlertDialog.Builder(this@MainActivity)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            // Start wave animation on loading text
            loadingAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1200
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART

                addUpdateListener { animation ->
                    val progress = animation.animatedFraction
                    val alpha = 0.3f + 0.7f * kotlin.math.abs(kotlin.math.sin(progress * Math.PI * 2)).toFloat()
                    loadingText.alpha = alpha
                }
                start()
            }
        }
        loadingDialog?.show()
    }
    fun animateLoadingText(textView: TextView) {
        val loadingText = textView.text.toString()
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART

            addUpdateListener { animation ->
                val progress = animation.animatedFraction
                // Calculate alpha based on progress to create wave effect
                val alpha = 0.3f + 0.7f * kotlin.math.abs(kotlin.math.sin(progress * Math.PI * 2)).toFloat()
                textView.alpha = alpha
            }
        }
        animator.start()
    }

    fun hideLoadingDialog() {
        loadingAnimator?.cancel()
        loadingDialog?.dismiss()
    }
    fun updateUsername() {
        findViewById<TextView>(R.id.tvUsername).text = currentAccountModel?.name ?: "Guest"
        findViewById<TextView>(R.id.tvExp).text = "EXP: ${currentAccountModel?.Exp ?: 0}"
        val rootView = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        bottomNavigation.menu.findItem(R.id.nav_report).icon = ContextCompat.getDrawable(this, R.drawable.baseline_assignment_24)
        applyFontIfAvailable(this, currentAccountModel?.setting, rootView)
    }
    fun applyFontIfAvailable(context: Context, setting: String?, targetView: View) {
        if (setting?.contains("FT-1") == true) {
            val customTypeface = Typeface.createFromAsset(context.assets, "fonts/Super Golden.ttf")
            applyFontRecursively(targetView, customTypeface)
        }
    }
    private fun applyFontRecursively(view: View, typeface: Typeface) {
        when (view) {
            is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    applyFontRecursively(view.getChildAt(i), typeface)
                }
            }
            is TextView -> {
                view.typeface = typeface
            }
        }
    }
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Requesting notification permission.")
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            } else {
                Log.d("MainActivity", "Notification permission already granted.")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            100 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity", "Notification permission granted.")
                } else {
                    Log.d("MainActivity", "Notification permission denied.")
                }
            }
        }
    }

    internal fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun getLockedIcon(base: Int, overlay: Int): Drawable {
        val baseDrawable = ContextCompat.getDrawable(this, base)!!
        val overlayDrawable = ContextCompat.getDrawable(this, overlay)!!
        val layers = arrayOf(baseDrawable, overlayDrawable)
        return LayerDrawable(layers)
    }

    fun scheduleDailyCalorieXpWorker(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<DailyCalorieXpWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS) // Delay until midnight if needed
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    fun calculateInitialDelay(): Long {
        val calendar = Calendar.getInstance()
        val midnight = Calendar.getInstance()
        midnight.set(Calendar.HOUR_OF_DAY, 0)
        midnight.set(Calendar.MINUTE, 0)
        midnight.set(Calendar.SECOND, 0)
        midnight.add(Calendar.DAY_OF_YEAR, 1) // Move to the next midnight

        return midnight.timeInMillis - calendar.timeInMillis
    }
}
