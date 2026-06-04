package com.example.digitalpass.database

import androidx.room.Entity

@Entity(tableName = "batches", primaryKeys = ["batchName", "campus"])
data class BatchEntity(
    val batchName: String,
    val type: String,
    val campus: String
)
