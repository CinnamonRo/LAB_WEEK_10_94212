package com.example.lab_week_10.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "total")
data class Total(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: Long = 1,

    @ColumnInfo(name = "total")
    val total: Int = 0
)
