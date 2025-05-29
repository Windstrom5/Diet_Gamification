package com.example.diet_gamification.todolist

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.diet_gamification.model.AccountModel
import com.example.diet_gamification.model.FoodItem
import com.example.diet_gamification.room.FoodItemEntity
import com.example.diet_gamification.model.FoodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ToDoListViewModel(
    private val repository: FoodRepository,
    application: Application,
    private val currentAccountModel: AccountModel?
) : AndroidViewModel(application) {

    private val _selectedDate = MutableLiveData<String>().apply {
        value = getTodayDate()
    }
    val selectedDate: LiveData<String> = _selectedDate

    private val _breakfastFoods = MutableLiveData<List<FoodItemEntity>>()
    val breakfastFoods: LiveData<List<FoodItemEntity>> = _breakfastFoods

    private val _lunchFoods = MutableLiveData<List<FoodItemEntity>>()
    val lunchFoods: LiveData<List<FoodItemEntity>> = _lunchFoods

    private val _dinnerFoods = MutableLiveData<List<FoodItemEntity>>()
    val dinnerFoods: LiveData<List<FoodItemEntity>> = _dinnerFoods

    val totalCalories: LiveData<Int> = MutableLiveData(0)
    val calorieGoal = MutableLiveData(2000)

    init {
        if (currentAccountModel == null) {
            Log.w("ToDoListViewModel", "Warning: AccountModel is null. Some data may not load.")
        }
        loadMealsForDate(getTodayDate())
    }

    fun addFoodToMeal(category: String, foodItem: FoodItem) {
        val date = _selectedDate.value ?: getTodayDate()
        val foodItemEntity = FoodItemEntity(
            id_account = currentAccountModel?.id,
            category = foodItem.category,
            name = foodItem.name,
            calories = foodItem.calories,
            date = date,
        )
        viewModelScope.launch {
            repository.insert(foodItemEntity)
            loadMealsForDate(date)
        }
    }

    fun loadMealsForDate(date: String) {
        viewModelScope.launch {
            val breakfast: List<FoodItemEntity>
            val lunch: List<FoodItemEntity>
            val dinner: List<FoodItemEntity>

            withContext(Dispatchers.IO) {
                if (currentAccountModel == null) {
                    Log.d("AccountID", "null")
                    breakfast = repository.getFoodItemsByCategoryAndDate("Breakfast", date)
                    lunch = repository.getFoodItemsByCategoryAndDate("Lunch", date)
                    dinner = repository.getFoodItemsByCategoryAndDate("Dinner", date)
                } else {
                    val accountId = currentAccountModel.id
                    Log.d("AccountID", accountId.toString())
                    breakfast = repository.getFoodItemsByCategoryAndDateAccount("Breakfast", date, accountId)
                    lunch = repository.getFoodItemsByCategoryAndDateAccount("Lunch", date, accountId)
                    dinner = repository.getFoodItemsByCategoryAndDateAccount("Dinner", date, accountId)
                }
            }

            _breakfastFoods.value = breakfast
            _lunchFoods.value = lunch
            _dinnerFoods.value = dinner
            _selectedDate.value = date
        }
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    suspend fun checkCaloriesInPreviousWeek(): Int? {
        val calendar = Calendar.getInstance()

        // End of last week (Sunday)
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val formattedEndDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        // Start of last week (Monday)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val formattedStartDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        return withContext(Dispatchers.IO) {
            repository.getTotalCaloriesForWeek(formattedStartDate, formattedEndDate)
        }
    }
}
