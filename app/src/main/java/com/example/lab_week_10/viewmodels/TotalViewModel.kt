package com.example.lab_week_10.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TotalViewModel : ViewModel() {

    private val _date = MutableLiveData<String>()
    val date: LiveData<String> = _date


    private val _total = MutableLiveData<Int>(0)
    val total: LiveData<Int>
        get() = _total

    fun incrementTotal(): Int {
        val new = (_total.value ?: 0) + 1
        _total.value = new
        return new
    }

    fun setTotal(newTotal: Int, newDate: String = "") {
        _total.postValue(newTotal)
        if (newDate.isNotEmpty()) _date.postValue(newDate)
    }

    fun setDate(newDate: String) {
        _date.postValue(newDate)
    }
}
