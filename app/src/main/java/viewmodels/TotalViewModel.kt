package com.example.lab_week_10.viewmodels

import androidx.lifecycle.ViewModel

class TotalViewModel : ViewModel() {
    var total: Int = 0

    fun incrementTotal(): Int {
        total++
        return total
    }
}
