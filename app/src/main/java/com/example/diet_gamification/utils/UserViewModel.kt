package com.example.diet_gamification.utils

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.diet_gamification.model.AccountModel

class UserViewModel : ViewModel() {
    private val _account = MutableLiveData<AccountModel?>()
    val account: LiveData<AccountModel?> = _account

    fun setAccount(account: AccountModel?) {
        _account.value = account
    }
}
