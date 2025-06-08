package com.example.diet_gamification.workout

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.diet_gamification.R
import com.example.diet_gamification.model.AccountModel
import com.example.diet_gamification.room.AppDatabase
import com.example.diet_gamification.room.XpHistoryDao
import com.example.diet_gamification.room.XpHistoryEntity
import com.example.diet_gamification.shop.ShopRepository
import com.example.diet_gamification.utils.ApiService
import com.example.diet_gamification.utils.XpRepository
import com.example.diet_gamifikasi.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class WorkoutFragment : Fragment() {
    private var accountModel: AccountModel? = null
    private lateinit var etWorkoutName: AutoCompleteTextView
    private lateinit var etWeightKg: EditText
    private lateinit var textHours: TextView
    private lateinit var textMinutes: TextView
    private lateinit var textSeconds: TextView
    private lateinit var textCaloriesBurned: TextView
    private lateinit var textSuggestion: TextView
    private lateinit var buttonStart: Button
    private lateinit var buttonFinish: Button
    private lateinit var buttonReset: Button
    private var workoutCaloriesMap: Map<String, Double> = emptyMap()
    private var timer: Timer? = null
    private var totalSeconds = 0
    private var caloriesBurned = 0.0
    private var selectedActivity = ""
    private var weightKg = 70.0
    private lateinit var xpHistoryDao: XpHistoryDao
    private var account: AccountModel?= null
    private var isDaoInitialized = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.workout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        getAccountFromActivity()
        if (!isDaoInitialized) {
            val dao = AppDatabase.getInstance(requireContext()).xpHistoryDao()
            XpRepository.init(dao)
            isDaoInitialized = true
        }
        xpHistoryDao = AppDatabase.getInstance(requireContext()).xpHistoryDao()
        etWorkoutName = view.findViewById(R.id.etWorkoutName)
        etWeightKg = view.findViewById(R.id.etWeightKg)
        accountModel?.let {
            etWeightKg.setText(it.berat.toString())
            etWeightKg.isEnabled = false
        }
        textHours = view.findViewById(R.id.textHours)
        textMinutes = view.findViewById(R.id.textMinutes)
        textSeconds = view.findViewById(R.id.textSeconds)
        textCaloriesBurned = view.findViewById(R.id.textCaloriesBurned)
        textSuggestion = view.findViewById(R.id.text_suggestion)
        buttonStart = view.findViewById(R.id.button_start)
        buttonFinish = view.findViewById(R.id.button_finish)
        buttonReset = view.findViewById(R.id.button_reset)
        buttonReset.visibility = View.GONE
        loadWorkoutCalories()
        setupWorkoutDropdown()
        buttonStart.setOnClickListener {
            etWorkoutName.isEnabled = false
            etWorkoutName.isFocusable = false
            etWorkoutName.isClickable = false
            val input = etWeightKg.text.toString()
            val workoutInput = etWorkoutName.text.toString()

            if (input.isBlank()) {
                Toast.makeText(requireContext(), "Please enter your weight in kg", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (workoutInput.isBlank()) {
                Toast.makeText(requireContext(), "Please select a workout", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            weightKg = input.toDoubleOrNull() ?: run {
                Toast.makeText(requireContext(), "Invalid weight format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            buttonReset.visibility = View.VISIBLE
            selectedActivity = workoutInput.trim()

            startTimer()
        }

        buttonReset.setOnClickListener {
            resetWorkout()
        }
        buttonFinish.setOnClickListener {
            stopTimer()
            onWorkoutCompleted()
            etWorkoutName.isEnabled = true
            etWorkoutName.isFocusable = true
            etWorkoutName.isClickable = true
            Toast.makeText(requireContext(), "Workout completed!", Toast.LENGTH_SHORT).show()
        }
    }
    private fun calculateAndApplyXP() {
        val account = accountModel ?: return

        val heightFactor = 1 + (account.tinggi - 150) / 100.0
        val weightFactor = 1 + (account.berat - 50) / 100.0
        val intensityFactor = workoutCaloriesMap[selectedActivity] ?: 0.0
        val durationHours = totalSeconds / 3600.0
        val basexp = 20
        val calculatedXP = (basexp + (durationHours * intensityFactor * weightFactor * heightFactor * 10)).toInt()

        account.exp += calculatedXP

        val mainActivity = activity as? MainActivity
        mainActivity?.currentAccountModel = account
        mainActivity?.updateUsername()

        Toast.makeText(requireContext(), "Workout completed! +$calculatedXP XP applied!", Toast.LENGTH_SHORT).show()
    }
    private fun getAccountFromActivity() {
        val mainActivity = activity as? MainActivity
        accountModel = mainActivity?.currentAccountModel

        if (accountModel != null) {
            Log.d("ProfileFragment2", "Received Account: ${accountModel!!.name}")
//            showLoggedInState()

        } else {
            Log.e("ProfileFragment2", "AccountModel is null in MainActivity")
//            showLoggedOutState()
        }
    }

    private fun onWorkoutCompleted() {
        val mainActivity = activity as? MainActivity
        val accountModel = mainActivity?.currentAccountModel ?: run {
            Toast.makeText(requireContext(), "Account not found.", Toast.LENGTH_SHORT).show()
            return
        }
        // 1) Base XP from calories
        val basexp = (caloriesBurned / 10).toInt()
        // 2) Duration bonus
        val durationXp = (totalSeconds / 60.0 / 5).toInt()

        val weight = accountModel.berat.toDouble()
        val height = accountModel.tinggi.toDouble()

        // 3) Get both additive and multiplier buffs
        val buff = ShopRepository.getWorkoutBuff(accountModel.inventory, weight, height)

        // 4) Compute total
        val rawTotal = basexp + durationXp + buff.additivexp
        val finalXp = (rawTotal * buff.multiplier).toInt()

        // 5) Apply to account
        accountModel.exp += finalXp
        mainActivity.currentAccountModel = accountModel
        mainActivity.updateUsername()
        updatexpInRoomDb(finalXp)
        updatexpToLaravel(finalXp)
        Toast.makeText(requireContext(), "Workout OK! +$finalXp XP earned!", Toast.LENGTH_LONG).show()
    }
    private fun updatexpInRoomDb(xp: Int) {
        // Launch a coroutine in the lifecycleScope of the Fragment or Activity
        lifecycleScope.launch {
            val today = LocalDate.now().toString()
            val accountId = accountModel?.id ?: return@launch

            val xpHistory = XpHistoryEntity(
                accountId = accountId,
                xpGained = xp,
                category = "Workout",
                date = today
            )

            // Call the suspend function inside the coroutine
            xpHistoryDao.insertXpHistory(xpHistory)

            // Update the UI after inserting XP history
            Toast.makeText(requireContext(), "XP Updated! You earned $xp XP for meeting your calorie target!", Toast.LENGTH_SHORT).show()
        }
    }
    private fun updatexpToLaravel(xp: Int) {
        val account = accountModel ?: return
        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.4:8000") // Replace with actual base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)

        val request = mapOf(
            "account_id" to account.id,
            "xpGained" to xp,
            "category" to "Workout",
            "date" to LocalDate.now().toString()
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = api.createxpEntry(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "XP saved to server!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to save XP", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadWorkoutCalories() {
        val map = mutableMapOf<String, Double>()
        val inputStream = requireContext().assets.open("exercise_dataset.csv")
        val reader = BufferedReader(InputStreamReader(inputStream))
        reader.readLine() // skip header
        reader.forEachLine {
            val parts = it.split(",")
            if (parts.size >= 6) {
                val name = parts[0].replace("\"", "").trim()
                val perKg = parts[5].toDoubleOrNull()
                if (perKg != null) {
                    map[name] = perKg
                }
            }
        }
        workoutCaloriesMap = map
    }

    private fun generateActivitySuggestion(activity: String, caloriesBurned: Double): String {
        val trimmedActivity = activity.trim().lowercase()
        Log.d("Activity2", "Normalized activity: $trimmedActivity")

        return when {
            trimmedActivity.contains("cycling") || trimmedActivity.contains("mountain bike") || trimmedActivity.contains("bmx") -> {
                when {
                    caloriesBurned < 50 -> "Keep pedaling! You're doing great!"
                    caloriesBurned < 100 -> "Nice job! Keep pushing, you're almost there!"
                    else -> "Awesome! You've burned a lot of calories!"
                }
            }
            trimmedActivity.contains("leisure bicycling") -> {
                when {
                    caloriesBurned < 30 -> "Cruise on! You're doing well!"
                    caloriesBurned < 60 -> "Well done! Keep it up!"
                    else -> "Great work! You've burned some serious calories!"
                }
            }
            trimmedActivity.contains("unicycling") -> {
                when {
                    caloriesBurned < 40 -> "You're balancing it out! Keep going!"
                    caloriesBurned < 80 -> "Great balance, keep it up!"
                    else -> "Incredible! You've worked up a serious sweat!"
                }
            }
            else -> {
                when {
                    caloriesBurned < 50 -> "Keep it up, you're doing great!"
                    caloriesBurned < 100 -> "Amazing! You're getting stronger!"
                    else -> "Fantastic! You've burned a lot of calories!"
                }
            }
        }
    }

    private fun setupWorkoutDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            workoutCaloriesMap.keys.toList()
        )
        etWorkoutName.setAdapter(adapter)
    }
    private fun resetWorkout() {
        stopTimer()
        totalSeconds = 0
        caloriesBurned = 0.0

        textHours.text = "00"
        textMinutes.text = "00"
        textSeconds.text = "00"
        textCaloriesBurned.text = "0.00 kcal"

        Toast.makeText(requireContext(), "Workout reset", Toast.LENGTH_SHORT).show()
    }
    private fun startTimer() {
        stopTimer()
        totalSeconds = 0
        caloriesBurned = 0.0

        timer = Timer()
        timer?.scheduleAtFixedRate(0, 1000) {
            totalSeconds++
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60

            val caloriesPerKgPerHour = workoutCaloriesMap[selectedActivity] ?: 0.0
            val caloriesPerSecond = (caloriesPerKgPerHour * weightKg) / 3600.0
            caloriesBurned += caloriesPerSecond

            val suggestion = generateActivitySuggestion(selectedActivity, caloriesBurned)

            activity?.runOnUiThread {
                textHours.text = String.format("%02d", hours)
                textMinutes.text = String.format("%02d", minutes)
                textSeconds.text = String.format("%02d", seconds)
                textCaloriesBurned.text = "🔥 Burned: %.2f kcal".format(caloriesBurned)
                textSuggestion.text = suggestion

                Log.d("Activity2", selectedActivity)
            }
        }
    }


    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTimer()
    }
}
