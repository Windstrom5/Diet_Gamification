package com.example.diet_gamification.todolist

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.graphics.Color
import android.provider.MediaStore
import android.text.format.DateUtils
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.diet_gamification.R
import com.example.diet_gamification.room.AppDatabase
import com.example.diet_gamification.model.FoodItem
import com.example.diet_gamification.model.FoodRepository
import com.example.diet_gamification.model.FoodSuggestion
import com.example.diet_gamification.model.ToDoListViewModelFactory
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.lifecycleScope
import com.example.diet_gamification.model.AccountModel
import com.example.diet_gamification.room.XpHistoryDao
import com.example.diet_gamification.room.XpHistoryEntity
import com.example.diet_gamification.shop.ShopRepository.getCalorieBuff
import com.example.diet_gamification.utils.ApiService
import com.example.diet_gamification.utils.FoodClassifier
import com.example.diet_gamification.utils.NotificationUtils
import com.example.diet_gamification.utils.SharedPrefsManager
import com.example.diet_gamifikasi.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate

class ToDoListFragment : Fragment() {
    private lateinit var viewModel: ToDoListViewModel
    private lateinit var recyclerBreakfast: RecyclerView
    private lateinit var recyclerLunch: RecyclerView
    private lateinit var recyclerDinner: RecyclerView
    private lateinit var buttonAddBreakfast: Button
    private lateinit var buttonAddLunch: Button
    private lateinit var buttonAddDinner: Button
    private lateinit var filterButton: Button
    private lateinit var targetButton: Button
    private lateinit var saveCaloriesButton: Button
    private var currentSelectedDate: String? = null
    private lateinit var circularProgress: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var calorieSuggestion: TextView
    private lateinit var camerabutton: Button
    private var targetCalories: Int = 2000  // Default target
    private var currentCalories: Int = 0
    private lateinit var xpHistoryDao: XpHistoryDao
    private lateinit var sharedPrefsManager: SharedPrefsManager
    companion object {
        private const val CAMERA_REQUEST_CODE = 100
    }
    private var accountModel: AccountModel? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_todolist, container, false)
        xpHistoryDao = AppDatabase.getInstance(requireContext()).xpHistoryDao()
        // UI Components
        filterButton = view.findViewById(R.id.btn_filter_date)
        recyclerBreakfast = view.findViewById(R.id.recycler_breakfast)
        recyclerLunch = view.findViewById(R.id.recycler_lunch)
        recyclerDinner = view.findViewById(R.id.recycler_dinner)
        buttonAddBreakfast = view.findViewById(R.id.button_add_breakfast)
        buttonAddLunch = view.findViewById(R.id.button_add_lunch)
        buttonAddDinner = view.findViewById(R.id.button_add_dinner)
        targetButton = view.findViewById(R.id.button_set_target)
        // Set up RecyclerViews
        recyclerBreakfast.layoutManager = LinearLayoutManager(requireContext())
        recyclerLunch.layoutManager = LinearLayoutManager(requireContext())
        recyclerDinner.layoutManager = LinearLayoutManager(requireContext())
        circularProgress = view.findViewById(R.id.circular_calorie_progress)
        progressText = view.findViewById(R.id.progress_text)
        calorieSuggestion = view.findViewById(R.id.calorie_suggestion)
        camerabutton = view.findViewById(R.id.button_scan_food)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getAccountFromActivity()
        // Initialize ViewModel
        val dao = AppDatabase.getInstance(requireContext()).foodItemDao()
        val repository = FoodRepository(dao)
        val factory = ToDoListViewModelFactory(requireActivity().application, repository,accountModel)
        viewModel = ViewModelProvider(this, factory).get(ToDoListViewModel::class.java)
        checkCaloriesInPreviousWeek()
        // Adapters
        val breakfastAdapter = FoodAdapter(emptyList()) { onFoodClicked(it) }
        val lunchAdapter = FoodAdapter(emptyList()) { onFoodClicked(it) }
        val dinnerAdapter = FoodAdapter(emptyList()) { onFoodClicked(it) }
        recyclerBreakfast.adapter = breakfastAdapter
        recyclerLunch.adapter = lunchAdapter
        recyclerDinner.adapter = dinnerAdapter
        saveCaloriesButton = view.findViewById(R.id.button_save_calories)

        // Observe LiveData
        viewModel.breakfastFoods.observe(viewLifecycleOwner) { entities ->
            breakfastAdapter.updateList(entities.map { FoodItem(it.category, it.name, it.calories) })
        }
        viewModel.lunchFoods.observe(viewLifecycleOwner) { entities ->
            lunchAdapter.updateList(entities.map { FoodItem(it.category, it.name, it.calories) })
        }
        viewModel.dinnerFoods.observe(viewLifecycleOwner) { entities ->
            dinnerAdapter.updateList(entities.map { FoodItem(it.category, it.name, it.calories) })
        }

        // Load today's meals by default
        viewModel = ViewModelProvider(this, factory).get(ToDoListViewModel::class.java)

        // Observe selectedDate to keep track of the user's chosen date
        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            currentSelectedDate = date
            checkAndUpdateButtonVisibility()
        }

        // Filter button click listener
        filterButton.setOnClickListener {
            showDatePickerDialog()
        }
        targetButton.setOnClickListener {
            showSetCalorieTargetDialog()
        }
        // Add buttons
        buttonAddBreakfast.setOnClickListener { showAddDialog("Breakfast") }
        buttonAddLunch.setOnClickListener { showAddDialog("Lunch") }
        buttonAddDinner.setOnClickListener { showAddDialog("Dinner") }
        viewModel.breakfastFoods.observe(viewLifecycleOwner) { updateTotalCalories() }
        viewModel.lunchFoods.observe(viewLifecycleOwner) { updateTotalCalories() }
        viewModel.dinnerFoods.observe(viewLifecycleOwner) { updateTotalCalories() }
        sharedPrefsManager = SharedPrefsManager(requireContext())

        // Load saved calorie target from SharedPreferences
        val lastSetDate = sharedPrefsManager.getLastSetDate() // This is a Long value
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

        val lastSetDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(lastSetDate))

// Compare the dates as strings
        if (lastSetDateString != currentDate) {
            sharedPrefsManager.resetCalorieTarget()
            sharedPrefsManager.saveLastSetDate(currentDate)
        }

        targetCalories = sharedPrefsManager.getCalorieTarget()
        camerabutton.setOnClickListener {
            openCamera()
        }
    }

    private fun calculatexpForCalories(calories: Int): Int {
        // Calculate percentage of calories consumed
        val mainActivity = activity as? MainActivity
        val accountModel = mainActivity?.currentAccountModel
        val percentage = (calories.toDouble() / targetCalories) * 100

        // Define the base XP that gets added per calorie
        val xpPerCalorieBase = 1

        // Get the calorie-related buff from the inventory
        val weight = accountModel!!.berat.toDouble()
        val height = accountModel!!.tinggi.toDouble()
        val calorieBuff = getCalorieBuff(accountModel!!.inventory, weight, height)

        // Define how much XP gets added as they approach the target
        var xpMultiplier = 1.0

        // Adjust multiplier based on percentage, applying the buff
        when {
            percentage < 50 -> xpMultiplier = 1.0 // Low XP multiplier (initial phase)
            percentage in 50.0..79.9 -> xpMultiplier = 1.5 // Moderate XP multiplier
            percentage in 80.0..99.9 -> xpMultiplier = 2.0 // High XP multiplier
            percentage >= 100 -> xpMultiplier = 2.5 // Maximum XP multiplier when the goal is achieved
        }

        // Apply any available buffs from the inventory
        xpMultiplier *= calorieBuff.multiplier
        val additivexp = calorieBuff.additivexp

        // Calculate XP based on the multiplier, additive XP, and calories consumed
        val xpEarned = (calories * xpPerCalorieBase * xpMultiplier).toInt() + additivexp

        return xpEarned
    }

    // Override the onActivityResult method to capture the image and classify it
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            // Get the captured image as a bitmap
            val imageBitmap = data?.extras?.get("data") as Bitmap

            // Use FoodClassifier to classify the image and get food name and calories
            val classifier = FoodClassifier(requireContext()) // Ensure you have access to FoodClassifier
            val results = classifier.classify(imageBitmap)

            // Handle the classification results
            results?.let { classifications ->
                if (classifications.isNotEmpty()) {
                    // Assuming you are interested in the top classification
                    val topResult = classifications[0].getCategories().first()

                    val foodName = topResult.getLabel()  // Get food name (label)
                    val foodCalories = (topResult.getScore() * 1000).toInt()  // Get confidence as an estimate for calories

                    // Show the "Add to meal" dialog
                    addScannedFood(foodName, foodCalories)
                }
            }
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }
    private fun checkAndUpdateButtonVisibility() {
        // Check if the selected date is today
        val selectedDate = currentSelectedDate?.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)
        }
        val today = Calendar.getInstance().time

        if (selectedDate != null && DateUtils.isToday(selectedDate.time)) {
            // If selected date is today, show the buttons
            buttonAddBreakfast.visibility = View.VISIBLE
            buttonAddLunch.visibility = View.VISIBLE
            buttonAddDinner.visibility = View.VISIBLE
        } else {
            // If selected date is not today, hide the buttons
            buttonAddBreakfast.visibility = View.GONE
            buttonAddLunch.visibility = View.GONE
            buttonAddDinner.visibility = View.GONE
        }
    }
    private fun checkCaloriesInPreviousWeek() {
        lifecycleScope.launch {
            val calendar = Calendar.getInstance()

            // Calculate the start date (Monday) of the previous week
            calendar.add(Calendar.WEEK_OF_YEAR, -1)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            // Calculate the end date (Sunday) of the previous week
            calendar.add(Calendar.DAY_OF_WEEK, 6)
            val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            // Fetch total calories for the previous week
            val totalCalories = withContext(Dispatchers.IO) {
                viewModel.checkCaloriesInPreviousWeek()
            }

            // Show or hide the button based on total calories
            if (totalCalories != null && totalCalories > 0) {
                saveCaloriesButton.visibility = View.VISIBLE
            } else {
                saveCaloriesButton.visibility = View.GONE
            }
        }
    }

    private fun updateTotalCalories() {
        val allFoods = viewModel.breakfastFoods.value.orEmpty() + viewModel.lunchFoods.value.orEmpty() + viewModel.dinnerFoods.value.orEmpty()
        currentCalories = allFoods.sumOf { it.calories }
        circularProgress.max = targetCalories
        circularProgress.progress = currentCalories
        progressText.text = "$currentCalories / $targetCalories cal"

        val percentage = (currentCalories.toDouble() / targetCalories) * 100
        calorieSuggestion.text = when {
            percentage < 50 -> "You’ve barely started eating today. How about a healthy meal?"
            percentage in 50.0..79.9 -> "You're halfway there. A light snack or small meal could help reach your goal."
            percentage in 80.0..99.9 -> "Almost there! A small bite or drink might just push you to your target."
            percentage >= 100 -> "Great job! You've hit your calorie target for today."
            else -> "Tracking in progress..."
        }

        // Automatically update Room DB when target is met
        if (currentCalories >= targetCalories) {
            val xpEarned = calculatexpForCalories(currentCalories)
            updatexpInRoomDb(xpEarned)
            updatexpToLaravel(xpEarned)
        }
    }
    private fun updatexpInRoomDb(xp: Int) {
        // Launch a coroutine in the lifecycleScope of the Fragment or Activity
        lifecycleScope.launch {
            val today = LocalDate.now().toString()
            val accountId = accountModel?.id ?: return@launch

            val xpHistory = XpHistoryEntity(
                accountId = accountId,
                xpGained = xp,
                category = "Calories",
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
            .baseUrl("https://selected-jaguar-presently.ngrok-free.app") // Replace with actual base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)

        val request = mapOf(
            "account_id" to account.id,
            "xpGained" to xp,
            "category" to "Calories",
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
    private fun showDatePickerDialog() {
        val now = Calendar.getInstance()
        val startOfWeek = now.clone() as Calendar
        startOfWeek.set(Calendar.DAY_OF_WEEK, startOfWeek.firstDayOfWeek)

        val endOfWeek = now.clone() as Calendar
        endOfWeek.set(Calendar.DAY_OF_WEEK, startOfWeek.firstDayOfWeek + 6)

        // Temp variable for selection
        var tempSelectedDate: String? = null

        val initialDate = currentSelectedDate?.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)
        } ?: now.time

        val cal = Calendar.getInstance().apply { time = initialDate }

        val dpd = DatePickerDialog.newInstance(
            { _, year, monthOfYear, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, monthOfYear, dayOfMonth)
                }
                val internalFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                tempSelectedDate = internalFormat.format(selectedCalendar.time)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )

        // Ensure today is included and selectable
        dpd.minDate = startOfWeek
        dpd.maxDate = endOfWeek
        dpd.setAccentColor(resources.getColor(android.R.color.black))
        // Enable all dates in range (fixes today being gray sometimes)
        val days = mutableListOf<Calendar>()
        val dayIterator = startOfWeek.clone() as Calendar
        while (!dayIterator.after(endOfWeek)) {
            days.add(dayIterator.clone() as Calendar)
            dayIterator.add(Calendar.DATE, 1)
        }
        dpd.disabledDays = arrayOf() // no disabled days
        dpd.selectableDays = days.toTypedArray() // force all visible days as selectable

        dpd.setOnCancelListener {
            tempSelectedDate = null
        }

        dpd.setOnDismissListener {
            tempSelectedDate?.let {
                currentSelectedDate = it
                Log.d("AccointID",accountModel.toString())
                viewModel.loadMealsForDate(it)
            }
        }

        dpd.show(childFragmentManager, "DatePickerDialog")
    }
    private fun scheduleDailyCalorieReminder() {
        scheduleFixedMealReminders()
//        scheduleRandomReminders()
    }
    private fun scheduleFixedMealReminders() {
        NotificationUtils.rescheduleAllReminders(requireContext())
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
//    private fun scheduleRandomReminders() {
//        val random = java.util.Random()
//        val usedHours = mutableListOf(8, 12, 19) // Don't overlap with meals
//
//        val reminderTypes = listOf("Snack", "Hydration", "CheckCalories") // Random types
//
//        for (reminderType in reminderTypes) {
//            var randomHour: Int
//            val random = Random
//
//            do {
//                randomHour = random.nextInt(7, 22) // Between 7 AM and 10 PM
//            } while (usedHours.any { Math.abs(it - randomHour) < 2 }) // Minimum 2 hours apart
//
//            usedHours.add(randomHour)
//
//            val randomMinute = random.nextInt(0, 60)
//
//            NotificationUtils.setReminder(requireContext(), randomHour, randomMinute, reminderType)
//        }
//    }

    private fun showSetCalorieTargetDialog() {
        // Create a LinearLayout to hold both TextView and EditText
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 50, 50, 50)

        // Create the TextView programmatically to show suggestions
        val suggestionText = TextView(requireContext())
        suggestionText.text = "Suggested intake:\n- Male: ~2500 kcal/day\n- Female: ~2000 kcal/day\n\nEnter your target calorie goal"
        suggestionText.setTextColor(Color.BLACK)
        suggestionText.setPadding(0, 0, 0, 20)

        // Create the EditText for user to input their calorie goal
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_NUMBER // Ensuring the input is numeric
        input.hint = "Enter your target calorie goal"

        // Add the TextView and EditText to the layout
        layout.addView(suggestionText)
        layout.addView(input)

        // Set up the AlertDialog
        AlertDialog.Builder(requireContext())
            .setTitle("Set Your Calorie Goal")
            .setView(layout) // Use the custom layout with TextView and EditText
            .setPositiveButton("Set") { dialog, _ ->
                val inputText = input.text.toString().trim()
                if (inputText.isNotEmpty()) {
                    val goal = inputText.toIntOrNull()
                    if (goal != null) {
                        viewModel.calorieGoal.value = goal
                        Toast.makeText(requireContext(), "Goal set to $goal kcal", Toast.LENGTH_SHORT).show()
                        targetCalories = goal
                        updateTotalCalories()
                        sharedPrefsManager.saveCalorieTarget(goal)
                        // Schedule random reminder for calorie intake
                        scheduleDailyCalorieReminder()
                    } else {
                        Toast.makeText(requireContext(), "Invalid number", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    


    private fun onFoodClicked(item: FoodItem) {
        Toast.makeText(requireContext(), "${item.name} has ${item.calories} cal", Toast.LENGTH_SHORT).show()
    }

    private fun loadFoodSuggestionsFromCSV(context: Context): List<FoodSuggestion> {
        val suggestions = mutableListOf<FoodSuggestion>()
        context.assets.open("calories.csv").bufferedReader().useLines { lines ->
            lines.drop(1).forEach { line ->
                val parts = line.split(",")
                if (parts.size >= 5) {
                    suggestions.add(
                        FoodSuggestion(
                            parts[0].trim(),
                            parts[1].trim(),
                            parts[3].removeSuffix(" cal").toIntOrNull() ?: 0,
                            parts[4].removeSuffix(" kJ").toIntOrNull() ?: 0
                        )
                    )
                }
            }
        }
        return suggestions
    }

    private fun showAddDialog(category: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_food, null)
        val foodNameInput = dialogView.findViewById<AutoCompleteTextView>(R.id.etFoodName)
        val caloriesInput = dialogView.findViewById<EditText>(R.id.etCalories)

        val suggestions = loadFoodSuggestionsFromCSV(requireContext())
        val names = suggestions.map { it.name }
        foodNameInput.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names))
        foodNameInput.threshold = 1

        foodNameInput.setOnItemClickListener { _, _, _, _ ->
            suggestions.find { it.name == foodNameInput.text.toString() }
                ?.let { caloriesInput.setText(it.caloriesPer100g.toString()) }
        }

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        dialogView.findViewById<Button>(R.id.btnSave).setOnClickListener {
            val name = foodNameInput.text.toString().trim()
            val cal = caloriesInput.text.toString().trim().toIntOrNull() ?: 0
            if (name.isNotEmpty()) {
                viewModel.addFoodToMeal(category, FoodItem(category, name, cal))
                dialog.dismiss()
            } else Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
        }
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
    fun addScannedFood(name: String, calories: Int) {
        val options = arrayOf("Breakfast", "Lunch", "Dinner")

        AlertDialog.Builder(requireContext())
            .setTitle("Add to which meal?")
            .setItems(options) { _, which ->
                val selectedCategory = options[which]
                viewModel.addFoodToMeal(
                    selectedCategory,
                    FoodItem(selectedCategory, name, calories)
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}