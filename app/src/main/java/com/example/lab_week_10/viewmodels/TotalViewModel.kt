package com.example.lab_week_10.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TotalViewModel : ViewModel() {
    private val _total = MutableLiveData<Int>(0)
    val total: LiveData<Int>
        get() = _total

    fun incrementTotal(): Int {
        val new = (_total.value ?: 0) + 1
        _total.value = new
        return new
    }
}
