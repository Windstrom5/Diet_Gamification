package com.example.diet_gamification.model

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.diet_gamification.todolist.ToDoListViewModel

class ToDoListViewModelFactory(
    private val application: Application,
    private val repository: FoodRepository,
    private val currentAccountModel: AccountModel?
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ToDoListViewModel::class.java)) {
            return ToDoListViewModel(repository, application, currentAccountModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
